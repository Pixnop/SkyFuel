package leonfvt.skyfuel_app.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

/**
 * Classe utilitaire pour générer des QR codes
 */
object QrCodeGenerator {
    
    /**
     * Génère un QR code sous forme de Bitmap à partir d'un contenu
     * @param content Le contenu à encoder dans le QR code
     * @param size La taille du QR code en pixels
     * @param padding Le padding autour du QR code
     * @return Le Bitmap contenant le QR code généré
     */
    fun generateQrCodeBitmap(
        content: String,
        size: Int = 512,
        padding: Int = 2
    ): Bitmap? {
        return try {
            // Configuration du QR code avec correction d'erreur
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                put(EncodeHintType.MARGIN, padding)
            }
            
            // Génération de la matrice du QR code
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            
            // Conversion en Bitmap
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            
            // Remplissage du Bitmap avec les données du QR code
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x, y,
                        if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                    )
                }
            }
            
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Fonction Composable pour générer un QR code
     * @param content Le contenu à encoder dans le QR code
     * @param size La taille du QR code en dp
     * @param padding Le padding autour du QR code
     * @return Le Bitmap contenant le QR code généré, converti en ImageBitmap pour Compose
     */
    @Composable
    fun rememberQrCodeBitmap(
        content: String,
        size: Dp = 200.dp,
        padding: Int = 2
    ): androidx.compose.ui.graphics.ImageBitmap? {
        val density = LocalDensity.current
        val sizeInPx = with(density) { size.toPx().toInt() }
        
        return remember(content, sizeInPx, padding) {
            generateQrCodeBitmap(content, sizeInPx, padding)?.asImageBitmap()
        }
    }
}