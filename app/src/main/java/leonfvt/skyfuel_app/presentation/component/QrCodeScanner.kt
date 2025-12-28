package leonfvt.skyfuel_app.presentation.component

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.delay
import leonfvt.skyfuel_app.util.ScanFeedback
import java.util.concurrent.Executors

private const val TAG = "QrCodeScanner"

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun QrCodeScanner(
    onQrCodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
    cooldownMs: Long = 2000L
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Clé unique pour forcer la recréation du scanner
    val scannerKey = remember { System.currentTimeMillis() }
    
    // Feedback pour le scan
    val scanFeedback = remember { ScanFeedback(context) }
    
    // État pour l'animation - utiliser remember avec clé pour réinitialiser
    var showSuccessAnimation by remember { mutableStateOf(false) }
    
    // Références mutables pour l'analyzer (évite les problèmes de capture)
    val lastScanTimeRef = remember { mutableLongStateOf(0L) }
    val lastScannedCodeRef = remember { mutableStateOf<String?>(null) }
    
    // Animation de succès
    val successScale by animateFloatAsState(
        targetValue = if (showSuccessAnimation) 1.2f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "successScale"
    )
    val successAlpha by animateFloatAsState(
        targetValue = if (showSuccessAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "successAlpha"
    )
    
    // Reset de l'animation après affichage
    LaunchedEffect(showSuccessAnimation) {
        if (showSuccessAnimation) {
            delay(1500)
            showSuccessAnimation = false
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Utiliser key pour forcer la recréation de AndroidView
        androidx.compose.runtime.key(scannerKey) {
            var previewView: PreviewView? by remember { mutableStateOf(null) }
            var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
            val executor = remember { Executors.newSingleThreadExecutor() }
            
            // Nettoyage des ressources et libération de la caméra
            DisposableEffect(Unit) {
                onDispose {
                    scanFeedback.release()
                    try {
                        cameraProvider?.unbindAll()
                        Log.d(TAG, "Camera unbound on dispose")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error unbinding camera", e)
                    }
                    executor.shutdown()
                }
            }
            
            // Initialiser la caméra
            LaunchedEffect(previewView) {
                previewView?.let { view ->
                    try {
                        val provider = ProcessCameraProvider.getInstance(context).get()
                        cameraProvider = provider
                        
                        // S'assurer de libérer les anciennes liaisons
                        provider.unbindAll()
                        
                        // Configuration de l'aperçu
                        val preview = Preview.Builder().build()
                        preview.setSurfaceProvider(view.surfaceProvider)
                        
                        // Configuration de l'analyseur d'image
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        
                        imageAnalysis.setAnalyzer(executor) { imageProxy ->
                            val currentTime = System.currentTimeMillis()
                            
                            // Vérifier le cooldown
                            if (currentTime - lastScanTimeRef.longValue < cooldownMs) {
                                imageProxy.close()
                                return@setAnalyzer
                            }
                            
                            // Traitement de l'image pour détecter les QR codes
                            val buffer = imageProxy.planes[0].buffer
                            val data = ByteArray(buffer.remaining())
                            buffer.get(data)
                            
                            val width = imageProxy.width
                            val height = imageProxy.height
                            
                            val source = PlanarYUVLuminanceSource(
                                data, width, height, 0, 0, width, height, false
                            )
                            
                            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                            
                            try {
                                val result = MultiFormatReader().decode(binaryBitmap)
                                val scannedText = result.text
                                
                                // Éviter de scanner le même code plusieurs fois de suite
                                if (scannedText != lastScannedCodeRef.value ||
                                    currentTime - lastScanTimeRef.longValue > cooldownMs * 2) {
                                    lastScanTimeRef.longValue = currentTime
                                    lastScannedCodeRef.value = scannedText

                                    Log.d(TAG, "QR Code scanné: $scannedText")

                                    // Exécuter sur le thread principal pour les mises à jour Compose
                                    ContextCompat.getMainExecutor(context).execute {
                                        // Feedback haptic + sonore
                                        scanFeedback.triggerSuccessFeedback()

                                        // Afficher l'animation de succès
                                        showSuccessAnimation = true

                                        Log.d(TAG, "Calling onQrCodeScanned callback on main thread")
                                        onQrCodeScanned(scannedText)
                                    }
                                }
                            } catch (e: Exception) {
                                // Pas de QR code détecté, on continue à scanner
                            } finally {
                                imageProxy.close()
                            }
                        }
                        
                        // Lier à la lifecycle
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                        
                        Log.d(TAG, "Camera bound successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur de liaison à la caméra", e)
                    }
                }
            }
            
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).also { previewView = it }
                }
            )
        }
        
        // Overlay de scan avec animation
        Box(
            modifier = Modifier
                .size(250.dp)
                .scale(successScale)
                .background(
                    color = if (showSuccessAnimation) 
                        Color.Green.copy(alpha = 0.3f) 
                    else 
                        Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 3.dp,
                    color = if (showSuccessAnimation)
                        Color.Green
                    else
                        Color.White.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Icône qui change selon l'état
            if (showSuccessAnimation) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Scan réussi",
                    modifier = Modifier
                        .size(72.dp)
                        .alpha(successAlpha),
                    tint = Color.Green
                )
            } else {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scanner un QR code",
                    modifier = Modifier.size(72.dp),
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        }
        
        // Message de succès
        AnimatedVisibility(
            visible = showSuccessAnimation,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
        ) {
            Surface(
                color = Color.Green,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "QR Code scanné !",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // Instructions
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Placez le QR code de la batterie dans le cadre pour le scanner",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Composant pour afficher un écran d'erreur lorsque la caméra n'est pas disponible
 */
@Composable
fun CameraPermissionDenied(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "Autorisation de la caméra requise",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                
                Text(
                    text = "L'accès à la caméra est nécessaire pour scanner les QR codes des batteries.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                androidx.compose.material3.Button(
                    onClick = onRequestPermission
                ) {
                    Text("Autoriser l'accès à la caméra")
                }
            }
        }
    }
}

/**
 * Composant pour afficher un indicateur de chargement lors de l'initialisation du scanner
 */
@Composable
fun ScannerLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}