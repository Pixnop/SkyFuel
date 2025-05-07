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
    val state by viewModel.state.collectAsState()
    
    // Action à effectuer après un scan
    LaunchedEffect(state.scanResult) {
        state.scanResult?.let { qrContent ->
            if (!hasScannedQrCode) {
                hasScannedQrCode = true
                onQrCodeScanned(qrContent)
            }
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
                .height(600.dp)
                .size(width = androidx.compose.ui.unit.Dp.Unspecified, height = 600.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(contentAlignment = Alignment.Center) {
                when {
                    // Affiche l'erreur si présente
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
                    
                    // Affiche l'indicateur de chargement pendant le traitement
                    state.isProcessingQrCode || hasScannedQrCode -> {
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
                    
                    // Affiche le message de succès
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
                                text = state.successMessage ?: "QR code traité avec succès",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = Color.Green,
                                modifier = Modifier.padding(top = 16.dp)
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
                                    // En cas de scan, traiter le QR code avec le nouveau service
                                    viewModel.processScannedQrCode(qrContent)
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