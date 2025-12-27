package leonfvt.skyfuel_app.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Modèle de domaine représentant un rappel de charge
 */
data class ChargeReminder(
    val id: Long = 0,
    val batteryId: Long,           // ID de la batterie concernée
    val title: String,              // Titre du rappel
    val time: LocalTime,            // Heure du rappel
    val daysOfWeek: Set<DayOfWeek>, // Jours où le rappel est actif (vide = tous les jours)
    val isEnabled: Boolean = true,  // Rappel activé/désactivé
    val reminderType: ReminderType = ReminderType.CHARGE,
    val notes: String = ""
) {
    fun getFormattedTime(): String {
        return String.format("%02d:%02d", time.hour, time.minute)
    }
    
    fun getFormattedDays(): String {
        if (daysOfWeek.isEmpty()) return "Tous les jours"
        
        val dayNames = mapOf(
            DayOfWeek.MONDAY to "Lun",
            DayOfWeek.TUESDAY to "Mar",
            DayOfWeek.WEDNESDAY to "Mer",
            DayOfWeek.THURSDAY to "Jeu",
            DayOfWeek.FRIDAY to "Ven",
            DayOfWeek.SATURDAY to "Sam",
            DayOfWeek.SUNDAY to "Dim"
        )
        
        return daysOfWeek
            .sortedBy { it.value }
            .mapNotNull { dayNames[it] }
            .joinToString(", ")
    }
}

/**
 * Types de rappels disponibles
 */
enum class ReminderType {
    CHARGE,      // Rappel pour charger la batterie
    STORAGE,     // Rappel pour mettre en mode stockage
    MAINTENANCE, // Rappel de maintenance
    VOLTAGE_CHECK // Rappel de vérification de tension
}
