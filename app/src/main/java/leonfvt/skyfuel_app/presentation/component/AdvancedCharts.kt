package leonfvt.skyfuel_app.presentation.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import leonfvt.skyfuel_app.domain.service.BatteryPrediction
import leonfvt.skyfuel_app.domain.service.VoltageTrend
import leonfvt.skyfuel_app.presentation.theme.HealthCritical
import leonfvt.skyfuel_app.presentation.theme.HealthExcellent
import leonfvt.skyfuel_app.presentation.theme.HealthGood
import leonfvt.skyfuel_app.presentation.theme.HealthModerate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ============================================================
// COURBE DE DÉGRADATION SANTÉ AVEC PROJECTION
// ============================================================

@Composable
fun HealthDegradationChart(
    prediction: BatteryPrediction,
    modifier: Modifier = Modifier
) {
    val points = prediction.healthCurve
    if (points.size < 2) return

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        animatedProgress.animateTo(1f, tween(1500, easing = FastOutSlowInEasing))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Courbe de vie", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Santé estimée dans le temps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(16.dp))

            val healthyColor = HealthExcellent
            val dangerColor = HealthCritical
            val predictionColor = MaterialTheme.colorScheme.outline

            Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                val padding = 40f
                val chartWidth = size.width - padding * 2
                val chartHeight = size.height - padding * 2

                val firstDate = points.first().date
                val lastDate = points.last().date
                val totalDays = ChronoUnit.DAYS.between(firstDate, lastDate).toFloat().coerceAtLeast(1f)

                // Grille
                for (pct in listOf(0f, 25f, 50f, 75f, 100f)) {
                    val y = padding + chartHeight * (1f - pct / 100f)
                    drawLine(Color.Gray.copy(alpha = 0.15f), Offset(padding, y), Offset(padding + chartWidth, y), 1f)
                    drawContext.canvas.nativeCanvas.drawText(
                        "${pct.toInt()}%", 4f, y + 4f,
                        android.graphics.Paint().apply { color = android.graphics.Color.GRAY; textSize = 24f }
                    )
                }

                // Zone de danger
                drawRect(dangerColor.copy(alpha = 0.05f), Offset(padding, padding + chartHeight * 0.8f), Size(chartWidth, chartHeight * 0.2f))

                val realPoints = points.filter { !it.isPredicted }
                val predictedPoints = points.filter { it.isPredicted }

                fun dateToX(date: LocalDate) = padding + (ChronoUnit.DAYS.between(firstDate, date).toFloat() / totalDays) * chartWidth
                fun healthToY(health: Float) = padding + chartHeight * (1f - health / 100f)

                val progress = animatedProgress.value

                // Gradient fill
                if (realPoints.size >= 2) {
                    val fillPath = Path().apply {
                        moveTo(dateToX(realPoints.first().date), padding + chartHeight)
                        realPoints.forEach { lineTo(dateToX(it.date), healthToY(it.healthPercent * progress)) }
                        lineTo(dateToX(realPoints.last().date), padding + chartHeight)
                        close()
                    }
                    drawPath(fillPath, Brush.verticalGradient(listOf(healthyColor.copy(alpha = 0.3f), Color.Transparent), padding, padding + chartHeight))

                    val realPath = Path().apply {
                        moveTo(dateToX(realPoints.first().date), healthToY(realPoints.first().healthPercent * progress))
                        realPoints.drop(1).forEach { lineTo(dateToX(it.date), healthToY(it.healthPercent * progress)) }
                    }
                    drawPath(realPath, healthyColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                }

                // Projection
                if (predictedPoints.isNotEmpty() && realPoints.isNotEmpty()) {
                    val conn = realPoints.last()
                    val predictPath = Path().apply {
                        moveTo(dateToX(conn.date), healthToY(conn.healthPercent * progress))
                        predictedPoints.forEach { lineTo(dateToX(it.date), healthToY(it.healthPercent * progress)) }
                    }
                    drawPath(predictPath, predictionColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f))))
                }

                // Point actuel
                realPoints.lastOrNull()?.let {
                    val x = dateToX(it.date); val y = healthToY(it.healthPercent * progress)
                    drawCircle(healthyColor, 6.dp.toPx(), Offset(x, y))
                    drawCircle(Color.White, 3.dp.toPx(), Offset(x, y))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ChartLegendItem(color = HealthExcellent, label = "Historique")
                ChartLegendItem(color = MaterialTheme.colorScheme.outline, label = "Projection", isDashed = true)
            }
        }
    }
}

