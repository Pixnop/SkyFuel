package leonfvt.skyfuel_app.presentation.component

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * Liste chronologique de l'historique d'une batterie
 */
@Composable
fun BatteryHistoryList(
    history: List<BatteryHistory>,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when {
            isLoading -> {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            history.isEmpty() -> {
                EmptyHistory(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.Center)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = history,
                        key = { it.id }
                    ) { entry ->
                        HistoryItem(entry = entry)
                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
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
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Aucun historique disponible",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "Les activités de la batterie apparaîtront ici",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

/**
 * Élément individuel d'historique
 */
@Composable
fun HistoryItem(
    entry: BatteryHistory,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icône d'événement avec fond coloré
        EventIcon(
            eventType = entry.eventType,
            impact = entry.getEventImpact()
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Contenu de l'événement
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = entry.getDescription(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            if (entry.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = entry.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Date et heure
            Text(
                text = entry.timestamp.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Icône pour les différents types d'événements
 */
@Composable
fun EventIcon(
    eventType: BatteryEventType,
    impact: EventImpact,
    modifier: Modifier = Modifier
) {
    // Paramètres visuels selon le type d'événement
    val (icon, backgroundColor) = when (eventType) {
        BatteryEventType.STATUS_CHANGE -> Icons.Default.Battery5Bar to getImpactColor(impact)
        BatteryEventType.CYCLE_COMPLETED -> Icons.Default.Battery4Bar to getImpactColor(impact)
        BatteryEventType.VOLTAGE_READING -> Icons.Default.ElectricBolt to Color(0xFF2196F3) // Bleu
        BatteryEventType.NOTE_ADDED -> Icons.Default.Note to Color(0xFF9C27B0) // Violet
        BatteryEventType.MAINTENANCE -> Icons.Default.Settings to Color(0xFF009688) // Teal
    }
    
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(backgroundColor.copy(alpha = 0.1f))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = backgroundColor
        )
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