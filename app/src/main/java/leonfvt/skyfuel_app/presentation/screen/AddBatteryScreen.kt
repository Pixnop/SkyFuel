package leonfvt.skyfuel_app.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import leonfvt.skyfuel_app.presentation.component.BatteryForm
import leonfvt.skyfuel_app.presentation.viewmodel.state.AddBatteryEvent
import leonfvt.skyfuel_app.presentation.viewmodel.state.AddBatteryState

/**
 * Écran d'ajout d'une nouvelle batterie
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBatteryScreen(
    state: AddBatteryState,
    onEvent: (AddBatteryEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollState = rememberScrollState()
    
    // État pour la boîte de dialogue d'abandon
    var showDiscardDialog by remember { mutableStateOf(false) }
    
    // Affichage de la boîte de dialogue d'abandon
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Annuler les modifications") },
            text = { Text("Voulez-vous vraiment annuler? Toutes les informations saisies seront perdues.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEvent(AddBatteryEvent.NavigateBack)
                        showDiscardDialog = false
                    }
                ) {
                    Text("Oui, annuler")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Continuer l'édition")
                }
            }
        )
    }
    
    // Animation de succès
    var showSuccessAnimation by remember { mutableStateOf(false) }
    
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            showSuccessAnimation = true
            delay(1500)
            onEvent(AddBatteryEvent.NavigateBack)
        }
    }
    
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Ajouter une batterie") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (state.isFormModified) {
                                showDiscardDialog = true
                            } else {
                                onEvent(AddBatteryEvent.NavigateBack)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                // Titre et sous-titre
                Text(
                    text = "Informations de la batterie",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Remplissez les informations pour ajouter une nouvelle batterie à votre inventaire",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Formulaire de batterie
                BatteryForm(
                    state = state,
                    onBrandChange = { onEvent(AddBatteryEvent.UpdateBrand(it)) },
                    onModelChange = { onEvent(AddBatteryEvent.UpdateModel(it)) },
                    onSerialNumberChange = { onEvent(AddBatteryEvent.UpdateSerialNumber(it)) },
                    onBatteryTypeChange = { onEvent(AddBatteryEvent.UpdateBatteryType(it)) },
                    onCellsChange = { onEvent(AddBatteryEvent.UpdateCells(it)) },
                    onCapacityChange = { onEvent(AddBatteryEvent.UpdateCapacity(it)) },
                    onPurchaseDateChange = { onEvent(AddBatteryEvent.UpdatePurchaseDate(it)) },
                    onNotesChange = { onEvent(AddBatteryEvent.UpdateNotes(it)) }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Bouton de soumission
                Button(
                    onClick = { onEvent(AddBatteryEvent.SubmitBattery) },
                    enabled = state.isFormValid && !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Ajouter la batterie")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Animation de succès
            AnimatedVisibility(
                visible = showSuccessAnimation,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(72.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Batterie ajoutée avec succès!",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}