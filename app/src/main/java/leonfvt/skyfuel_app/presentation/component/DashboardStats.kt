package leonfvt.skyfuel_app.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import leonfvt.skyfuel_app.domain.model.BatteryStatistics
import leonfvt.skyfuel_app.presentation.theme.SkyFuelTheme

/**
 * Carte de statistiques du tableau de bord
 */
@Composable
fun DashboardStats(
    statistics: BatteryStatistics,
    onViewFullStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var animatedCharged by remember { mutableFloatStateOf(0f) }
    var animatedDischarged by remember { mutableFloatStateOf(0f) }
    var animatedStorage by remember { mutableFloatStateOf(0f) }
    
    val chargedPercent by animateFloatAsState(
        targetValue = animatedCharged,
        animationSpec = tween(1000),
        label = "Charged animation"
    )
    
    val dischargedPercent by animateFloatAsState(
        targetValue = animatedDischarged,
        animationSpec = tween(1200),
        label = "Discharged animation"
    )
    
    val storagePercent by animateFloatAsState(
        targetValue = animatedStorage,
        animationSpec = tween(1400),
        label = "Storage animation"
    )
    
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300),
        label = "Expand icon rotation"
    )
    
    LaunchedEffect(statistics) {
        animatedCharged = statistics.getChargedPercentage() / 100f
        animatedDischarged = statistics.getDischargedPercentage() / 100f
        animatedStorage = statistics.getStoragePercentage() / 100f
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ShowChart, 
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "Vue d'ensemble",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Réduire" else "Développer",
                    modifier = Modifier
                        .rotate(rotationAngle)
                        .clip(CircleShape)
                        .clickable { expanded = !expanded }
                        .padding(4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Affichage du nombre total de batteries
            TotalBatteriesRow(totalCount = statistics.totalCount)
            
            // Section des détails (avec animation)
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + slideInHorizontally(),
                exit = fadeOut() + slideOutHorizontally()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Détails du statut des batteries
                    BatteryStatusDetails(
                        chargedPercent = chargedPercent,
                        chargedCount = statistics.chargedCount,
                        dischargedPercent = dischargedPercent,
                        dischargedCount = statistics.dischargedCount,
                        storagePercent = storagePercent,
                        storageCount = statistics.storageCount,
                        outOfServiceCount = statistics.outOfServiceCount
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cycles moyen: ${String.format("%.1f", statistics.averageCycleCount)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable(onClick = onViewFullStats)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Voir statistiques",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Ligne d'affichage du nombre total de batteries
 */
@Composable
fun TotalBatteriesRow(totalCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Total de batteries",
            style = MaterialTheme.typography.bodyLarge
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (totalCount > 0) {
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.BatteryAlert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = totalCount.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (totalCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

/**
 * Détails du statut des batteries avec indicateurs visuels
 */
@Composable
fun BatteryStatusDetails(
    chargedPercent: Float,
    chargedCount: Int,
    dischargedPercent: Float,
    dischargedCount: Int,
    storagePercent: Float,
    storageCount: Int,
    outOfServiceCount: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        StatusProgressRow(
            statusName = "Chargées",
            count = chargedCount,
            percent = chargedPercent,
            color = Color(0xFF4CAF50) // Vert
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        StatusProgressRow(
            statusName = "Déchargées",
            count = dischargedCount,
            percent = dischargedPercent,
            color = Color(0xFFFFC107) // Jaune
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        StatusProgressRow(
            statusName = "Stockage",
            count = storageCount,
            percent = storagePercent,
            color = Color(0xFF2196F3) // Bleu
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hors service",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = outOfServiceCount.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE91E63) // Rose
            )
        }
    }
}

/**
 * Ligne avec nom de statut, compteur et barre de progression
 */
@Composable
fun StatusProgressRow(
    statusName: String,
    count: Int,
    percent: Float,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = statusName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(100.dp)
        )
        
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percent)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardStatsPreview() {
    SkyFuelTheme {
        DashboardStats(
            statistics = BatteryStatistics(
                totalCount = 24,
                chargedCount = 12,
                dischargedCount = 6,
                storageCount = 4,
                outOfServiceCount = 2,
                averageCycleCount = 18.5f
            ),
            onViewFullStats = {}
        )
    }
}