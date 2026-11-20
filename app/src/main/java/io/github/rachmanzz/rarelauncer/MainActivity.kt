package io.github.rachmanzz.rarelauncer

import android.Manifest
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import io.github.rachmanzz.rarelauncer.service.LockScreenService
import io.github.rachmanzz.rarelauncer.ui.LauncherScreen
import io.github.rachmanzz.rarelauncer.ui.theme.RareLauncerTheme
import io.github.rachmanzz.rarelauncer.ui.viewmodel.LauncherViewModel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : FragmentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startLockScreenService()
        setContent {
            RareLauncerTheme {
                LauncherScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                    onRequestBiometricAuth = { title, onSuccess ->
                        showBiometricPrompt(title, onSuccess)
                    },
                    onPickCustomWallpaper = { launchPhotoPicker() },
                    onRequestContactsPermission = { requestContactsPermission() },
                    hasUsageStatsPermission = { hasUsageStatsPermission() },
                    onRequestUsageStatsPermission = { openUsageAccessSettings() },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.resumeClock()
    }

    override fun onStop() {
        super.onStop()
        viewModel.pauseClock()
    }

    private fun startLockScreenService() {
        runCatching {
            ContextCompat.startForegroundService(this, Intent(this, LockScreenService::class.java))
        }
        val missingPermissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            missingPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val prefs = getSharedPreferences("rare_launcher_prefs", MODE_PRIVATE)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED &&
            !prefs.getBoolean("contacts_prompted", false)
        ) {
            prefs.edit().putBoolean("contacts_prompted", true).apply()
            missingPermissions.add(Manifest.permission.READ_CONTACTS)
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                NOTIFICATION_PERMISSION_REQUEST,
            )
        } else {
            requestFullScreenIntentAccessOnce()
        }
    }

    private fun requestContactsPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_CALL_LOG,
            ),
            CONTACTS_PERMISSION_REQUEST,
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST -> {
                viewModel.loadContacts()
                runCatching {
                    ContextCompat.startForegroundService(this, Intent(this, LockScreenService::class.java))
                }
                requestFullScreenIntentAccessOnce()
            }
            CONTACTS_PERMISSION_REQUEST -> {
                viewModel.loadContacts()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun launchPhotoPicker() {
        val intent = ActivityResultContracts.PickVisualMedia().createIntent(
            this,
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
        runCatching {
            startActivityForResult(intent, PHOTO_PICKER_REQUEST)
        }.onFailure {
            Toast.makeText(this, "Unable to open photo picker", Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PHOTO_PICKER_REQUEST && resultCode == RESULT_OK) {
            val uri = ActivityResultContracts.PickVisualMedia().parseResult(resultCode, data)
                ?: data?.data
                ?: return
            persistAndApplyWallpaper(uri)
        }
    }

    private fun persistAndApplyWallpaper(uri: Uri) {
        lifecycleScope.launch {
            val savedUri = withContext(Dispatchers.IO) {
                runCatching {
                    filesDir.listFiles { file ->
                        file.name.startsWith("custom_wallpaper_")
                    }?.forEach { it.delete() }
                    val target = File(filesDir, "custom_wallpaper_${System.currentTimeMillis()}.img")
                    contentResolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                    if (target.exists() && target.length() > 0L) {
                        Uri.fromFile(target).toString()
                    } else {
                        null
                    }
                }.getOrNull()
            } ?: uri.toString()
            viewModel.setCustomWallpaper(savedUri)
        }
    }

    private companion object {
        const val NOTIFICATION_PERMISSION_REQUEST = 1001
        const val PHOTO_PICKER_REQUEST = 1002
        const val CONTACTS_PERMISSION_REQUEST = 1003
    }

    /** Ensures the lock screen can be displayed above the keyguard on Android 14/15+. */
    private fun requestFullScreenIntentAccessOnce() {
        val prefs = getSharedPreferences("rare_launcher_prefs", MODE_PRIVATE)
        if (!Settings.canDrawOverlays(this)) {
            if (!prefs.getBoolean("overlay_prompted", false)) {
                prefs.edit().putBoolean("overlay_prompted", true).apply()
                Toast.makeText(
                    this,
                    "Allow \"Display over other apps\" for Rare Launcer lock screen",
                    Toast.LENGTH_LONG,
                ).show()
                runCatching {
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName"),
                        ),
                    )
                }
            }
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        if (canUseFullScreenIntentCompat()) return
        if (prefs.getBoolean("fsi_prompted", false)) return
        prefs.edit().putBoolean("fsi_prompted", true).apply()
        runCatching {
            startActivity(
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName")),
            )
        }
    }

    private fun canUseFullScreenIntentCompat(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        val manager = getSystemService(NotificationManager::class.java)
        if (!manager.canUseFullScreenIntent()) return false
        val appOps = getSystemService(AppOpsManager::class.java)
        val op = AppOpsManager.permissionToOp(Manifest.permission.USE_FULL_SCREEN_INTENT) ?: return true
        @Suppress("DEPRECATION")
        val mode = appOps.unsafeCheckOpNoThrow(op, Process.myUid(), packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun showBiometricPrompt(title: String = "Unlock Hidden Apps", onSuccess: () -> Unit) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val canAuth = BiometricManager.from(this).canAuthenticate(authenticators)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(
                this,
                "Set up fingerprint or screen lock in device Settings first",
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        Toast.makeText(this@MainActivity, errString, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle("Confirm your fingerprint or screen lock")
            .setAllowedAuthenticators(authenticators)
            .build()

        biometricPrompt.authenticate(promptInfo)
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

    private fun openUsageAccessSettings() {
        val withPackage = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:$packageName"))
        val started = runCatching { startActivity(withPackage) }.isSuccess
        if (!started) {
            runCatching { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
        }
    }

    override fun onResume() {
        super.onResume()
        LockScreenService.clearTempUnlock()
        viewModel.onLauncherResumed()
        viewModel.loadInstalledAppsAndDirs()
        viewModel.loadContacts()
    }

    override fun onPause() {
        super.onPause()
        // Automatically re-lock vault when leaving the launcher
        viewModel.lockVault()
    }
}
