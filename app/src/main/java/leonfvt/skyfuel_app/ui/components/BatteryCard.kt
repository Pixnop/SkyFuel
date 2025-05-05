package leonfvt.skyfuel_app.ui.components

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import leonfvt.skyfuel_app.data.model.Battery
import leonfvt.skyfuel_app.data.model.BatteryStatus
import leonfvt.skyfuel_app.data.model.BatteryType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Renvoie la couleur correspondant au statut de la batterie
 */
fun getBatteryStatusColor(status: BatteryStatus): Color {
    return when (status) {
        BatteryStatus.CHARGED -> Color(0xFF4CAF50)       // Vert
        BatteryStatus.DISCHARGED -> Color(0xFFFFC107)    // Jaune/Ambre
        BatteryStatus.STORAGE -> Color(0xFF2196F3)       // Bleu
        BatteryStatus.OUT_OF_SERVICE -> Color(0xFFE91E63) // Rose/Rouge
    }
}

/**
 * Renvoie le texte correspondant au statut de la batterie
 */
fun getBatteryStatusText(status: BatteryStatus): String {
    return when (status) {
        BatteryStatus.CHARGED -> "Chargée"
        BatteryStatus.DISCHARGED -> "Déchargée"
        BatteryStatus.STORAGE -> "Stockage"
        BatteryStatus.OUT_OF_SERVICE -> "Hors service"
    }
}

/**
 * Composant représentant une carte de batterie dans la liste
 */
@Composable
fun BatteryCard(
    battery: Battery,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicateur de statut
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(getBatteryStatusColor(battery.status))
                    .border(1.dp, Color.White, CircleShape)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Informations principales
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${battery.brand} ${battery.model}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "S/N: ${battery.serialNumber}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${battery.cells}S - ${battery.capacity} mAh",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Indicateurs secondaires
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = getBatteryStatusText(battery.status),
                    color = getBatteryStatusColor(battery.status),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Cycles: ${battery.cycleCount}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BatteryCardPreview() {
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