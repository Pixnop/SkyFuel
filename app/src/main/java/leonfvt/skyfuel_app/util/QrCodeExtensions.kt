package leonfvt.skyfuel_app.util

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.QrCodeData
import leonfvt.skyfuel_app.presentation.viewmodel.QrCodeViewModel

/**
 * Extensions utilitaires pour faciliter le traitement des QR codes
 */
object QrCodeExtensions {
    private const val TAG = "QrCodeExtensions"

    /**
     * Traite un QR code de batterie et navigue vers l'écran de détail si la batterie est trouvée
     * @param qrContent Le contenu du QR code scanné
     * @param viewModel Le ViewModel QrCode à utiliser pour le traitement
     * @param scope CoroutineScope pour exécuter les opérations asynchrones
     * @param onBatteryFound Callback exécuté lorsqu'une batterie est trouvée
     * @param onError Callback exécuté en cas d'erreur
     */
    fun processBatteryQrCode(
        qrContent: String,
        viewModel: QrCodeViewModel,
        scope: CoroutineScope,
        onBatteryFound: (Battery) -> Unit,
        onError: (String) -> Unit
    ) {
        // Vérifie si c'est le format ancien ("BATTERY_123_SERIALNUMBER")
        if (qrContent.startsWith("BATTERY_")) {
            try {
                // Extraire l'ID de la batterie du QR code
                val batteryIdStr = qrContent.split("_").getOrNull(1)
                
                if (batteryIdStr != null) {
                    try {
                        val batteryId = batteryIdStr.toLong()
                        Log.d(TAG, "Format ancien détecté, ID: $batteryId")
                        // Utiliser le ViewModel pour obtenir la batterie par son ID
                        scope.launch {
                            viewModel.getBatteryFromQrCode(
                                // Créer manuellement un format compatible avec le nouveau système
                                QrCodeData.forBattery(batteryId, "").encode(),
                                onBatteryFound = onBatteryFound,
                                onBatteryNotFound = onError
                            )
                        }
                    } catch (e: NumberFormatException) {
                        Log.e(TAG, "ID de batterie invalide: $batteryIdStr", e)
                        onError("QR code invalide. Format attendu: BATTERY_ID_SERIALNUMBER")
                    }
                } else {
                    Log.e(TAG, "Format de QR code incorrect: $qrContent")
                    onError("QR code invalide. Format attendu: BATTERY_ID_SERIALNUMBER")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du traitement du QR code: ${e.message}", e)
                onError("Erreur lors du traitement du QR code: ${e.message}")
            }
            return
        }

        // Traiter le nouveau format "SKYFUEL::BATTERY::123::..."
        Log.d(TAG, "Processing new format QR code: $qrContent")
        scope.launch {
            viewModel.getBatteryFromQrCode(
                qrContent,
                onBatteryFound = { battery ->
                    Log.d(TAG, "Battery found: ${battery.id} - ${battery.brand} ${battery.model}")
                    onBatteryFound(battery)
                },
                onBatteryNotFound = { error ->
                    Log.e(TAG, "Battery not found: $error")
                    onError(error)
                }
            )
        }
    }
}