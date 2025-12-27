package leonfvt.skyfuel_app.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import leonfvt.skyfuel_app.presentation.viewmodel.state.SortOption

/**
 * Composant pour sélectionner l'option de tri des batteries
 */
@Composable
fun SortSelector(
    currentSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.Sort,
                contentDescription = "Trier",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (option.name.endsWith("ASC") ||
                                    option.name.endsWith("LOW") ||
                                    option.name.endsWith("OLDEST"))
                                    Icons.Default.ArrowUpward
                                else
                                    Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (currentSort == option)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = getSortOptionLabel(option),
                                color = if (currentSort == option)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    onClick = {
                        onSortSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Retourne le libellé français pour une option de tri
 */
private fun getSortOptionLabel(option: SortOption): String {
    return when (option) {
        SortOption.NAME_ASC -> "Nom (A-Z)"
        SortOption.NAME_DESC -> "Nom (Z-A)"
        SortOption.DATE_NEWEST -> "Plus récent"
        SortOption.DATE_OLDEST -> "Plus ancien"
        SortOption.CAPACITY_HIGH -> "Capacité (haute)"
        SortOption.CAPACITY_LOW -> "Capacité (basse)"
        SortOption.CYCLES_HIGH -> "Cycles (haut)"
        SortOption.CYCLES_LOW -> "Cycles (bas)"
    }
}
