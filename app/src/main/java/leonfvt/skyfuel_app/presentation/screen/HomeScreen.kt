package leonfvt.skyfuel_app.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryUnknown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.model.BatteryStatistics
import leonfvt.skyfuel_app.presentation.component.BatteryCard
import leonfvt.skyfuel_app.presentation.component.DashboardStats
import leonfvt.skyfuel_app.presentation.component.FilterSection
import leonfvt.skyfuel_app.presentation.component.SearchBar
import leonfvt.skyfuel_app.presentation.viewmodel.state.BatteryListEvent
import leonfvt.skyfuel_app.presentation.viewmodel.state.BatteryListState

/**
 * Écran d'accueil principal (dashboard) de l'application SkyFuel
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    state: BatteryListState,
    onEvent: (BatteryListEvent) -> Unit,
    onNavigateToAddBattery: () -> Unit,
    onNavigateToBatteryDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // État pour la fonctionnalité de pull-to-refresh
    var refreshing by remember { mutableStateOf<Boolean>(false) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = {
            refreshing = true
            onEvent(BatteryListEvent.RefreshList)
            coroutineScope.launch {
                delay(1500) // Délai pour simuler le rafraîchissement
                refreshing = false
            }
        }
    )
    
    // Déterminer si le FAB doit être étendu
    val showExtendedFab by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0
        }
    }
    
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SkyFuel",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                scrollBehavior = scrollBehavior,
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
                        enter = fadeIn(),
                        exit = fadeOut()
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
            // Add PullRefreshIndicator
            PullRefreshIndicator(
                refreshing = refreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
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
                            batteries = currentState.batteries,
                            searchQuery = currentState.searchQuery,
                            filterStatus = currentState.filterStatus,
                            onSearchQueryChange = { query -> onEvent(BatteryListEvent.Search(query)) },
                            onClearSearch = { onEvent(BatteryListEvent.ClearSearch) },
                            onFilterSelected = { status -> onEvent(BatteryListEvent.Filter(status)) },
                            onBatteryClick = { battery -> 
                                onEvent(BatteryListEvent.SelectBattery(battery))
                                onNavigateToBatteryDetail(battery.id)
                            },
                            onViewStats = { /* Navigation vers les statistiques complètes */ },
                            paddingValues = paddingValues,
                            lazyListState = lazyListState
                        )
                    }
                }
            }
        }
    }
}

/**
 * Écran de chargement
 */
@Composable
fun LoadingScreen(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
    }
}

/**
 * Écran d'erreur
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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Une erreur est survenue",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        androidx.compose.material3.Button(
            onClick = onRetry,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        ) {
            Text("Réessayer")
        }
    }
}

/**
 * Écran vide (aucune batterie)
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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.BatteryUnknown,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Aucune batterie",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Commencez par ajouter votre première batterie pour suivre son état et son utilisation.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        androidx.compose.material3.Button(
            onClick = onAddBattery,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            
            Spacer(modifier = Modifier.size(8.dp))
            
            Text("Ajouter une batterie")
        }
    }
}

/**
 * Écran affiché quand aucune batterie ne correspond au filtre actif
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icône avec couleur spécifique au statut
        val iconTint = when (filterStatus) {
            BatteryStatus.CHARGED -> Color(0xFF4CAF50) // Vert
            BatteryStatus.DISCHARGED -> Color(0xFFFFC107) // Jaune
            BatteryStatus.STORAGE -> Color(0xFF2196F3) // Bleu
            BatteryStatus.OUT_OF_SERVICE -> Color(0xFFE91E63) // Rose
        }
        
        Icon(
            imageVector = Icons.Default.BatteryUnknown,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = iconTint.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Aucune batterie $statusText",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Il n'y a actuellement aucune batterie correspondant à ce filtre.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        androidx.compose.material3.Button(
            onClick = onClearFilter,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            
            Spacer(modifier = Modifier.size(8.dp))
            
            Text("Afficher toutes les batteries")
        }
    }
}

/**
 * Écran principal avec contenu
 */
@Composable
fun ContentScreen(
    batteries: List<Battery>,
    searchQuery: String,
    filterStatus: leonfvt.skyfuel_app.domain.model.BatteryStatus?,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onFilterSelected: (leonfvt.skyfuel_app.domain.model.BatteryStatus?) -> Unit,
    onBatteryClick: (Battery) -> Unit,
    onViewStats: () -> Unit,
    paddingValues: PaddingValues,
    lazyListState: androidx.compose.foundation.lazy.LazyListState
) {
    // Statistiques calculées
    val stats = BatteryStatistics(
        totalCount = batteries.size,
        chargedCount = batteries.count { it.status == leonfvt.skyfuel_app.domain.model.BatteryStatus.CHARGED },
        dischargedCount = batteries.count { it.status == leonfvt.skyfuel_app.domain.model.BatteryStatus.DISCHARGED },
        storageCount = batteries.count { it.status == leonfvt.skyfuel_app.domain.model.BatteryStatus.STORAGE },
        outOfServiceCount = batteries.count { it.status == leonfvt.skyfuel_app.domain.model.BatteryStatus.OUT_OF_SERVICE },
        averageCycleCount = batteries.map { it.cycleCount }.average().toFloat()
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // Barre de recherche
        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onSearch = {},
            onClear = onClearSearch
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Filtres
        FilterSection(
            currentFilter = filterStatus,
            onFilterSelected = onFilterSelected
        )
        
        // Liste des batteries
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            // Statistiques en haut de la liste
            item {
                DashboardStats(
                    statistics = stats,
                    onViewFullStats = onViewStats
                )
            }
            
            // Liste des batteries
            items(
                items = batteries,
                key = { it.id }
            ) { battery ->
                BatteryCard(
                    battery = battery,
                    onClick = { onBatteryClick(battery) }
                )
            }
            
            // Espace en bas de la liste
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}