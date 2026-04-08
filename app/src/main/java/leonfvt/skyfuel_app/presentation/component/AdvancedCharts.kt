package leonfvt.skyfuel_app.presentation.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
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
import leonfvt.skyfuel_app.domain.service.FleetHealthScore
import leonfvt.skyfuel_app.domain.service.FleetTrend
import leonfvt.skyfuel_app.domain.service.PredictionPoint
import leonfvt.skyfuel_app.domain.service.VoltageTrend
import leonfvt.skyfuel_app.domain.service.WeeklyActivity
import leonfvt.skyfuel_app.presentation.theme.HealthCritical
import leonfvt.skyfuel_app.presentation.theme.HealthExcellent
import leonfvt.skyfuel_app.presentation.theme.HealthGood
import leonfvt.skyfuel_app.presentation.theme.HealthModerate
import leonfvt.skyfuel_app.presentation.theme.HealthPoor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ============================================================
// 1. COURBE DE DÉGRADATION SANTÉ AVEC PROJECTION
// ============================================================

/**
 * Graphique de dégradation de santé avec zone de projection future en pointillés
 */
@Composable
fun HealthDegradationChart(
    prediction: BatteryPrediction,
    modifier: Modifier = Modifier
) {
    val points = prediction.healthCurve
    if (points.size < 2) return

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(1500, easing = FastOutSlowInEasing)
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Courbe de vie",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Santé estimée dans le temps",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            val healthyColor = HealthExcellent
            val warningColor = HealthModerate
            val dangerColor = HealthCritical
            val predictionColor = MaterialTheme.colorScheme.outline

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val padding = 40f
                val chartWidth = size.width - padding * 2
                val chartHeight = size.height - padding * 2

                val firstDate = points.first().date
                val lastDate = points.last().date
                val totalDays = ChronoUnit.DAYS.between(firstDate, lastDate).toFloat().coerceAtLeast(1f)

                // Grille horizontale
                for (pct in listOf(0f, 25f, 50f, 75f, 100f)) {
                    val y = padding + chartHeight * (1f - pct / 100f)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.15f),
                        start = Offset(padding, y),
                        end = Offset(padding + chartWidth, y),
                        strokeWidth = 1f
                    )
                    // Labels
                    drawContext.canvas.nativeCanvas.drawText(
                        "${pct.toInt()}%",
                        4f,
                        y + 4f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 24f
                        }
                    )
                }

                // Zone de danger (rouge sous 20%)
                drawRect(
                    color = dangerColor.copy(alpha = 0.05f),
                    topLeft = Offset(padding, padding + chartHeight * 0.8f),
                    size = Size(chartWidth, chartHeight * 0.2f)
                )

                // Séparer données réelles et projection
                val realPoints = points.filter { !it.isPredicted }
                val predictedPoints = points.filter { it.isPredicted }

                fun dateToX(date: LocalDate): Float {
                    val days = ChronoUnit.DAYS.between(firstDate, date).toFloat()
                    return padding + (days / totalDays) * chartWidth
                }

                fun healthToY(health: Float): Float {
                    return padding + chartHeight * (1f - health / 100f)
                }

                val progress = animatedProgress.value

                // Gradient fill sous la courbe réelle
                if (realPoints.size >= 2) {
                    val fillPath = Path().apply {
                        val first = realPoints.first()
                        moveTo(dateToX(first.date), padding + chartHeight)
                        realPoints.forEach { pt ->
                            lineTo(dateToX(pt.date), healthToY(pt.healthPercent * progress))
                        }
                        lineTo(dateToX(realPoints.last().date), padding + chartHeight)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(healthyColor.copy(alpha = 0.3f), Color.Transparent),
                            startY = padding,
                            endY = padding + chartHeight
                        )
                    )

                    // Ligne réelle (solide)
                    val realPath = Path().apply {
                        val first = realPoints.first()
                        moveTo(dateToX(first.date), healthToY(first.healthPercent * progress))
                        realPoints.drop(1).forEach { pt ->
                            lineTo(dateToX(pt.date), healthToY(pt.healthPercent * progress))
                        }
                    }
                    drawPath(
                        path = realPath,
                        color = healthyColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Ligne de projection (pointillés)
                if (predictedPoints.isNotEmpty() && realPoints.isNotEmpty()) {
                    val connectionPoint = realPoints.last()
                    val predictPath = Path().apply {
                        moveTo(dateToX(connectionPoint.date), healthToY(connectionPoint.healthPercent * progress))
                        predictedPoints.forEach { pt ->
                            lineTo(dateToX(pt.date), healthToY(pt.healthPercent * progress))
                        }
                    }
                    drawPath(
                        path = predictPath,
                        color = predictionColor,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f))
                        )
                    )
                }

                // Points-clés
                realPoints.lastOrNull()?.let { pt ->
                    val x = dateToX(pt.date)
                    val y = healthToY(pt.healthPercent * progress)
                    drawCircle(healthyColor, 6.dp.toPx(), Offset(x, y))
                    drawCircle(Color.White, 3.dp.toPx(), Offset(x, y))
                }
            }

            // Légende
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ChartLegendItem(color = HealthExcellent, label = "Historique")
                ChartLegendItem(color = MaterialTheme.colorScheme.outline, label = "Projection", isDashed = true)
            }
        }
    }
}

// ============================================================
// 2. CARTE DE PRÉDICTION FIN DE VIE
// ============================================================

