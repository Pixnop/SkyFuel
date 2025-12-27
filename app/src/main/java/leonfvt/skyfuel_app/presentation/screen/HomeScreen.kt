package leonfvt.skyfuel_app.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryUnknown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.Toast
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryAlert
import leonfvt.skyfuel_app.domain.model.BatteryStatistics
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.presentation.component.AlertsSection
import leonfvt.skyfuel_app.presentation.component.BatteryCard
import leonfvt.skyfuel_app.presentation.component.DashboardStats
import leonfvt.skyfuel_app.presentation.component.FilterSection
import leonfvt.skyfuel_app.presentation.component.QrScannerDialog
import leonfvt.skyfuel_app.presentation.component.SearchBar
import leonfvt.skyfuel_app.presentation.component.SortSelector
import leonfvt.skyfuel_app.presentation.component.BatteryListShimmer
import leonfvt.skyfuel_app.presentation.component.DashboardStatsShimmer
import leonfvt.skyfuel_app.presentation.viewmodel.state.SortOption
import leonfvt.skyfuel_app.presentation.theme.StatusAvailable
import leonfvt.skyfuel_app.presentation.theme.StatusDecommissioned
import leonfvt.skyfuel_app.presentation.theme.StatusInUse
import leonfvt.skyfuel_app.presentation.theme.StatusMaintenance
import leonfvt.skyfuel_app.presentation.viewmodel.QrCodeViewModel
import leonfvt.skyfuel_app.presentation.viewmodel.state.BatteryListEvent
import leonfvt.skyfuel_app.presentation.viewmodel.state.BatteryListState

