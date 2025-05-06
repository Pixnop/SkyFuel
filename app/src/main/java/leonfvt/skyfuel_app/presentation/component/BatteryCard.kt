package leonfvt.skyfuel_app.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.rounded.HistoryToggleOff
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.model.BatteryType
import leonfvt.skyfuel_app.presentation.theme.HealthCritical
import leonfvt.skyfuel_app.presentation.theme.HealthExcellent
import leonfvt.skyfuel_app.presentation.theme.HealthGood
import leonfvt.skyfuel_app.presentation.theme.HealthModerate
import leonfvt.skyfuel_app.presentation.theme.HealthPoor
import leonfvt.skyfuel_app.presentation.theme.SkyFuelTheme
import leonfvt.skyfuel_app.presentation.theme.StatusAvailable
import leonfvt.skyfuel_app.presentation.theme.StatusDecommissioned
import leonfvt.skyfuel_app.presentation.theme.StatusInUse
import leonfvt.skyfuel_app.presentation.theme.StatusMaintenance
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Composant de carte moderne pour afficher les informations d'une batterie
 * 
 * Utilise des animations, des indicateurs visuels améliorés et un design Material 3
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
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "Health progress animation"
    )
    
    // Animation de la couleur du statut
    val statusColor by animateColorAsState(
        targetValue = getStatusColor(battery.status),
        animationSpec = tween(durationMillis = 500),
        label = "Status color animation"
    )
    
    // Animation du chargement initial
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    // Calculer la santé de la batterie
    val healthPercentage = battery.getHealthPercentage()
    val healthText = "$healthPercentage%"
    
    // Définir la couleur de la santé
    val healthColor = when {
        healthPercentage > 80 -> HealthExcellent
        healthPercentage > 60 -> HealthGood
        healthPercentage > 40 -> HealthModerate
        healthPercentage > 20 -> HealthPoor
        else -> HealthCritical
    }
    
    // Lancer l'animation de progression au chargement
    LaunchedEffect(healthPercentage) {
        progress = healthPercentage / 100f
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(300)) + 
               slideInVertically(animationSpec = tween(400), initialOffsetY = { it / 5 })
    ) {
        ElevatedCard(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable(onClick = onClick)
                .graphicsLayer {
                    shadowElevation = 8f
                    shape = RoundedCornerShape(24.dp)
                },
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header with status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Status indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusIndicator(
                            status = battery.status,
                            color = statusColor,
                            modifier = Modifier.size(28.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = getStatusText(battery.status),
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColor
                        )
                    }
                    
                    // Battery type
                    BatteryTypeIndicator(type = battery.type)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Main battery info
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left section with main info
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${battery.brand} ${battery.model}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "S/N: ${battery.serialNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cell and capacity info
                            Icon(
                                imageVector = Icons.Rounded.Memory,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            Text(
                                text = "${battery.cells}S - ${battery.capacity} mAh",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1
                            )
                        }
                    }
                    
                    // Cycle count with icon
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HistoryToggleOff,
                                contentDescription = "Cycle count",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        Text(
                            text = "${battery.cycleCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Health indicator
                HealthProgressBar(
                    progress = animatedProgress,
                    healthColor = healthColor,
                    healthText = healthText
                )
            }
        }
    }
}

/**
 * Indicateur visuel moderne du statut de la batterie
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
            modifier = Modifier.size(28.dp),
            color = color.copy(alpha = 0.2f),
            strokeWidth = 2.dp
        )
        
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Indicateur moderne pour le type de batterie
 */
@Composable
fun BatteryTypeIndicator(type: BatteryType) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = type.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Barre de progression améliorée pour la santé de la batterie
 */
@Composable
fun HealthProgressBar(
    progress: Float,
    healthColor: Color,
    healthText: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "État de santé",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = healthText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = healthColor
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(healthColor)
            )
        }
    }
}

/**
 * Renvoie la couleur correspondant à un statut
 */
fun getStatusColor(status: BatteryStatus): Color {
    return when (status) {
        BatteryStatus.CHARGED -> StatusAvailable      // Vert
        BatteryStatus.DISCHARGED -> StatusMaintenance // Orange
        BatteryStatus.STORAGE -> StatusInUse          // Bleu
        BatteryStatus.OUT_OF_SERVICE -> StatusDecommissioned // Gris
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