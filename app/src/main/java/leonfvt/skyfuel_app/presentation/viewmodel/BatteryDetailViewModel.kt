package leonfvt.skyfuel_app.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import leonfvt.skyfuel_app.data.local.dao.BatteryDao
import leonfvt.skyfuel_app.data.local.dao.ChargeReminderDao
import leonfvt.skyfuel_app.data.local.entity.ChargeReminderEntity
import leonfvt.skyfuel_app.data.repository.CategoryRepository
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.ChargeReminder
import leonfvt.skyfuel_app.domain.model.ReminderType
import leonfvt.skyfuel_app.domain.usecase.AddBatteryNoteUseCase
import leonfvt.skyfuel_app.domain.usecase.DeleteBatteryUseCase
import leonfvt.skyfuel_app.domain.usecase.GetBatteryDetailUseCase
import leonfvt.skyfuel_app.domain.usecase.GetBatteryHistoryUseCase
import leonfvt.skyfuel_app.domain.usecase.RecordMaintenanceUseCase
import leonfvt.skyfuel_app.domain.usecase.RecordVoltageReadingUseCase
import leonfvt.skyfuel_app.domain.usecase.UpdateBatteryStatusUseCase
import leonfvt.skyfuel_app.presentation.viewmodel.state.BatteryDetailEvent
import leonfvt.skyfuel_app.presentation.viewmodel.state.BatteryDetailState
import leonfvt.skyfuel_app.util.ErrorHandler
import leonfvt.skyfuel_app.util.UseCaseExtensions.executeFlowUseCase
import leonfvt.skyfuel_app.util.UseCaseExtensions.executeUseCase
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

/**
 * ViewModel pour l'écran de détails d'une batterie
 * Gère les détails, catégories et rappels de charge
 */
