package leonfvt.skyfuel_app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.model.BatteryType
import java.time.LocalDate

/**
 * Entité Room pour la table des batteries
 */
@Entity(tableName = "batteries")
data class BatteryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Identifiants de la batterie
    val brand: String,
    val model: String,
    val serialNumber: String,
    
    // Caractéristiques techniques
    val type: BatteryType,
    val cells: Int,
    val capacity: Int,
    
    // Informations de gestion
    val purchaseDate: LocalDate,
    val status: BatteryStatus,
    val cycleCount: Int,
    
    // Informations supplémentaires
    val notes: String,
    val lastUseDate: LocalDate?,
    val lastChargeDate: LocalDate?,
    
    // Identifiant unique pour QR code
    val qrCodeId: String = "",
    
    // Chemin vers la photo de la batterie
    val photoPath: String? = null
) {
    /**
     * Convertit l'entité en modèle de domaine
     */
    fun toDomainModel(): Battery {
        return Battery(
            id = id,
            brand = brand,
            model = model,
            serialNumber = serialNumber,
            type = type,
            cells = cells,
            capacity = capacity,
            purchaseDate = purchaseDate,
            status = status,
            cycleCount = cycleCount,
            notes = notes,
            lastUseDate = lastUseDate,
            lastChargeDate = lastChargeDate,
            qrCodeId = qrCodeId,
            photoPath = photoPath
        )
    }
    
    companion object {
        /**
         * Crée une entité à partir du modèle de domaine
         */
        fun fromDomainModel(battery: Battery): BatteryEntity {
            // Génère un ID QR code si nécessaire
            val qrId = if (battery.qrCodeId.isNotEmpty()) battery.qrCodeId 
                       else "BATTERY_${battery.id}_${battery.serialNumber}"
            
            return BatteryEntity(
                id = battery.id,
                brand = battery.brand,
                model = battery.model,
                serialNumber = battery.serialNumber,
                type = battery.type,
                cells = battery.cells,
                capacity = battery.capacity,
                purchaseDate = battery.purchaseDate,
                status = battery.status,
                cycleCount = battery.cycleCount,
                notes = battery.notes,
                lastUseDate = battery.lastUseDate,
                lastChargeDate = battery.lastChargeDate,
                qrCodeId = qrId,
                photoPath = battery.photoPath
            )
        }
    }
}