@Composable
fun LifespanPredictionCard(
    prediction: BatteryPrediction,
    modifier: Modifier = Modifier
) {
    val battery = prediction.battery

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Prédiction de durée de vie",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Jours restants
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

                // Cycles restants
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

                // Rythme
                PredictionMetric(
                    icon = Icons.Default.Speed,
                    value = String.format(java.util.Locale.getDefault(), "%.1f", prediction.cyclesPerMonth),
                    label = "Cycles/mois",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Barre de confiance
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Confiance",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(prediction.confidencePercent / 100f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                Text(
                    text = "${prediction.confidencePercent}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Remplacement estimé : ${prediction.estimatedEndOfLife.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PredictionMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================
// 3. TENDANCE DE TENSION
// ============================================================

@Composable
fun VoltageTrendChart(
    voltages: List<VoltageTrend>,
    modifier: Modifier = Modifier
) {
    if (voltages.size < 2) return

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(voltages) {
        animatedProgress.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
    }

    val lineColor = MaterialTheme.colorScheme.tertiary

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Tendance de tension",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val avgVoltage = voltages.map { it.voltage }.average()
            Text(
                text = "Moyenne : ${String.format(java.util.Locale.getDefault(), "%.2f", avgVoltage)}V",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                val padding = 20f
                val chartW = size.width - padding * 2
                val chartH = size.height - padding * 2

                val minV = voltages.minOf { it.voltage } - 0.2f
                val maxV = voltages.maxOf { it.voltage } + 0.2f
                val rangeV = (maxV - minV).coerceAtLeast(0.1f)

                val progress = animatedProgress.value

                // Gradient fill
                val fillPath = Path().apply {
                    moveTo(padding, padding + chartH)
                    voltages.forEachIndexed { i, vt ->
                        val x = padding + (i.toFloat() / (voltages.size - 1)) * chartW
                        val y = padding + chartH * (1f - (vt.voltage - minV) / rangeV) * progress
                        lineTo(x, y)
                    }
                    lineTo(padding + chartW, padding + chartH)
                    close()
                }
                drawPath(
                    fillPath,
                    brush = Brush.verticalGradient(
                        listOf(lineColor.copy(alpha = 0.25f), Color.Transparent),
                        startY = padding,
                        endY = padding + chartH
                    )
                )

                // Ligne
                val linePath = Path()
                voltages.forEachIndexed { i, vt ->
                    val x = padding + (i.toFloat() / (voltages.size - 1)) * chartW
                    val y = padding + chartH * (1f - (vt.voltage - minV) / rangeV) * progress
                    if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                }
                drawPath(linePath, lineColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))

                // Points
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
// 4. JAUGE DE SANTÉ FLOTTE (GAUGE)
// ============================================================

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

            // Jauge semi-circulaire
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(180.dp)) {
                    val strokeWidth = 16.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                    // Arc de fond
                    drawArc(
                        color = Color.Gray.copy(alpha = 0.15f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )

                    // Arc de progression
                    drawArc(
                        color = gaugeColor,
                        startAngle = 135f,
                        sweepAngle = 270f * animatedProgress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
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
                    Text(
                        text = "Score",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val trendText = when (fleetHealth.trend) {
                        FleetTrend.IMPROVING -> "En amélioration"
                        FleetTrend.STABLE -> "Stable"
                        FleetTrend.DECLINING -> "En déclin"
                    }
                    Text(
                        text = trendText,
                        style = MaterialTheme.typography.labelSmall,
                        color = gaugeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Distribution de santé en barres horizontales empilées
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                val total = (fleetHealth.excellentCount + fleetHealth.goodCount +
                        fleetHealth.fairCount + fleetHealth.poorCount +
                        fleetHealth.criticalCount).coerceAtLeast(1)
                val segments = listOf(
                    fleetHealth.excellentCount to HealthExcellent,
                    fleetHealth.goodCount to HealthGood,
                    fleetHealth.fairCount to HealthModerate,
                    fleetHealth.poorCount to HealthPoor,
                    fleetHealth.criticalCount to HealthCritical
                )
                segments.forEach { (count, color) ->
                    if (count > 0) {
                        Box(
                            modifier = Modifier
                                .weight(count.toFloat() / total)
                                .height(12.dp)
                                .background(color)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Légende
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

            // Prochaine batterie à remplacer
            fleetHealth.nextReplacementBattery?.let { battery ->
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = HealthPoor,
                        modifier = Modifier.size(16.dp)
                    )
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
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label ($count)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================
// 5. HEATMAP D'ACTIVITÉ
// ============================================================

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

            // Heures regroupées par blocs de 4h
            val hourBlocks = listOf("0-4h", "4-8h", "8-12h", "12-16h", "16-20h", "20-24h")

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2.5f)
            ) {
                val labelWidth = 40f
                val cellWidth = (size.width - labelWidth) / 6f
                val cellHeight = size.height / 7f

                for (day in 0..6) {
                    // Label du jour
                    drawContext.canvas.nativeCanvas.drawText(
                        dayLabels[day],
                        2f,
                        (day + 0.6f) * cellHeight,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 22f
                        }
                    )

                    for (block in 0..5) {
                        // Agréger l'activité du bloc de 4h
                        val blockCount = activities
                            .filter { it.dayOfWeek == day + 1 && it.hourOfDay / 4 == block }
                            .sumOf { it.count }

                        val intensity = if (maxCount > 0) blockCount.toFloat() / maxCount else 0f
                        val x = labelWidth + block * cellWidth
                        val y = day * cellHeight

                        drawRoundRect(
                            color = heatColor.copy(alpha = (intensity * 0.8f + 0.05f).coerceIn(0.05f, 0.9f)),
                            topLeft = Offset(x + 2, y + 2),
                            size = Size(cellWidth - 4, cellHeight - 4),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )
                    }
                }
            }

            // Légende heures
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                hourBlocks.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                drawLine(
                    color = color,
                    start = Offset.Zero,
                    end = Offset(size.width, 0f),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
