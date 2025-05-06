package leonfvt.skyfuel_app.domain.model

import java.time.LocalDate

/**
 * Modèle de domaine représentant une batterie de drone
 * Cette classe est indépendante de la couche de données et contient la logique métier
 */
data class Battery(
    val id: Long = 0,
    
    // Identifiants de la batterie
    val brand: String,
    val model: String,
    val serialNumber: String,
    
    // Caractéristiques techniques
    val type: BatteryType,
    val cells: Int, // Nombre de cellules
    val capacity: Int, // en mAh
    
    // Informations de gestion
    val purchaseDate: LocalDate,
    val status: BatteryStatus = BatteryStatus.CHARGED,
    val cycleCount: Int = 0,
    
    // Informations supplémentaires
    val notes: String = "",
    val lastUseDate: LocalDate? = null,
    val lastChargeDate: LocalDate? = null,
    
    // Identifiant unique pour QR code (utilise l'ID par défaut)
    val qrCodeId: String = ""
) {
    /**
     * Calcule l'âge de la batterie en jours
     */
    fun getAgeInDays(): Long {
        return java.time.Duration.between(
            purchaseDate.atStartOfDay(),
            LocalDate.now().atStartOfDay()
        ).toDays()
    }
    
    /**
     * Détermine si la batterie doit être chargée bientôt 
     * (si elle est déchargée depuis plus de 7 jours)
     */
    fun shouldBeCharged(): Boolean {
        if (status != BatteryStatus.DISCHARGED) return false
        
        val lastDischargeDate = lastUseDate ?: return false
        val daysSinceDischarge = java.time.Duration.between(
            lastDischargeDate.atStartOfDay(),
            LocalDate.now().atStartOfDay()
        ).toDays()
        
        return daysSinceDischarge > 7
    }
    
    /**
     * Calcule l'état de santé estimé de la batterie basé sur le nombre de cycles
     * et l'âge de la batterie. Retourne une valeur entre 0 et 100.
     */
    fun getHealthPercentage(): Int {
        // La santé diminue avec le nombre de cycles (approximation)
        val cycleImpact = when (type) {
            BatteryType.LIPO -> cycleCount * 0.25 // LiPo supporte environ 400 cycles
            BatteryType.LI_ION -> cycleCount * 0.15 // Li-Ion supporte environ 650 cycles
            BatteryType.NIMH -> cycleCount * 0.1 // NiMH supporte environ 1000 cycles
            BatteryType.LIFE -> cycleCount * 0.05 // LiFe supporte environ 2000 cycles
            BatteryType.OTHER -> cycleCount * 0.2 // Valeur par défaut
        }
        
        // L'âge a aussi un impact (vieillissement naturel)
        val ageInYears = getAgeInDays() / 365.0
        val ageImpact = when (type) {
            BatteryType.LIPO -> ageInYears * 10 // LiPo vieillit plus vite
            BatteryType.LI_ION -> ageInYears * 7
            BatteryType.NIMH -> ageInYears * 5
            BatteryType.LIFE -> ageInYears * 4
            BatteryType.OTHER -> ageInYears * 8
        }
        
        val health = 100 - (cycleImpact + ageImpact).toInt()
        return health.coerceIn(0, 100) // Limiter entre 0 et 100%
    }
}