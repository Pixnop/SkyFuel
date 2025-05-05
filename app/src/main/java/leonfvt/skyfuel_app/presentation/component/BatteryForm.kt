package leonfvt.skyfuel_app.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import leonfvt.skyfuel_app.domain.model.BatteryType
import leonfvt.skyfuel_app.presentation.theme.SkyFuelTheme
import leonfvt.skyfuel_app.presentation.viewmodel.state.AddBatteryState
import java.time.LocalDate
import leonfvt.skyfuel_app.presentation.component.DateSelector

/**
 * Formulaire d'ajout ou de modification d'une batterie
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryForm(
    state: AddBatteryState,
    onBrandChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onSerialNumberChange: (String) -> Unit,
    onBatteryTypeChange: (BatteryType) -> Unit,
    onCellsChange: (String) -> Unit,
    onCapacityChange: (String) -> Unit,
    onPurchaseDateChange: (LocalDate) -> Unit,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var batteryTypeExpanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Première ligne: Marque et modèle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Champ de la marque
            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = state.brand,
                    onValueChange = onBrandChange,
                    label = { Text("Marque") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = state.brandError,
                    supportingText = {
                        if (state.brandError) {
                            Row {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                
                                Spacer(modifier = Modifier.padding(4.dp))
                                
                                Text(
                                    text = "La marque est requise",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                )
            }
            
            // Champ du modèle
            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = state.model,
                    onValueChange = onModelChange,
                    label = { Text("Modèle") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = state.modelError,
                    supportingText = {
                        if (state.modelError) {
                            Row {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                
                                Spacer(modifier = Modifier.padding(4.dp))
                                
                                Text(
                                    text = "Le modèle est requis",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                )
            }
        }
        
        // Champ du numéro de série
        OutlinedTextField(
            value = state.serialNumber,
            onValueChange = onSerialNumberChange,
            label = { Text("Numéro de série") },
            modifier = Modifier.fillMaxWidth(),
            isError = state.serialNumberError,
            supportingText = {
                if (state.serialNumberError) {
                    Row {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.padding(4.dp))
                        
                        Text(
                            text = "Le numéro de série est requis",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
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
                value = state.batteryType.name,
                onValueChange = { },
                label = { Text("Type de batterie") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = batteryTypeExpanded) }
            )
            
            ExposedDropdownMenu(
                expanded = batteryTypeExpanded,
                onDismissRequest = { batteryTypeExpanded = false }
            ) {
                BatteryType.values().forEach { batteryType ->
                    DropdownMenuItem(
                        text = { Text(batteryType.name) },
                        onClick = {
                            onBatteryTypeChange(batteryType)
                            batteryTypeExpanded = false
                        }
                    )
                }
            }
        }
        
        // Ligne pour les caractéristiques: Cellules et capacité
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Champ du nombre de cellules
            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = state.cells,
                    onValueChange = onCellsChange,
                    label = { Text("Nombre de cellules") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = state.cellsError,
                    supportingText = {
                        if (state.cellsError) {
                            Row {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                
                                Spacer(modifier = Modifier.padding(4.dp))
                                
                                Text(
                                    text = "Valeur requise > 0",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                )
            }
            
            // Champ de la capacité
            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = state.capacity,
                    onValueChange = onCapacityChange,
                    label = { Text("Capacité (mAh)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = state.capacityError,
                    supportingText = {
                        if (state.capacityError) {
                            Row {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                
                                Spacer(modifier = Modifier.padding(4.dp))
                                
                                Text(
                                    text = "Valeur requise > 0",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                )
            }
        }
        
        // Champ de la date d'achat
        DateSelector(
            selectedDate = state.purchaseDate,
            onDateSelected = onPurchaseDateChange,
            label = "Date d'achat",
            modifier = Modifier.fillMaxWidth()
        )
        
        // Champ des notes
        OutlinedTextField(
            value = state.notes,
            onValueChange = onNotesChange,
            label = { Text("Notes (optionnel)") },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Message d'erreur global
        AnimatedVisibility(
            visible = state.errorMessage != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    
                    Text(
                        text = state.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun BatteryFormPreview() {
    SkyFuelTheme {
        BatteryForm(
            state = AddBatteryState(
                brand = "Turnigy",
                model = "Graphene",
                serialNumber = "TG001",
                batteryType = BatteryType.LIPO,
                cells = "4",
                capacity = "1300",
                purchaseDate = LocalDate.now(),
                notes = "Pour drone de course"
            ),
            onBrandChange = {},
            onModelChange = {},
            onSerialNumberChange = {},
            onBatteryTypeChange = {},
            onCellsChange = {},
            onCapacityChange = {},
            onPurchaseDateChange = {},
            onNotesChange = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}