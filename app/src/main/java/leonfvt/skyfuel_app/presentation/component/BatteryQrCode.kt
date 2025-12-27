package leonfvt.skyfuel_app.presentation.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.QrCodeData
import leonfvt.skyfuel_app.util.QrCodeGenerator
import leonfvt.skyfuel_app.presentation.viewmodel.QrCodeViewModel
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import java.time.format.DateTimeFormatter

/**
 * Composant qui affiche le QR Code d'une batterie dans une boîte de dialogue
 * Utilise le nouveau système normalisé de gestion des QR codes
 * 
 * @param battery La batterie pour laquelle générer le QR code
 * @param onDismiss Callback appelé lorsque l'utilisateur ferme la boîte de dialogue
 * @param onShare Callback pour partager le QR code
 * @param onSave Callback pour enregistrer le QR code sur l'appareil
 * @param viewModel ViewModel pour la gestion des QR codes (injecté automatiquement)
 */
@Composable
fun BatteryQrCodeDialog(
    battery: Battery,
    onDismiss: () -> Unit,
    onShare: (Bitmap) -> Unit,
    onSave: (Bitmap) -> Unit,
    viewModel: QrCodeViewModel = hiltViewModel()
) {
    // Générer les données du QR code avec le nouveau format standardisé
    val qrData = QrCodeData.forBattery(
        batteryId = battery.id,
        serialNumber = battery.serialNumber,
        brand = battery.brand,
        model = battery.model
    )
    
    val qrContent = qrData.encode()
    
    // Générer l'image du QR code
    val qrCodeBitmap = QrCodeGenerator.rememberQrCodeBitmap(
        content = qrContent,
        size = 300.dp,
        padding = 2
    )
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // En-tête avec icône et titre
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "QR Code Batterie",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Sous-titre avec les infos de la batterie
                Text(
                    text = "${battery.brand} ${battery.model}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                
                // Numéro de série
                Text(
                    text = "S/N: ${battery.serialNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // QR Code avec style amélioré
                Box(
                    modifier = Modifier
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrCodeBitmap != null) {
                        Image(
                            bitmap = qrCodeBitmap,
                            contentDescription = "QR Code pour batterie ${battery.serialNumber}",
                            modifier = Modifier
                                .size(250.dp)
                                .align(Alignment.Center)
                        )
                    } else {
                        // Fallback amélioré si le QR code ne peut pas être généré
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                modifier = Modifier.size(100.dp),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Impossible de générer le QR code",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                
                // Instructions
                Text(
                    text = "Scannez ce QR code pour accéder rapidement aux informations de cette batterie",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Boutons d'action avec layout optimisé
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Bouton Partager - Style amélioré avec largeur adaptée
                    OutlinedButton(
                        onClick = {
                            viewModel.getBatteryQrCodeBitmap(battery, 512)?.let { bitmap ->
                                onShare(bitmap)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Partager",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    }
                    
                    // Bouton Enregistrer - Style amélioré avec largeur adaptée
                    Button(
                        onClick = {
                            viewModel.getBatteryQrCodeBitmap(battery, 1024)?.let { bitmap ->
                                onSave(bitmap)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Enregistrer",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}


/**
 * Dialogue pour partager une batterie avec toutes ses données
 * Génère un QR code contenant les informations complètes de la batterie
 */
@Composable
fun ShareBatteryQrCodeDialog(
    battery: Battery,
    onDismiss: () -> Unit,
    onShareFull: (Bitmap) -> Unit,
    onShareSimple: (Bitmap) -> Unit,
    viewModel: QrCodeViewModel = hiltViewModel()
) {
    // Générer le QR code de partage complet
    val shareQrData = QrCodeData.forShareBattery(battery)
    val shareQrContent = shareQrData.encode()
    
    val shareQrCodeBitmap = QrCodeGenerator.rememberQrCodeBitmap(
        content = shareQrContent,
        size = 300.dp,
        padding = 2
    )
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // En-tête
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Partager la batterie",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Info batterie
                Text(
                    text = "${battery.brand} ${battery.model}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "S/N: ${battery.serialNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                // QR Code de partage
                Box(
                    modifier = Modifier
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (shareQrCodeBitmap != null) {
                        Image(
                            bitmap = shareQrCodeBitmap,
                            contentDescription = "QR Code de partage",
                            modifier = Modifier.size(220.dp)
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Erreur de génération",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                
                // Information sur le partage
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ce QR code contient toutes les informations de la batterie. Le destinataire pourra l'importer directement dans SkyFuel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Boutons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Bouton partage complet
                    Button(
                        onClick = {
                            viewModel.getShareBatteryQrCodeBitmap(battery, 512)?.let { bitmap ->
                                onShareFull(bitmap)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Partager (données complètes)")
                    }
                    
                    // Bouton partage simple (référence uniquement)
                    OutlinedButton(
                        onClick = {
                            viewModel.getBatteryQrCodeBitmap(battery, 512)?.let { bitmap ->
                                onShareSimple(bitmap)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Partager (référence seule)")
                    }
                }
            }
        }
    }
}

/**
 * Dialogue de confirmation pour importer une batterie depuis un QR code
 */
@Composable
fun ImportBatteryConfirmDialog(
    battery: Battery,
    alreadyExists: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // En-tête
                Icon(
                    imageVector = if (alreadyExists) Icons.Default.Warning else Icons.Default.BatteryFull,
                    contentDescription = null,
                    tint = if (alreadyExists) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                
                Text(
                    text = if (alreadyExists) "Batterie existante" else "Importer la batterie",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Détails de la batterie
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ImportDetailRow("Marque", battery.brand)
                        ImportDetailRow("Modèle", battery.model)
                        ImportDetailRow("N° Série", battery.serialNumber)
                        ImportDetailRow("Type", battery.type.name)
                        ImportDetailRow("Cellules", "${battery.cells}S")
                        ImportDetailRow("Capacité", "${battery.capacity} mAh")
                        ImportDetailRow("Cycles", battery.cycleCount.toString())
                        ImportDetailRow("Achat", battery.purchaseDate.format(dateFormatter))
                        ImportDetailRow("Statut", battery.status.name)
                    }
                }
                
                // Message d'avertissement si existe déjà
                if (alreadyExists) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Une batterie avec ce numéro de série existe déjà dans votre inventaire.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Boutons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Annuler")
                    }
                    
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        enabled = !alreadyExists,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Importer")
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
