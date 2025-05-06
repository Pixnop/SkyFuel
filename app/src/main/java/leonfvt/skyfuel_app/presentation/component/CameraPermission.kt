package leonfvt.skyfuel_app.presentation.component

import android.Manifest
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Composant qui gère les permissions de caméra et affiche le contenu approprié
 * @param onPermissionGranted Appelé lorsque la permission est accordée
 * @param permissionNotAvailableContent Contenu à afficher lorsque la permission n'est pas disponible
 * @param content Contenu à afficher lorsque la permission est accordée
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermission(
    onPermissionGranted: () -> Unit,
    permissionNotAvailableContent: @Composable () -> Unit = { },
    content: @Composable () -> Unit
) {
    var showRationale by remember { mutableStateOf(false) }
    
    val cameraPermissionState = rememberPermissionState(
        Manifest.permission.CAMERA
    ) { isGranted ->
        if (isGranted) {
            onPermissionGranted()
        }
    }
    
    // Observer le cycle de vie pour demander la permission
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(key1 = lifecycleOwner, key2 = cameraPermissionState) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (!cameraPermissionState.status.isGranted) {
                    cameraPermissionState.launchPermissionRequest()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Afficher le dialogue de justification si nécessaire
    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Autorisation de la caméra requise") },
            text = {
                Text("L'accès à la caméra est nécessaire pour scanner les QR codes des batteries.")
            },
            confirmButton = {
                Button(onClick = {
                    showRationale = false
                    cameraPermissionState.launchPermissionRequest()
                }) {
                    Text("Demander l'autorisation")
                }
            },
            dismissButton = {
                Button(onClick = { showRationale = false }) {
                    Text("Annuler")
                }
            }
        )
    }
    
    when {
        // Si la permission est accordée, afficher le contenu principal
        cameraPermissionState.status.isGranted -> {
            content()
        }
        // Si on doit afficher la justification
        cameraPermissionState.status.shouldShowRationale -> {
            showRationale = true
            permissionNotAvailableContent()
        }
        // Si la permission est définitivement refusée
        else -> {
            permissionNotAvailableContent()
        }
    }
}