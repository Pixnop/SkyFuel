package leonfvt.skyfuel_app.presentation.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import leonfvt.skyfuel_app.data.preferences.ThemeMode
import leonfvt.skyfuel_app.domain.service.ExportFormat
import leonfvt.skyfuel_app.presentation.component.NotificationSettingRow
import leonfvt.skyfuel_app.presentation.component.NotificationPermissionCard
import leonfvt.skyfuel_app.presentation.viewmodel.SettingsViewModel
import leonfvt.skyfuel_app.presentation.viewmodel.ThemeViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val themeState by themeViewModel.themeState.collectAsState()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var includeHistory by remember { mutableStateOf(true) }
    var replaceExisting by remember { mutableStateOf(false) }
    var selectedFormat by remember { mutableStateOf(ExportFormat.JSON) }
    
    // File picker pour l'import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.readText()
                if (content != null) {
                    val format = if (it.path?.endsWith(".csv") == true) ExportFormat.CSV else ExportFormat.JSON
                    viewModel.importData(content, format, replaceExisting)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur lors de la lecture du fichier", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    // Gestion de l'export réussi
    LaunchedEffect(state.exportSuccess) {
        state.exportSuccess?.let { export ->
            shareExportFile(context, export.content, export.fileName)
            viewModel.clearExportSuccess()
        }
    }
    
    // Gestion de l'import réussi
    LaunchedEffect(state.importSuccess) {
        state.importSuccess?.let { import ->
            Toast.makeText(
                context,
                "${import.importedCount} batteries importées (${import.skippedCount} ignorées)",
                Toast.LENGTH_LONG
            ).show()
            viewModel.clearImportSuccess()
        }
    }
    
    // Gestion des erreurs
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }
    
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Paramètres") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Carte de permission notifications (s'affiche si non accordée)
            NotificationPermissionCard(
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Section Notifications
            SettingsSection(title = "Notifications") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        NotificationSettingRow(
                            title = "Alertes batteries",
                            description = "Notifications pour batteries faibles ou en mauvais état",
                            enabled = state.alertsEnabled,
                            onEnabledChange = { viewModel.setAlertsEnabled(it) }
                        )
                        
                        HorizontalDivider()
                        
                        NotificationSettingRow(
                            title = "Rappels de charge",
                            description = "Rappels programmés pour charger vos batteries",
                            enabled = state.chargeRemindersEnabled,
                            onEnabledChange = { viewModel.setChargeRemindersEnabled(it) }
                        )
                        
                        HorizontalDivider()
                        
                        NotificationSettingRow(
                            title = "Rappels de maintenance",
                            description = "Notifications pour l'entretien périodique",
                            enabled = state.maintenanceRemindersEnabled,
                            onEnabledChange = { viewModel.setMaintenanceRemindersEnabled(it) }
                        )
                        
                        HorizontalDivider()
                        
                        NotificationSettingRow(
                            title = "Alertes niveau bas",
                            description = "Avertissements pour batteries bientôt déchargées",
                            enabled = state.lowBatteryWarningsEnabled,
                            onEnabledChange = { viewModel.setLowBatteryWarningsEnabled(it) }
                        )
                        
                        HorizontalDivider()
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text("Vibration", style = MaterialTheme.typography.bodyLarge)
                            }
                            Switch(
                                checked = state.vibrationEnabled,
                                onCheckedChange = { viewModel.setVibrationEnabled(it) }
                            )
                        }
                        
                        HorizontalDivider()
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text("Son", style = MaterialTheme.typography.bodyLarge)
                            }
                            Switch(
                                checked = state.soundEnabled,
                                onCheckedChange = { viewModel.setSoundEnabled(it) }
                            )
                        }
                    }
                }
            }
            
            // Section Thème
            SettingsSection(title = "Apparence") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Mode du thème",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeModeButton(
                                icon = Icons.Default.BrightnessMedium,
                                label = "Système",
                                selected = themeState.themeMode == ThemeMode.SYSTEM,
                                onClick = { themeViewModel.setThemeMode(ThemeMode.SYSTEM) },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeModeButton(
                                icon = Icons.Default.LightMode,
                                label = "Clair",
                                selected = themeState.themeMode == ThemeMode.LIGHT,
                                onClick = { themeViewModel.setThemeMode(ThemeMode.LIGHT) },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeModeButton(
                                icon = Icons.Default.DarkMode,
                                label = "Sombre",
                                selected = themeState.themeMode == ThemeMode.DARK,
                                onClick = { themeViewModel.setThemeMode(ThemeMode.DARK) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Couleurs dynamiques",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Text(
                                    text = "Utilise les couleurs du fond d'écran (Android 12+)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Switch(
                                checked = themeState.dynamicColorsEnabled,
                                onCheckedChange = { themeViewModel.setDynamicColorsEnabled(it) }
                            )
                        }
                    }
                }
            }
            
            // Section Export/Import
            SettingsSection(title = "Données") {
                SettingsCard(
                    icon = Icons.Default.CloudUpload,
                    title = "Exporter les données",
                    description = "Sauvegarder vos batteries en JSON ou CSV",
                    onClick = { showExportDialog = true },
                    isLoading = state.isExporting
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                SettingsCard(
                    icon = Icons.Default.CloudDownload,
                    title = "Importer des données",
                    description = "Restaurer depuis une sauvegarde",
                    onClick = { showImportDialog = true },
                    isLoading = state.isImporting
                )
            }
            
            // Section À propos
            SettingsSection(title = "À propos") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "SkyFuel",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Version 1.0",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Application de gestion de batteries de drones",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
    
    // Dialog Export
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Exporter les données") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Choisissez le format d'export:")
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FormatButton(
                            label = "JSON",
                            selected = selectedFormat == ExportFormat.JSON,
                            onClick = { selectedFormat = ExportFormat.JSON }
                        )
                        FormatButton(
                            label = "CSV",
                            selected = selectedFormat == ExportFormat.CSV,
                            onClick = { selectedFormat = ExportFormat.CSV }
                        )
                    }
                    
                    if (selectedFormat == ExportFormat.JSON) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Inclure l'historique")
                            Switch(
                                checked = includeHistory,
                                onCheckedChange = { includeHistory = it }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.exportData(selectedFormat, includeHistory)
                        showExportDialog = false
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exporter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
    
    // Dialog Import
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Importer des données") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Sélectionnez un fichier JSON ou CSV à importer.")
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Remplacer les données existantes")
                            Text(
                                text = "Supprime toutes les batteries avant l'import",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = replaceExisting,
                            onCheckedChange = { replaceExisting = it }
                        )
                    }
                    
                    if (replaceExisting) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    "Attention: toutes vos données actuelles seront supprimées!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        importLauncher.launch("*/*")
                        showImportDialog = false
                    }
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choisir un fichier")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    isLoading: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun FormatButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        Button(onClick = onClick) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick) {
            Text(label)
        }
    }
}

@Composable
private fun ThemeModeButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    }
    
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }
}

private fun shareExportFile(context: Context, content: String, fileName: String) {
    try {
        // Créer un fichier temporaire
        val file = File(context.cacheDir, fileName)
        file.writeText(content)
        
        // Obtenir l'URI via FileProvider
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        // Créer l'intent de partage
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (fileName.endsWith(".json")) "application/json" else "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Partager la sauvegarde"))
    } catch (e: Exception) {
        Toast.makeText(context, "Erreur lors du partage: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
