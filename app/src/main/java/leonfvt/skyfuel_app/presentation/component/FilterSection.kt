package leonfvt.skyfuel_app.presentation.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.presentation.theme.SkyFuelTheme

/**
 * Section de filtrage des batteries par statut
 */
@Composable
fun FilterSection(
    currentFilter: BatteryStatus?,
    onFilterSelected: (BatteryStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            text = "Toutes",
            selected = currentFilter == null,
            color = MaterialTheme.colorScheme.primary,
            onClick = { onFilterSelected(null) },
            modifier = Modifier.weight(1f)
        )
        
        FilterChip(
            text = "Chargées",
            selected = currentFilter == BatteryStatus.CHARGED,
            color = Color(0xFF4CAF50), // Vert
            onClick = { onFilterSelected(BatteryStatus.CHARGED) },
            modifier = Modifier.weight(1f)
        )
        
        FilterChip(
            text = "Déchargées",
            selected = currentFilter == BatteryStatus.DISCHARGED,
            color = Color(0xFFFFC107), // Jaune
            onClick = { onFilterSelected(BatteryStatus.DISCHARGED) },
            modifier = Modifier.weight(1f)
        )
    }
    
    Spacer(modifier = Modifier.height(4.dp))
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            text = "Stockage",
            selected = currentFilter == BatteryStatus.STORAGE,
            color = Color(0xFF2196F3), // Bleu
            onClick = { onFilterSelected(BatteryStatus.STORAGE) },
            modifier = Modifier.weight(1f)
        )
        
        FilterChip(
            text = "Hors service",
            selected = currentFilter == BatteryStatus.OUT_OF_SERVICE,
            color = Color(0xFFE91E63), // Rose
            onClick = { onFilterSelected(BatteryStatus.OUT_OF_SERVICE) },
            modifier = Modifier.weight(1f)
        )
        
        // Espace vide pour équilibrer la dernière ligne
        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * Composant de filtre cliquable
 */
@Composable
fun FilterChip(
    text: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animations
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) color.copy(alpha = 0.1f) else Color.Transparent,
        animationSpec = tween(300),
        label = "Background color animation"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (selected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        animationSpec = tween(300),
        label = "Border color animation"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (selected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        animationSpec = tween(300),
        label = "Text color animation"
    )
    
    val checkScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(150),
        label = "Check mark scale animation"
    )
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.scale(checkScale).size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
            }
            
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = textColor
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FilterSectionPreview() {
    SkyFuelTheme {
        FilterSection(
            currentFilter = BatteryStatus.CHARGED,
            onFilterSelected = {}
        )
    }
}