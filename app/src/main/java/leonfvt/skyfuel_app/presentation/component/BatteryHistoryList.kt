package leonfvt.skyfuel_app.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import leonfvt.skyfuel_app.domain.model.BatteryEventType
import leonfvt.skyfuel_app.domain.model.BatteryHistory
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.model.EventImpact
import leonfvt.skyfuel_app.presentation.theme.SkyFuelTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Liste chronologique moderne de l'historique d'une batterie
 * avec animation et visualisation améliorée des événements
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BatteryHistoryList(
    history: List<BatteryHistory>,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        when {
            isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Chargement de l'historique...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            history.isEmpty() -> {
                EmptyHistory(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                        .align(Alignment.Center)
                )
            }
            else -> {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.elevatedCardElevation(
                        defaultElevation = 2.dp
                    ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Titre de la section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = "Historique d'activité",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(350.dp), // Set a fixed height to avoid infinite height issue
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(
                                items = history,
                                key = { it.id }
                            ) { entry ->
                                HistoryItem(
                                    entry = entry,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItemPlacement(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Affichage quand l'historique est vide
 */
@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Aucun historique disponible",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Les activités de la batterie apparaîtront ici",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/**
 * Élément individuel d'historique avec design moderne
 */
@Composable
fun HistoryItem(
    entry: BatteryHistory,
    modifier: Modifier = Modifier
) {
    // Déterminer l'apparence selon le type d'événement
    val impact = entry.getEventImpact()
    val iconAndColorAndLabel: Triple<androidx.compose.ui.graphics.vector.ImageVector, Color, String> = when (entry.eventType) {
        BatteryEventType.STATUS_CHANGE -> Triple(
            Icons.Default.BatteryChargingFull,
            getImpactColor(impact),
            "Changement de statut"
        )
        BatteryEventType.CYCLE_COMPLETED -> Triple(
            Icons.Default.BatteryFull,
            getImpactColor(impact),
            "Cycle complété"
        )
        BatteryEventType.VOLTAGE_READING -> Triple(
            Icons.Default.ElectricBolt,
            Color(0xFF2196F3), // Bleu
            "Lecture de tension"
        )
        BatteryEventType.NOTE_ADDED -> Triple(
            Icons.Default.Note,
            Color(0xFF9C27B0), // Violet
            "Note ajoutée"
        )
        BatteryEventType.MAINTENANCE -> Triple(
            Icons.Default.Settings,
            Color(0xFF009688), // Teal
            "Maintenance"
        )
    }
    val icon = iconAndColorAndLabel.first
    val iconColor = iconAndColorAndLabel.second
    val label = iconAndColorAndLabel.third
    
    OutlinedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icône d'événement avec fond coloré
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Contenu de l'événement
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Type d'événement en chip
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Description principale
                Text(
                    text = entry.getDescription(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Notes additionnelles si présentes
                if (entry.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = entry.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Date et heure stylisées
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = entry.timestamp.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Renvoie une couleur selon l'impact d'un événement
 */
private fun getImpactColor(impact: EventImpact): Color {
    return when (impact) {
        EventImpact.POSITIVE -> Color(0xFF4CAF50) // Vert
        EventImpact.NEUTRAL -> Color(0xFF2196F3) // Bleu
        EventImpact.NEGATIVE -> Color(0xFFF44336) // Rouge
    }
}

@Preview(showBackground = true)
@Composable
fun BatteryHistoryListPreview() {
    SkyFuelTheme {
        val now = LocalDateTime.now()
        val sampleHistory = listOf(
            BatteryHistory(
                id = 1,
                batteryId = 1,
                timestamp = now.minusHours(1),
                eventType = BatteryEventType.STATUS_CHANGE,
                previousStatus = BatteryStatus.CHARGED,
                newStatus = BatteryStatus.DISCHARGED,
                notes = "Utilisation normale pour un vol de 10 minutes"
            ),
            BatteryHistory(
                id = 2,
                batteryId = 1,
                timestamp = now.minusHours(3),
                eventType = BatteryEventType.VOLTAGE_READING,
                voltage = 16.8f,
                notes = "Tension max atteinte"
            ),
            BatteryHistory(
                id = 3,
                batteryId = 1,
                timestamp = now.minusDays(1),
                eventType = BatteryEventType.CYCLE_COMPLETED,
                cycleNumber = 23
            )
        )
        
        BatteryHistoryList(history = sampleHistory)
    }
}