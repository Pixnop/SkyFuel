package leonfvt.skyfuel_app.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryUnknown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.presentation.component.BatteryActionsCard
import leonfvt.skyfuel_app.presentation.component.BatteryDetailHeader
import leonfvt.skyfuel_app.presentation.component.BatteryHistoryList
import leonfvt.skyfuel_app.presentation.viewmodel.state.BatteryDetailEvent
import leonfvt.skyfuel_app.presentation.viewmodel.state.BatteryDetailState

/**
 * Écran de détails d'une batterie
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryDetailScreen(
    state: BatteryDetailState,
    onEvent: (BatteryDetailEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollState = rememberScrollState()
    
    // État pour la boîte de dialogue de confirmation de suppression
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Affichage de la boîte de dialogue de confirmation
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer la batterie") },
            text = { Text("Êtes-vous sûr de vouloir supprimer cette batterie ? Cette action ne peut pas être annulée.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEvent(BatteryDetailEvent.DeleteBattery)
                        showDeleteDialog = false
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
    
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Détails de la batterie") },
                navigationIcon = {
                    IconButton(onClick = { onEvent(BatteryDetailEvent.NavigateBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Retour"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {  /* Fonctionnalité de partage */ }) {
                        Icon(
                            imageVector = Icons.Default.Share, 
                            contentDescription = "Partager"
                        )
                    }
                    
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete, 
                            contentDescription = "Supprimer"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* Fonctionnalité de QR code */ },
                icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                text = { Text("QR Code") },
                expanded = !scrollState.canScrollForward || scrollState.value == 0
            )
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "Screen state animation"
        ) { currentState ->
            when {
                currentState.isLoading -> {
                    LoadingContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                }
                currentState.error != null -> {
                    ErrorContent(
                        errorMessage = currentState.error,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                }
                currentState.battery == null -> {
                    NotFoundContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                }
                else -> {
                    DetailContent(
                        state = currentState,
                        onStatusChange = { newStatus ->
                            onEvent(BatteryDetailEvent.UpdateStatus(newStatus))
                        },
                        onVoltageRecord = { voltage, notes ->
                            onEvent(BatteryDetailEvent.RecordVoltage(voltage.toFloat(), notes))
                        },
                        onAddNote = { note ->
                            onEvent(BatteryDetailEvent.AddNote(note))
                        },
                        onMaintenance = {
                            // Navigation vers l'écran de maintenance ou dialogue
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                }
            }
        }
    }
}

/**
 * Contenu pendant le chargement
 */
@Composable
fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp)
        )
    }
}

/**
 * Contenu en cas d'erreur
 */
@Composable
fun ErrorContent(
    errorMessage: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Une erreur est survenue",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Contenu quand la batterie n'est pas trouvée
 */
@Composable
fun NotFoundContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.BatteryUnknown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Batterie non trouvée",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "La batterie que vous recherchez semble ne plus exister.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

/**
 * Contenu principal avec les détails
 */
@Composable
fun DetailContent(
    state: BatteryDetailState,
    onStatusChange: (BatteryStatus) -> Unit,
    onVoltageRecord: (String, String) -> Unit,
    onAddNote: (String) -> Unit,
    onMaintenance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val battery = state.battery ?: return
    
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Entête avec détails de la batterie
        BatteryDetailHeader(battery = battery)
        
        // Section des actions
        BatteryActionsCard(
            currentStatus = battery.status,
            onStatusChange = onStatusChange,
            onVoltageRecord = onVoltageRecord,
            onAddNote = onAddNote,
            onMaintenance = onMaintenance
        )
        
        // Section d'historique
        Text(
            text = "Historique",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        BatteryHistoryList(
            history = state.batteryHistory,
            isLoading = state.isHistoryLoading,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Espace en bas pour éviter que le FAB ne cache du contenu
        Spacer(modifier = Modifier.height(80.dp))
    }
}