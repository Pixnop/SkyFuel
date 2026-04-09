package leonfvt.skyfuel_app.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Warning
import leonfvt.skyfuel_app.domain.service.BatteryPrediction
import leonfvt.skyfuel_app.domain.service.CapacityPoint
import leonfvt.skyfuel_app.domain.service.StressFactors
import leonfvt.skyfuel_app.domain.service.VoltageTrend
import leonfvt.skyfuel_app.presentation.theme.HealthCritical
import leonfvt.skyfuel_app.presentation.theme.HealthExcellent
import leonfvt.skyfuel_app.presentation.theme.HealthGood
import leonfvt.skyfuel_app.presentation.theme.HealthModerate
import leonfvt.skyfuel_app.presentation.theme.HealthPoor
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Section complète d'analytics pour une batterie individuelle.
 * Affiche la capacité réelle, la courbe de dégradation, et les prédictions.
 */
@Composable
fun BatteryAnalyticsSection(
    prediction: BatteryPrediction,
    voltageTrends: List<VoltageTrend>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300),
        label = "expand_rotation"
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Titre cliquable pour expand/collapse
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Santé & Prédictions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Réduire" else "Développer",
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { rotationZ = rotationAngle },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Capacité réelle estimée — toujours visible
        CapacityCard(prediction)

        // 3. Facteurs de stress — toujours visible si détectés (important)
        if (prediction.stressFactors.hasWarnings) {
            StressFactorsCard(prediction.stressFactors)
        }

        // Sections dépliables
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 2. Courbe de capacité
                CapacityDegradationChart(prediction)

                // 4. Prédiction de durée de vie
                LifespanCard(prediction)

                // 5. Tendance de tension
                if (voltageTrends.size >= 2) {
                    BatteryVoltageTrendCard(voltageTrends)
                }
            }
        }

        if (!expanded) {
            // Résumé compact quand replié
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { expanded = true },
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val remainText = when {
                    prediction.remainingDays > 365 -> "${prediction.remainingDays / 365} ans"
                    prediction.remainingDays > 30 -> "${prediction.remainingDays / 30} mois"
                    else -> "${prediction.remainingDays}j"
                }
                CompactMetric("Restant", remainText, HealthGood)
                CompactMetric("Cycles", "${prediction.remainingCycles}", MaterialTheme.colorScheme.primary)
                CompactMetric("Rythme", "${String.format(java.util.Locale.getDefault(), "%.1f", prediction.cyclesPerMonth)}/m", MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
private fun CompactMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ============================================================
// 1. CARTE CAPACITÉ RÉELLE
// ============================================================

@Composable
private fun CapacityCard(prediction: BatteryPrediction) {
    val battery = prediction.battery
    val retention = prediction.capacityRetentionPercent

    var targetProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(1200, easing = LinearEasing),
        label = "capacity"
    )
    LaunchedEffect(retention) { targetProgress = retention / 100f }

    val capacityColor = when {
        retention > 85 -> HealthExcellent
        retention > 70 -> HealthGood
        retention > 55 -> HealthModerate
        retention > 40 -> HealthPoor
        else -> HealthCritical
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Battery4Bar,
                    contentDescription = null,
                    tint = capacityColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Capacité réelle estimée",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))

            // Capacité avec jauge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "${prediction.estimatedCapacityMah} mAh",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = capacityColor
                    )
                    Text(
                        text = "sur ${battery.capacity} mAh nominal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Mini jauge circulaire
                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(72.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 6.dp,
                        strokeCap = StrokeCap.Round
                    )
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.size(72.dp),
                        color = capacityColor,
                        strokeWidth = 6.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = "${retention.toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = capacityColor
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Barre de remplissage capacité
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(capacityColor.copy(alpha = 0.7f), capacityColor)
                            )
                        )
                )
            }

            Spacer(Modifier.height(8.dp))

            // Résistance interne
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ElectricalServices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Résistance interne",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "+${String.format(Locale.getDefault(), "%.0f", prediction.internalResistanceIncrease)}% vs neuf",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (prediction.internalResistanceIncrease > 50) HealthCritical else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============================================================
// 2. COURBE DE DÉGRADATION DE CAPACITÉ
// ============================================================