// ============================================================
// CARTE DE PRÉDICTION FIN DE VIE
// ============================================================

@Composable
fun LifespanPredictionCard(
    prediction: BatteryPrediction,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Prédiction de durée de vie", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                PredictionMetric(
                    icon = Icons.Default.Schedule,
                    value = when {
                        prediction.remainingDays > 365 -> "${prediction.remainingDays / 365} ans"
                        prediction.remainingDays > 30 -> "${prediction.remainingDays / 30} mois"
                        else -> "${prediction.remainingDays} jours"
                    },
                    label = "Durée restante",
                    color = when {
                        prediction.remainingDays > 365 -> HealthExcellent
                        prediction.remainingDays > 90 -> HealthGood
                        prediction.remainingDays > 30 -> HealthModerate
                        else -> HealthCritical
                    }
                )
                PredictionMetric(
                    icon = Icons.Default.Speed,
                    value = "${prediction.remainingCycles}",
                    label = "Cycles restants",
                    color = when {
                        prediction.remainingCycles > 200 -> HealthExcellent
                        prediction.remainingCycles > 50 -> HealthGood
                        else -> HealthCritical
                    }
                )
                PredictionMetric(
                    icon = Icons.Default.Speed,
                    value = String.format(java.util.Locale.getDefault(), "%.1f", prediction.cyclesPerMonth),
                    label = "Cycles/mois",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Barre de confiance
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Confiance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Box(modifier = Modifier.fillMaxWidth(prediction.confidencePercent / 100f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.primary))
                }
                Text("${prediction.confidencePercent}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Remplacement estimé : ${prediction.estimatedEndOfLife.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PredictionMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String, label: String, color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ============================================================
// TENDANCE DE TENSION
// ============================================================

@Composable
fun VoltageTrendChart(
    voltages: List<VoltageTrend>,
    modifier: Modifier = Modifier
) {
    if (voltages.size < 2) return

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(voltages) { animatedProgress.animateTo(1f, tween(1200, easing = FastOutSlowInEasing)) }

    val lineColor = MaterialTheme.colorScheme.tertiary

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Tendance de tension", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Moyenne : ${String.format(java.util.Locale.getDefault(), "%.2f", voltages.map { it.voltage }.average())}V",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                val padding = 20f
                val chartW = size.width - padding * 2
                val chartH = size.height - padding * 2

                val minV = voltages.minOf { it.voltage } - 0.2f
                val maxV = voltages.maxOf { it.voltage } + 0.2f
                val rangeV = (maxV - minV).coerceAtLeast(0.1f)
                val progress = animatedProgress.value

                val fillPath = Path().apply {
                    moveTo(padding, padding + chartH)
                    voltages.forEachIndexed { i, vt ->
                        lineTo(padding + (i.toFloat() / (voltages.size - 1)) * chartW, padding + chartH * (1f - (vt.voltage - minV) / rangeV) * progress)
                    }
                    lineTo(padding + chartW, padding + chartH)
                    close()
                }
                drawPath(fillPath, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.25f), Color.Transparent), padding, padding + chartH))

                val linePath = Path()
                voltages.forEachIndexed { i, vt ->
                    val x = padding + (i.toFloat() / (voltages.size - 1)) * chartW
                    val y = padding + chartH * (1f - (vt.voltage - minV) / rangeV) * progress
                    if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                }
                drawPath(linePath, lineColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))

                voltages.forEachIndexed { i, vt ->
                    val x = padding + (i.toFloat() / (voltages.size - 1)) * chartW
                    val y = padding + chartH * (1f - (vt.voltage - minV) / rangeV) * progress
                    drawCircle(lineColor, 4.dp.toPx(), Offset(x, y))
                    drawCircle(Color.White, 2.dp.toPx(), Offset(x, y))
                }
            }
        }
    }
}

// ============================================================
// UTILS
// ============================================================

@Composable
fun ChartLegendItem(color: Color, label: String, isDashed: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isDashed) {
            Canvas(modifier = Modifier.size(16.dp, 2.dp)) {
                drawLine(color, Offset.Zero, Offset(size.width, 0f), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
            }
        } else {
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
