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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import leonfvt.skyfuel_app.presentation.component.ActivityHeatmap
import leonfvt.skyfuel_app.presentation.component.BatteryHealthIndicator
import leonfvt.skyfuel_app.presentation.component.CyclesByBrandChart
import leonfvt.skyfuel_app.presentation.component.FleetHealthGauge
import leonfvt.skyfuel_app.presentation.component.HealthDegradationChart
import leonfvt.skyfuel_app.presentation.component.HorizontalBarChart
import leonfvt.skyfuel_app.presentation.component.LifespanPredictionCard
import leonfvt.skyfuel_app.presentation.component.StatusDistributionChart
import leonfvt.skyfuel_app.presentation.component.VoltageTrendChart
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
                title = { Text("Analytics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.totalBatteries == 0 -> {
                EmptyStatisticsScreen(modifier = Modifier.padding(paddingValues))
            }
            else -> {
                StatisticsContent(
                    state = state,
                    onSelectPrediction = { viewModel.selectBatteryPrediction(it) },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun EmptyStatisticsScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Aucune donnée", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Ajoutez des batteries pour voir les statistiques",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun StatisticsContent(
    state: StatisticsState,
    onSelectPrediction: (leonfvt.skyfuel_app.domain.service.BatteryPrediction?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ========== RÉSUMÉ ==========
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

        // ========== JAUGE DE SANTÉ FLOTTE ==========
        state.fleetHealth?.let { fleetHealth ->
            FleetHealthGauge(fleetHealth = fleetHealth)
        }

        // ========== PRÉDICTION PAR BATTERIE ==========
        if (state.predictions.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Analyse par batterie",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Sélecteur de batterie
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.predictions.take(5).forEach { prediction ->
                            FilterChip(
                                selected = state.selectedBatteryPrediction?.battery?.id == prediction.battery.id,
                                onClick = { onSelectPrediction(prediction) },
                                label = {
                                    Text(
                                        "${prediction.battery.brand} ${prediction.battery.model}",
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Graphique de dégradation + prédiction pour la batterie sélectionnée
            state.selectedBatteryPrediction?.let { prediction ->
                HealthDegradationChart(prediction = prediction)
                LifespanPredictionCard(prediction = prediction)
            }
        }

        // ========== TENDANCE DE TENSION ==========
        if (state.voltageTrends.size >= 2) {
            VoltageTrendChart(voltages = state.voltageTrends)
        }

        // ========== DISTRIBUTION PAR STATUT ==========
        StatusDistributionChart(
            chargedCount = state.chargedCount,
            dischargedCount = state.dischargedCount,
            storageCount = state.storageCount,
            outOfServiceCount = state.outOfServiceCount
        )

        // ========== SANTÉ GLOBALE ==========
        BatteryHealthIndicator(
            averageCycles = state.averageCycleCount,
            maxCycles = state.maxCycles,
            healthyCount = state.healthyCount,
            warningCount = state.warningCount,
            criticalCount = state.criticalCount
        )

        // ========== CYCLES PAR MARQUE ==========
        CyclesByBrandChart(cyclesByBrand = state.cyclesByBrand)

        // ========== CYCLES PAR TYPE ==========
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

        // ========== HEATMAP D'ACTIVITÉ ==========
        if (state.activityHeatmap.isNotEmpty()) {
            ActivityHeatmap(activities = state.activityHeatmap)
        }

        // ========== BATTERIES REMARQUABLES ==========
        if (state.oldestBattery != null || state.mostUsedBattery != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Batteries remarquables",
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
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            label = "Plus utilisée",
                            batteryName = "${battery.brand} ${battery.model}",
                            detail = "${battery.cycleCount} cycles"
                        )
                    }
                }
            }
        }

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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
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
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(batteryName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
