package leonfvt.skyfuel_app.presentation.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.model.BatteryType
import leonfvt.skyfuel_app.presentation.theme.SkyFuelTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Composant de carte pour afficher les informations d'une batterie dans la liste
 * 
 * Utilise des animations et des indicateurs visuels pour montrer l'état de la batterie
 */
@Composable
fun BatteryCard(
    battery: Battery,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation de la progression de santé
    var progress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000),
        label = "Health progress animation"
    )
    
    // Animation de la couleur du statut
    val statusColor by animateColorAsState(
        targetValue = getStatusColor(battery.status),
        animationSpec = tween(durationMillis = 500),
        label = "Status color animation"
    )
    
    // Calculer la santé de la batterie
    val healthPercentage = battery.getHealthPercentage()
    val healthText = "$healthPercentage%"
    
    // Définir la couleur de la santé
    val healthColor = when {
        healthPercentage > 75 -> MaterialTheme.colorScheme.primary
        healthPercentage > 50 -> Color(0xFF4CAF50) // Vert
        healthPercentage > 25 -> Color(0xFFFFC107) // Jaune
        else -> Color(0xFFF44336) // Rouge
    }
    
    // Lancer l'animation de progression au chargement
    LaunchedEffect(healthPercentage) {
        progress = healthPercentage / 100f
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icône du statut
            StatusIndicator(
                status = battery.status,
                color = statusColor,
                modifier = Modifier.size(36.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Informations principales
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${battery.brand} ${battery.model}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    BatteryTypeIndicator(
                        type = battery.type
                    )
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = "S/N: ${battery.serialNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = "${battery.cells}S - ${battery.capacity} mAh",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Barre de progression de la santé
                HealthProgressBar(
                    progress = animatedProgress,
                    healthColor = healthColor,
                    healthText = healthText
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Informations supplémentaires
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${battery.cycleCount} cycles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    StatusChip(status = battery.status)
                }
            }
        }
    }
}

/**
 * Indicateur visuel du statut de la batterie
 */
@Composable
fun StatusIndicator(
    status: BatteryStatus,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val icon = when (status) {
            BatteryStatus.CHARGED -> Icons.Default.BatteryChargingFull
            BatteryStatus.DISCHARGED -> Icons.Default.BatteryStd
            BatteryStatus.STORAGE -> Icons.Default.BatteryFull
            BatteryStatus.OUT_OF_SERVICE -> Icons.Default.Error
        }
        
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.size(36.dp),
            color = color.copy(alpha = 0.2f),
            strokeWidth = 4.dp
        )
        
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Indicateur pour le type de batterie
 */
@Composable
fun BatteryTypeIndicator(type: BatteryType) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = type.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/**
 * Barre de progression pour la santé de la batterie
 */
@Composable
fun HealthProgressBar(
    progress: Float,
    healthColor: Color,
    healthText: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = healthColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = healthText,
            style = MaterialTheme.typography.labelMedium,
            color = healthColor
        )
    }
}

/**
 * Indicateur de statut en forme de pastille
 */
@Composable
fun StatusChip(status: BatteryStatus) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(getStatusColor(status).copy(alpha = 0.1f))
            .border(
                width = 1.dp,
                color = getStatusColor(status).copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = getStatusText(status),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = getStatusColor(status)
        )
    }
}

/**
 * Renvoie la couleur correspondant à un statut
 */
fun getStatusColor(status: BatteryStatus): Color {
    return when (status) {
        BatteryStatus.CHARGED -> Color(0xFF4CAF50)      // Vert
        BatteryStatus.DISCHARGED -> Color(0xFFFFC107)   // Jaune
        BatteryStatus.STORAGE -> Color(0xFF2196F3)      // Bleu
        BatteryStatus.OUT_OF_SERVICE -> Color(0xFFE91E63) // Rose
    }
}

/**
 * Renvoie le texte correspondant à un statut
 */
fun getStatusText(status: BatteryStatus): String {
    return when (status) {
        BatteryStatus.CHARGED -> "Chargée"
        BatteryStatus.DISCHARGED -> "Déchargée"
        BatteryStatus.STORAGE -> "Stockage"
        BatteryStatus.OUT_OF_SERVICE -> "Hors service"
    }
}

@Preview(showBackground = true)
@Composable
fun BatteryCardPreview() {
    SkyFuelTheme {
        val sampleBattery = Battery(
            id = 1,
            brand = "TurnigyGraphene",
            model = "4S 75C",
            serialNumber = "TG4S001",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 1300,
            purchaseDate = LocalDate.now().minusMonths(6),
            status = BatteryStatus.CHARGED,
            cycleCount = 25,
            notes = "Pour drone de course"
        )
        
        BatteryCard(
            battery = sampleBattery,
            onClick = {}
        )
    }
}