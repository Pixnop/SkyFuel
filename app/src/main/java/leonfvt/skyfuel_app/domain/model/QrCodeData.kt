package leonfvt.skyfuel_app.domain.model

import java.io.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class QrCodeEntityType {
    BATTERY,       // QR code pour identifier une batterie (référence locale)
    BATTERY_SHARE, // QR code pour partager une batterie (données complètes)
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
                
                // Parcourir les parties restantes
                if (parts.size > 5) {
                    // Vérifier si c'est des métadonnées (contient "=") ou un checksum
                    val part5 = parts[5]
                    if (part5.contains("=")) {
                        // C'est des métadonnées, pas de checksum
                        metadata = parseMetadata(part5)
                    } else {
                        // C'est un checksum
                        checksum = part5
                        // Les métadonnées sont dans la partie suivante
                        if (parts.size > 6) {
                            metadata = parseMetadata(parts[6])
                        }
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
        
        private fun parseMetadata(metadataString: String): Map<String, String> {
            return metadataString.split(",")
                .filter { it.contains("=") }
                .associate { 
                    val keyValue = it.split("=", limit = 2)
                    keyValue[0] to keyValue[1]
                }
        }
        
        private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        
        /**
         * Crée un QR code pour identifier une batterie (référence locale uniquement)
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
        
        /**
         * Crée un QR code pour partager une batterie avec toutes ses données
         * Permet de recréer la batterie sur un autre appareil
         */
        fun forShareBattery(battery: Battery): QrCodeData {
            val metadata = mutableMapOf<String, String>()
            
            // Identifiants
            metadata["brand"] = battery.brand
            metadata["model"] = battery.model
            metadata["sn"] = battery.serialNumber
            
            // Caractéristiques techniques
            metadata["type"] = battery.type.name
            metadata["cells"] = battery.cells.toString()
            metadata["capacity"] = battery.capacity.toString()
            
            // Informations de gestion
            metadata["purchaseDate"] = battery.purchaseDate.format(dateFormatter)
            metadata["status"] = battery.status.name
            metadata["cycleCount"] = battery.cycleCount.toString()
            
            // Informations supplémentaires
            if (battery.notes.isNotBlank()) {
                // Encoder les caractères spéciaux dans les notes
                metadata["notes"] = battery.notes.replace(",", "&#44;").replace("=", "&#61;")
            }
            battery.lastUseDate?.let { metadata["lastUseDate"] = it.format(dateFormatter) }
            battery.lastChargeDate?.let { metadata["lastChargeDate"] = it.format(dateFormatter) }
            
            return QrCodeData(
                entityType = QrCodeEntityType.BATTERY_SHARE,
                entityId = battery.serialNumber, // Utilise le numéro de série comme ID
                timestamp = System.currentTimeMillis(),
                version = 2, // Version 2 pour le format de partage
                metadata = metadata
            )
        }
        
        /**
         * Reconstruit une batterie à partir des données du QR code
         * @return Battery si les données sont valides, null sinon
         */
        fun QrCodeData.toBattery(): Battery? {
            if (entityType != QrCodeEntityType.BATTERY_SHARE) return null
            
            return try {
                Battery(
                    id = 0, // Nouvel ID sera attribué lors de l'insertion
                    brand = metadata["brand"] ?: return null,
                    model = metadata["model"] ?: return null,
                    serialNumber = metadata["sn"] ?: entityId,
                    type = BatteryType.valueOf(metadata["type"] ?: return null),
                    cells = metadata["cells"]?.toIntOrNull() ?: return null,
                    capacity = metadata["capacity"]?.toIntOrNull() ?: return null,
                    purchaseDate = LocalDate.parse(metadata["purchaseDate"] ?: return null, dateFormatter),
                    status = BatteryStatus.valueOf(metadata["status"] ?: "CHARGED"),
                    cycleCount = metadata["cycleCount"]?.toIntOrNull() ?: 0,
                    notes = metadata["notes"]?.replace("&#44;", ",")?.replace("&#61;", "=") ?: "",
                    lastUseDate = metadata["lastUseDate"]?.let { LocalDate.parse(it, dateFormatter) },
                    lastChargeDate = metadata["lastChargeDate"]?.let { LocalDate.parse(it, dateFormatter) },
                    qrCodeId = ""
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}