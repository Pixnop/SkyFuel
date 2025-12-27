package leonfvt.skyfuel_app.presentation.screen

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import leonfvt.skyfuel_app.presentation.component.BatteryHealthIndicator
import leonfvt.skyfuel_app.presentation.component.CyclesByBrandChart
import leonfvt.skyfuel_app.presentation.component.HorizontalBarChart
import leonfvt.skyfuel_app.presentation.component.StatusDistributionChart
import leonfvt.skyfuel_app.presentation.viewmodel.StatisticsState
import leonfvt.skyfuel_app.presentation.viewmodel.StatisticsViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Statistiques") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
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
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state.totalBatteries == 0) {
            EmptyStatisticsScreen(
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            StatisticsContent(
                state = state,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun EmptyStatisticsScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Aucune donnée",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Ajoutez des batteries pour voir les statistiques",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun StatisticsContent(
    state: StatisticsState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cartes de résumé
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatSummaryCard(
                icon = Icons.Default.BatteryFull,
                title = "Total",
                value = state.totalBatteries.toString(),
                subtitle = "batteries",
                modifier = Modifier.weight(1f)
            )
            StatSummaryCard(
                icon = Icons.Default.Loop,
                title = "Cycles",
                value = state.totalCycles.toString(),
                subtitle = "total",
                modifier = Modifier.weight(1f)
            )
        }
        
        // Distribution par statut
        StatusDistributionChart(
            chargedCount = state.chargedCount,
            dischargedCount = state.dischargedCount,
            storageCount = state.storageCount,
            outOfServiceCount = state.outOfServiceCount
        )
        
        // Indicateur de santé
        BatteryHealthIndicator(
            averageCycles = state.averageCycleCount,
            maxCycles = state.maxCycles,
            healthyCount = state.healthyCount,
            warningCount = state.warningCount,
            criticalCount = state.criticalCount
        )
        
        // Cycles par marque
        CyclesByBrandChart(
            cyclesByBrand = state.cyclesByBrand
        )
        
        // Cycles par type
        if (state.cyclesByType.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Cycles moyens par type",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalBarChart(
                        data = state.cyclesByType.toList(),
                        barColor = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
        
        // Batteries remarquables
        if (state.oldestBattery != null || state.mostUsedBattery != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Batteries remarquables",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    state.oldestBattery?.let { battery ->
                        NotableBatteryItem(
                            icon = Icons.Default.CalendarMonth,
                            label = "Plus ancienne",
                            batteryName = "${battery.brand} ${battery.model}",
                            detail = "Depuis ${battery.purchaseDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}"
                        )
                    }
                    
                    if (state.oldestBattery != null && state.mostUsedBattery != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    state.mostUsedBattery?.let { battery ->
                        NotableBatteryItem(
                            icon = Icons.Default.TrendingUp,
                            label = "Plus utilisée",
                            batteryName = "${battery.brand} ${battery.model}",
                            detail = "${battery.cycleCount} cycles"
                        )
                    }
                }
            }
        }
        
        // Espace en bas
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun StatSummaryCard(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun NotableBatteryItem(
    icon: ImageVector,
    label: String,
    batteryName: String,
    detail: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = batteryName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
