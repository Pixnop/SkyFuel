package leonfvt.skyfuel_app.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import leonfvt.skyfuel_app.domain.model.BatteryEventType
import leonfvt.skyfuel_app.domain.model.BatteryHistory
import leonfvt.skyfuel_app.domain.model.EventImpact
import leonfvt.skyfuel_app.presentation.viewmodel.HistoryViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    batteryId: Long,
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    // Filtres
    var selectedEventType by remember { mutableStateOf<BatteryEventType?>(null) }
    
    // Charger l'historique au démarrage
    androidx.compose.runtime.LaunchedEffect(batteryId) {
        viewModel.loadHistory(batteryId)
    }
    
    // Filtrer l'historique
    val filteredHistory = remember(state.history, selectedEventType) {
        if (selectedEventType != null) {
            state.history.filter { it.eventType == selectedEventType }
        } else {
            state.history
        }
    }
    
    // Grouper par date
    val groupedHistory = remember(filteredHistory) {
        filteredHistory.groupBy { it.timestamp.toLocalDate() }
            .toSortedMap(compareByDescending { it })
    }
    
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { 
                    Column {
                        Text("Historique")
                        if (state.battery != null) {
                            Text(
                                text = "${state.battery?.brand} ${state.battery?.model}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadHistory(batteryId) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualiser"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Statistiques rapides
            if (!state.isLoading && state.history.isNotEmpty()) {
                HistoryStatsCard(
                    totalEvents = state.history.size,
                    cyclesCompleted = state.history.count { it.eventType == BatteryEventType.CYCLE_COMPLETED },
                    statusChanges = state.history.count { it.eventType == BatteryEventType.STATUS_CHANGE },
                    maintenances = state.history.count { it.eventType == BatteryEventType.MAINTENANCE },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Filtres par type d'événement
            if (state.history.isNotEmpty()) {
                EventTypeFilterChips(
                    selectedType = selectedEventType,
                    onTypeSelected = { selectedEventType = it },
                    availableTypes = state.history.map { it.eventType }.distinct(),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Contenu principal
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredHistory.isEmpty()) {
                EmptyHistoryScreen(
                    hasFilter = selectedEventType != null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                TimelineContent(
                    groupedHistory = groupedHistory,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun HistoryStatsCard(
    totalEvents: Int,
    cyclesCompleted: Int,
    statusChanges: Int,
    maintenances: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = Icons.Default.Timeline,
                value = totalEvents.toString(),
                label = "Événements"
            )
            StatItem(
                icon = Icons.Default.BatteryFull,
                value = cyclesCompleted.toString(),
                label = "Cycles"
            )
            StatItem(
                icon = Icons.Default.BatteryChargingFull,
                value = statusChanges.toString(),
                label = "Statuts"
            )
            StatItem(
                icon = Icons.Default.Settings,
                value = maintenances.toString(),
                label = "Maintenances"
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventTypeFilterChips(
    selectedType: BatteryEventType?,
    onTypeSelected: (BatteryEventType?) -> Unit,
    availableTypes: List<BatteryEventType>,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        item {
            FilterChip(
                selected = selectedType == null,
                onClick = { onTypeSelected(null) },
                label = { Text("Tous") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
        
        items(availableTypes) { type ->
            val (icon, label) = getEventTypeInfo(type)
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(if (selectedType == type) null else type) },
                label = { Text(label) },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = getEventTypeColor(type).copy(alpha = 0.2f)
                )
            )
        }
    }
}

@Composable
private fun TimelineContent(
    groupedHistory: Map<LocalDate, List<BatteryHistory>>,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        groupedHistory.forEach { (date, events) ->
            // En-tête de date
            item(key = "header_$date") {
                DateHeader(
                    date = date,
                    formatter = dateFormatter,
                    eventCount = events.size
                )
            }
            
            // Événements de la journée
            itemsIndexed(
                items = events,
                key = { _, event -> event.id }
            ) { index, event ->
                TimelineItem(
                    event = event,
                    timeFormatter = timeFormatter,
                    isFirst = index == 0,
                    isLast = index == events.lastIndex
                )
            }
            
            // Espacement entre les groupes
            item(key = "spacer_$date") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DateHeader(
    date: LocalDate,
    formatter: DateTimeFormatter,
    eventCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = date.format(formatter).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "$eventCount événement${if (eventCount > 1) "s" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun TimelineItem(
    event: BatteryHistory,
    timeFormatter: DateTimeFormatter,
    isFirst: Boolean,
    isLast: Boolean
) {
    val eventColor = getEventTypeColor(event.eventType)
    val (icon, _) = getEventTypeInfo(event.eventType)
    
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Timeline avec ligne et point
        Box(
            modifier = Modifier.width(40.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // Ligne verticale
            Canvas(
                modifier = Modifier
                    .width(2.dp)
                    .height(if (isLast) 40.dp else 100.dp)
            ) {
                val startY = if (isFirst) size.height / 2 else 0f
                val endY = if (isLast) size.height / 2 else size.height
                
                drawLine(
                    color = eventColor.copy(alpha = 0.5f),
                    start = Offset(size.width / 2, startY),
                    end = Offset(size.width / 2, endY),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = if (!isFirst && !isLast) null else PathEffect.dashPathEffect(
                        floatArrayOf(10f, 10f)
                    )
                )
            }
            
            // Point/Cercle de l'événement
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(eventColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        
        // Contenu de l'événement
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, bottom = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Heure
                Text(
                    text = event.timestamp.format(timeFormatter),
                    style = MaterialTheme.typography.labelMedium,
                    color = eventColor,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Description
                Text(
                    text = event.getDescription(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                // Notes si présentes
                if (event.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Données spécifiques
                event.voltage?.let { voltage ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "%.2f V".format(voltage),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryScreen(
    hasFilter: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (hasFilter) "Aucun événement de ce type" else "Aucun historique",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (hasFilter) "Essayez un autre filtre" else "L'historique apparaîtra ici",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

// Helpers
private fun getEventTypeInfo(type: BatteryEventType): Pair<ImageVector, String> {
    return when (type) {
        BatteryEventType.STATUS_CHANGE -> Icons.Default.BatteryChargingFull to "Statut"
        BatteryEventType.CYCLE_COMPLETED -> Icons.Default.BatteryFull to "Cycle"
        BatteryEventType.VOLTAGE_READING -> Icons.Default.ElectricBolt to "Tension"
        BatteryEventType.NOTE_ADDED -> Icons.Default.Note to "Note"
        BatteryEventType.MAINTENANCE -> Icons.Default.Settings to "Maintenance"
    }
}

private fun getEventTypeColor(type: BatteryEventType): Color {
    return when (type) {
        BatteryEventType.STATUS_CHANGE -> Color(0xFF2196F3)
        BatteryEventType.CYCLE_COMPLETED -> Color(0xFFFF9800)
        BatteryEventType.VOLTAGE_READING -> Color(0xFF9C27B0)
        BatteryEventType.NOTE_ADDED -> Color(0xFF607D8B)
        BatteryEventType.MAINTENANCE -> Color(0xFF009688)
    }
}
