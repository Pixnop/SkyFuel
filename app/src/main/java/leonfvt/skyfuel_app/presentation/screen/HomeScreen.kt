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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.automirrored.filled.BatteryUnknown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Print
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
import leonfvt.skyfuel_app.domain.model.Category
import leonfvt.skyfuel_app.domain.model.getComposeColor
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import leonfvt.skyfuel_app.presentation.component.AlertsSection
import leonfvt.skyfuel_app.presentation.component.BatteryGridCard
import leonfvt.skyfuel_app.presentation.component.DashboardStats
import leonfvt.skyfuel_app.presentation.component.QrLabelPrintDialog
import leonfvt.skyfuel_app.util.QrLabelPdfGenerator
import leonfvt.skyfuel_app.presentation.component.FilterChip
import leonfvt.skyfuel_app.presentation.component.QrScannerDialog
import leonfvt.skyfuel_app.presentation.component.SearchBar
import leonfvt.skyfuel_app.presentation.component.SortSelector
import leonfvt.skyfuel_app.presentation.component.BatteryListShimmer
import leonfvt.skyfuel_app.presentation.component.DashboardStatsShimmer
import leonfvt.skyfuel_app.presentation.viewmodel.state.SortOption
import leonfvt.skyfuel_app.presentation.theme.Info
import leonfvt.skyfuel_app.presentation.theme.StatusAvailable
import leonfvt.skyfuel_app.presentation.theme.StatusDecommissioned
import leonfvt.skyfuel_app.presentation.theme.StatusInUse
import leonfvt.skyfuel_app.presentation.theme.StatusMaintenance
import leonfvt.skyfuel_app.presentation.theme.StatusOutOfService
import leonfvt.skyfuel_app.presentation.theme.Success
import leonfvt.skyfuel_app.presentation.theme.Warning
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
        val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // État pour la boîte de dialogue de scan QR code
    var showQrScanner by remember { mutableStateOf(false) }
    var isSearchingBattery by remember { mutableStateOf(false) }
    var showQrLabelDialog by remember { mutableStateOf(false) }
    
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
    
    // Dialog d'impression d'étiquettes QR
    if (showQrLabelDialog && state.batteries.isNotEmpty()) {
        QrLabelPrintDialog(
            batteries = state.batteries,
            labelGenerator = QrLabelPdfGenerator(),
            onDismiss = { showQrLabelDialog = false }
        )
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
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SkyFuel",
                        style = MaterialTheme.typography.titleLarge,
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
                    
                    // Bouton Impression étiquettes QR
                    IconButton(
                        onClick = { showQrLabelDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Imprimer étiquettes QR",
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
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
                            categories = currentState.categories,
                            filterCategoryId = currentState.filterCategoryId,
                            onSearchQueryChange = { query -> onEvent(BatteryListEvent.Search(query)) },
                            onClearSearch = { onEvent(BatteryListEvent.ClearSearch) },
                            onSortSelected = { option -> onEvent(BatteryListEvent.Sort(option)) },
                            onFilterSelected = { status -> onEvent(BatteryListEvent.Filter(status)) },
                            onFilterByCategory = { categoryId -> onEvent(BatteryListEvent.FilterByCategory(categoryId)) },
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
            imageVector = Icons.AutoMirrored.Filled.BatteryUnknown,
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
                imageVector = Icons.AutoMirrored.Filled.BatteryUnknown,
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
                imageVector = Icons.AutoMirrored.Filled.BatteryUnknown,
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
 * Écran principal avec contenu — grille compacte de batteries
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContentScreen(
    batteries: List<Battery>,
    alerts: List<BatteryAlert>,
    searchQuery: String,
    filterStatus: BatteryStatus?,
    sortOption: SortOption,
    categories: List<Category> = emptyList(),
    filterCategoryId: Long? = null,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSortSelected: (SortOption) -> Unit,
    onFilterSelected: (BatteryStatus?) -> Unit,
    onFilterByCategory: (Long?) -> Unit = {},
    onBatteryClick: (Battery) -> Unit,
    onAlertClick: (BatteryAlert) -> Unit,
    onDismissAlert: (BatteryAlert) -> Unit,
    onViewStats: () -> Unit,
    paddingValues: PaddingValues,
    lazyListState: androidx.compose.foundation.lazy.LazyListState
) {
    val stats = BatteryStatistics(
        totalCount = batteries.size,
        chargedCount = batteries.count { it.status == BatteryStatus.CHARGED },
        dischargedCount = batteries.count { it.status == BatteryStatus.DISCHARGED },
        storageCount = batteries.count { it.status == BatteryStatus.STORAGE },
        outOfServiceCount = batteries.count { it.status == BatteryStatus.OUT_OF_SERVICE },
        averageCycleCount = if (batteries.isNotEmpty()) batteries.map { it.cycleCount }.average().toFloat() else 0f
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // En-tête fixe : recherche + filtres compacts
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Recherche + tri
            Row(
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
                SortSelector(
                    currentSort = sortOption,
                    onSortSelected = onSortSelected
                )
            }

            // Filtres statut + catégories en scroll horizontal unifié
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Filtres statut
                FilterChip(text = "Toutes", selected = filterStatus == null && filterCategoryId == null, color = MaterialTheme.colorScheme.primary, onClick = { onFilterSelected(null); onFilterByCategory(null) })
                FilterChip(text = "Chargées", selected = filterStatus == BatteryStatus.CHARGED, color = Success, onClick = { onFilterSelected(BatteryStatus.CHARGED) })
                FilterChip(text = "Déchargées", selected = filterStatus == BatteryStatus.DISCHARGED, color = Warning, onClick = { onFilterSelected(BatteryStatus.DISCHARGED) })
                FilterChip(text = "Stockage", selected = filterStatus == BatteryStatus.STORAGE, color = Info, onClick = { onFilterSelected(BatteryStatus.STORAGE) })
                FilterChip(text = "H.S.", selected = filterStatus == BatteryStatus.OUT_OF_SERVICE, color = StatusOutOfService, onClick = { onFilterSelected(BatteryStatus.OUT_OF_SERVICE) })

                // Séparateur visuel
                if (categories.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    // Filtres catégories
                    categories.forEach { category ->
                        FilterChip(
                            text = category.name,
                            selected = filterCategoryId == category.id,
                            color = category.getComposeColor(),
                            onClick = { onFilterByCategory(if (filterCategoryId == category.id) null else category.id) }
                        )
                    }
                }
            }
        }

        // Contenu scrollable
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Alertes
            if (alerts.isNotEmpty()) {
                item {
                    AlertsSection(
                        alerts = alerts,
                        onAlertClick = onAlertClick,
                        onDismissAlert = onDismissAlert,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // Stats compacts
            item {
                DashboardStats(
                    statistics = stats,
                    onViewFullStats = onViewStats
                )
            }

            // Grille de batteries (2 colonnes)
            val chunked = batteries.chunked(2)
            items(
                count = chunked.size,
                key = { index -> chunked[index].first().id }
            ) { index ->
                val row = chunked[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { battery ->
                        BatteryGridCard(
                            battery = battery,
                            onClick = { onBatteryClick(battery) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Si nombre impair, remplir l'espace
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}