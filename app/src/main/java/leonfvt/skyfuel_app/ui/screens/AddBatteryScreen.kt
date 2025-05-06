package leonfvt.skyfuel_app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import leonfvt.skyfuel_app.domain.model.BatteryType
import leonfvt.skyfuel_app.ui.viewmodel.BatteryViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Écran d'ajout d'une nouvelle batterie
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBatteryScreen(
    navController: NavController,
    viewModel: BatteryViewModel = hiltViewModel()
) {
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var serialNumber by remember { mutableStateOf("") }
    var batteryType by remember { mutableStateOf(BatteryType.LIPO) }
    var cells by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    // État pour le dropdown du type de batterie
    var batteryTypeExpanded by remember { mutableStateOf(false) }
    
    // Validations
    val isFormValid = brand.isNotBlank() && model.isNotBlank() && serialNumber.isNotBlank() &&
                     cells.isNotBlank() && capacity.isNotBlank()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajouter une batterie") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Marque et modèle
            OutlinedTextField(
                value = brand,
                onValueChange = { brand = it },
                label = { Text("Marque") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Modèle") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Numéro de série
            OutlinedTextField(
                value = serialNumber,
                onValueChange = { serialNumber = it },
                label = { Text("Numéro de série") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Type de batterie (dropdown)
            ExposedDropdownMenuBox(
                expanded = batteryTypeExpanded,
                onExpandedChange = { batteryTypeExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    value = batteryType.name,
                    onValueChange = { },
                    label = { Text("Type de batterie") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = batteryTypeExpanded)
                    }
                )
                
                ExposedDropdownMenu(
                    expanded = batteryTypeExpanded,
                    onDismissRequest = { batteryTypeExpanded = false }
                ) {
                    BatteryType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name) },
                            onClick = {
                                batteryType = type
                                batteryTypeExpanded = false
                            }
                        )
                    }
                }
            }
            
            // Caractéristiques techniques
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = cells,
                    onValueChange = { cells = it.filter { char -> char.isDigit() } },
                    label = { Text("Nombre de cellules") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                
                OutlinedTextField(
                    value = capacity,
                    onValueChange = { capacity = it.filter { char -> char.isDigit() } },
                    label = { Text("Capacité (mAh)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optionnel)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Bouton d'enregistrement
            Button(
                onClick = {
                    if (isFormValid) {
                        viewModel.addBattery(
                            brand = brand,
                            model = model,
                            serialNumber = serialNumber,
                            type = batteryType,
                            cells = cells.toIntOrNull() ?: 0,
                            capacity = capacity.toIntOrNull() ?: 0,
                            purchaseDate = LocalDate.now(),
                            notes = notes
                        )
                        navController.popBackStack()
                    }
                },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
            ) {
                Text("Enregistrer la batterie")
            }
        }
    }
}