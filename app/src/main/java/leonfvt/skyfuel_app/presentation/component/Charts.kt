package leonfvt.skyfuel_app.presentation.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import leonfvt.skyfuel_app.presentation.theme.StatusAvailable
import leonfvt.skyfuel_app.presentation.theme.StatusDecommissioned
import leonfvt.skyfuel_app.presentation.theme.StatusInUse
import leonfvt.skyfuel_app.presentation.theme.StatusMaintenance

/**
 * Données pour un segment du graphique en anneau
 */
data class PieChartSegment(
    val label: String,
    val value: Float,
    val color: Color
)

/**
 * Graphique en anneau (donut chart) animé
 */
@Composable
fun DonutChart(
    segments: List<PieChartSegment>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 24.dp,
    animationDuration: Int = 1000
) {
    val total = segments.sumOf { it.value.toDouble() }.toFloat()
    if (total == 0f) return
    
    val animatedProgress = remember { Animatable(0f) }
    
    LaunchedEffect(segments) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = animationDuration,
                easing = FastOutSlowInEasing
            )
        )
    }
    
    Canvas(modifier = modifier) {
        val canvasSize = size.minDimension
        val radius = (canvasSize - strokeWidth.toPx()) / 2
        val center = Offset(size.width / 2, size.height / 2)
        
        var startAngle = -90f
        
        segments.forEach { segment ->
            val sweepAngle = (segment.value / total) * 360f * animatedProgress.value
            
            drawArc(
                color = segment.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt)
            )
            
            startAngle += (segment.value / total) * 360f
        }
    }
}

/**
 * Card avec graphique de distribution des statuts
 */
@Composable
fun StatusDistributionChart(
    chargedCount: Int,
    dischargedCount: Int,
    storageCount: Int,
    outOfServiceCount: Int,
    modifier: Modifier = Modifier
) {
    val segments = listOf(
        PieChartSegment("Chargées", chargedCount.toFloat(), StatusAvailable),
        PieChartSegment("Déchargées", dischargedCount.toFloat(), StatusMaintenance),
        PieChartSegment("Stockage", storageCount.toFloat(), StatusInUse),
        PieChartSegment("Hors service", outOfServiceCount.toFloat(), StatusDecommissioned)
    ).filter { it.value > 0 }
    
    val total = chargedCount + dischargedCount + storageCount + outOfServiceCount
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Distribution par statut",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    DonutChart(
                        segments = segments,
                        modifier = Modifier.size(120.dp),
                        strokeWidth = 20.dp
                    )
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = total.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "batteries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    segments.forEach { segment ->
                        LegendItem(
                            color = segment.color,
                            label = segment.label,
                            value = segment.value.toInt(),
                            percentage = if (total > 0) (segment.value / total * 100).toInt() else 0
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    value: Int,
    percentage: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "$value ($percentage%)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Graphique à barres horizontales
 */
@Composable
fun HorizontalBarChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    maxValue: Float = data.maxOfOrNull { it.second } ?: 1f
) {
    val animatedProgress = remember { Animatable(0f) }
    
    LaunchedEffect(data) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        data.forEach { (label, value) ->
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = value.toInt().toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                ) {
                    // Background
                    drawRoundRect(
                        color = barColor.copy(alpha = 0.2f),
                        size = Size(size.width, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )
                    
                    // Filled bar
                    val filledWidth = (value / maxValue) * size.width * animatedProgress.value
                    drawRoundRect(
                        color = barColor,
                        size = Size(filledWidth, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )
                }
            }
        }
    }
}

/**
 * Card pour les cycles par marque
 */
@Composable
fun CyclesByBrandChart(
    cyclesByBrand: Map<String, Float>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Cycles moyens par marque",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (cyclesByBrand.isEmpty()) {
                Text(
                    text = "Aucune donnée disponible",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                HorizontalBarChart(
                    data = cyclesByBrand.toList()
                )
            }
        }
    }
}

/**
 * Card pour la santé des batteries (basée sur les cycles)
 */
@Composable
fun BatteryHealthIndicator(
    averageCycles: Float,
    maxCycles: Int,
    healthyCount: Int,
    warningCount: Int,
    criticalCount: Int,
    modifier: Modifier = Modifier
) {
    val healthPercentage = ((maxCycles - averageCycles) / maxCycles * 100).coerceIn(0f, 100f)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Santé globale du parc",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HealthStatItem(
                    label = "Bon état",
                    count = healthyCount,
                    color = StatusAvailable
                )
                HealthStatItem(
                    label = "Attention",
                    count = warningCount,
                    color = StatusMaintenance
                )
                HealthStatItem(
                    label = "Critique",
                    count = criticalCount,
                    color = StatusDecommissioned
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress bar de santé globale
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Cycles moyens: ${averageCycles.toInt()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Max recommandé: $maxCycles",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val progressColor = when {
                    healthPercentage > 60 -> StatusAvailable
                    healthPercentage > 30 -> StatusMaintenance
                    else -> StatusDecommissioned
                }
                
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                ) {
                    drawRoundRect(
                        color = Color.Gray.copy(alpha = 0.3f),
                        size = Size(size.width, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                    )
                    
                    drawRoundRect(
                        color = progressColor,
                        size = Size(size.width * (healthPercentage / 100f), size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                    )
                }
                
                Text(
                    text = "${healthPercentage.toInt()}% de durée de vie restante estimée",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun HealthStatItem(
    label: String,
    count: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Graphique en ligne pour l'historique des cycles
 */
@Composable
fun LineChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    if (data.isEmpty()) return
    
    val animatedProgress = remember { Animatable(0f) }
    
    LaunchedEffect(data) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }
    
    val maxValue = data.maxOfOrNull { it.second } ?: 1f
    val minValue = data.minOfOrNull { it.second } ?: 0f
    val range = (maxValue - minValue).coerceAtLeast(1f)
    
    Canvas(modifier = modifier) {
        val pointSpacing = size.width / (data.size - 1).coerceAtLeast(1)
        val points = data.mapIndexed { index, (_, value) ->
            val x = index * pointSpacing
            val y = size.height - ((value - minValue) / range * size.height)
            Offset(x, y)
        }
        
        // Draw line segments
        val visiblePoints = (points.size * animatedProgress.value).toInt().coerceAtLeast(1)
        for (i in 0 until visiblePoints - 1) {
            drawLine(
                color = lineColor,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        
        // Draw points
        points.take(visiblePoints).forEach { point ->
            drawCircle(
                color = lineColor,
                radius = 5.dp.toPx(),
                center = point
            )
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = point
            )
        }
    }
}
