package com.os11.app.platform.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import com.os11.app.service.settings.SettingsService
import com.os11.app.ui.theme.OS11Haptic
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooGenericExceptionCaught", "MagicNumber")
class HapticsGateway @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsService: SettingsService,
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    val isHapticsEnabled: StateFlow<Boolean> = settingsService
        .observeHapticsEnabled()
        .stateIn(scope, SharingStarted.Eagerly, true)

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun performHaptic(haptic: OS11Haptic) {
        val vib = vibrator
        if (!isHapticsEnabled.value || vib == null || !vib.hasVibrator()) {
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibratePredefinedOrWaveform(vib, haptic)
            } else {
                vibrateLegacyOneShot(vib, haptic)
            }
        } catch (_: Exception) {
            // Ignore system vibration errors
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun vibratePredefinedOrWaveform(vib: Vibrator, haptic: OS11Haptic) {
        when (haptic) {
            OS11Haptic.Complete -> vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            OS11Haptic.DragStart -> vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            OS11Haptic.DragDrop -> vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            OS11Haptic.TimerEnd -> {
                val waveform = longArrayOf(0, 100, 100, 200)
                val amplitudes = intArrayOf(0, 255, 0, 255)
                vib.vibrate(VibrationEffect.createWaveform(waveform, amplitudes, -1))
            }
            OS11Haptic.Error -> {
                val waveform = longArrayOf(0, 50, 50, 50)
                val amplitudes = intArrayOf(0, 200, 0, 200)
                vib.vibrate(VibrationEffect.createWaveform(waveform, amplitudes, -1))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun vibrateLegacyOneShot(vib: Vibrator, haptic: OS11Haptic) {
        when (haptic) {
            OS11Haptic.Complete, OS11Haptic.DragDrop -> vib.vibrate(VibrationEffect.createOneShot(15, 100))
            OS11Haptic.DragStart -> vib.vibrate(VibrationEffect.createOneShot(30, 200))
            OS11Haptic.TimerEnd -> vib.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 100, 100, 200), -1),
            )
            OS11Haptic.Error -> vib.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 50), -1),
            )
        }
    }
}
