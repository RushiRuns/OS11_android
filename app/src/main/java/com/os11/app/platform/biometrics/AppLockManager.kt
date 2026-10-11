package com.os11.app.platform.biometrics

import android.content.Context
import android.os.SystemClock
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.os11.app.service.settings.SettingsService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooGenericExceptionCaught", "MagicNumber")
class AppLockManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsService: SettingsService,
) {
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var backgroundTimestampMs: Long? = null

    fun canAuthenticate(): Boolean = try {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    } catch (_: Exception) {
        false
    }

    fun onAppBackgrounded() {
        scope.launch {
            val enabled = settingsService.isAppLockEnabled()
            if (enabled) {
                backgroundTimestampMs = SystemClock.elapsedRealtime()
            }
        }
    }

    fun onAppForegrounded() {
        scope.launch {
            val enabled = settingsService.isAppLockEnabled()
            if (enabled) {
                val bgTime = backgroundTimestampMs
                if (bgTime == null) {
                    _isLocked.value = true
                } else {
                    val timeoutSec = settingsService.getAppLockTimeoutSeconds()
                    val elapsedSec = (SystemClock.elapsedRealtime() - bgTime) / 1000
                    if (elapsedSec >= timeoutSec) {
                        _isLocked.value = true
                    }
                }
            } else {
                _isLocked.value = false
            }
        }
    }

    fun unlock() {
        _isLocked.value = false
        backgroundTimestampMs = null
    }

    fun lock() {
        _isLocked.value = true
    }

    fun promptBiometric(activity: FragmentActivity, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock OS11")
            .setSubtitle("Confirm your identity with biometric or device PIN")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            )
            .build()

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlock()
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    // Biometric didn't match, prompt remains open
                }
            },
        )

        try {
            prompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onError(e.message ?: "Authentication error")
        }
    }
}
