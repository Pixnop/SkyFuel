package leonfvt.skyfuel_app.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import leonfvt.skyfuel_app.domain.model.ChargeReminder
import leonfvt.skyfuel_app.domain.model.ReminderType
import java.time.DayOfWeek

/**
 * Section de gestion des rappels dans l'écran de détail
 */
@Composable
fun RemindersSection(
    reminders: List<ChargeReminder>,
    onAddReminder: () -> Unit,
    onEditReminder: (ChargeReminder) -> Unit,
    onToggleReminder: (ChargeReminder) -> Unit,
    onDeleteReminder: (ChargeReminder) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Rappels",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                IconButton(onClick = onAddReminder) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Ajouter un rappel",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (reminders.isEmpty()) {
                Text(
                    text = "Aucun rappel configuré",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                reminders.forEach { reminder ->
                    ReminderItem(
                        reminder = reminder,
                        onEdit = { onEditReminder(reminder) },
                        onToggle = { onToggleReminder(reminder) },
                        onDelete = { onDeleteReminder(reminder) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ReminderItem(
    reminder: ChargeReminder,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.isEnabled)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = reminder.getFormattedTime(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (reminder.isEnabled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = reminder.getFormattedDays(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Modifier",
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Switch(
                    checked = reminder.isEnabled,
                    onCheckedChange = { onToggle() }
                )
            }
        }
    }
}

/**
 * Dialog pour ajouter/modifier un rappel
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderDialog(
    editingReminder: ChargeReminder?,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int, Set<DayOfWeek>, ReminderType, String) -> Unit
) {
    var title by remember { mutableStateOf(editingReminder?.title ?: "Rappel de charge") }
    var selectedDays by remember { mutableStateOf(editingReminder?.daysOfWeek ?: emptySet()) }
    var reminderType by remember { mutableStateOf(editingReminder?.reminderType ?: ReminderType.CHARGE) }
    var notes by remember { mutableStateOf(editingReminder?.notes ?: "") }
    
    val timePickerState = rememberTimePickerState(
        initialHour = editingReminder?.time?.hour ?: 9,
        initialMinute = editingReminder?.time?.minute ?: 0
    )
    
    val dayNames = mapOf(
        DayOfWeek.MONDAY to "Lun",
        DayOfWeek.TUESDAY to "Mar",
        DayOfWeek.WEDNESDAY to "Mer",
        DayOfWeek.THURSDAY to "Jeu",
        DayOfWeek.FRIDAY to "Ven",
        DayOfWeek.SATURDAY to "Sam",
        DayOfWeek.SUNDAY to "Dim"
    )
    
    val reminderTypes = listOf(
        ReminderType.CHARGE to "Charge",
        ReminderType.STORAGE to "Stockage",
        ReminderType.MAINTENANCE to "Maintenance",
        ReminderType.VOLTAGE_CHECK to "Tension"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (editingReminder != null) "Modifier le rappel" else "Nouveau rappel")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text(
                    text = "Heure du rappel",
                    style = MaterialTheme.typography.labelMedium
                )
                
                TimePicker(state = timePickerState)
                
                Text(
                    text = "Jours (vide = tous les jours)",
                    style = MaterialTheme.typography.labelMedium
                )
                
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DayOfWeek.values().forEach { day ->
                        FilterChip(
                            selected = selectedDays.contains(day),
                            onClick = {
                                selectedDays = if (selectedDays.contains(day)) {
                                    selectedDays - day
                                } else {
                                    selectedDays + day
                                }
                            },
                            label = { Text(dayNames[day] ?: "") }
                        )
                    }
                }
                
                Text(
                    text = "Type de rappel",
                    style = MaterialTheme.typography.labelMedium
                )
                
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    reminderTypes.forEach { (type, label) ->
                        FilterChip(
                            selected = reminderType == type,
                            onClick = { reminderType = type },
                            label = { Text(label) }
                        )
                    }
                }
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        title,
                        timePickerState.hour,
                        timePickerState.minute,
                        selectedDays,
                        reminderType,
                        notes
                    )
                }
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
