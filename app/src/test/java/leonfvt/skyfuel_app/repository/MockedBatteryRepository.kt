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
import leonfvt.skyfuel_app.util.ErrorHandler
import java.time.LocalDateTime

/**
 * Implémentation mockée du repository pour les tests unitaires avancés
 * Permet de simuler des erreurs et des comportements spécifiques
 */
class MockedBatteryRepository : BatteryRepository {
    
    // Données mockées
    private val batteries = mutableListOf<Battery>()
    private val batteriesFlow = MutableStateFlow<List<Battery>>(emptyList())
    private val batteryHistory = mutableMapOf<Long, MutableList<BatteryHistory>>()
    private var idCounter = 1L
    
    // Configuration des simulations
    var shouldThrowOnGetAll: Boolean = false
    var shouldThrowOnGetById: Boolean = false
    var shouldThrowOnAdd: Boolean = false
    var shouldThrowOnUpdate: Boolean = false
    var shouldThrowOnDelete: Boolean = false
    
    // Simulation de latence réseau
    var simulatedDelay: Long = 0
    
    override fun getAllBatteries(): Flow<List<Battery>> {
        if (shouldThrowOnGetAll) {
            return flow { throw ErrorHandler.AppError.DatabaseError("Erreur simulée dans getAllBatteries") }
        }
        
        return batteriesFlow
    }
    
    override fun getBatteriesByStatus(status: BatteryStatus): Flow<List<Battery>> {
        if (shouldThrowOnGetAll) {
            return flow { throw ErrorHandler.AppError.DatabaseError("Erreur simulée dans getBatteriesByStatus") }
        }
        
        return batteriesFlow.map { list -> list.filter { it.status == status } }
    }
    
    override fun searchBatteries(query: String): Flow<List<Battery>> {
        if (shouldThrowOnGetAll) {
            return flow { throw ErrorHandler.AppError.DatabaseError("Erreur simulée dans searchBatteries") }
        }
        
        return batteriesFlow.map { list ->
            list.filter { battery ->
                battery.brand.contains(query, ignoreCase = true) ||
                battery.model.contains(query, ignoreCase = true) ||
                battery.serialNumber.contains(query, ignoreCase = true)
            }
        }
    }
    
    override suspend fun getBatteryById(id: Long): Battery? {
        if (shouldThrowOnGetById) {
            throw ErrorHandler.AppError.ResourceNotFoundError("Battery", id.toString())
        }
        
        simulateDelay()
        return batteries.find { it.id == id }
    }
    
    override fun getBatteryHistory(batteryId: Long): Flow<List<BatteryHistory>> {
        return flow { 
            simulateDelay()
            emit(batteryHistory[batteryId] ?: emptyList()) 
        }
    }
    
    override suspend fun addBattery(battery: Battery): Long {
        if (shouldThrowOnAdd) {
            throw ErrorHandler.AppError.DatabaseError("Erreur simulée dans addBattery")
        }
        
        simulateDelay()
        val id = idCounter++
        val newBattery = battery.copy(id = id)
        batteries.add(newBattery)
        updateBatteriesFlow()
        
        // Ajout d'un événement initial dans l'historique
        val initialEvent = BatteryHistory(
            id = 0,
            batteryId = id,
            timestamp = LocalDateTime.now(),
            eventType = BatteryEventType.STATUS_CHANGE,
            newStatus = battery.status,
            notes = "Batterie ajoutée au système"
        )
        batteryHistory[id] = mutableListOf(initialEvent)
        
        return id
    }
    
    override suspend fun updateBattery(battery: Battery) {
        if (shouldThrowOnUpdate) {
            throw ErrorHandler.AppError.DatabaseError("Erreur simulée dans updateBattery")
        }
        
        simulateDelay()
        val index = batteries.indexOfFirst { it.id == battery.id }
        if (index != -1) {
            batteries[index] = battery
            updateBatteriesFlow()
        } else {
            throw ErrorHandler.AppError.ResourceNotFoundError("Battery", battery.id.toString())
        }
    }
    
    override suspend fun deleteBattery(battery: Battery) {
        if (shouldThrowOnDelete) {
            throw ErrorHandler.AppError.DatabaseError("Erreur simulée dans deleteBattery")
        }
        
        simulateDelay()
        val removed = batteries.removeIf { it.id == battery.id }
        if (removed) {
            batteryHistory.remove(battery.id)
            updateBatteriesFlow()
        } else {
            throw ErrorHandler.AppError.ResourceNotFoundError("Battery", battery.id.toString())
        }
    }
    
