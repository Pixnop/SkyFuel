package leonfvt.skyfuel_app.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import leonfvt.skyfuel_app.domain.model.AlertPriority
import leonfvt.skyfuel_app.domain.model.AlertType
import leonfvt.skyfuel_app.domain.model.BatteryAlert

/**
 * Couleurs selon la priorité
 */
@Composable
fun AlertPriority.toColor(): Color {
    return when (this) {
        AlertPriority.CRITICAL -> Color(0xFFD32F2F) // Rouge foncé
        AlertPriority.HIGH -> Color(0xFFF44336)     // Rouge
        AlertPriority.MEDIUM -> Color(0xFFFF9800)   // Orange
        AlertPriority.LOW -> Color(0xFF2196F3)      // Bleu
    }
}

/**
 * Icône selon le type d'alerte
 */
fun AlertType.toIcon(): ImageVector {
    return when (this) {
        AlertType.NEEDS_CHARGING -> Icons.Default.ElectricBolt
        AlertType.LOW_HEALTH -> Icons.Default.HealthAndSafety
        AlertType.MAINTENANCE_DUE -> Icons.Default.Build
        AlertType.HIGH_CYCLE_COUNT -> Icons.Default.Loop
    }
}

/**
 * Bannière compacte pour afficher le nombre d'alertes
 */
@Composable
fun AlertSummaryBanner(
    alerts: List<BatteryAlert>,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (alerts.isEmpty()) return
    
    val criticalCount = alerts.count { it.priority == AlertPriority.CRITICAL || it.priority == AlertPriority.HIGH }
    val backgroundColor by animateColorAsState(
        targetValue = when {
            alerts.any { it.priority == AlertPriority.CRITICAL } -> Color(0xFFD32F2F)
            alerts.any { it.priority == AlertPriority.HIGH } -> Color(0xFFF44336)
            alerts.any { it.priority == AlertPriority.MEDIUM } -> Color(0xFFFF9800)
            else -> Color(0xFF2196F3)
        },
        label = "bannerColor"
    )
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onExpandClick() },
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = Color.White,
                            contentColor = backgroundColor
                        ) {
                            Text("${alerts.size}")
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                
                Column {
                    Text(
                        text = if (criticalCount > 0) 
                            "$criticalCount alerte(s) importante(s)" 
                        else 
                            "${alerts.size} alerte(s)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Appuyez pour voir les détails",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Voir les alertes",
                tint = Color.White
            )
        }
    }
}

/**
 * Carte d'alerte individuelle
 */
@Composable
fun AlertCard(
    alert: BatteryAlert,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priorityColor = alert.priority.toColor()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = priorityColor.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Icône avec indicateur de priorité
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(priorityColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = alert.type.toIcon(),
                    contentDescription = null,
                    tint = priorityColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Contenu
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = alert.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = priorityColor
                    )
                    
                    // Badge de priorité pour CRITICAL/HIGH
                    if (alert.priority == AlertPriority.CRITICAL || alert.priority == AlertPriority.HIGH) {
                        Surface(
                            color = priorityColor,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (alert.priority == AlertPriority.CRITICAL) "URGENT" else "IMPORTANT",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = alert.batteryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Bouton fermer
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Ignorer",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Section d'alertes expansible pour l'écran principal
 */
@Composable
fun AlertsSection(
    alerts: List<BatteryAlert>,
    onAlertClick: (BatteryAlert) -> Unit,
    onDismissAlert: (BatteryAlert) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    if (alerts.isEmpty()) return
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Bannière résumée ou liste complète
        AnimatedVisibility(
            visible = !isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            AlertSummaryBanner(
                alerts = alerts,
                onExpandClick = { isExpanded = true }
            )
        }
        
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                // En-tête avec bouton réduire
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Alertes (${alerts.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    TextButton(onClick = { isExpanded = false }) {
                        Icon(
                            imageVector = Icons.Default.ExpandLess,
                            contentDescription = "Réduire",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Réduire")
                    }
                }
                
                // Liste des alertes
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    alerts.take(5).forEach { alert ->
                        AlertCard(
                            alert = alert,
                            onDismiss = { onDismissAlert(alert) },
                            onClick = { onAlertClick(alert) }
                        )
                    }
                    
                    if (alerts.size > 5) {
                        Text(
                            text = "+${alerts.size - 5} autres alertes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
