package leonfvt.skyfuel_app.presentation.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.model.BatteryType
import leonfvt.skyfuel_app.presentation.theme.SkyFuelTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Error

/**
 * Entête des détails d'une batterie avec indicateur de santé circulaire
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
        animationSpec = tween(durationMillis = 1500),
        label = "Health progress animation"
    )
    
    // Calculer la santé de la batterie
    val healthPercentage = battery.getHealthPercentage()
    
    // Définir la couleur de la santé
    val healthColor = when {
        healthPercentage > 75 -> Color(0xFF4CAF50) // Vert
        healthPercentage > 50 -> Color(0xFF8BC34A) // Vert clair
        healthPercentage > 25 -> Color(0xFFFFC107) // Jaune
        else -> Color(0xFFF44336) // Rouge
    }
    
    // Lancer l'animation au chargement
    LaunchedEffect(healthPercentage) {
        progress = healthPercentage / 100f
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
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
            // Indicateur de santé circulaire
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(100.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 8.dp,
                    strokeCap = StrokeCap.Round
                )
                
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(100.dp),
                    color = healthColor,
                    strokeWidth = 8.dp,
                    strokeCap = StrokeCap.Round
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icône de statut au centre
                    val icon = when (battery.status) {
                        BatteryStatus.CHARGED -> Icons.Default.BatteryChargingFull
                        BatteryStatus.DISCHARGED -> Icons.Default.BatteryStd
                        BatteryStatus.STORAGE -> Icons.Default.BatteryFull
                        BatteryStatus.OUT_OF_SERVICE -> Icons.Default.Error
                    }
                    
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = getStatusColor(battery.status),
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Text(
                        text = "$healthPercentage%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = healthColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.size(16.dp))
            
            // Informations principales
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${battery.brand} ${battery.model}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BatteryTypeChip(battery.type)
                    StatusChip(battery.status)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "S/N: ${battery.serialNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = "${battery.cells}S - ${battery.capacity} mAh",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = "Cycles: ${battery.cycleCount}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = "Achetée le: ${battery.purchaseDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Affichage du type de batterie (LiPo, etc.)
 */
@Composable
fun BatteryTypeChip(type: BatteryType) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = type.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
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