    override suspend fun updateBatteryStatus(batteryId: Long, newStatus: BatteryStatus, notes: String) {
        simulateDelay()
        val battery = getBatteryById(batteryId) ?: 
            throw ErrorHandler.AppError.ResourceNotFoundError("Battery", batteryId.toString())
        
        val oldStatus = battery.status
        val updatedBattery = battery.copy(status = newStatus)
        updateBattery(updatedBattery)
        
        // Ajout d'un événement de changement de statut
        val statusChangeEvent = BatteryHistory(
            id = 0,
            batteryId = batteryId,
            timestamp = LocalDateTime.now(),
            eventType = BatteryEventType.STATUS_CHANGE,
            previousStatus = oldStatus,
            newStatus = newStatus,
            notes = notes.ifBlank { "Statut changé à ${newStatus.name}" }
        )
        batteryHistory.getOrPut(batteryId) { mutableListOf() }.add(statusChangeEvent)
    }
    
    override suspend fun recordVoltageReading(batteryId: Long, voltage: Float, notes: String) {
        simulateDelay()
        if (!batteries.any { it.id == batteryId }) {
            throw ErrorHandler.AppError.ResourceNotFoundError("Battery", batteryId.toString())
        }
        
        // Ajout d'un événement de lecture de tension
        val voltageEvent = BatteryHistory(
            id = 0,
            batteryId = batteryId,
            timestamp = LocalDateTime.now(),
            eventType = BatteryEventType.VOLTAGE_READING,
            voltage = voltage,
            notes = "Tension: $voltage V" + if (notes.isNotBlank()) " - $notes" else ""
        )
        batteryHistory.getOrPut(batteryId) { mutableListOf() }.add(voltageEvent)
    }
    
    override suspend fun addBatteryNote(batteryId: Long, note: String) {
        simulateDelay()
        if (!batteries.any { it.id == batteryId }) {
            throw ErrorHandler.AppError.ResourceNotFoundError("Battery", batteryId.toString())
        }
        
        // Ajout d'une note à l'historique
        val noteEvent = BatteryHistory(
            id = 0,
            batteryId = batteryId,
            timestamp = LocalDateTime.now(),
            eventType = BatteryEventType.NOTE_ADDED,
            notes = note
        )
        batteryHistory.getOrPut(batteryId) { mutableListOf() }.add(noteEvent)
    }
    
    override suspend fun recordMaintenance(batteryId: Long, description: String) {
        simulateDelay()
        if (!batteries.any { it.id == batteryId }) {
            throw ErrorHandler.AppError.ResourceNotFoundError("Battery", batteryId.toString())
        }
        
        // Ajout d'un événement de maintenance
        val maintenanceEvent = BatteryHistory(
            id = 0,
            batteryId = batteryId,
            timestamp = LocalDateTime.now(),
            eventType = BatteryEventType.MAINTENANCE,
            notes = description
        )
        batteryHistory.getOrPut(batteryId) { mutableListOf() }.add(maintenanceEvent)
    }
    
    override suspend fun getTotalBatteryCount(): Int {
        simulateDelay()
        return batteries.size
    }
    
    override suspend fun getBatteryCountByStatus(status: BatteryStatus): Int {
        simulateDelay()
        return batteries.count { it.status == status }
    }
    
    override suspend fun getAverageCycleCount(): Float {
        simulateDelay()
        if (batteries.isEmpty()) return 0f
        return batteries.sumOf { it.cycleCount }.toFloat() / batteries.size
    }
    
    // Export/Import
    override suspend fun getAllHistory(): List<BatteryHistory> {
        simulateDelay()
        return batteryHistory.values.flatten()
    }
    
    override suspend fun deleteAllBatteries() {
        simulateDelay()
        batteries.clear()
        batteryHistory.clear()
        updateBatteriesFlow()
    }
    
    override suspend fun getBatteryBySerialNumber(serialNumber: String): Battery? {
        simulateDelay()
        return batteries.find { it.serialNumber == serialNumber }
    }
    
    override suspend fun insertBattery(battery: Battery): Long {
        return addBattery(battery)
    }
    
    // Méthodes spécifiques aux tests
    
    fun addTestBattery(battery: Battery) {
        val newBattery = battery.copy(id = idCounter++)
        batteries.add(newBattery)
        updateBatteriesFlow()
    }
    
    fun clearAll() {
        batteries.clear()
        batteryHistory.clear()
        idCounter = 1L
        updateBatteriesFlow()
        
        // Réinitialiser les configurations
        shouldThrowOnGetAll = false
        shouldThrowOnGetById = false
        shouldThrowOnAdd = false
        shouldThrowOnUpdate = false
        shouldThrowOnDelete = false
        simulatedDelay = 0
    }
    
    private fun updateBatteriesFlow() {
        batteriesFlow.value = batteries.toList()
    }
    
    private suspend fun simulateDelay() {
        if (simulatedDelay > 0) {
            kotlinx.coroutines.delay(simulatedDelay)
        }
    }
}