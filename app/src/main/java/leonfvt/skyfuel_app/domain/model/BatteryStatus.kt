package leonfvt.skyfuel_app.domain.model

/**
 * Représente les différents états dans lesquels peut se trouver une batterie
 */
enum class BatteryStatus {
    CHARGED,      // Chargée, prête à l'emploi
    DISCHARGED,   // Déchargée, nécessite une charge
    STORAGE,      // En stockage, niveau optimal pour stockage long terme
    OUT_OF_SERVICE // Hors service, à recycler
}