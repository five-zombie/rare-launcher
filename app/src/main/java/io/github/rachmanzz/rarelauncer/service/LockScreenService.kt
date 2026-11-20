package io.github.rachmanzz.rarelauncer.service

import android.Manifest
import android.app.AppOpsManager
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.github.rachmanzz.rarelauncer.AppLockActivity
import io.github.rachmanzz.rarelauncer.LockScreenActivity
import io.github.rachmanzz.rarelauncer.R

/**
 * Lightweight foreground service that:
 * 1. Displays [LockScreenActivity] whenever the screen turns on.
 * 2. Monitors foreground app switches via [UsageStatsManager] so locked apps
 *    opened from notifications or Recent Apps still require biometric/PIN verification.
 */
class LockScreenService : Service() {

    private var workerThread: HandlerThread? = null
    private var workerHandler: Handler? = null
    private var isPolling = false
    private var lastEventTimestamp = 0L
    private var lastPromptPackage: String? = null
    private var lastPromptAt = 0L

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == "locked_apps_set") {
            val lockedApps = prefs.getStringSet("locked_apps_set", emptySet()) ?: emptySet()
            val powerManager = getSystemService(PowerManager::class.java)
            if (lockedApps.isNotEmpty() && powerManager?.isInteractive == true) {
                startForegroundPolling()
            } else {
                stopForegroundPolling()
            }
        }
    }

    private val pollRunnable = object : Runnable {
        override fun run() {
            checkForegroundApp()
            if (isPolling) {
                val prefs = getSharedPreferences("rare_launcher_prefs", MODE_PRIVATE)
                val lockedApps = prefs.getStringSet("locked_apps_set", emptySet()) ?: emptySet()
                if (lockedApps.isEmpty()) {
                    stopForegroundPolling()
                } else {
                    workerHandler?.postDelayed(this, POLL_INTERVAL_MS)
                }
            }
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    showLockScreen()
                    lastEventTimestamp = System.currentTimeMillis() - 1000L
                    startForegroundPolling()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    clearTempUnlock()
                    lastPromptPackage = null
                    stopForegroundPolling()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val thread = HandlerThread("LockServiceWorker").apply { start() }
        workerThread = thread
        workerHandler = Handler(thread.looper)

        val prefs = getSharedPreferences("rare_launcher_prefs", MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(prefListener)

        createChannels()
        val serviceNotification = buildServiceNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                SERVICE_NOTIFICATION_ID,
                serviceNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(SERVICE_NOTIFICATION_ID, serviceNotification)
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        lastEventTimestamp = System.currentTimeMillis()
        val powerManager = getSystemService(PowerManager::class.java)
        if (powerManager.isInteractive) {
            startForegroundPolling()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        stopForegroundPolling()
        val prefs = getSharedPreferences("rare_launcher_prefs", MODE_PRIVATE)
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        workerThread?.quitSafely()
        workerThread = null
        workerHandler = null
        runCatching { unregisterReceiver(screenReceiver) }
        super.onDestroy()
    }

    private fun startForegroundPolling() {
        val prefs = getSharedPreferences("rare_launcher_prefs", MODE_PRIVATE)
        val lockedApps = prefs.getStringSet("locked_apps_set", emptySet()) ?: emptySet()
        if (lockedApps.isEmpty()) {
            stopForegroundPolling()
            return
        }
        if (isPolling) return
        isPolling = true
        workerHandler?.post(pollRunnable)
    }

    private fun stopForegroundPolling() {
        isPolling = false
        workerHandler?.removeCallbacks(pollRunnable)
    }

    private fun checkForegroundApp() {
        val prefs = getSharedPreferences("rare_launcher_prefs", MODE_PRIVATE)
        val lockedApps = prefs.getStringSet("locked_apps_set", emptySet()) ?: emptySet()
        if (lockedApps.isEmpty()) return
        if (!hasUsageStatsPermission() || !Settings.canDrawOverlays(this)) return
        if (AppLockActivity.activePackage != null) return

        val keyguardManager = getSystemService(KeyguardManager::class.java)
        if (keyguardManager.isKeyguardLocked) return

        val now = System.currentTimeMillis()
        val usageStatsManager = getSystemService(UsageStatsManager::class.java) ?: return
        val events = runCatching {
            usageStatsManager.queryEvents(lastEventTimestamp, now)
        }.getOrNull() ?: return

        val resumedEventType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            UsageEvents.Event.ACTIVITY_RESUMED
        } else {
            @Suppress("DEPRECATION")
            UsageEvents.Event.MOVE_TO_FOREGROUND
        }

        val event = UsageEvents.Event()
        var latestResumedPkg: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.timeStamp > lastEventTimestamp) {
                lastEventTimestamp = event.timeStamp
            }
            if (event.eventType == resumedEventType && !event.packageName.isNullOrEmpty()) {
                latestResumedPkg = event.packageName
            }
        }

        val pkg = latestResumedPkg ?: return
        if (pkg == packageName) return

        if (pkg in lockedApps) {
            if (pkg == tempUnlockedPackage) return
            if (pkg == lastPromptPackage && now - lastPromptAt < 1200L) return
            lastPromptPackage = pkg
            lastPromptAt = now
            val lockIntent = Intent(this, AppLockActivity::class.java)
                .putExtra(AppLockActivity.EXTRA_PACKAGE_NAME, pkg)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP,
                )
            runCatching { startActivity(lockIntent) }
        } else if (pkg != tempUnlockedPackage && packageManager.getLaunchIntentForPackage(pkg) != null) {
            // Clear session unlock once user switches to another launchable app
            clearTempUnlock()
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(AppOpsManager::class.java) ?: return false
        @Suppress("DEPRECATION")
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun showLockScreen() {
        val launchIntent = Intent(this, LockScreenActivity::class.java)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP,
            )

        // Direct start works reliably when "Display over other apps" is granted,
        // even under Do Not Disturb or before the keyguard timeout locks.
        if (Settings.canDrawOverlays(this)) {
            val started = runCatching { startActivity(launchIntent) }.isSuccess
            if (started) return
        }

        // Fallback: full-screen-intent notification (requires POST_NOTIFICATIONS + USE_FULL_SCREEN_INTENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_LOCK)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("Rare Launcer")
            .setContentText("Lock screen")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setSilent(true)
            .setAutoCancel(true)
            .setTimeoutAfter(3000)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .build()
        getSystemService(NotificationManager::class.java).notify(LOCK_NOTIFICATION_ID, notification)
    }

    private fun buildServiceNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("Rare Launcer")
            .setContentText("Lock screen is active")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()

    private fun createChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SERVICE,
                "Lock screen service",
                NotificationManager.IMPORTANCE_MIN,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_LOCK,
                "Lock screen",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                setSound(null, null)
                enableVibration(false)
            },
        )
    }

    companion object {
        private const val CHANNEL_SERVICE = "lock_service"
        private const val CHANNEL_LOCK = "lock_screen"
        private const val SERVICE_NOTIFICATION_ID = 1001
        private const val LOCK_NOTIFICATION_ID = 1002
        private const val POLL_INTERVAL_MS = 1200L

        @Volatile
        private var tempUnlockedPackage: String? = null

        fun markTempUnlocked(packageName: String) {
            tempUnlockedPackage = packageName
        }

        fun clearTempUnlock() {
            tempUnlockedPackage = null
        }
    }
}