@Composable
private fun CapacityDegradationChart(prediction: BatteryPrediction) {
    val points = prediction.capacityCurve
    if (points.size < 2) return

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        animatedProgress.animateTo(1f, tween(1500, easing = FastOutSlowInEasing))
    }

    val goodColor = HealthExcellent
    val predColor = MaterialTheme.colorScheme.outline

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Dégradation de capacité",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Capacité estimée au fil des cycles",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val pad = 50f
                val chartW = size.width - pad * 2
                val chartH = size.height - pad

                val maxCycle = points.last().cycleNumber.toFloat().coerceAtLeast(1f)
                val maxMah = points.first().capacityMah.toFloat()
                val minMah = (maxMah * 0.4f) // Show down to 40%

                val progress = animatedProgress.value

                // Grille
                for (pct in listOf(50f, 60f, 70f, 80f, 90f, 100f)) {
                    val mah = maxMah * pct / 100f
                    val y = pad + chartH * (1f - (mah - minMah) / (maxMah - minMah))
                    drawLine(Color.Gray.copy(0.1f), Offset(pad, y), Offset(pad + chartW, y))
                    drawContext.canvas.nativeCanvas.drawText(
                        "${pct.toInt()}%", 2f, y + 4f,
                        android.graphics.Paint().apply { color = android.graphics.Color.GRAY; textSize = 22f }
                    )
                }

                fun cx(cycle: Int) = pad + (cycle / maxCycle) * chartW
                fun cy(mah: Int) = pad + chartH * (1f - (mah - minMah) / (maxMah - minMah))

                val realPts = points.filter { !it.isPredicted }
                val predPts = points.filter { it.isPredicted }

                // Zone de remplissage sous la courbe réelle
                if (realPts.size >= 2) {
                    val fillPath = Path().apply {
                        moveTo(cx(realPts.first().cycleNumber), pad + chartH)
                        realPts.forEach { moveTo(cx(it.cycleNumber), cy((it.capacityMah * progress).toInt())); lineTo(cx(it.cycleNumber), cy((it.capacityMah * progress).toInt())) }
                        // Refaire proprement
                    }
                    // Ligne réelle
                    val line = Path()
                    realPts.forEachIndexed { i, pt ->
                        val x = cx(pt.cycleNumber)
                        val y = cy((pt.capacityMah * progress).toInt())
                        if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
                    }

                    // Gradient fill
                    val fill = Path().apply {
                        moveTo(cx(realPts.first().cycleNumber), pad + chartH)
                        realPts.forEach { lineTo(cx(it.cycleNumber), cy((it.capacityMah * progress).toInt())) }
                        lineTo(cx(realPts.last().cycleNumber), pad + chartH)
                        close()
                    }
                    drawPath(fill, Brush.verticalGradient(
                        listOf(goodColor.copy(0.25f), Color.Transparent), pad, pad + chartH
                    ))
                    drawPath(line, goodColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }

                // Projection pointillée
                if (predPts.isNotEmpty() && realPts.isNotEmpty()) {
                    val predLine = Path().apply {
                        val last = realPts.last()
                        moveTo(cx(last.cycleNumber), cy((last.capacityMah * progress).toInt()))
                        predPts.forEach { lineTo(cx(it.cycleNumber), cy((it.capacityMah * progress).toInt())) }
                    }
                    drawPath(predLine, predColor, style = Stroke(
                        2.dp.toPx(), cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                    ))
                }

                // Point actuel
                realPts.lastOrNull()?.let { pt ->
                    val x = cx(pt.cycleNumber); val y = cy((pt.capacityMah * progress).toInt())
                    drawCircle(goodColor, 6.dp.toPx(), Offset(x, y))
                    drawCircle(Color.White, 3.dp.toPx(), Offset(x, y))
                }

                // Axe X labels
                val labelCycles = listOf(0, maxCycle.toInt() / 2, maxCycle.toInt())
                labelCycles.forEach { c ->
                    drawContext.canvas.nativeCanvas.drawText(
                        "$c", cx(c), pad + chartH + 30f,
                        android.graphics.Paint().apply { color = android.graphics.Color.GRAY; textSize = 22f; textAlign = android.graphics.Paint.Align.CENTER }
                    )
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "cycles", pad + chartW / 2, pad + chartH + 48f,
                    android.graphics.Paint().apply { color = android.graphics.Color.GRAY; textSize = 20f; textAlign = android.graphics.Paint.Align.CENTER }
                )
            }

            // Légende
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ChartLegendItem(color = HealthExcellent, label = "Mesuré")
                ChartLegendItem(color = MaterialTheme.colorScheme.outline, label = "Projection", isDashed = true)
            }
        }
    }
}

// ============================================================
// 3. FACTEURS DE STRESS
// ============================================================

