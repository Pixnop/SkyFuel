package leonfvt.skyfuel_app.presentation.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import leonfvt.skyfuel_app.domain.service.FleetHealthScore
import leonfvt.skyfuel_app.domain.service.FleetTrend
import leonfvt.skyfuel_app.domain.service.WeeklyActivity
import leonfvt.skyfuel_app.presentation.theme.HealthCritical
import leonfvt.skyfuel_app.presentation.theme.HealthExcellent
import leonfvt.skyfuel_app.presentation.theme.HealthGood
import leonfvt.skyfuel_app.presentation.theme.HealthModerate
import leonfvt.skyfuel_app.presentation.theme.HealthPoor

/**
 * Jauge semi-circulaire de santé globale de la flotte
 */
@Composable
fun FleetHealthGauge(
    fleetHealth: FleetHealthScore,
    modifier: Modifier = Modifier
) {
    var targetProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(1500, easing = LinearEasing),
        label = "Fleet health gauge"
    )

    LaunchedEffect(fleetHealth.overallScore) {
        targetProgress = fleetHealth.overallScore / 100f
    }

    val gaugeColor = when {
        fleetHealth.overallScore > 80 -> HealthExcellent
        fleetHealth.overallScore > 60 -> HealthGood
        fleetHealth.overallScore > 40 -> HealthModerate
        fleetHealth.overallScore > 20 -> HealthPoor
        else -> HealthCritical
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Santé de la flotte",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(180.dp)) {
                    val strokeWidth = 16.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                    drawArc(
                        color = Color.Gray.copy(alpha = 0.15f),
                        startAngle = 135f, sweepAngle = 270f,
                        useCenter = false, topLeft = topLeft, size = arcSize,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = gaugeColor,
                        startAngle = 135f, sweepAngle = 270f * animatedProgress,
                        useCenter = false, topLeft = topLeft, size = arcSize,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${fleetHealth.overallScore}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = gaugeColor
                    )
                    Text("Score", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = when (fleetHealth.trend) {
                            FleetTrend.IMPROVING -> "En amélioration"
                            FleetTrend.STABLE -> "Stable"
                            FleetTrend.DECLINING -> "En déclin"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = gaugeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Distribution de santé
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                val total = (fleetHealth.excellentCount + fleetHealth.goodCount +
                        fleetHealth.fairCount + fleetHealth.poorCount +
                        fleetHealth.criticalCount).coerceAtLeast(1)
                listOf(
                    fleetHealth.excellentCount to HealthExcellent,
                    fleetHealth.goodCount to HealthGood,
                    fleetHealth.fairCount to HealthModerate,
                    fleetHealth.poorCount to HealthPoor,
                    fleetHealth.criticalCount to HealthCritical
                ).forEach { (count, color) ->
                    if (count > 0) {
                        Box(modifier = Modifier.weight(count.toFloat() / total).height(12.dp).background(color))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                HealthLegendChip("Excellent", fleetHealth.excellentCount, HealthExcellent)
                HealthLegendChip("Bon", fleetHealth.goodCount, HealthGood)
                HealthLegendChip("Moyen", fleetHealth.fairCount, HealthModerate)
                HealthLegendChip("Faible", fleetHealth.poorCount, HealthPoor)
                HealthLegendChip("Critique", fleetHealth.criticalCount, HealthCritical)
            }

            fleetHealth.nextReplacementBattery?.let { battery ->
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, null, tint = HealthPoor, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Prochain remplacement : ${battery.brand} ${battery.model}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthLegendChip(label: String, count: Int, color: Color) {
    if (count == 0) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text("$label ($count)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Heatmap d'activité par jour/heure
 */
@Composable
fun ActivityHeatmap(
    activities: List<WeeklyActivity>,
    modifier: Modifier = Modifier
) {
    if (activities.isEmpty()) return

    val maxCount = activities.maxOf { it.count }
    val heatColor = MaterialTheme.colorScheme.primary
    val dayLabels = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Activité par jour et heure",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            val hourBlocks = listOf("0-4h", "4-8h", "8-12h", "12-16h", "16-20h", "20-24h")

            Canvas(
                modifier = Modifier.fillMaxWidth().aspectRatio(2.5f)
            ) {
                val labelWidth = 40f
                val cellWidth = (size.width - labelWidth) / 6f
                val cellHeight = size.height / 7f

                for (day in 0..6) {
                    drawContext.canvas.nativeCanvas.drawText(
                        dayLabels[day], 2f, (day + 0.6f) * cellHeight,
                        android.graphics.Paint().apply { color = android.graphics.Color.GRAY; textSize = 22f }
                    )

                    for (block in 0..5) {
                        val blockCount = activities
                            .filter { it.dayOfWeek == day + 1 && it.hourOfDay / 4 == block }
                            .sumOf { it.count }

                        val intensity = if (maxCount > 0) blockCount.toFloat() / maxCount else 0f
                        drawRoundRect(
                            color = heatColor.copy(alpha = (intensity * 0.8f + 0.05f).coerceIn(0.05f, 0.9f)),
                            topLeft = Offset(labelWidth + block * cellWidth + 2, day * cellHeight + 2),
                            size = Size(cellWidth - 4, cellHeight - 4),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 40.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                hourBlocks.forEach { label ->
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
