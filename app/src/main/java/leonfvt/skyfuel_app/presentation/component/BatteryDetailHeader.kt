package leonfvt.skyfuel_app.presentation.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import leonfvt.skyfuel_app.presentation.theme.StatusAvailable
import leonfvt.skyfuel_app.presentation.theme.StatusDecommissioned
import leonfvt.skyfuel_app.presentation.theme.StatusInUse
import leonfvt.skyfuel_app.presentation.theme.StatusMaintenance
import leonfvt.skyfuel_app.presentation.theme.SkyFuelTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

/**
 * En-tête moderne des détails d'une batterie avec indicateur de santé circulaire
 * et informations détaillées organisées avec une meilleure visualisation des données
 */
@Composable
fun BatteryDetailHeader(
    battery: Battery,
    modifier: Modifier = Modifier
) {
    // Animation de l'indicateur de santé
    var progress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1500, easing = LinearEasing),
        label = "Health progress animation"
    )
    
    // Calculer la santé de la batterie
    val healthPercentage = battery.getHealthPercentage()
    
    // Définir la couleur de la santé
    val healthColor = when {
        healthPercentage > 80 -> HealthExcellent
        healthPercentage > 60 -> HealthGood
        healthPercentage > 40 -> HealthModerate
        healthPercentage > 20 -> HealthPoor
        else -> HealthCritical
    }
    
    // Lancer l'animation au chargement
    LaunchedEffect(healthPercentage) {
        progress = healthPercentage / 100f
    }
    
    // Calcul de l'âge de la batterie pour l'affichage
    val ageInDays = battery.getAgeInDays()
    val ageDisplay = when {
        ageInDays < 30 -> "$ageInDays jours"
        ageInDays < 365 -> "${ageInDays / 30} mois"
        else -> "${ageInDays / 365} ans"
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Card principale avec les informations essentielles
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 6.dp
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
                // Titre et type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${battery.brand} ${battery.model}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    SuggestionChip(
                        onClick = { /* Non cliquable */ },
                        label = { Text(battery.type.name) },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Memory,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Section principale avec indicateur de santé et informations
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Indicateur de santé circulaire
                    HealthIndicator(
                        healthPercentage = healthPercentage,
                        healthColor = healthColor,
                        progress = animatedProgress,
                        batteryStatus = battery.status,
                        modifier = Modifier.size(140.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Séparateur vertical
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Informations détaillées
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Numéro de série
                        InfoRow(
                            label = "Numéro de série",
                            value = battery.serialNumber,
                            icon = Icons.Rounded.Bolt,
                            iconTint = MaterialTheme.colorScheme.primary
                        )
                        
                        // Capacité et cellules
                        InfoRow(
                            label = "Capacité",
                            value = "${battery.cells}S - ${battery.capacity} mAh",
                            icon = Icons.Filled.Battery4Bar,
                            iconTint = MaterialTheme.colorScheme.secondary
                        )
                        
                        // Cycles
                        InfoRow(
                            label = "Cycles",
                            value = "${battery.cycleCount}",
                            icon = Icons.Rounded.Loop,
                            iconTint = MaterialTheme.colorScheme.tertiary
                        )
                        
                        // Âge
                        InfoRow(
                            label = "Âge",
                            value = ageDisplay,
                            icon = Icons.Rounded.AccessTime,
                            iconTint = MaterialTheme.colorScheme.error
                        )
                        
                        // Date d'achat
                        InfoRow(
                            label = "Achat",
                            value = battery.purchaseDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                            icon = Icons.Rounded.DateRange,
                            iconTint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Statut actuel
                StatusChip(
                    status = battery.status,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

/**
 * Indicateur circulaire de santé amélioré avec animation
 */
@Composable
fun HealthIndicator(
    healthPercentage: Int,
    healthColor: Color,
    progress: Float,
    batteryStatus: BatteryStatus,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Anneau de fond
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.size(140.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = 10.dp,
            strokeCap = StrokeCap.Round
        )
        
        // Anneau de progression
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(140.dp),
            color = healthColor,
            strokeWidth = 10.dp,
            strokeCap = StrokeCap.Round
        )
        
        // Indicateur central avec informations
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icône de statut
            val icon = when (batteryStatus) {
                BatteryStatus.CHARGED -> Icons.Default.BatteryChargingFull
                BatteryStatus.DISCHARGED -> Icons.Default.BatteryStd
                BatteryStatus.STORAGE -> Icons.Default.BatteryFull
                BatteryStatus.OUT_OF_SERVICE -> Icons.Default.Error
            }
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(healthColor.copy(alpha = 0.1f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = healthColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Pourcentage de santé
            Text(
                text = "$healthPercentage%",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = healthColor
            )
            
            Text(
                text = "Santé",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Ligne d'information avec icône, label et valeur
 */
@Composable
fun InfoRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icône avec fond coloré
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.1f))
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Textes
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Étiquette de statut améliorée
 */
@Composable
fun StatusChip(
    status: BatteryStatus,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (status) {
        BatteryStatus.CHARGED -> StatusAvailable to "Chargée"
        BatteryStatus.DISCHARGED -> StatusMaintenance to "Déchargée"
        BatteryStatus.STORAGE -> StatusInUse to "Stockage"
        BatteryStatus.OUT_OF_SERVICE -> StatusDecommissioned to "Hors service"
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = color.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BatteryDetailHeaderPreview() {
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
        
        BatteryDetailHeader(battery = sampleBattery)
    }
}