package leonfvt.skyfuel_app.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryEventType
import leonfvt.skyfuel_app.domain.model.BatteryHistory
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Implémentation factice du repository pour les tests unitaires
 * Cette implémentation stocke les données en mémoire
 */
class FakeBatteryRepository : BatteryRepository {
    
    private val batteries = mutableListOf<Battery>()
    private val batteriesFlow = MutableStateFlow<List<Battery>>(emptyList())
    private val batteryHistory = mutableMapOf<Long, MutableList<BatteryHistory>>()
    private var idCounter = 1L
    
    // Récupération de données
    override fun getAllBatteries(): Flow<List<Battery>> = batteriesFlow
    
    override fun getBatteriesByStatus(status: BatteryStatus): Flow<List<Battery>> {
        return batteriesFlow.map { list -> list.filter { it.status == status } }
    }
    
    override fun searchBatteries(query: String): Flow<List<Battery>> {
        return batteriesFlow.map { list ->
            list.filter { battery ->
                battery.brand.contains(query, ignoreCase = true) ||
                battery.model.contains(query, ignoreCase = true) ||
                battery.serialNumber.contains(query, ignoreCase = true)
            }
        }
    }
    
    override suspend fun getBatteryById(id: Long): Battery? {
        return batteries.find { it.id == id }
    }
    
    override fun getBatteryHistory(batteryId: Long): Flow<List<BatteryHistory>> {
        return flow { emit(batteryHistory[batteryId] ?: emptyList()) }
    }
    
    // Opérations de modification
    override suspend fun addBattery(battery: Battery): Long {
        val id = idCounter++
        val newBattery = battery.copy(id = id)
        batteries.add(newBattery)
        batteriesFlow.value = batteries.toList()
        
        // Ajout d'un événement initial dans l'historique
        val initialEvent = BatteryHistory(
            id = 0,
            batteryId = id,
            timestamp = LocalDateTime.now(),
            eventType = BatteryEventType.STATUS_CHANGE,
            notes = "Batterie ajoutée au système"
        )
        batteryHistory[id] = mutableListOf(initialEvent)
        
        return id
    }
    
    override suspend fun updateBattery(battery: Battery) {
        val index = batteries.indexOfFirst { it.id == battery.id }
        if (index != -1) {
            batteries[index] = battery
            batteriesFlow.value = batteries.toList()
        }
    }
    
    override suspend fun deleteBattery(battery: Battery) {
        batteries.removeIf { it.id == battery.id }
        batteryHistory.remove(battery.id)
        batteriesFlow.value = batteries.toList()
    }
    
    override suspend fun updateBatteryStatus(batteryId: Long, newStatus: BatteryStatus, notes: String) {
        val battery = getBatteryById(batteryId) ?: return
        val updatedBattery = battery.copy(status = newStatus)
        updateBattery(updatedBattery)
        
        // Ajout d'un événement de changement de statut
        val statusChangeEvent = BatteryHistory(
            id = 0,
            batteryId = batteryId,
            timestamp = LocalDateTime.now(),
            eventType = BatteryEventType.STATUS_CHANGE,
            previousStatus = battery.status,
            newStatus = newStatus,
            notes = notes.ifBlank { "Statut changé à ${newStatus.name}" }
        )
        batteryHistory[batteryId]?.add(statusChangeEvent)
    }
    
    override suspend fun recordVoltageReading(batteryId: Long, voltage: Float, notes: String) {
        // Ajout d'un événement de lecture de tension
        val voltageEvent = BatteryHistory(
            id = 0,
            batteryId = batteryId,
            timestamp = LocalDateTime.now(),
            eventType = BatteryEventType.VOLTAGE_READING,
            voltage = voltage,
            notes = "Tension: $voltage V" + if (notes.isNotBlank()) " - $notes" else ""
        )
        batteryHistory[batteryId]?.add(voltageEvent)
    }
    
    override suspend fun addBatteryNote(batteryId: Long, note: String) {
        // Ajout d'une note à l'historique
        val noteEvent = BatteryHistory(
            id = 0,
            batteryId = batteryId,
            timestamp = LocalDateTime.now(),
            eventType = BatteryEventType.NOTE_ADDED,
            notes = note
        )
        batteryHistory[batteryId]?.add(noteEvent)
    }
    
    override suspend fun recordMaintenance(batteryId: Long, description: String) {
        // Ajout d'un événement de maintenance
        val maintenanceEvent = BatteryHistory(
            id = 0,
            batteryId = batteryId,
            timestamp = LocalDateTime.now(),
            eventType = BatteryEventType.MAINTENANCE,
            notes = description
        )
        batteryHistory[batteryId]?.add(maintenanceEvent)
    }
    
    // Statistiques
    override suspend fun getTotalBatteryCount(): Int {
        return batteries.size
    }
    
    override suspend fun getBatteryCountByStatus(status: BatteryStatus): Int {
        return batteries.count { it.status == status }
    }
    
    override suspend fun getAverageCycleCount(): Float {
        if (batteries.isEmpty()) return 0f
        return batteries.sumOf { it.cycleCount }.toFloat() / batteries.size
    }
    
    // Méthodes supplémentaires pour les tests
    
    /**
     * Réinitialise le repository en supprimant toutes les données
     */
    fun clearAll() {
        batteries.clear()
        batteryHistory.clear()
        idCounter = 1L
        batteriesFlow.value = emptyList()
    }
}