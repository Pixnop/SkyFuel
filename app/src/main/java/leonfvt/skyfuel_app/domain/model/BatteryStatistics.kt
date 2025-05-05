package leonfvt.skyfuel_app.domain.model

/**
 * Statistiques globales des batteries
 */
data class BatteryStatistics(
    val totalCount: Int = 0,
    val chargedCount: Int = 0,
    val dischargedCount: Int = 0,
    val storageCount: Int = 0,
    val outOfServiceCount: Int = 0,
    val averageCycleCount: Float = 0f
) {
    /**
     * Calcule le pourcentage de batteries chargées
     */
    fun getChargedPercentage(): Float {
        if (totalCount == 0) return 0f
        return (chargedCount.toFloat() / totalCount) * 100
    }
    
    /**
     * Calcule le pourcentage de batteries déchargées
     */
    fun getDischargedPercentage(): Float {
        if (totalCount == 0) return 0f
        return (dischargedCount.toFloat() / totalCount) * 100
    }
    
    /**
     * Calcule le pourcentage de batteries en stockage
     */
    fun getStoragePercentage(): Float {
        if (totalCount == 0) return 0f
        return (storageCount.toFloat() / totalCount) * 100
    }
    
    /**
     * Calcule le pourcentage de batteries hors service
     */
    fun getOutOfServicePercentage(): Float {
        if (totalCount == 0) return 0f
        return (outOfServiceCount.toFloat() / totalCount) * 100
    }
}