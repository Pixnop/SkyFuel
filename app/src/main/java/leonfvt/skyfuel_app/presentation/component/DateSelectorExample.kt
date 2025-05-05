package leonfvt.skyfuel_app.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import leonfvt.skyfuel_app.presentation.theme.SkyFuelTheme

@Composable
fun DateSelectorExample() {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Test du sélecteur de date",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Cliquez n'importe où sur le composant ci-dessous pour sélectionner une date:",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Notre composant DateSelector
        DateSelector(
            selectedDate = selectedDate,
            onDateSelected = { selectedDate = it },
            label = "Date d'achat"
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Date sélectionnée: ${selectedDate.format(dateFormatter)}",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DateSelectorExamplePreview() {
    SkyFuelTheme {
        Surface {
            DateSelectorExample()
        }
    }
}