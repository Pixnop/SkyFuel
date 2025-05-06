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
import leonfvt.skyfuel_app.util.UseCaseExtensions.executeUseCase
import javax.inject.Inject

/**
 * État interne pour la gestion des QR codes
 */
data class QrCodeState(
    val isProcessingQrCode: Boolean = false,
    val scanResult: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
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
}