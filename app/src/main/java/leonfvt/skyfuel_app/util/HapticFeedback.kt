package leonfvt.skyfuel_app.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

/**
 * Utilitaires pour le retour haptique dans l'application
 */
object HapticUtils {
    
    /**
     * Effectue un retour haptique léger (pour les interactions basiques)
     */
    fun performLightHaptic(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
    
    /**
     * Effectue un retour haptique moyen (pour les actions importantes)
     */
    fun performMediumHaptic(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
    
    /**
     * Effectue un retour haptique fort (pour les actions critiques comme la suppression)
     */
    fun performHeavyHaptic(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
    
    /**
     * Effectue un retour haptique de succès
     */
    fun performSuccessHaptic(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
    
    /**
     * Effectue un retour haptique d'erreur
     */
    fun performErrorHaptic(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
    
    /**
     * Effectue une vibration personnalisée (nécessite la permission VIBRATE)
     */
    fun performCustomVibration(context: Context, durationMs: Long = 50) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }
}

/**
 * Classe wrapper pour le retour haptique dans Compose
 */
class ComposeHapticFeedback(
    private val view: View,
    private val hapticFeedback: HapticFeedback
) {
    fun performClick() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    
    fun performLight() {
        HapticUtils.performLightHaptic(view)
    }
    
    fun performMedium() {
        HapticUtils.performMediumHaptic(view)
    }
    
    fun performHeavy() {
        HapticUtils.performHeavyHaptic(view)
    }
    
    fun performSuccess() {
        HapticUtils.performSuccessHaptic(view)
    }
    
    fun performError() {
        HapticUtils.performErrorHaptic(view)
    }
}

/**
 * Composable pour obtenir le retour haptique dans une composition
 */
@Composable
fun rememberHapticFeedback(): ComposeHapticFeedback {
    val view = LocalView.current
    val hapticFeedback = LocalHapticFeedback.current
    return remember(view, hapticFeedback) {
        ComposeHapticFeedback(view, hapticFeedback)
    }
}
