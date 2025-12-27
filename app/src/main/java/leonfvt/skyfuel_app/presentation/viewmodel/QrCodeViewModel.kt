package leonfvt.skyfuel_app.presentation.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.QrCodeData
import leonfvt.skyfuel_app.domain.service.QrCodeService
import leonfvt.skyfuel_app.domain.service.QrScanResult
import leonfvt.skyfuel_app.util.UseCaseExtensions.executeUseCase
import javax.inject.Inject

/**
 * État interne pour la gestion des QR codes
 */
data class QrCodeState(
    val isProcessingQrCode: Boolean = false,
    val scanResult: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    // État pour l'import de batterie
    val batteryToImport: Battery? = null,
    val importAlreadyExists: Boolean = false,
    val showImportDialog: Boolean = false
)

/**
 * ViewModel pour la gestion des QR codes
 * Fournit une interface entre l'UI et le service QrCodeService
 */
@HiltViewModel
class QrCodeViewModel @Inject constructor(
    private val qrCodeService: QrCodeService
) : ViewModel() {
    
    // État interne mutable
    private val _state = MutableStateFlow(QrCodeState())
    
    // État exposé pour l'UI
    val state: StateFlow<QrCodeState> = _state.asStateFlow()
    
    /**
     * Obtient le bitmap du QR code pour une batterie
     * @param battery Batterie pour laquelle générer le QR code
     * @param size Taille du QR code en pixels
     * @return Bitmap contenant le QR code
     */
    fun getBatteryQrCodeBitmap(battery: Battery, size: Int = 512): Bitmap? {
        return qrCodeService.generateBatteryQrCode(battery, size)
    }
    
    /**
     * Partage le QR code d'une batterie
     * @param battery Batterie dont le QR code doit être partagé
     */
    fun shareBatteryQrCode(battery: Battery) {
        qrCodeService.shareBatteryQrCode(battery)
    }
    
    /**
     * Enregistre le QR code d'une batterie dans la galerie
     * @param battery Batterie dont le QR code doit être enregistré
     */
    fun saveBatteryQrCodeToGallery(battery: Battery) {
        qrCodeService.saveBatteryQrCodeToGallery(battery)
    }
    
    /**
     * Traite un QR code scanné
     * @param qrContent Contenu du QR code scanné
     */
    fun processScannedQrCode(qrContent: String) {
        _state.update { it.copy(isProcessingQrCode = true, scanResult = null, errorMessage = null, successMessage = null) }
        
        executeUseCase(
            useCase = { qrCodeService.processScannedQrCode(qrContent) },
            onError = { error ->
                _state.update { it.copy(
                    isProcessingQrCode = false,
                    errorMessage = error.message ?: "Erreur lors du traitement du QR code"
                ) }
            },
            onSuccess = { result ->
                val (success, message) = result
                
                if (success) {
                    _state.update { it.copy(
                        isProcessingQrCode = false,
                        successMessage = message,
                        scanResult = qrContent
                    ) }
                } else {
                    _state.update { it.copy(
                        isProcessingQrCode = false,
                        errorMessage = message
                    ) }
                }
            }
        )
    }
    
    /**
     * Réinitialise l'état du scan
     */
    fun resetScanState() {
        _state.update { it.copy(
            isProcessingQrCode = false,
            scanResult = null,
            errorMessage = null,
            successMessage = null
        ) }
    }
    
    /**
     * Récupère une batterie à partir d'un QR code
     * @param qrContent Contenu du QR code scanné
     * @param onBatteryFound Callback appelé lorsque la batterie est trouvée
     * @param onBatteryNotFound Callback appelé lorsque la batterie n'est pas trouvée
     */
    fun getBatteryFromQrCode(
        qrContent: String,
        onBatteryFound: (Battery) -> Unit,
        onBatteryNotFound: (String) -> Unit
    ) {
        _state.update { it.copy(isProcessingQrCode = true) }
        
        executeUseCase(
            useCase = { qrCodeService.getBatteryFromQrCode(qrContent) },
            onError = { error ->
                _state.update { it.copy(isProcessingQrCode = false) }
                onBatteryNotFound(error.message ?: "Erreur lors de la récupération de la batterie")
            },
            onSuccess = { battery ->
                _state.update { it.copy(isProcessingQrCode = false) }
                
                if (battery != null) {
                    onBatteryFound(battery)
                } else {
                    onBatteryNotFound("Batterie non trouvée pour ce QR code")
                }
            }
        )
    }
    
    // ==================== FONCTIONNALITÉS DE PARTAGE ====================
    
    /**
     * Obtient le bitmap du QR code de partage complet pour une batterie
     * @param battery Batterie pour laquelle générer le QR code
     * @param size Taille du QR code en pixels
     * @return Bitmap contenant le QR code
     */
    fun getShareBatteryQrCodeBitmap(battery: Battery, size: Int = 512): Bitmap? {
        return qrCodeService.generateShareQrCode(battery, size)
    }
    
    /**
     * Partage le QR code complet d'une batterie (avec toutes les données)
     * @param battery Batterie à partager
     */
    fun shareFullBatteryQrCode(battery: Battery) {
        qrCodeService.shareFullBatteryQrCode(battery)
    }
    
    /**
     * Traite un QR code scanné avec le nouveau système amélioré
     * Gère les deux types de QR codes: référence et partage
     */
    fun processScannedQrCodeEnhanced(
        qrContent: String,
        onBatteryFound: (Battery) -> Unit,
        onBatteryNotFound: (Long) -> Unit,
        onShareableBattery: (Battery, Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        _state.update { it.copy(isProcessingQrCode = true, errorMessage = null) }
        
        executeUseCase(
            useCase = { qrCodeService.processScannedQrCodeEnhanced(qrContent) },
            onError = { error ->
                _state.update { it.copy(isProcessingQrCode = false) }
                onError(error.message ?: "Erreur lors du traitement du QR code")
            },
            onSuccess = { result ->
                _state.update { it.copy(isProcessingQrCode = false) }
                
                when (result) {
                    is QrScanResult.BatteryFound -> onBatteryFound(result.battery)
                    is QrScanResult.BatteryNotFound -> onBatteryNotFound(result.batteryId)
                    is QrScanResult.ShareableBattery -> {
                        _state.update { it.copy(
                            batteryToImport = result.battery,
                            importAlreadyExists = result.alreadyExists,
                            showImportDialog = true
                        ) }
                        onShareableBattery(result.battery, result.alreadyExists)
                    }
                    is QrScanResult.Error -> onError(result.message)
                }
            }
        )
    }
    
    /**
     * Importe une batterie depuis un QR code de partage
     */
    fun importBatteryFromQrCode(qrContent: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        _state.update { it.copy(isProcessingQrCode = true) }
        
        executeUseCase(
            useCase = { qrCodeService.importBatteryFromQrCode(qrContent) },
            onError = { error ->
                _state.update { it.copy(isProcessingQrCode = false, showImportDialog = false) }
                onError(error.message ?: "Erreur lors de l'import")
            },
            onSuccess = { (success, message) ->
                _state.update { it.copy(
                    isProcessingQrCode = false,
                    showImportDialog = false,
                    batteryToImport = null,
                    successMessage = if (success) message else null,
                    errorMessage = if (!success) message else null
                ) }
                if (success) onSuccess(message) else onError(message)
            }
        )
    }
    
    /**
     * Confirme l'import de la batterie affichée dans le dialogue
     */
    fun confirmImport(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val battery = _state.value.batteryToImport ?: return
        
        _state.update { it.copy(isProcessingQrCode = true) }
        
        viewModelScope.launch {
            try {
                // Créer le QR code data pour cette batterie et le ré-encoder
                val qrData = QrCodeData.forShareBattery(battery)
                val qrContent = qrData.encode()
                
                val (success, message) = qrCodeService.importBatteryFromQrCode(qrContent)
                
                _state.update { it.copy(
                    isProcessingQrCode = false,
                    showImportDialog = false,
                    batteryToImport = null,
                    successMessage = if (success) message else null
                ) }
                
                if (success) onSuccess(message) else onError(message)
                
            } catch (e: Exception) {
                _state.update { it.copy(isProcessingQrCode = false) }
                onError(e.message ?: "Erreur lors de l'import")
            }
        }
    }
    
    /**
     * Ferme le dialogue d'import
     */
    fun dismissImportDialog() {
        _state.update { it.copy(
            showImportDialog = false,
            batteryToImport = null,
            importAlreadyExists = false
        ) }
    }
    
    /**
     * Vérifie si un QR code est un QR de partage
     */
    fun isShareQrCode(qrContent: String): Boolean {
        return qrCodeService.isShareQrCode(qrContent)
    }
}