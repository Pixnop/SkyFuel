package leonfvt.skyfuel_app.presentation.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.presentation.theme.HealthCritical
import leonfvt.skyfuel_app.presentation.theme.HealthExcellent
import leonfvt.skyfuel_app.presentation.theme.HealthGood
import leonfvt.skyfuel_app.presentation.theme.HealthModerate
import leonfvt.skyfuel_app.presentation.theme.HealthPoor

/**
 * Carte compacte de batterie pour affichage en grille.
 * Affiche : QR ID, nom, santé (jauge), statut (point coloré), cycles.
 */
@Composable
fun BatteryGridCard(
    battery: Battery,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val healthPct = battery.getHealthPercentage()

    var progress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "health"
    )
    LaunchedEffect(healthPct) { progress = healthPct / 100f }

    val healthColor = when {
        healthPct > 80 -> HealthExcellent
        healthPct > 60 -> HealthGood
        healthPct > 40 -> HealthModerate
        healthPct > 20 -> HealthPoor
        else -> HealthCritical
    }

    val statusColor = getStatusColor(battery.status)
    val statusEmoji = when (battery.status) {
        BatteryStatus.CHARGED -> "⚡"
        BatteryStatus.DISCHARGED -> "▼"
        BatteryStatus.STORAGE -> "◆"
        BatteryStatus.OUT_OF_SERVICE -> "✕"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ligne 1 : ID + statut
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // QR ID badge
                Text(
                    text = battery.qrCodeId.ifEmpty { "SF-${battery.id}" },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )

                // Statut dot
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = getStatusText(battery.status),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Jauge de santé circulaire
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(64.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 5.dp,
                    strokeCap = StrokeCap.Round
                )
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(64.dp),
                    color = healthColor,
                    strokeWidth = 5.dp,
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "$healthPct%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = healthColor
                )
            }

            Spacer(Modifier.height(8.dp))

            // Nom batterie
            Text(
                text = "${battery.brand} ${battery.model}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            // Infos compactes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${battery.cells}S ${battery.capacity}mAh",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = "${battery.cycleCount}cy",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
