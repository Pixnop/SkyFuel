package leonfvt.skyfuel_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import leonfvt.skyfuel_app.data.model.Battery
import leonfvt.skyfuel_app.data.model.BatteryEventType
import leonfvt.skyfuel_app.data.model.BatteryHistory
import leonfvt.skyfuel_app.data.model.BatteryStatus
import leonfvt.skyfuel_app.ui.components.getBatteryStatusColor
import leonfvt.skyfuel_app.ui.components.getBatteryStatusText
import leonfvt.skyfuel_app.ui.viewmodel.BatteryViewModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Écran de détails d'une batterie
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryDetailsScreen(
    batteryId: Long,
    navController: NavController,
    viewModel: BatteryViewModel = hiltViewModel()
) {
    val selectedBattery by viewModel.selectedBattery.collectAsState()
    val batteryHistory by viewModel.batteryHistory.collectAsState()
    
    var showStatusMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détails de la batterie") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearSelectedBattery()
                        navController.popBackStack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Naviguer vers l'écran d'édition */ }) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier")
                    }
                    IconButton(onClick = {
                        selectedBattery?.let { viewModel.deleteBattery(it) }
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        selectedBattery?.let { battery ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // Carte de détails
                BatteryDetailsCard(
                    battery = battery,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Actions de statut
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(onClick = { showStatusMenu = true }) {
                        Text("Changer le statut")
                    }
                    
                    StatusDropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false },
                        onStatusSelected = { newStatus ->
                            viewModel.updateBatteryStatus(
                                batteryId = battery.id,
                                newStatus = newStatus
                            )
                            showStatusMenu = false
                        },
                        currentStatus = battery.status
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Section d'historique
                Text(
                    "Historique",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                BatteryHistoryList(
                    history = batteryHistory,
                    modifier = Modifier.weight(1f)
                )
            }
        } ?: run {
            // Si la batterie n'est pas trouvée
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        "Batterie non trouvée",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }
}

/**
 * Carte de détails d'une batterie
 */
@Composable
fun BatteryDetailsCard(
    battery: Battery,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // En-tête avec nom et statut
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${battery.brand} ${battery.model}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(getBatteryStatusColor(battery.status))
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        getBatteryStatusText(battery.status),
                        style = MaterialTheme.typography.bodyMedium,
                        color = getBatteryStatusColor(battery.status)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Détails techniques
            DetailItem(label = "Numéro de série", value = battery.serialNumber)
            DetailItem(label = "Type", value = battery.type.name)
            DetailItem(label = "Configuration", value = "${battery.cells}S / ${battery.capacity} mAh")
            DetailItem(label = "Cycles", value = battery.cycleCount.toString())
            DetailItem(
                label = "Date d'achat",
                value = battery.purchaseDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Notes
            if (battery.notes.isNotBlank()) {
                Text(
                    "Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    battery.notes,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Élément détail avec libellé et valeur
 */
@Composable
fun DetailItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Menu déroulant pour changer le statut
 */
@Composable
fun StatusDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onStatusSelected: (BatteryStatus) -> Unit,
    currentStatus: BatteryStatus
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        BatteryStatus.values().forEach { status ->
            if (status != currentStatus) {
                DropdownMenuItem(
                    text = { Text(getBatteryStatusText(status)) },
                    onClick = { onStatusSelected(status) }
                )
            }
        }
    }
}

/**
 * Liste de l'historique d'une batterie
 */
@Composable
fun BatteryHistoryList(
    history: List<BatteryHistory>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (history.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Aucun historique disponible",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            items(history) { entry ->
                HistoryEntryItem(entry = entry)
                Divider()
            }
        }
    }
}

/**
 * Élément d'historique
 */
@Composable
fun HistoryEntryItem(entry: BatteryHistory) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Type d'événement
            Text(
                when (entry.eventType) {
                    BatteryEventType.STATUS_CHANGE -> {
                        if (entry.previousStatus != null && entry.newStatus != null) {
                            "Changement de statut: ${getBatteryStatusText(entry.previousStatus)} → ${getBatteryStatusText(entry.newStatus)}"
                        } else {
                            "Batterie ajoutée"
                        }
                    }
                    BatteryEventType.CYCLE_COMPLETED -> "Cycle #${entry.cycleNumber} complété"
                    BatteryEventType.VOLTAGE_READING -> "Relevé de tension: ${entry.voltage}V"
                    BatteryEventType.NOTE_ADDED -> "Note ajoutée"
                    BatteryEventType.MAINTENANCE -> "Maintenance effectuée"
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            // Notes
            if (entry.notes.isNotBlank()) {
                Text(
                    entry.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
        
        // Date et heure
        Text(
            entry.timestamp.format(dateFormatter),
            style = MaterialTheme.typography.bodySmall
        )
    }
}