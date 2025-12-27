package leonfvt.skyfuel_app.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import leonfvt.skyfuel_app.data.local.dao.ChargeReminderDao
import leonfvt.skyfuel_app.data.local.entity.ChargeReminderEntity
import leonfvt.skyfuel_app.domain.model.ChargeReminder
import leonfvt.skyfuel_app.domain.model.ReminderType
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

data class ReminderState(
    val reminders: List<ChargeReminder> = emptyList(),
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val editingReminder: ChargeReminder? = null
)

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val reminderDao: ChargeReminderDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val batteryId: Long = savedStateHandle["batteryId"] ?: 0L
    
    private val _state = MutableStateFlow(ReminderState())
    val state: StateFlow<ReminderState> = _state.asStateFlow()
    
    init {
        loadReminders()
    }
    
    private fun loadReminders() {
        viewModelScope.launch {
            reminderDao.getRemindersByBattery(batteryId).collect { entities ->
                _state.update {
                    it.copy(
                        reminders = entities.map { entity -> entity.toDomainModel() },
                        isLoading = false
                    )
                }
            }
        }
    }
    
    fun showAddDialog() {
        _state.update { it.copy(showAddDialog = true, editingReminder = null) }
    }
    
    fun showEditDialog(reminder: ChargeReminder) {
        _state.update { it.copy(showAddDialog = true, editingReminder = reminder) }
    }
    
    fun dismissDialog() {
        _state.update { it.copy(showAddDialog = false, editingReminder = null) }
    }
    
    fun saveReminder(
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
            
            dismissDialog()
        }
    }
    
    fun toggleReminder(reminder: ChargeReminder) {
        viewModelScope.launch {
            reminderDao.setReminderEnabled(reminder.id, !reminder.isEnabled)
        }
    }
    
    fun deleteReminder(reminder: ChargeReminder) {
        viewModelScope.launch {
            val entity = ChargeReminderEntity.fromDomainModel(reminder)
            reminderDao.deleteReminder(entity)
        }
    }
}