/**
 * Écran d'accueil principal moderne (dashboard) de l'application SkyFuel
 * Utilise les dernières conventions Material 3 et des animations fluides
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    state: BatteryListState,
    onEvent: (BatteryListEvent) -> Unit,
    onNavigateToAddBattery: () -> Unit,
    onNavigateToBatteryDetail: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // État pour la boîte de dialogue de scan QR code
    var showQrScanner by remember { mutableStateOf(false) }
    var isSearchingBattery by remember { mutableStateOf(false) }
    
    // État pour la fonctionnalité de rafraîchissement
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Configuration du state pour PullRefresh
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            onEvent(BatteryListEvent.RefreshList)
        }
    )
    
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            delay(1500) // Délai pour simuler le rafraîchissement
            isRefreshing = false
        }
    }
    
    // Déterminer si le FAB doit être étendu
    val showExtendedFab by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0
        }
    }
    
    // Afficher la boîte de dialogue de scan QR code si nécessaire
    if (showQrScanner) {
        // Créer une nouvelle instance du ViewModel QrCode à chaque scan
        val qrCodeViewModel: QrCodeViewModel = hiltViewModel()
        
        // Réinitialiser l'état du scan au démarrage
        LaunchedEffect(key1 = showQrScanner) {
            qrCodeViewModel.resetScanState()
        }
        
        QrScannerDialog(
            onDismiss = { 
                // Réinitialiser l'état lors de la fermeture
                qrCodeViewModel.resetScanState()
                showQrScanner = false 
            },
            viewModel = qrCodeViewModel,
            onQrCodeScanned = { qrContent ->
                isSearchingBattery = true
                
                // Utiliser l'extension utilitaire pour traiter le QR code
                leonfvt.skyfuel_app.util.QrCodeExtensions.processBatteryQrCode(
                    qrContent = qrContent,
                    viewModel = qrCodeViewModel,
                    scope = coroutineScope,
                    onBatteryFound = { battery ->
                        // Succès : batterie trouvée
                        qrCodeViewModel.resetScanState() // Réinitialiser l'état
                        showQrScanner = false
                        isSearchingBattery = false
                        
                        // Naviguer vers les détails de la batterie
                        onNavigateToBatteryDetail(battery.id)
                        
                        // Afficher un message de confirmation
                        Toast.makeText(
                            context,
                            "Batterie trouvée: ${battery.brand} ${battery.model}",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    onError = { errorMessage ->
                        // Échec : afficher l'erreur
                        Toast.makeText(
                            context,
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                        
                        qrCodeViewModel.resetScanState() // Réinitialiser l'état
                        showQrScanner = false
                        isSearchingBattery = false
                    }
                )
            }
        )
    }
    
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "SkyFuel",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Bouton Scanner QR Code
                    IconButton(
                        onClick = { showQrScanner = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = "Scanner un QR code",
                            modifier = Modifier.size(26.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Bouton Paramètres
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Paramètres",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                )
            )
        },
        floatingActionButton = {
            Box {
                // FAB Principal pour ajouter une batterie
                ExtendedFloatingActionButton(
                    onClick = onNavigateToAddBattery,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Ajouter une batterie"
                        )
                    },
                    text = {
                        AnimatedVisibility(
                            visible = showExtendedFab,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            Text("Ajouter une batterie")
                        }
                    },
                    expanded = showExtendedFab,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                )
                
                // FAB secondaire pour scanner (visible seulement en scroll)
                AnimatedVisibility(
                    visible = !showExtendedFab,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(bottom = 72.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = { showQrScanner = true },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.shadow(4.dp, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scanner un QR code"
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            // Main content with SwipeRefresh
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    fadeIn(spring(stiffness = Spring.StiffnessLow)) togetherWith
                            fadeOut(spring(stiffness = Spring.StiffnessLow))
                },
                label = "Screen content animation"
            ) { currentState ->
                when {
                    currentState.isLoading -> {
                        LoadingScreen(paddingValues = paddingValues)
                    }
                    currentState.error != null -> {
                        ErrorScreen(
                            errorMessage = currentState.error,
                            onRetry = { onEvent(BatteryListEvent.RefreshList) },
                            paddingValues = paddingValues
                        )
                    }
                    currentState.batteries.isEmpty() -> {
                        if (currentState.filterStatus != null) {
                            // Si c'est vide à cause d'un filtre, afficher un message spécifique
                            EmptyFilterScreen(
                                filterStatus = currentState.filterStatus,
                                onClearFilter = { onEvent(BatteryListEvent.Filter(null)) },
                                paddingValues = paddingValues
                            )
                        } else {
                            // Si c'est vraiment vide (pas de batteries du tout)
                            EmptyScreen(
                                onAddBattery = onNavigateToAddBattery,
                                paddingValues = paddingValues
                            )
                        }
                    }
                    else -> {
                        ContentScreen(
                            batteries = currentState.sortedBatteries,
                            alerts = currentState.alerts,
                            searchQuery = currentState.searchQuery,
                            filterStatus = currentState.filterStatus,
                            sortOption = currentState.sortOption,
                            onSearchQueryChange = { query -> onEvent(BatteryListEvent.Search(query)) },
                            onClearSearch = { onEvent(BatteryListEvent.ClearSearch) },
                            onSortSelected = { option -> onEvent(BatteryListEvent.Sort(option)) },
                            onFilterSelected = { status -> onEvent(BatteryListEvent.Filter(status)) },
                            onBatteryClick = { battery -> 
                                onEvent(BatteryListEvent.SelectBattery(battery))
                                onNavigateToBatteryDetail(battery.id)
                            },
                            onAlertClick = { alert ->
                                onNavigateToBatteryDetail(alert.batteryId)
                            },
                            onDismissAlert = { alert ->
                                onEvent(BatteryListEvent.DismissAlert(alert))
                            },
                            onViewStats = onNavigateToStatistics,
                            paddingValues = paddingValues,
                            lazyListState = lazyListState
                        )
                    }
                }
            }
            
            // PullRefreshIndicator
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Écran de chargement moderne avec animation
 */
@Composable
fun LoadingScreen(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // Shimmer pour les stats
        DashboardStatsShimmer(
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Shimmer pour la liste de batteries
        BatteryListShimmer(count = 5)
    }
}

/**
 * Écran d'erreur amélioré
 */
