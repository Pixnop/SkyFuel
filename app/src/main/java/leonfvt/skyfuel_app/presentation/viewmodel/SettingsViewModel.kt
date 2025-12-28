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
import leonfvt.skyfuel_app.domain.usecase.GetAllBatteriesUseCase
import leonfvt.skyfuel_app.data.preferences.UserPreferencesRepository
import leonfvt.skyfuel_app.data.sync.FirebaseSyncService
import leonfvt.skyfuel_app.data.sync.SyncResult
import leonfvt.skyfuel_app.util.PdfExporter
import android.content.Context
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject

data class SettingsState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isExportingPdf: Boolean = false,
    val exportSuccess: ExportSuccessData? = null,
    val importSuccess: ImportSuccessData? = null,
    val pdfExportSuccess: File? = null,
    val error: String? = null,
    // Notification preferences
    val alertsEnabled: Boolean = true,
    val chargeRemindersEnabled: Boolean = true,
    val maintenanceRemindersEnabled: Boolean = true,
    val lowBatteryWarningsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    // Firebase sync state
    val syncEnabled: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncTime: Long? = null,
    val syncError: String? = null,
    val userEmail: String? = null,
    val syncResult: String? = null
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
    private val getAllBatteriesUseCase: GetAllBatteriesUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val pdfExporter: PdfExporter,
    private val firebaseSyncService: FirebaseSyncService
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
        
        // Observer l'état de synchronisation Firebase
        viewModelScope.launch {
            firebaseSyncService.syncState.collect { syncState ->
                _state.update { state ->
                    state.copy(
                        syncEnabled = syncState.isEnabled,
                        isAuthenticated = syncState.isAuthenticated,
                        isSyncing = syncState.isSyncing,
                        lastSyncTime = syncState.lastSyncTime,
                        syncError = syncState.error,
                        userEmail = syncState.userEmail
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
    
    fun exportToPdf(context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(isExportingPdf = true, error = null, pdfExportSuccess = null) }
            
            try {
                val batteries = getAllBatteriesUseCase().first()
                val pdfFile = pdfExporter.exportBatteriesToPdf(context, batteries)
                
                if (pdfFile != null) {
                    _state.update { it.copy(isExportingPdf = false, pdfExportSuccess = pdfFile) }
                } else {
                    _state.update { it.copy(isExportingPdf = false, error = "Erreur lors de la création du PDF") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isExportingPdf = false, error = "Erreur: ${e.message}") }
            }
        }
    }
    
    // ============ Méthodes Firebase ============
    
    fun signInAnonymously() {
        viewModelScope.launch {
            val success = firebaseSyncService.signInAnonymously()
            if (success) {
                _state.update { it.copy(syncResult = "Connecté en mode anonyme") }
            }
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            firebaseSyncService.signOut()
            _state.update { it.copy(syncResult = "Déconnecté") }
        }
    }
    
    fun toggleSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            firebaseSyncService.setSyncEnabled(enabled)
        }
    }
    
    fun syncNow() {
        viewModelScope.launch {
            val batteries = getAllBatteriesUseCase().first()
            when (val result = firebaseSyncService.syncBatteries(batteries)) {
                is SyncResult.Success -> {
                    _state.update { 
                        it.copy(syncResult = "Synchronisé: ${result.uploadedCount} batteries envoyées") 
                    }
                }
                is SyncResult.Error -> {
                    _state.update { it.copy(syncError = result.message) }
                }
                is SyncResult.NotAuthenticated -> {
                    _state.update { it.copy(syncError = "Non authentifié") }
                }
                is SyncResult.Disabled -> {
                    _state.update { it.copy(syncError = "Synchronisation désactivée") }
                }
            }
        }
    }
    
    fun clearSyncError() {
        firebaseSyncService.clearError()
        _state.update { it.copy(syncError = null) }
    }
    
    fun clearSyncResult() {
        _state.update { it.copy(syncResult = null) }
    }
    
    fun clearExportSuccess() {
        _state.update { it.copy(exportSuccess = null) }
    }
    
    fun clearImportSuccess() {
        _state.update { it.copy(importSuccess = null) }
    }
    
    fun clearPdfExportSuccess() {
        _state.update { it.copy(pdfExportSuccess = null) }
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
