package leonfvt.skyfuel_app.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Battery1Bar
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.presentation.theme.SkyFuelTheme

/**
 * Carte regroupant les actions possibles sur une batterie
 */
@Composable
fun BatteryActionsCard(
    currentStatus: BatteryStatus,
    onStatusChange: (BatteryStatus) -> Unit,
    onVoltageRecord: (String, String) -> Unit,
    onAddNote: (String) -> Unit,
    onMaintenance: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            StatusActionRow(
                currentStatus = currentStatus,
                onStatusChange = onStatusChange
            )
            
            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            
            VoltageRecordAction(onVoltageRecord = onVoltageRecord)
            
            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            
            AddNoteAction(onAddNote = onAddNote)
            
            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            
            MaintenanceAction(onMaintenance = onMaintenance)
        }
    }
}

/**
 * Action pour changer le statut de la batterie
 */
@Composable
fun StatusActionRow(
    currentStatus: BatteryStatus,
    onStatusChange: (BatteryStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "Dropdown rotation"
    )
    
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Battery5Bar, 
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Changer le statut",
                    style = MaterialTheme.typography.titleSmall
                )
                
                Text(
                    text = "Statut actuel: ${getStatusText(currentStatus)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = getStatusColor(currentStatus)
                )
            }
            
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Réduire" else "Développer",
                modifier = Modifier.rotate(rotationState)
            )
        }
        
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp, top = 8.dp, bottom = 8.dp)
            ) {
                BatteryStatus.values()
                    .filter { it != currentStatus }
                    .forEach { status ->
                        StatusOption(
                            status = status,
                            onClick = { onStatusChange(status) }
                        )
                    }
            }
        }
    }
}

/**
 * Option de statut pour la batterie
 */
@Composable
fun StatusOption(
    status: BatteryStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, text) = when (status) {
        BatteryStatus.CHARGED -> Icons.Default.Battery5Bar to "Marquer comme chargée"
        BatteryStatus.DISCHARGED -> Icons.Default.Battery1Bar to "Marquer comme déchargée"
        BatteryStatus.STORAGE -> Icons.Default.BatteryAlert to "Mettre en stockage"
        BatteryStatus.OUT_OF_SERVICE -> Icons.Default.Close to "Mettre hors service"
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = getStatusColor(status),
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Action pour enregistrer une mesure de tension
 */
@Composable
fun VoltageRecordAction(
    onVoltageRecord: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var voltage by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "Dropdown rotation"
    )
    
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Speed, 
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = "Enregistrer une mesure de tension",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Réduire" else "Développer",
                modifier = Modifier.rotate(rotationState)
            )
        }
        
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp, top = 8.dp, bottom = 8.dp)
            ) {
                OutlinedTextField(
                    value = voltage,
                    onValueChange = { voltage = it },
                    label = { Text("Tension (V)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optionnel)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            expanded = false
                            voltage = ""
                            notes = ""
                        }
                    ) {
                        Text("Annuler")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            if (voltage.isNotBlank()) {
                                onVoltageRecord(voltage, notes)
                                expanded = false
                                voltage = ""
                                notes = ""
                            }
                        },
                        enabled = voltage.isNotBlank()
                    ) {
                        Text("Enregistrer")
                    }
                }
            }
        }
    }
}

/**
 * Action pour ajouter une note
 */
@Composable
fun AddNoteAction(
    onAddNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "Dropdown rotation"
    )
    
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Notes, 
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = "Ajouter une note",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Réduire" else "Développer",
                modifier = Modifier.rotate(rotationState)
            )
        }
        
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp, top = 8.dp, bottom = 8.dp)
            ) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Envoyer",
                            modifier = Modifier
                                .clickable {
                                    if (note.isNotBlank()) {
                                        onAddNote(note)
                                        note = ""
                                        expanded = false
                                    }
                                }
                                .padding(8.dp)
                        )
                    }
                )
            }
        }
    }
}

/**
 * Action pour enregistrer une maintenance
 */
@Composable
fun MaintenanceAction(
    onMaintenance: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onMaintenance)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Settings, 
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = "Enregistrer une maintenance",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BatteryActionsCardPreview() {
    SkyFuelTheme {
        BatteryActionsCard(
            currentStatus = BatteryStatus.CHARGED,
            onStatusChange = {},
            onVoltageRecord = { _, _ -> },
            onAddNote = {},
            onMaintenance = {}
        )
    }
}