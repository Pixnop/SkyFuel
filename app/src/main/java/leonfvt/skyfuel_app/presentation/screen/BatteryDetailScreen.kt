package leonfvt.skyfuel_app.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryUnknown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.presentation.component.BatteryActionsCard
import leonfvt.skyfuel_app.presentation.component.BatteryDetailHeader
import leonfvt.skyfuel_app.presentation.component.BatteryHistoryList
import leonfvt.skyfuel_app.presentation.component.BatteryQrCodeDialog
import leonfvt.skyfuel_app.presentation.component.ShareBatteryQrCodeDialog
import leonfvt.skyfuel_app.presentation.component.BatteryDetailShimmer
import leonfvt.skyfuel_app.util.QrCodeUtils
import leonfvt.skyfuel_app.util.rememberHapticFeedback
import leonfvt.skyfuel_app.presentation.viewmodel.state.BatteryDetailEvent
import leonfvt.skyfuel_app.presentation.viewmodel.state.BatteryDetailState

/**
 * Écran moderne de détails d'une batterie
 * Utilise un design Material 3 avec animations fluides et affichage amélioré
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryDetailScreen(
    state: BatteryDetailState,
    onEvent: (BatteryDetailEvent) -> Unit,
    onNavigateToHistory: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    
    // Haptic feedback
    val haptic = rememberHapticFeedback()
    
    // État pour les boîtes de dialogue
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showQrCodeDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    
    // Affichage de la boîte de dialogue de confirmation de suppression
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { 
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Supprimer la batterie") },
            text = { 
                Text(
                    text = "Êtes-vous sûr de vouloir supprimer cette batterie ? Cette action ne peut pas être annulée.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.errorContainer),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    TextButton(
                        onClick = {
                            haptic.performHeavy()
                            onEvent(BatteryDetailEvent.DeleteBattery)
                            showDeleteDialog = false
                        }
                    ) {
                        Text(
                            text = "Supprimer",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(
                        text = "Annuler",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp)
        )
    }
    
    // Affichage de la boîte de dialogue du QR code
    if (showQrCodeDialog && state.battery != null) {
        BatteryQrCodeDialog(
            battery = state.battery,
            onDismiss = { showQrCodeDialog = false },
            onShare = { bitmap ->
                QrCodeUtils.shareQrCode(context, bitmap, state.battery)
            },
            onSave = { bitmap ->
                QrCodeUtils.saveQrCodeToGallery(context, bitmap, state.battery)
            }
        )
    }
    
    // Affichage de la boîte de dialogue de partage complet
    if (showShareDialog && state.battery != null) {
        ShareBatteryQrCodeDialog(
            battery = state.battery,
            onDismiss = { showShareDialog = false },
            onShareFull = { bitmap ->
                QrCodeUtils.shareQrCode(context, bitmap, state.battery, isShareMode = true)
                showShareDialog = false
            },
            onShareSimple = { bitmap ->
                QrCodeUtils.shareQrCode(context, bitmap, state.battery, isShareMode = false)
                showShareDialog = false
            }
        )
    }
    
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        text = "Détails de la batterie",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { onEvent(BatteryDetailEvent.NavigateBack) },
                        modifier = Modifier
                            .padding(8.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Retour",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                },
                actions = {
                    // Action de partage
                    IconButton(
                        onClick = { showShareDialog = true },
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share, 
                            contentDescription = "Partager"
                        )
                    }
                    
                    // Action de suppression
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete, 
                            contentDescription = "Supprimer",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showQrCodeDialog = true },
                icon = { 
                    Icon(
                        imageVector = Icons.Rounded.QrCode2,
                        contentDescription = null
                    )
                },
                text = { Text("QR Code") },
                expanded = !scrollState.canScrollForward || scrollState.value == 0,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            )
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                fadeIn(tween(300)) + slideInVertically(tween(500)) togetherWith 
                fadeOut(tween(300))
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
                        onViewFullHistory = {
                            currentState.battery?.id?.let { batteryId ->
                                onNavigateToHistory(batteryId)
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .verticalScroll(scrollState)
                    )
                }
            }
        }
    }
}

/**
 * Contenu amélioré pendant le chargement
 */
@Composable
fun LoadingContent(modifier: Modifier = Modifier) {
    BatteryDetailShimmer(modifier = modifier)
}

/**
 * Contenu en cas d'erreur avec design amélioré
 */
@Composable
fun ErrorContent(
    errorMessage: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Une erreur est survenue",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Contenu quand la batterie n'est pas trouvée (design amélioré)
 */
@Composable
fun NotFoundContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.BatteryUnknown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(72.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Batterie non trouvée",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "La batterie que vous recherchez n'existe plus ou a été supprimée.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Contenu principal avec les détails (modernisé)
 */
@Composable
fun DetailContent(
    state: BatteryDetailState,
    onStatusChange: (BatteryStatus) -> Unit,
    onVoltageRecord: (String, String) -> Unit,
    onAddNote: (String) -> Unit,
    onMaintenance: () -> Unit,
    onViewFullHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val battery = state.battery ?: return
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // En-tête avec détails de la batterie
        BatteryDetailHeader(battery = battery)
        
        // Section des actions (à moderniser séparément)
        BatteryActionsCard(
            currentStatus = battery.status,
            onStatusChange = onStatusChange,
            onVoltageRecord = onVoltageRecord,
            onAddNote = onAddNote,
            onMaintenance = onMaintenance
        )
        
        // Section d'historique
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Historique d'activité",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            TextButton(onClick = onViewFullHistory) {
                Text(
                    text = "Voir tout",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Text(
            text = "${state.batteryHistory.size} événements",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        BatteryHistoryList(
            history = state.batteryHistory.take(5),
            isLoading = state.isHistoryLoading
        )
        
        // Espace en bas pour éviter que le FAB ne cache du contenu
        Spacer(modifier = Modifier.height(100.dp))
    }
}