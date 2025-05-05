package leonfvt.skyfuel_app.domain.model

/**
 * Type d'évènement enregistré dans l'historique des batteries
 */
enum class BatteryEventType {
    STATUS_CHANGE,     // Changement de statut (chargé, déchargé, stockage, etc.)
    CYCLE_COMPLETED,   // Cycle de charge/décharge complet
    VOLTAGE_READING,   // Relevé de tension
    NOTE_ADDED,        // Note ajoutée
    MAINTENANCE        // Opération de maintenance
}