package leonfvt.skyfuel_app.presentation.component

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/**
 * Dialog pour générer et imprimer des étiquettes QR code par lot
 */
@Composable
fun QrLabelPrintDialog(
    batteries: List<Battery>,
    labelGenerator: QrLabelPdfGenerator,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedIds by remember { mutableStateOf(batteries.map { it.id }.toSet()) }
    var labelSize by remember { mutableStateOf(QrLabelPdfGenerator.LabelSize.MEDIUM) }
    var copies by remember { mutableStateOf(1) }
    var isGenerating by remember { mutableStateOf(false) }

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
                // Taille d'étiquette
                Text("Taille des étiquettes", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QrLabelPdfGenerator.LabelSize.entries.forEach { size ->
                        FilterChip(
                            selected = labelSize == size,
                            onClick = { labelSize = size },
                            label = { Text(size.displayName, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Copies
                Text("Copies par batterie", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1, 2, 3).forEach { n ->
                        FilterChip(
                            selected = copies == n,
                            onClick = { copies = n },
                            label = { Text("×$n") }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Sélection des batteries
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${selectedIds.size}/${batteries.size} sélectionnées",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        selectedIds = if (selectedIds.size == batteries.size) emptySet()
                        else batteries.map { it.id }.toSet()
                    }) {
                        Text(if (selectedIds.size == batteries.size) "Désélectionner" else "Tout sélectionner")
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Liste des batteries avec checkboxes
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(batteries, key = { it.id }) { battery ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedIds = if (selectedIds.contains(battery.id))
                                        selectedIds - battery.id
                                    else
                                        selectedIds + battery.id
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedIds.contains(battery.id),
                                onCheckedChange = {
                                    selectedIds = if (it) selectedIds + battery.id else selectedIds - battery.id
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    QrCodeGenerator.generateShortId(battery.id),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "${battery.brand} ${battery.model}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                "${battery.type.name} ${battery.cells}S",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Info : nombre total d'étiquettes
                val totalLabels = selectedIds.size * copies
                Spacer(Modifier.height(8.dp))
                Text(
                    "$totalLabels étiquettes seront générées",
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
                    scope.launch {
                        val file = withContext(Dispatchers.IO) {
                            labelGenerator.generateLabelSheet(context, selectedBatteries, labelSize, copies)
                        }
                        isGenerating = false
                        if (file != null) {
                            sharePdf(context, file)
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Erreur lors de la génération", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = selectedIds.isNotEmpty() && !isGenerating
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isGenerating) "Génération..." else "Générer le PDF")
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
        context.startActivity(Intent.createChooser(intent, "Imprimer les étiquettes").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Toast.makeText(context, "Erreur de partage: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