@Composable
fun ErrorScreen(
    errorMessage: String,
    onRetry: () -> Unit,
    paddingValues: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.BatteryUnknown,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Une erreur est survenue",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        androidx.compose.material3.Button(
            onClick = onRetry,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Text(
                text = "Réessayer",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * Écran vide (aucune batterie) avec design moderne
 */
@Composable
fun EmptyScreen(
    onAddBattery: () -> Unit,
    paddingValues: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.BatteryUnknown,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Aucune batterie",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Commencez par ajouter votre première batterie pour suivre son état et son utilisation.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        androidx.compose.material3.ElevatedButton(
            onClick = onAddBattery,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            modifier = Modifier.height(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.size(8.dp))
            
            Text(
                text = "Ajouter une batterie",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/**
 * Écran affiché quand aucune batterie ne correspond au filtre actif (design moderne)
 */
@Composable
fun EmptyFilterScreen(
    filterStatus: BatteryStatus,
    onClearFilter: () -> Unit,
    paddingValues: PaddingValues
) {
    // Détermine le texte à afficher selon le statut
    val statusText = when (filterStatus) {
        BatteryStatus.CHARGED -> "chargées"
        BatteryStatus.DISCHARGED -> "déchargées"
        BatteryStatus.STORAGE -> "en stockage" 
        BatteryStatus.OUT_OF_SERVICE -> "hors service"
    }
    
    // Icône avec couleur spécifique au statut
    val iconTint = when (filterStatus) {
        BatteryStatus.CHARGED -> StatusAvailable
        BatteryStatus.DISCHARGED -> StatusMaintenance
        BatteryStatus.STORAGE -> StatusInUse
        BatteryStatus.OUT_OF_SERVICE -> StatusDecommissioned
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.BatteryUnknown,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = iconTint
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Aucune batterie $statusText",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Il n'y a actuellement aucune batterie correspondant à ce filtre.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        androidx.compose.material3.OutlinedButton(
            onClick = onClearFilter,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            
            Spacer(modifier = Modifier.size(8.dp))
            
            Text(
                text = "Afficher toutes les batteries",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * Écran principal avec contenu (modernisé)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContentScreen(
    batteries: List<Battery>,
    alerts: List<BatteryAlert>,
    searchQuery: String,
    filterStatus: BatteryStatus?,
    sortOption: SortOption,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSortSelected: (SortOption) -> Unit,
    onFilterSelected: (BatteryStatus?) -> Unit,
    onBatteryClick: (Battery) -> Unit,
    onAlertClick: (BatteryAlert) -> Unit,
    onDismissAlert: (BatteryAlert) -> Unit,
    onViewStats: () -> Unit,
    paddingValues: PaddingValues,
    lazyListState: androidx.compose.foundation.lazy.LazyListState
) {
    // Statistiques calculées
    val stats = BatteryStatistics(
        totalCount = batteries.size,
        chargedCount = batteries.count { it.status == BatteryStatus.CHARGED },
        dischargedCount = batteries.count { it.status == BatteryStatus.DISCHARGED },
        storageCount = batteries.count { it.status == BatteryStatus.STORAGE },
        outOfServiceCount = batteries.count { it.status == BatteryStatus.OUT_OF_SERVICE },
        averageCycleCount = batteries.map { it.cycleCount }.average().toFloat()
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // Liste des batteries avec en-têtes
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Barre de recherche (animée et épinglée)
            stickyHeader {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                        .padding(bottom = 8.dp)
                ) {
                    // Barre de recherche avec bouton de tri
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = onSearchQueryChange,
                            onSearch = {},
                            onClear = onClearSearch,
                            modifier = Modifier.weight(1f)
                        )

                        // Sélecteur de tri
                        SortSelector(
                            currentSort = sortOption,
                            onSortSelected = onSortSelected
                        )
                    }

                    // Filtres
                    FilterSection(
                        currentFilter = filterStatus,
                        onFilterSelected = onFilterSelected
                    )
                }
            }
            
            // Section d'alertes (si présentes)
            if (alerts.isNotEmpty()) {
                item {
                    AlertsSection(
                        alerts = alerts,
                        onAlertClick = onAlertClick,
                        onDismissAlert = onDismissAlert,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            
            // Statistiques en haut de la liste
            item {
                DashboardStats(
                    statistics = stats,
                    onViewFullStats = onViewStats
                )
            }
            
            // Liste des batteries avec animations
            items(
                items = batteries,
                key = { it.id }
            ) { battery ->
                BatteryCard(
                    battery = battery,
                    onClick = { onBatteryClick(battery) },
                    modifier = Modifier.animateItemPlacement(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                )
            }
            
            // Espace en bas de la liste
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}