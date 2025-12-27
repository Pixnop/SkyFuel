package leonfvt.skyfuel_app.presentation.component

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Composant qui gère les permissions de notification pour Android 13+
 * Sur les versions antérieures, les notifications sont automatiquement autorisées
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermission(
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {},
    content: @Composable (isGranted: Boolean, requestPermission: () -> Unit) -> Unit
) {
    // Sur Android 12 et moins, les notifications sont automatiquement autorisées
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        content(true) { }
        LaunchedEffect(Unit) {
            onPermissionGranted()
        }
        return
    }
    
    var showRationale by remember { mutableStateOf(false) }
    
    val notificationPermissionState = rememberPermissionState(
        Manifest.permission.POST_NOTIFICATIONS
    ) { isGranted ->
        if (isGranted) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }
    
    // Afficher le dialogue de justification si nécessaire
    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Autorisation des notifications") },
            text = {
                Text(
                    "Les notifications vous permettent de recevoir des rappels " +
                    "pour charger vos batteries et des alertes importantes concernant " +
                    "leur maintenance."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showRationale = false
                    notificationPermissionState.launchPermissionRequest()
                }) {
                    Text("Autoriser")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) {
                    Text("Plus tard")
                }
            }
        )
    }
    
    val requestPermission: () -> Unit = {
        if (notificationPermissionState.status.shouldShowRationale) {
            showRationale = true
        } else {
            notificationPermissionState.launchPermissionRequest()
        }
    }
    
    content(notificationPermissionState.status.isGranted, requestPermission)
}

/**
 * Carte d'information pour demander la permission de notification
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermissionCard(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    // Sur Android 12 et moins, ne pas afficher la carte
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return
    }
    
    val notificationPermissionState = rememberPermissionState(
        Manifest.permission.POST_NOTIFICATIONS
    )
    
    // Si la permission est déjà accordée, ne pas afficher la carte
    if (notificationPermissionState.status.isGranted) {
        return
    }
    
    var showRationale by remember { mutableStateOf(false) }
    
    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Autorisation des notifications") },
            text = {
                Text(
                    "Les notifications vous permettent de recevoir des rappels " +
                    "pour charger vos batteries et des alertes importantes concernant " +
                    "leur maintenance."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showRationale = false
                    notificationPermissionState.launchPermissionRequest()
                }) {
                    Text("Autoriser")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showRationale = false
                    onDismiss()
                }) {
                    Text("Plus tard")
                }
            }
        )
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Activer les notifications",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Recevez des rappels de charge et des alertes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Button(
                onClick = {
                    if (notificationPermissionState.status.shouldShowRationale) {
                        showRationale = true
                    } else {
                        notificationPermissionState.launchPermissionRequest()
                    }
                }
            ) {
                Text("Activer")
            }
        }
    }
}

/**
 * Ligne de paramètre pour activer/désactiver les notifications
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationSettingRow(
    title: String,
    description: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Sur Android 12 et moins, utiliser directement le switch
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        SettingRow(
            title = title,
            description = description,
            enabled = enabled,
            onEnabledChange = onEnabledChange,
            modifier = modifier
        )
        return
    }
    
    val notificationPermissionState = rememberPermissionState(
        Manifest.permission.POST_NOTIFICATIONS
    )
    
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Autorisation requise") },
            text = {
                Text(
                    "Pour activer cette fonctionnalité, vous devez autoriser " +
                    "les notifications dans les paramètres de l'application."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showPermissionDialog = false
                    notificationPermissionState.launchPermissionRequest()
                }) {
                    Text("Autoriser")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
    
    val handleChange: (Boolean) -> Unit = { newValue ->
        if (newValue && !notificationPermissionState.status.isGranted) {
            showPermissionDialog = true
        } else {
            onEnabledChange(newValue)
        }
    }
    
    SettingRow(
        title = title,
        description = description,
        enabled = enabled && notificationPermissionState.status.isGranted,
        onEnabledChange = handleChange,
        hasPermission = notificationPermissionState.status.isGranted,
        modifier = modifier
    )
}

@Composable
private fun SettingRow(
    title: String,
    description: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    hasPermission: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (!hasPermission) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.NotificationsOff,
                        contentDescription = "Permission requise",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange
        )
    }
}
