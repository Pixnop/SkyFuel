package leonfvt.skyfuel_app.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Utilitaire pour fournir un feedback haptique et sonore lors du scan QR
 */
class ScanFeedback(private val context: Context) {
    
    private var toneGenerator: ToneGenerator? = null
    
    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (e: Exception) {
            // ToneGenerator peut échouer sur certains appareils
            toneGenerator = null
        }
    }
    
    /**
     * Déclenche le feedback de succès (vibration + son)
     */
    fun triggerSuccessFeedback() {
        triggerHapticFeedback()
        playSuccessSound()
    }
    
    /**
     * Déclenche une vibration courte de succès
     */
    private fun triggerHapticFeedback() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Pattern: court-pause-court pour un feedback de succès
                val effect = VibrationEffect.createWaveform(
                    longArrayOf(0, 50, 50, 50), // timings
                    intArrayOf(0, 200, 0, 150), // amplitudes
                    -1 // don't repeat
                )
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 50, 50, 50), -1)
            }
        } catch (e: SecurityException) {
            // Permission VIBRATE non accordée, ignorer silencieusement
        }
    }
    
    /**
     * Joue le son de succès (bip système)
     */
    private fun playSuccessSound() {
        try {
            // Utilise un ton système de confirmation
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 150)
        } catch (e: Exception) {
            // Ignorer si le son ne peut pas être joué
        }
    }
    
    /**
     * Libère les ressources
     */
    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
