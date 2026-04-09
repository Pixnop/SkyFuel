package leonfvt.skyfuel_app.presentation.component

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.util.QrCodeGenerator
import leonfvt.skyfuel_app.util.QrLabelPdfGenerator

@Composable
fun QrLabelPrintDialog(
    batteries: List<Battery>,
    labelGenerator: QrLabelPdfGenerator,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedIds by remember { mutableStateOf(batteries.map { it.id }.toSet()) }
    // Taille par défaut appliquée aux nouvelles sélections
    var defaultPreset by remember { mutableStateOf<QrLabelPdfGenerator.LabelSize?>(QrLabelPdfGenerator.LabelSize.MEDIUM) }
    var customWidthMm by remember { mutableFloatStateOf(25f) }
    var customHeightMm by remember { mutableFloatStateOf(35f) }
    // Taille individuelle par batterie (id -> LabelSize, null = custom global)
    var perBatterySize by remember {
        mutableStateOf(batteries.associate { it.id to QrLabelPdfGenerator.LabelSize.MEDIUM as QrLabelPdfGenerator.LabelSize? })
    }
    var copies by remember { mutableIntStateOf(1) }
    var isGenerating by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Batteries filtrées par recherche
    val filteredBatteries = remember(batteries, searchQuery) {
        if (searchQuery.isBlank()) batteries
        else batteries.filter { b ->
            val query = searchQuery.lowercase()
            b.brand.lowercase().contains(query) ||
            b.model.lowercase().contains(query) ||
            b.serialNumber.lowercase().contains(query) ||
            QrCodeGenerator.generateShortId(b.id).lowercase().contains(query)
        }
    }

    // Dimensions custom globales
    val customDimensions = QrLabelPdfGenerator.LabelDimensions(customWidthMm, customHeightMm)

    // Résoudre les dimensions pour une batterie
    fun dimensionsFor(batteryId: Long): QrLabelPdfGenerator.LabelDimensions {
        val size = perBatterySize[batteryId]
        return size?.toDimensions() ?: customDimensions
    }

    AlertDialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Print, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Imprimer les étiquettes QR")
            }
        },
        text = {
            Column {
                // Taille par défaut
                Text("Taille par défaut", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    QrLabelPdfGenerator.LabelSize.entries.forEach { size ->
                        FilterChip(
                            selected = defaultPreset == size,
                            onClick = {
                                defaultPreset = size
                                // Appliquer à toutes les batteries sélectionnées
                                perBatterySize = perBatterySize.mapValues { (id, _) ->
                                    if (id in selectedIds) size else perBatterySize[id]
                                }
                            },
                            label = { Text(size.displayName, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    FilterChip(
                        selected = defaultPreset == null,
                        onClick = {
                            defaultPreset = null
                            perBatterySize = perBatterySize.mapValues { (id, _) ->
                                if (id in selectedIds) null else perBatterySize[id]
                            }
                        },
                        label = { Text("Perso", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                // Sliders pour taille personnalisée globale
                if (defaultPreset == null) {
                    Spacer(Modifier.height(4.dp))
                    Text("Largeur : ${customWidthMm.toInt()} mm", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = customWidthMm,
                        onValueChange = { customWidthMm = it },
                        valueRange = 15f..60f,
                        steps = 44
                    )
                    Text("Hauteur : ${customHeightMm.toInt()} mm", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = customHeightMm,
                        onValueChange = { customHeightMm = it },
                        valueRange = 20f..80f,
                        steps = 59
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Copies
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Copies :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    listOf(1, 2, 3, 5).forEach { n ->
                        FilterChip(
                            selected = copies == n,
                            onClick = { copies = n },
                            label = { Text("x$n") }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Recherche + sélection
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${selectedIds.size}/${batteries.size}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        selectedIds = if (selectedIds.size == batteries.size) emptySet()
                        else batteries.map { it.id }.toSet()
                    }) {
                        Text(if (selectedIds.size == batteries.size) "Aucune" else "Toutes")
                    }
                }

                // Barre de recherche
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher...", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(Modifier.height(4.dp))

                // Liste des batteries avec taille individuelle
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(filteredBatteries, key = { it.id }) { battery ->
                        val isSelected = selectedIds.contains(battery.id)
                        val batterySize = perBatterySize[battery.id]
                        val dims = dimensionsFor(battery.id)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedIds = if (isSelected) selectedIds - battery.id
                                    else selectedIds + battery.id
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    selectedIds = if (it) selectedIds + battery.id else selectedIds - battery.id
                                }
                            )
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        QrCodeGenerator.generateShortId(battery.id),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "${battery.brand} ${battery.model}",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1
                                    )
                                }
                                // Sélecteur de taille par batterie
                                if (isSelected) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        QrLabelPdfGenerator.LabelSize.entries.forEach { size ->
                                            FilterChip(
                                                selected = batterySize == size,
                                                onClick = {
                                                    perBatterySize = perBatterySize + (battery.id to size)
                                                },
                                                label = {
                                                    Text(size.displayName, style = MaterialTheme.typography.labelSmall)
                                                },
                                                modifier = Modifier.height(24.dp)
                                            )
                                        }
                                        FilterChip(
                                            selected = batterySize == null,
                                            onClick = {
                                                perBatterySize = perBatterySize + (battery.id to null)
                                            },
                                            label = {
                                                Text(
                                                    "${dims.widthMm.toInt()}x${dims.heightMm.toInt()}",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            },
                                            modifier = Modifier.height(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Résumé
                val totalLabels = selectedIds.size * copies
                val distinctSizes = selectedIds.map { dimensionsFor(it) }
                    .distinct()
                    .joinToString(", ") { "${it.widthMm.toInt()}x${it.heightMm.toInt()}" }
                Spacer(Modifier.height(4.dp))
                Text(
                    "$totalLabels etiquettes ($distinctSizes mm)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isGenerating = true
                    val selectedBatteries = batteries.filter { it.id in selectedIds }
                    val entries = selectedBatteries.flatMap { battery ->
                        val dims = dimensionsFor(battery.id)
                        List(copies) { QrLabelPdfGenerator.LabelEntry(battery, dims) }
                    }
                    scope.launch {
                        val file = withContext(Dispatchers.IO) {
                            labelGenerator.generateLabelSheet(context, entries)
                        }
                        isGenerating = false
                        if (file != null) {
                            sharePdf(context, file)
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Erreur lors de la generation", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = selectedIds.isNotEmpty() && !isGenerating
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isGenerating) "Generation..." else "Generer le PDF")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isGenerating) {
                Text("Annuler")
            }
        }
    )
}

private fun sharePdf(context: Context, file: java.io.File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Imprimer les etiquettes").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Toast.makeText(context, "Erreur de partage: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
