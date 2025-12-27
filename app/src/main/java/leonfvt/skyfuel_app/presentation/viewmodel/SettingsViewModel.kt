package leonfvt.skyfuel_app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import leonfvt.skyfuel_app.domain.service.ExportFormat
import leonfvt.skyfuel_app.domain.usecase.ExportDataUseCase
import leonfvt.skyfuel_app.domain.usecase.ExportResult
import leonfvt.skyfuel_app.domain.usecase.ImportDataUseCase
import leonfvt.skyfuel_app.domain.usecase.ImportDataResult
import leonfvt.skyfuel_app.data.preferences.UserPreferencesRepository
import javax.inject.Inject

data class SettingsState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val exportSuccess: ExportSuccessData? = null,
    val importSuccess: ImportSuccessData? = null,
    val error: String? = null,
    // Notification preferences
    val alertsEnabled: Boolean = true,
    val chargeRemindersEnabled: Boolean = true,
    val maintenanceRemindersEnabled: Boolean = true,
    val lowBatteryWarningsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = true
)

data class ExportSuccessData(
    val content: String,
    val fileName: String,
    val batteryCount: Int
)

data class ImportSuccessData(
    val importedCount: Int,
    val skippedCount: Int,
    val totalInFile: Int
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exportDataUseCase: ExportDataUseCase,
    private val importDataUseCase: ImportDataUseCase,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()
    
    init {
        // Observer les préférences de notification
        viewModelScope.launch {
            userPreferencesRepository.notificationPreferences.collect { prefs ->
                _state.update { state ->
                    state.copy(
                        alertsEnabled = prefs.alertsEnabled,
                        chargeRemindersEnabled = prefs.chargeRemindersEnabled,
                        maintenanceRemindersEnabled = prefs.maintenanceRemindersEnabled,
                        lowBatteryWarningsEnabled = prefs.lowBatteryWarningsEnabled,
                        vibrationEnabled = prefs.vibrationEnabled,
                        soundEnabled = prefs.soundEnabled
                    )
                }
            }
        }
    }
    
    fun exportData(format: ExportFormat, includeHistory: Boolean = true) {
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true, error = null, exportSuccess = null) }
            
            when (val result = exportDataUseCase(format, includeHistory)) {
                is ExportResult.Success -> {
                    _state.update {
                        it.copy(
                            isExporting = false,
                            exportSuccess = ExportSuccessData(
                                content = result.content,
                                fileName = result.fileName,
                                batteryCount = result.batteryCount
                            )
                        )
                    }
                }
                is ExportResult.Error -> {
                    _state.update {
                        it.copy(isExporting = false, error = result.message)
                    }
                }
            }
        }
    }
    
    fun importData(content: String, format: ExportFormat, replaceExisting: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(isImporting = true, error = null, importSuccess = null) }
            
            when (val result = importDataUseCase(content, format, replaceExisting)) {
                is ImportDataResult.Success -> {
                    _state.update {
                        it.copy(
                            isImporting = false,
                            importSuccess = ImportSuccessData(
                                importedCount = result.importedCount,
                                skippedCount = result.skippedCount,
                                totalInFile = result.totalInFile
                            )
                        )
                    }
                }
                is ImportDataResult.Error -> {
                    _state.update {
                        it.copy(isImporting = false, error = result.message)
                    }
                }
            }
        }
    }
    
    // ============ Méthodes pour les notifications ============
    
    fun setAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAlertsEnabled(enabled)
        }
    }
    
    fun setChargeRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setChargeRemindersEnabled(enabled)
        }
    }
    
    fun setMaintenanceRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setMaintenanceRemindersEnabled(enabled)
        }
    }
    
    fun setLowBatteryWarningsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setLowBatteryWarningsEnabled(enabled)
        }
    }
    
    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setVibrationEnabled(enabled)
        }
    }
    
    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setSoundEnabled(enabled)
        }
    }
    
    fun clearExportSuccess() {
        _state.update { it.copy(exportSuccess = null) }
    }
    
    fun clearImportSuccess() {
        _state.update { it.copy(importSuccess = null) }
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
