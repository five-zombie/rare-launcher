package io.github.rachmanzz.rarelauncer

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import io.github.rachmanzz.rarelauncer.service.LockScreenService
import io.github.rachmanzz.rarelauncer.ui.theme.AetherGlassBorder
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnPrimary
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnSurface
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnSurfaceVariant
import io.github.rachmanzz.rarelauncer.ui.theme.AetherPrimary
import io.github.rachmanzz.rarelauncer.ui.theme.RareLauncerTheme

/**
 * Shown above a locked app when it is launched outside the launcher
 * (e.g., from a notification or Recent Apps).
 */
class AppLockActivity : FragmentActivity() {

    private var targetPackage: String = ""
    private var appLabel: String = ""
    private var promptShown = false
    private var promptInFlight = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (!loadTargetFromIntent(intent)) {
            finish()
            return
        }

        setContent {
            RareLauncerTheme {
                BackHandler(enabled = true) {
                    cancelAndGoHome()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xF5090C12))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF141822))
                            .border(1.dp, AetherGlassBorder, RoundedCornerShape(24.dp))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0x26FFD54F))
                                .border(1.dp, Color(0x66FFD54F), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(30.dp),
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = appLabel,
                            color = AetherOnSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Verifikasi sidik jari atau PIN untuk membuka aplikasi ini",
                            color = AetherOnSurfaceVariant,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AetherPrimary)
                                    .clickable { showBiometricPrompt() }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Verifikasi Sekarang",
                                    color = AetherOnPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF1D2230))
                                    .clickable { cancelAndGoHome() }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Kembali ke Home",
                                    color = AetherOnSurfaceVariant,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (loadTargetFromIntent(intent)) {
            promptShown = false
            promptInFlight = false
            showBiometricPrompt()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !promptShown && targetPackage.isNotEmpty()) {
            promptShown = true
            showBiometricPrompt()
        }
    }

    private fun loadTargetFromIntent(intent: Intent?): Boolean {
        val pkg = intent?.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        if (pkg.isEmpty()) return false
        targetPackage = pkg
        activePackage = pkg
        appLabel = runCatching {
            val info = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(pkg)
        return true
    }

    private fun showBiometricPrompt() {
        if (promptInFlight || targetPackage.isEmpty()) return
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (BiometricManager.from(this).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(
                this,
                "Set up fingerprint or screen lock in device Settings first",
                Toast.LENGTH_SHORT,
            ).show()
            cancelAndGoHome()
            return
        }

        promptInFlight = true
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    promptInFlight = false
                    LockScreenService.markTempUnlocked(targetPackage)
                    activePackage = null
                    finish()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    promptInFlight = false
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    ) {
                        cancelAndGoHome()
                    }
                }
            },
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Open $appLabel")
            .setSubtitle("Confirm your fingerprint or screen lock")
            .setAllowedAuthenticators(authenticators)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun cancelAndGoHome() {
        activePackage = null
        LockScreenService.clearTempUnlock()
        runCatching {
            startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
        finish()
    }

    override fun onDestroy() {
        if (activePackage == targetPackage) {
            activePackage = null
        }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"

        @Volatile
        var activePackage: String? = null
    }
}
