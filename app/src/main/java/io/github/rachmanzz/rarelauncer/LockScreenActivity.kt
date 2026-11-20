package io.github.rachmanzz.rarelauncer

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import io.github.rachmanzz.rarelauncer.data.repository.LauncherRepository
import io.github.rachmanzz.rarelauncer.ui.LockScreen
import io.github.rachmanzz.rarelauncer.ui.theme.RareLauncerTheme
import io.github.rachmanzz.rarelauncer.ui.viewmodel.ClockUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Custom lock screen shown ON TOP of the system keyguard.
 * It does not replace the system PIN / pattern / fingerprint: swiping up asks
 * the system to dismiss the keyguard, so the real credential is still required.
 */
class LockScreenActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        enableEdgeToEdge()

        val wallpaperConfig = LauncherRepository(applicationContext).getWallpaperConfig()

        setContent {
            RareLauncerTheme {
                // The lock screen can only be left through the unlock gesture
                BackHandler(enabled = true) {}

                val context = LocalContext.current

                // --- Live clock ---
                var clockState by remember { mutableStateOf(ClockUiState()) }
                LaunchedEffect(Unit) {
                    val hourFormat = SimpleDateFormat("HH", Locale.getDefault())
                    val minFormat = SimpleDateFormat("mm", Locale.getDefault())
                    val secFormat = SimpleDateFormat("ss", Locale.getDefault())
                    val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
                    while (true) {
                        val now = Date()
                        clockState = ClockUiState(
                            hours = hourFormat.format(now),
                            minutes = minFormat.format(now),
                            seconds = secFormat.format(now),
                            dateText = dateFormat.format(now),
                        )
                        delay(1000)
                    }
                }

                // --- Battery ---
                var batteryLevel by remember { mutableIntStateOf(0) }
                var isCharging by remember { mutableStateOf(false) }
                DisposableEffect(Unit) {
                    val update: (Intent?) -> Unit = { intent ->
                        if (intent != null) {
                            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                            batteryLevel = if (scale > 0) (level * 100) / scale else 0
                            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                status == BatteryManager.BATTERY_STATUS_FULL
                        }
                    }
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent?) = update(intent)
                    }
                    val sticky = ContextCompat.registerReceiver(
                        context,
                        receiver,
                        IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                        ContextCompat.RECEIVER_NOT_EXPORTED,
                    )
                    update(sticky)
                    onDispose { context.unregisterReceiver(receiver) }
                }

                // --- Flashlight (no permission needed for setTorchMode) ---
                val cameraManager = remember { context.getSystemService(CameraManager::class.java) }
                val torchCameraId = remember {
                    runCatching {
                        cameraManager.cameraIdList.firstOrNull {
                            cameraManager.getCameraCharacteristics(it)
                                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                        }
                    }.getOrNull()
                }
                var isTorchOn by remember { mutableStateOf(false) }
                DisposableEffect(Unit) {
                    val callback = object : CameraManager.TorchCallback() {
                        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                            if (cameraId == torchCameraId) isTorchOn = enabled
                        }
                    }
                    cameraManager.registerTorchCallback(callback, null)
                    onDispose { cameraManager.unregisterTorchCallback(callback) }
                }

                LockScreen(
                    clockState = clockState,
                    batteryLevel = batteryLevel,
                    isCharging = isCharging,
                    wallpaperConfig = wallpaperConfig,
                    isTorchOn = isTorchOn,
                    onTorchClick = {
                        val id = torchCameraId
                        if (id == null) {
                            Toast.makeText(context, "No flashlight available", Toast.LENGTH_SHORT).show()
                        } else {
                            try {
                                cameraManager.setTorchMode(id, !isTorchOn)
                            } catch (e: CameraAccessException) {
                                Toast.makeText(context, "Flashlight unavailable", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onCameraClick = { openCamera() },
                    onUnlock = { unlock() },
                )
            }
        }
    }

    /** Asks the system to dismiss the keyguard (PIN / pattern / fingerprint still enforced). */
    private fun unlock() {
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        if (keyguardManager.isKeyguardLocked) {
            keyguardManager.requestDismissKeyguard(
                this,
                object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        finish()
                    }
                },
            )
        } else {
            finish()
        }
    }

    private fun openCamera() {
        val secure = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(secure)
        } catch (e: Exception) {
            try {
                startActivity(
                    Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } catch (e2: Exception) {
                Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
