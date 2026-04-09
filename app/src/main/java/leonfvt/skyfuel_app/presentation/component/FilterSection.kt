package leonfvt.skyfuel_app.presentation.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
 * Section de filtrage en scroll horizontal compact (une seule ligne)
 */
@Composable
fun FilterSection(
    currentFilter: BatteryStatus?,
    onFilterSelected: (BatteryStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(
            text = "Toutes",
            selected = currentFilter == null,
            color = MaterialTheme.colorScheme.primary,
            onClick = { onFilterSelected(null) }
        )
        FilterChip(
            text = "Chargées",
            selected = currentFilter == BatteryStatus.CHARGED,
            color = Color(0xFF4CAF50),
            onClick = { onFilterSelected(BatteryStatus.CHARGED) }
        )
        FilterChip(
            text = "Déchargées",
            selected = currentFilter == BatteryStatus.DISCHARGED,
            color = Color(0xFFFFC107),
            onClick = { onFilterSelected(BatteryStatus.DISCHARGED) }
        )
        FilterChip(
            text = "Stockage",
            selected = currentFilter == BatteryStatus.STORAGE,
            color = Color(0xFF2196F3),
            onClick = { onFilterSelected(BatteryStatus.STORAGE) }
        )
        FilterChip(
            text = "Hors service",
            selected = currentFilter == BatteryStatus.OUT_OF_SERVICE,
            color = Color(0xFFE91E63),
            onClick = { onFilterSelected(BatteryStatus.OUT_OF_SERVICE) }
        )
    }
}

/**
 * Composant de filtre cliquable compact
 */
@Composable
fun FilterChip(
    text: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) color.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = tween(200),
        label = "bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        animationSpec = tween(200),
        label = "border"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        animationSpec = tween(200),
        label = "text"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(100),
        label = "check"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(
                    Icons.Default.Check, null,
                    tint = color,
                    modifier = Modifier
                        .scale(checkScale)
                        .size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(text, style = MaterialTheme.typography.labelSmall, color = textColor)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FilterSectionPreview() {
    SkyFuelTheme {
        FilterSection(currentFilter = BatteryStatus.CHARGED, onFilterSelected = {})
    }
}
