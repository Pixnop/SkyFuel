package leonfvt.skyfuel_app.domain.service

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.QrCodeData
import leonfvt.skyfuel_app.domain.model.QrCodeEntityType
import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import leonfvt.skyfuel_app.util.ErrorHandler
import leonfvt.skyfuel_app.util.QrCodeGenerator
import leonfvt.skyfuel_app.util.QrCodeUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service centralisant toute la logique liée aux QR codes dans l'application
 * Responsable de la génération, du décodage et de la gestion des QR codes
 */
@Singleton
class QrCodeService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val batteryRepository: BatteryRepository
) {
    private val TAG = "QrCodeService"
    
    /**
     * Génère un QR code pour une batterie
     * @param battery Batterie pour laquelle générer le QR code
     * @param size Taille du QR code en pixels
     * @return Bitmap contenant le QR code ou null en cas d'erreur
     */
    fun generateBatteryQrCode(battery: Battery, size: Int = 512): Bitmap? {
        try {
            val qrData = QrCodeData.forBattery(
                batteryId = battery.id,
                serialNumber = battery.serialNumber,
                brand = battery.brand,
                model = battery.model
            )
            
            val qrContent = qrData.encode()
            return QrCodeGenerator.generateQrCodeBitmap(qrContent, size)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la génération du QR code: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Partage le QR code d'une batterie
     * @param battery Batterie dont le QR code doit être partagé
     */
    fun shareBatteryQrCode(battery: Battery) {
        val qrBitmap = generateBatteryQrCode(battery) ?: return
        QrCodeUtils.shareQrCode(context, qrBitmap, battery)
    }
    
    /**
     * Enregistre le QR code d'une batterie dans la galerie
     * @param battery Batterie dont le QR code doit être enregistré
     */
    fun saveBatteryQrCodeToGallery(battery: Battery) {
        val qrBitmap = generateBatteryQrCode(battery) ?: return
        QrCodeUtils.saveQrCodeToGallery(context, qrBitmap, battery)
    }
    
    /**
     * Analyse un QR code et retourne les données associées
     * @param qrContent Contenu du QR code scanné
     * @return QrCodeData objet contenant les données décodées, ou null si le QR code est invalide
     */
    fun decodeQrCode(qrContent: String): QrCodeData? {
        return QrCodeData.decode(qrContent)
    }
    
    /**
     * Traite un QR code scanné et effectue les actions appropriées
     * @param qrContent Contenu du QR code scanné
     * @return Paire contenant un booléen (succès/échec) et un message de résultat
     */
    suspend fun processScannedQrCode(qrContent: String): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                val qrData = decodeQrCode(qrContent)
                
                if (qrData == null) {
                    return@withContext Pair(false, "QR code invalide ou non reconnu")
                }
                
                when (qrData.entityType) {
                    QrCodeEntityType.BATTERY -> {
                        val batteryId = qrData.entityId.toLongOrNull()
                        if (batteryId != null) {
                            val battery = batteryRepository.getBatteryById(batteryId)
                            if (battery != null) {
                                return@withContext Pair(true, "Batterie trouvée: ${battery.brand} ${battery.model}")
                            } else {
                                return@withContext Pair(false, "Batterie non trouvée (ID: $batteryId)")
                            }
                        } else {
                            return@withContext Pair(false, "ID de batterie invalide dans le QR code")
                        }
                    }
                    else -> {
                        return@withContext Pair(false, "Type d'entité non pris en charge: ${qrData.entityType}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du traitement du QR code: ${e.message}", e)
                return@withContext Pair(false, "Erreur: ${e.message}")
            }
        }
    }
    
    /**
     * Récupère une batterie à partir d'un QR code
     * @param qrContent Contenu du QR code scanné
     * @return Batterie correspondante ou null si non trouvée
     */
    suspend fun getBatteryFromQrCode(qrContent: String): Battery? {
        return withContext(Dispatchers.IO) {
            try {
                val qrData = decodeQrCode(qrContent) ?: return@withContext null
                
                if (qrData.entityType != QrCodeEntityType.BATTERY) {
                    return@withContext null
                }
                
                val batteryId = qrData.entityId.toLongOrNull() ?: return@withContext null
                return@withContext batteryRepository.getBatteryById(batteryId)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la récupération de la batterie depuis le QR code: ${e.message}", e)
                return@withContext null
            }
        }
    }
    
    /**
     * Génère un nouveau QR code ID pour une batterie
     * @param battery Batterie pour laquelle générer un nouvel ID
     * @return Chaîne de caractères contenant le nouvel identifiant QR code
     */
    fun generateNewQrCodeId(battery: Battery): String {
        return "BATTERY_${battery.id}_${battery.serialNumber}_${System.currentTimeMillis()}"
    }
}