@HiltViewModel
class BatteryDetailViewModel @Inject constructor(
    private val getBatteryDetailUseCase: GetBatteryDetailUseCase,
    private val getBatteryHistoryUseCase: GetBatteryHistoryUseCase,
    private val updateBatteryStatusUseCase: UpdateBatteryStatusUseCase,
    private val recordVoltageReadingUseCase: RecordVoltageReadingUseCase,
    private val addBatteryNoteUseCase: AddBatteryNoteUseCase,
    private val recordMaintenanceUseCase: RecordMaintenanceUseCase,
    private val deleteBatteryUseCase: DeleteBatteryUseCase,
    private val categoryRepository: CategoryRepository,
    private val reminderDao: ChargeReminderDao,
    private val batteryDao: BatteryDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Récupération de l'ID de la batterie depuis les arguments de navigation
    private val batteryId: Long = savedStateHandle.get<Long>("batteryId") ?: 0

    // État interne mutable
    private val _state = MutableStateFlow(BatteryDetailState(isLoading = true))

    // État exposé pour l'UI
    val state: StateFlow<BatteryDetailState> = _state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BatteryDetailState(isLoading = true)
    )

    // Pour les événements de navigation
    private val _navigationEvent = MutableStateFlow<String?>(null)
    val navigationEvent: StateFlow<String?> = _navigationEvent

    init {
        loadBatteryDetail()
        loadBatteryHistory()
        loadCategories()
        loadReminders()
    }

    /**
     * Gère les événements de l'UI
     */
    fun onEvent(event: BatteryDetailEvent) {
        when (event) {
            is BatteryDetailEvent.UpdateStatus -> {
                updateBatteryStatus(event.newStatus, event.notes)
            }
            is BatteryDetailEvent.RecordVoltage -> {
                recordVoltage(event.voltage, event.notes)
            }
            is BatteryDetailEvent.AddNote -> {
                addNote(event.note)
            }
            is BatteryDetailEvent.RecordMaintenance -> {
                recordMaintenance(event.description)
            }
            is BatteryDetailEvent.DeleteBattery -> {
                deleteBattery()
            }
            is BatteryDetailEvent.NavigateBack -> {
                _navigationEvent.value = "back"
            }
            is BatteryDetailEvent.ClearError -> {
                clearError()
            }
            // Catégories
            is BatteryDetailEvent.ShowCategorySelector -> {
                _state.update { it.copy(showCategorySelector = true) }
            }
            is BatteryDetailEvent.HideCategorySelector -> {
                _state.update { it.copy(showCategorySelector = false) }
            }
            is BatteryDetailEvent.UpdateCategories -> {
                updateBatteryCategories(event.categoryIds)
            }
            // Rappels
            is BatteryDetailEvent.ShowAddReminder -> {
                _state.update { it.copy(showReminderDialog = true, editingReminder = null) }
            }
            is BatteryDetailEvent.ShowEditReminder -> {
                _state.update { it.copy(showReminderDialog = true, editingReminder = event.reminder) }
            }
            is BatteryDetailEvent.HideReminderDialog -> {
                _state.update { it.copy(showReminderDialog = false, editingReminder = null) }
            }
            is BatteryDetailEvent.SaveReminder -> {
                saveReminder(event.title, event.hour, event.minute, event.daysOfWeek, event.reminderType, event.notes)
            }
            is BatteryDetailEvent.ToggleReminder -> {
                toggleReminder(event.reminder)
            }
            is BatteryDetailEvent.DeleteReminder -> {
                deleteReminder(event.reminder)
            }
            // Photo
            is BatteryDetailEvent.UpdatePhoto -> {
                updatePhoto(event.photoPath)
            }
            is BatteryDetailEvent.RemovePhoto -> {
                updatePhoto(null)
            }
        }
    }

    // ========== Photo ==========

    private fun updatePhoto(photoPath: String?) {
        viewModelScope.launch {
            try {
                batteryDao.updateBatteryPhoto(batteryId, photoPath)
                loadBatteryDetail()
            } catch (e: Exception) {
                _state.update { it.copy(error = "Erreur lors de la mise à jour de la photo") }
            }
        }
    }

    // ========== Catégories ==========

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getCategoriesForBattery(batteryId).collect { categories ->
                _state.update { it.copy(batteryCategories = categories) }
            }
        }
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                _state.update { it.copy(allCategories = categories) }
            }
        }
    }

    private fun updateBatteryCategories(categoryIds: List<Long>) {
        viewModelScope.launch {
            try {
                categoryRepository.updateBatteryCategories(batteryId, categoryIds)
                _state.update { it.copy(showCategorySelector = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Erreur lors de la mise à jour des catégories") }
            }
        }
    }

    // ========== Rappels ==========

    private fun loadReminders() {
        viewModelScope.launch {
            reminderDao.getRemindersByBattery(batteryId).collect { entities ->
                _state.update { it.copy(reminders = entities.map { e -> e.toDomainModel() }) }
            }
        }
    }

    private fun saveReminder(
        title: String,
        hour: Int,
        minute: Int,
        daysOfWeek: Set<DayOfWeek>,
        reminderType: ReminderType,
        notes: String
    ) {
        viewModelScope.launch {
            val reminder = ChargeReminder(
                id = _state.value.editingReminder?.id ?: 0,
                batteryId = batteryId,
                title = title,
                time = LocalTime.of(hour, minute),
                daysOfWeek = daysOfWeek,
                isEnabled = true,
                reminderType = reminderType,
                notes = notes
            )
            val entity = ChargeReminderEntity.fromDomainModel(reminder)
            if (_state.value.editingReminder != null) {
                reminderDao.updateReminder(entity)
            } else {
                reminderDao.insertReminder(entity)
            }
            _state.update { it.copy(showReminderDialog = false, editingReminder = null) }
        }
    }

    private fun toggleReminder(reminder: ChargeReminder) {
        viewModelScope.launch {
            reminderDao.setReminderEnabled(reminder.id, !reminder.isEnabled)
        }
    }

    private fun deleteReminder(reminder: ChargeReminder) {
        viewModelScope.launch {
            reminderDao.deleteReminder(ChargeReminderEntity.fromDomainModel(reminder))
        }
    }
    
    /**
     * Charge les détails de la batterie
     */
    private fun loadBatteryDetail() {
        executeUseCase(
            useCase = { getBatteryDetailUseCase(batteryId) },
            onStart = { _state.update { it.copy(isLoading = true) } },
            onError = { error ->
                val errorMessage = ErrorHandler.getUserMessage(error)
                ErrorHandler.logError(error, "Erreur lors du chargement des détails de la batterie $batteryId")
                _state.update { it.copy(isLoading = false, error = errorMessage) }
            },
            onSuccess = { battery ->
                _state.update {
                    it.copy(
                        battery = battery,
                        isLoading = false,
                        error = if (battery == null) "Batterie non trouvée" else null
                    )
                }
            }
        )
    }
    
    /**
     * Charge l'historique de la batterie
     */
    private fun loadBatteryHistory() {
        executeFlowUseCase(
            useCase = { getBatteryHistoryUseCase(batteryId) },
            onStart = { _state.update { it.copy(isHistoryLoading = true) } },
            onError = { error ->
                val errorMessage = ErrorHandler.getUserMessage(error)
                ErrorHandler.logError(error, "Erreur lors du chargement de l'historique de la batterie $batteryId")
                _state.update { it.copy(isHistoryLoading = false, error = errorMessage) }
            },
            onEach = { history ->
                _state.update {
                    it.copy(
                        batteryHistory = history,
                        isHistoryLoading = false
                    )
                }
            }
        )
    }
    
    /**
     * Met à jour le statut de la batterie
     */
    private fun updateBatteryStatus(newStatus: leonfvt.skyfuel_app.domain.model.BatteryStatus, notes: String) {
        executeUseCase(
            useCase = { updateBatteryStatusUseCase(batteryId, newStatus, notes) },
            onStart = { _state.update { it.copy(isActionInProgress = true) } },
            onError = { error ->
                val errorMessage = ErrorHandler.getUserMessage(error)
                ErrorHandler.logError(error, "Erreur lors de la mise à jour du statut de la batterie $batteryId")
                _state.update { it.copy(isActionInProgress = false, error = errorMessage) }
            },
            onSuccess = { 
                _state.update { it.copy(isActionInProgress = false) }
                // Recharger les détails de la batterie pour refléter les changements
                loadBatteryDetail()
                loadBatteryHistory()
            }
        )
    }
    
    /**
     * Enregistre une mesure de tension
     */
    private fun recordVoltage(voltage: Float, notes: String) {
        executeUseCase(
            useCase = { recordVoltageReadingUseCase(batteryId, voltage, notes) },
            onStart = { _state.update { it.copy(isActionInProgress = true) } },
            onError = { error ->
                val errorMessage = ErrorHandler.getUserMessage(error)
                ErrorHandler.logError(error, "Erreur lors de l'enregistrement de tension pour la batterie $batteryId")
                _state.update { it.copy(isActionInProgress = false, error = errorMessage) }
            },
            onSuccess = {
                _state.update { it.copy(isActionInProgress = false) }
                // Recharger l'historique pour afficher la nouvelle mesure
                loadBatteryHistory()
            }
        )
    }
    
    /**
     * Ajoute une note à la batterie
     */
    private fun addNote(note: String) {
        executeUseCase(
            useCase = { addBatteryNoteUseCase(batteryId, note) },
            onStart = { _state.update { it.copy(isActionInProgress = true) } },
            onError = { error ->
                val errorMessage = ErrorHandler.getUserMessage(error)
                ErrorHandler.logError(error, "Erreur lors de l'ajout de note pour la batterie $batteryId")
                _state.update { it.copy(isActionInProgress = false, error = errorMessage) }
            },
            onSuccess = {
                _state.update { it.copy(isActionInProgress = false) }
                // Recharger l'historique pour afficher la nouvelle note
                loadBatteryHistory()
            }
        )
    }

    /**
     * Enregistre une maintenance sur la batterie
     */
    private fun recordMaintenance(description: String) {
        executeUseCase(
            useCase = { recordMaintenanceUseCase(batteryId, description) },
            onStart = { _state.update { it.copy(isActionInProgress = true) } },
            onError = { error ->
                val errorMessage = ErrorHandler.getUserMessage(error)
                ErrorHandler.logError(error, "Erreur lors de l'enregistrement de maintenance pour la batterie $batteryId")
                _state.update { it.copy(isActionInProgress = false, error = errorMessage) }
            },
            onSuccess = {
                _state.update { it.copy(isActionInProgress = false) }
                // Recharger l'historique pour afficher la maintenance
                loadBatteryHistory()
            }
        )
    }

    /**
     * Supprime la batterie
     */
    private fun deleteBattery() {
        val battery = state.value.battery ?: return
        
        executeUseCase(
            useCase = { deleteBatteryUseCase(battery) },
            onStart = { _state.update { it.copy(isActionInProgress = true) } },
            onError = { error ->
                val errorMessage = ErrorHandler.getUserMessage(error)
                ErrorHandler.logError(error, "Erreur lors de la suppression de la batterie $batteryId")
                _state.update { it.copy(isActionInProgress = false, error = errorMessage) }
            },
            onSuccess = {
                _state.update { it.copy(isActionInProgress = false) }
                // Navigation vers l'écran précédent
                _navigationEvent.value = "back"
            }
        )
    }
    
    /**
     * Réinitialise l'événement de navigation après l'avoir consommé
     */
    fun onNavigationEventConsumed() {
        _navigationEvent.value = null
    }
    
    /**
     * Réinitialise l'erreur après l'avoir affichée
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}