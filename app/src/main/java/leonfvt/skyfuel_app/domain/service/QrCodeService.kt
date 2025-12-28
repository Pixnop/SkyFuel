package leonfvt.skyfuel_app.domain.service

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.QrCodeData
import leonfvt.skyfuel_app.domain.model.QrCodeData.Companion.toBattery
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
     * Génère un QR code pour une batterie avec son identifiant SF-XXX visible
     * @param battery Batterie pour laquelle générer le QR code
     * @param size Taille du QR code en pixels
     * @return Bitmap contenant le QR code avec le label ou null en cas d'erreur
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
            val label = QrCodeGenerator.generateShortId(battery.id)
            return QrCodeGenerator.generateQrCodeBitmapWithLabel(qrContent, size, 2, label)
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
    
    // ==================== FONCTIONNALITÉS DE PARTAGE ====================
    
    /**
     * Génère un QR code pour partager une batterie (contient toutes les données)
     * avec son identifiant SF-XXX visible
     * @param battery Batterie à partager
     * @param size Taille du QR code en pixels
     * @return Bitmap contenant le QR code avec le label ou null en cas d'erreur
     */
    fun generateShareQrCode(battery: Battery, size: Int = 512): Bitmap? {
        return try {
            val qrData = QrCodeData.forShareBattery(battery)
            val qrContent = qrData.encode()
            val label = QrCodeGenerator.generateShortId(battery.id)
            QrCodeGenerator.generateQrCodeBitmapWithLabel(qrContent, size, 2, label)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la génération du QR code de partage: ${e.message}", e)
            null
        }
    }
    
    /**
     * Partage le QR code contenant toutes les données de la batterie
     * @param battery Batterie à partager
     */
    fun shareFullBatteryQrCode(battery: Battery) {
        val qrBitmap = generateShareQrCode(battery) ?: return
        QrCodeUtils.shareQrCode(context, qrBitmap, battery, isShareMode = true)
    }
    
    /**
     * Extrait les données d'une batterie depuis un QR code de partage
     * @param qrContent Contenu du QR code scanné
     * @return Battery si le QR code est valide, null sinon
     */
    fun extractBatteryFromShareQrCode(qrContent: String): Battery? {
        val qrData = decodeQrCode(qrContent) ?: return null
        return qrData.toBattery()
    }
    
    /**
     * Vérifie si un QR code est un QR code de partage de batterie
     * @param qrContent Contenu du QR code scanné
     * @return true si c'est un QR code de partage
     */
    fun isShareQrCode(qrContent: String): Boolean {
        val qrData = decodeQrCode(qrContent) ?: return false
        return qrData.entityType == QrCodeEntityType.BATTERY_SHARE
    }
    
    /**
     * Importe une batterie depuis un QR code de partage
     * @param qrContent Contenu du QR code scanné
     * @return Pair<Boolean, String> - (succès, message)
     */
    suspend fun importBatteryFromQrCode(qrContent: String): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                val qrData = decodeQrCode(qrContent)
                
                if (qrData == null) {
                    return@withContext Pair(false, "QR code invalide ou non reconnu")
                }
                
                if (qrData.entityType != QrCodeEntityType.BATTERY_SHARE) {
                    return@withContext Pair(false, "Ce QR code ne contient pas de données de partage de batterie")
                }
                
                val battery = qrData.toBattery()
                
                if (battery == null) {
                    return@withContext Pair(false, "Impossible d'extraire les données de la batterie")
                }
                
                // Vérifier si une batterie avec le même numéro de série existe déjà
                val existingBattery = batteryRepository.getBatteryBySerialNumber(battery.serialNumber)
                
                if (existingBattery != null) {
                    return@withContext Pair(false, "Une batterie avec ce numéro de série existe déjà (${battery.serialNumber})")
                }
                
                // Insérer la nouvelle batterie
                val newId = batteryRepository.insertBattery(battery)
                
                if (newId > 0) {
                    Log.d(TAG, "Batterie importée avec succès: ${battery.brand} ${battery.model} (ID: $newId)")
                    return@withContext Pair(true, "Batterie importée: ${battery.brand} ${battery.model}")
                } else {
                    return@withContext Pair(false, "Erreur lors de l'insertion de la batterie")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de l'import de la batterie: ${e.message}", e)
                return@withContext Pair(false, "Erreur: ${e.message}")
            }
        }
    }
    
    /**
     * Traite un QR code scanné (gère les deux types: référence et partage)
     */
    suspend fun processScannedQrCodeEnhanced(qrContent: String): QrScanResult {
        return withContext(Dispatchers.IO) {
            try {
                val qrData = decodeQrCode(qrContent)
                
                if (qrData == null) {
                    return@withContext QrScanResult.Error("QR code invalide ou non reconnu")
                }
                
                when (qrData.entityType) {
                    QrCodeEntityType.BATTERY -> {
                        val batteryId = qrData.entityId.toLongOrNull()
                        if (batteryId != null) {
                            val battery = batteryRepository.getBatteryById(batteryId)
                            if (battery != null) {
                                return@withContext QrScanResult.BatteryFound(battery)
                            } else {
                                return@withContext QrScanResult.BatteryNotFound(batteryId)
                            }
                        } else {
                            return@withContext QrScanResult.Error("ID de batterie invalide")
                        }
                    }
                    QrCodeEntityType.BATTERY_SHARE -> {
                        val battery = qrData.toBattery()
                        if (battery != null) {
                            // Vérifier si elle existe déjà
                            val existing = batteryRepository.getBatteryBySerialNumber(battery.serialNumber)
                            return@withContext QrScanResult.ShareableBattery(battery, existing != null)
                        } else {
                            return@withContext QrScanResult.Error("Impossible d'extraire les données de la batterie")
                        }
                    }
                    else -> {
                        return@withContext QrScanResult.Error("Type de QR code non supporté")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du traitement du QR code: ${e.message}", e)
                return@withContext QrScanResult.Error("Erreur: ${e.message}")
            }
        }
    }
}

/**
 * Résultat du scan d'un QR code
 */
sealed class QrScanResult {
    data class BatteryFound(val battery: Battery) : QrScanResult()
    data class BatteryNotFound(val batteryId: Long) : QrScanResult()
    data class ShareableBattery(val battery: Battery, val alreadyExists: Boolean) : QrScanResult()
    data class Error(val message: String) : QrScanResult()
}