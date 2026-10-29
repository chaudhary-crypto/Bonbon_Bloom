package com.example.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object HapticManager {
    var hapticsEnabled: Boolean = true

    fun performHapticFeedback(context: Context, type: HapticType = HapticType.Light) {
        if (!hapticsEnabled) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (type) {
                    HapticType.Light -> VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
                    HapticType.Medium -> VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
                    HapticType.Heavy -> VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
                    HapticType.Success -> {
                        val timings = longArrayOf(0, 30, 40, 60)
                        val amplitudes = intArrayOf(0, 100, 0, 180)
                        VibrationEffect.createWaveform(timings, amplitudes, -1)
                    }
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(30)
            }
        } catch (_: Throwable) {}
    }
}

enum class HapticType {
    Light,
    Medium,
    Heavy,
    Success
}
