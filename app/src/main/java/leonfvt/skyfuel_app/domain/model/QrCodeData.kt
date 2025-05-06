package leonfvt.skyfuel_app.domain.model

import java.io.Serializable

/**
 * Représente les différents types d'entités qui peuvent être encodées dans un QR code
 */
enum class QrCodeEntityType {
    BATTERY,       // QR code pour une batterie
    MAINTENANCE,   // QR code pour une opération de maintenance
    USER,          // QR code pour un utilisateur
    LOCATION,      // QR code pour un emplacement de stockage
    DRONE,         // QR code pour un drone
    OTHER          // Autre type d'entité
}

/**
 * Classe de données qui représente l'information stockée dans un QR code
 * Cette structure standardisée facilite le parsing et la validation des QR codes
 */
data class QrCodeData(
    val entityType: QrCodeEntityType,    // Type d'entité encodée
    val entityId: String,                // Identifiant unique de l'entité
    val timestamp: Long,                 // Horodatage de la création du QR code
    val version: Int = 1,                // Version du format du QR code
    val checksum: String? = null,        // Somme de contrôle pour validation (optionnelle)
    val metadata: Map<String, String> = emptyMap() // Métadonnées supplémentaires
) : Serializable {

    /**
     * Encode les données en chaîne de caractères pour le QR code
     * Format: SKYFUEL::[TYPE]::[ID]::[TIMESTAMP]::[VERSION]::[CHECKSUM]::[METADATA]
     */
    fun encode(): String {
        val metadataString = if (metadata.isNotEmpty()) {
            metadata.entries.joinToString(",") { "${it.key}=${it.value}" }
        } else {
            ""
        }
        
        return buildString {
            append("SKYFUEL::${entityType.name}::$entityId::$timestamp::$version")
            if (checksum != null) append("::$checksum")
            if (metadataString.isNotEmpty()) append("::$metadataString")
        }
    }
    
    companion object {
        /**
         * Décode une chaîne de caractères provenant d'un QR code en objet QrCodeData
         * @param qrCodeString Chaîne à décoder
         * @return QrCodeData instance si la chaîne est valide, null sinon
         */
        fun decode(qrCodeString: String): QrCodeData? {
            try {
                if (!qrCodeString.startsWith("SKYFUEL::")) {
                    return null
                }
                
                val parts = qrCodeString.split("::")
                if (parts.size < 5) {
                    return null
                }
                
                val type = try {
                    QrCodeEntityType.valueOf(parts[1])
                } catch (e: IllegalArgumentException) {
                    QrCodeEntityType.OTHER
                }
                
                val entityId = parts[2]
                val timestamp = parts[3].toLongOrNull() ?: 0L
                val version = parts[4].toIntOrNull() ?: 1
                
                var checksum: String? = null
                var metadata = emptyMap<String, String>()
                
                if (parts.size > 5) {
                    checksum = parts[5]
                }
                
                if (parts.size > 6) {
                    val metadataString = parts[6]
                    metadata = metadataString.split(",")
                        .filter { it.contains("=") }
                        .associate { 
                            val keyValue = it.split("=", limit = 2)
                            keyValue[0] to keyValue[1]
                        }
                }
                
                return QrCodeData(
                    entityType = type,
                    entityId = entityId,
                    timestamp = timestamp,
                    version = version,
                    checksum = checksum,
                    metadata = metadata
                )
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
        
        /**
         * Crée un QR code pour une batterie
         */
        fun forBattery(
            batteryId: Long,
            serialNumber: String,
            brand: String? = null,
            model: String? = null
        ): QrCodeData {
            val metadata = mutableMapOf<String, String>()
            if (!brand.isNullOrEmpty()) metadata["brand"] = brand
            if (!model.isNullOrEmpty()) metadata["model"] = model
            if (serialNumber.isNotEmpty()) metadata["sn"] = serialNumber
            
            return QrCodeData(
                entityType = QrCodeEntityType.BATTERY,
                entityId = batteryId.toString(),
                timestamp = System.currentTimeMillis(),
                metadata = metadata
            )
        }
    }
}