@Composable
private fun StressFactorsCard(stress: StressFactors) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = HealthPoor.copy(alpha = 0.08f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = HealthPoor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Facteurs de dégradation détectés", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = HealthPoor)
            }

            Spacer(Modifier.height(12.dp))

            if (stress.fullChargeDays > 1) {
                StressRow(
                    icon = Icons.Default.BatteryChargingFull,
                    label = "Stockage chargée trop long",
                    detail = "${String.format(Locale.getDefault(), "%.0f", stress.fullChargeDays)} jours cumulés à 100%",
                    penalty = stress.fullChargeStressPenalty,
                    color = if (stress.fullChargeDays > 14) HealthCritical else HealthPoor
                )
                Spacer(Modifier.height(8.dp))
            }

            if (stress.deepDischargeDays > 1) {
                StressRow(
                    icon = Icons.Default.Battery4Bar,
                    label = "Décharge prolongée",
                    detail = "${String.format(Locale.getDefault(), "%.0f", stress.deepDischargeDays)} jours cumulés à vide",
                    penalty = stress.deepDischargeStressPenalty,
                    color = if (stress.deepDischargeDays > 7) HealthCritical else HealthPoor
                )
                Spacer(Modifier.height(8.dp))
            }

            if (stress.highFrequencyPeriods > 2) {
                StressRow(
                    icon = Icons.Default.Speed,
                    label = "Usage intensif",
                    detail = "${stress.highFrequencyPeriods} jours à >2 cycles/jour",
                    penalty = stress.highFrequencyPenalty,
                    color = HealthModerate
                )
                Spacer(Modifier.height(8.dp))
            }

            // Barre résumé pénalité totale
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Impact total sur la capacité", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "-${String.format(Locale.getDefault(), "%.1f", stress.totalStressPenalty)}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = HealthCritical
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(stress.totalStressPenalty / 30f) // Max 30%
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(HealthCritical)
                )
            }
        }
    }
}

@Composable
private fun StressRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    detail: String,
    penalty: Float,
    color: Color
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            "-${String.format(Locale.getDefault(), "%.1f", penalty)}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ============================================================
// 4. CARTE DURÉE DE VIE
// ============================================================

@Composable
private fun LifespanCard(prediction: BatteryPrediction) {
    val remainColor = when {
        prediction.remainingDays > 365 -> HealthExcellent
        prediction.remainingDays > 90 -> HealthGood
        prediction.remainingDays > 30 -> HealthModerate
        else -> HealthCritical
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Durée de vie", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LifespanMetric(
                    value = when {
                        prediction.remainingDays > 365 -> "${prediction.remainingDays / 365} ans"
                        prediction.remainingDays > 30 -> "${prediction.remainingDays / 30} mois"
                        else -> "${prediction.remainingDays} j"
                    },
                    label = "Restant",
                    color = remainColor
                )
                LifespanMetric(
                    value = "${prediction.remainingCycles}",
                    label = "Cycles restants",
                    color = MaterialTheme.colorScheme.primary
                )
                LifespanMetric(
                    value = String.format(Locale.getDefault(), "%.1f", prediction.cyclesPerMonth),
                    label = "Cycles/mois",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(Modifier.height(12.dp))

            // Barre de confiance
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Confiance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(prediction.confidencePercent / 100f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                Text("${prediction.confidencePercent}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(4.dp))

            Text(
                "Remplacement estimé : ${prediction.estimatedEndOfLife.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LifespanMetric(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ============================================================
// 4. TENDANCE DE TENSION PAR BATTERIE
// ============================================================

@Composable
private fun BatteryVoltageTrendCard(voltages: List<VoltageTrend>) {
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(voltages) { animatedProgress.animateTo(1f, tween(1200, easing = FastOutSlowInEasing)) }

    val lineColor = MaterialTheme.colorScheme.tertiary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, null, tint = lineColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tension mesurée", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            val avgV = voltages.map { it.voltage }.average()
            val minV = voltages.minOf { it.voltage }
            val maxV = voltages.maxOf { it.voltage }
            Text(
                "Moy: ${String.format(Locale.getDefault(), "%.2f", avgV)}V  Min: ${String.format(Locale.getDefault(), "%.2f", minV)}V  Max: ${String.format(Locale.getDefault(), "%.2f", maxV)}V",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val pad = 10f
                val cW = size.width - pad * 2
                val cH = size.height - pad * 2
                val vMin = voltages.minOf { it.voltage } - 0.1f
                val vMax = voltages.maxOf { it.voltage } + 0.1f
                val range = (vMax - vMin).coerceAtLeast(0.1f)
                val p = animatedProgress.value

                val line = Path()
                voltages.forEachIndexed { i, vt ->
                    val x = pad + (i.toFloat() / (voltages.size - 1)) * cW
                    val y = pad + cH * (1f - (vt.voltage - vMin) / range) * p
                    if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
                }
                // Fill
                val fill = Path().apply {
                    moveTo(pad, pad + cH)
                    voltages.forEachIndexed { i, vt ->
                        val x = pad + (i.toFloat() / (voltages.size - 1)) * cW
                        val y = pad + cH * (1f - (vt.voltage - vMin) / range) * p
                        lineTo(x, y)
                    }
                    lineTo(pad + cW, pad + cH)
                    close()
                }
                drawPath(fill, Brush.verticalGradient(listOf(lineColor.copy(0.2f), Color.Transparent), pad, pad + cH))
                drawPath(line, lineColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))

                // Points
                voltages.forEachIndexed { i, vt ->
                    val x = pad + (i.toFloat() / (voltages.size - 1)) * cW
                    val y = pad + cH * (1f - (vt.voltage - vMin) / range) * p
                    drawCircle(lineColor, 3.dp.toPx(), Offset(x, y))
                    drawCircle(Color.White, 1.5f.dp.toPx(), Offset(x, y))
                }
            }
        }
    }
}
