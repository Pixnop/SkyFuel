package leonfvt.skyfuel_app.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.presentation.viewmodel.QrCodeViewModel

/**
 * Boîte de dialogue permettant de scanner un QR code
 * Utilise le nouveau système normalisé de gestion des QR codes
 * 
 * @param onDismiss Appelé lorsque l'utilisateur ferme la boîte de dialogue
 * @param onQrCodeScanned Appelé lorsqu'un QR code est scanné avec succès
 * @param viewModel ViewModel pour la gestion des QR codes (injecté automatiquement)
 */
@Composable
fun QrScannerDialog(
    onDismiss: () -> Unit,
    onQrCodeScanned: (String) -> Unit,
    viewModel: QrCodeViewModel = hiltViewModel()
) {
    var hasScannedQrCode by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    
    // Réinitialisation de l'état lorsque la boîte de dialogue est fermée
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetScanState()
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(contentAlignment = Alignment.Center) {
                when {
                    // Affiche l'indicateur de chargement pendant le traitement
                    isProcessing -> {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Traitement du QR code...",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(top = 80.dp)
                            )
                        }
                    }
                    
                    // Affiche le scanner QR par défaut
                    else -> {
                        CameraPermission(
                            onPermissionGranted = { /* Permission accordée */ },
                            permissionNotAvailableContent = {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Permission d'accès à la caméra refusée",
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(top = 16.dp)
                                    )
                                }
                            }
                        ) {
                            QrCodeScanner(
                                onQrCodeScanned = { qrContent ->
                                    if (!hasScannedQrCode) {
                                        hasScannedQrCode = true
                                        isProcessing = true
                                        android.util.Log.d("QrScannerDialog", "QR scanned, calling callback: $qrContent")
                                        // Appeler directement le callback - la navigation sera gérée par HomeScreen
                                        onQrCodeScanned(qrContent)
                                    }
                                }
                            )
                        }
                    }
                }
                
                // Bouton de fermeture en haut à droite
                IconButton(
                    onClick = {
                        viewModel.resetScanState()
                        onDismiss()
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}


/**
 * Boîte de dialogue de scan avec support de l'import de batterie
 * Détecte automatiquement si le QR code est un partage et propose l'import
 */
@Composable
fun QrScannerWithImportDialog(
    onDismiss: () -> Unit,
    onBatteryFound: (Long) -> Unit, // ID de la batterie trouvée
    onBatteryImported: (String) -> Unit, // Message de succès
    viewModel: QrCodeViewModel = hiltViewModel()
) {
    var hasProcessedQrCode by remember { mutableStateOf(false) }
    val state by viewModel.state.collectAsState()
    
    // Gérer le dialogue d'import
    if (state.showImportDialog && state.batteryToImport != null) {
        ImportBatteryConfirmDialog(
            battery = state.batteryToImport!!,
            alreadyExists = state.importAlreadyExists,
            onConfirm = {
                viewModel.confirmImport(
                    onSuccess = { message ->
                        onBatteryImported(message)
                        onDismiss()
                    },
                    onError = { /* L'erreur sera affichée dans l'état */ }
                )
            },
            onDismiss = {
                viewModel.dismissImportDialog()
            }
        )
    }
    
    // Réinitialisation lors de la fermeture
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetScanState()
            viewModel.dismissImportDialog()
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(contentAlignment = Alignment.Center) {
                when {
                    // Erreur
                    state.errorMessage != null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = state.errorMessage ?: "Une erreur est survenue",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                    
                    // Chargement
                    state.isProcessingQrCode || hasProcessedQrCode -> {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Traitement...",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(top = 80.dp)
                            )
                        }
                    }
                    
                    // Succès
                    state.successMessage != null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = state.successMessage ?: "Succès",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = Color.Green,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                    
                    // Scanner par défaut
                    else -> {
                        CameraPermission(
                            onPermissionGranted = { },
                            permissionNotAvailableContent = {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Permission caméra refusée",
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(top = 16.dp)
                                    )
                                }
                            }
                        ) {
                            QrCodeScanner(
                                onQrCodeScanned = { qrContent ->
                                    hasProcessedQrCode = true
                                    viewModel.processScannedQrCodeEnhanced(
                                        qrContent = qrContent,
                                        onBatteryFound = { battery ->
                                            onBatteryFound(battery.id)
                                            onDismiss()
                                        },
                                        onBatteryNotFound = { _ ->
                                            // La batterie n'existe pas localement
                                            hasProcessedQrCode = false
                                        },
                                        onShareableBattery = { _, _ ->
                                            // Le dialogue d'import sera affiché automatiquement
                                            hasProcessedQrCode = false
                                        },
                                        onError = { _ ->
                                            hasProcessedQrCode = false
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
                
                // Bouton fermer
                IconButton(
                    onClick = {
                        viewModel.resetScanState()
                        viewModel.dismissImportDialog()
                        onDismiss()
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
