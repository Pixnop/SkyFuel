package leonfvt.skyfuel_app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import leonfvt.skyfuel_app.data.model.Battery
import leonfvt.skyfuel_app.data.model.BatteryStatus
import leonfvt.skyfuel_app.ui.components.BatteryCard
import leonfvt.skyfuel_app.ui.viewmodel.BatteriesLoadState
import leonfvt.skyfuel_app.ui.viewmodel.BatteryViewModel

/**
 * Écran principal (dashboard) de l'application
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: BatteryViewModel = hiltViewModel()
) {
    // Récupération des batteries
    val batteriesState by viewModel.batteriesState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentFilter by viewModel.filterStatus.collectAsState()
    
    // État de la barre de recherche
    var searchActive by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SkyFuel") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_battery") }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter une batterie")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Barre de recherche
            SearchBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                onSearch = { searchActive = false },
                active = searchActive,
                onActiveChange = { searchActive = it },
                placeholder = { Text("Rechercher une batterie") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Rechercher") }
            ) {
                // Contenu de la recherche active (vide pour l'instant)
            }
            
            // Section de filtrage
            StatusFilterChips(
                currentFilter = currentFilter,
                onFilterSelected = { viewModel.setStatusFilter(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            
            // Contenu principal
            when (val state = batteriesState) {
                is BatteriesLoadState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                
                is BatteriesLoadState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Erreur: ${state.message}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                is BatteriesLoadState.Success -> {
                    if (state.batteries.isEmpty()) {
                        EmptyBatteriesList()
                    } else {
                        BatteriesList(
                            batteries = state.batteries,
                            onBatteryClick = { battery ->
                                viewModel.selectBattery(battery)
                                navController.navigate("battery_details/${battery.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Composant pour les filtres de statut
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusFilterChips(
    currentFilter: BatteryStatus?,
    onFilterSelected: (BatteryStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf(
        null to "Toutes",
        BatteryStatus.CHARGED to "Chargées",
        BatteryStatus.DISCHARGED to "Déchargées",
        BatteryStatus.STORAGE to "En stockage",
        BatteryStatus.OUT_OF_SERVICE to "Hors service"
    )
    
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (status, label) ->
            FilterChip(
                selected = currentFilter == status,
                onClick = { onFilterSelected(status) },
                label = { Text(label) }
            )
        }
    }
}

/**
 * Liste des batteries
 */
@Composable
fun BatteriesList(
    batteries: List<Battery>,
    onBatteryClick: (Battery) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(batteries) { battery ->
            BatteryCard(
                battery = battery,
                onClick = { onBatteryClick(battery) }
            )
        }
    }
}

/**
 * État vide (aucune batterie)
 */
@Composable
fun EmptyBatteriesList() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Aucune batterie trouvée",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "Ajoutez votre première batterie avec le bouton +",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}