package leonfvt.skyfuel_app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
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
     * Génère un identifiant court pour une batterie
     * Format: SF-XXX (ex: SF-001, SF-042, SF-999)
     */
    fun generateShortId(batteryId: Long): String {
        return "SF-%03d".format(batteryId % 1000)
    }

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
        return generateQrCodeBitmapWithLabel(content, size, padding, null)
    }

    /**
     * Génère un QR code avec un label visible en dessous
     * @param content Le contenu à encoder dans le QR code
     * @param size La taille du QR code en pixels
     * @param padding Le padding autour du QR code
     * @param label Le texte à afficher sous le QR code (ex: "SF-001")
     * @return Le Bitmap contenant le QR code avec le label
     */
    fun generateQrCodeBitmapWithLabel(
        content: String,
        size: Int = 512,
        padding: Int = 2,
        label: String? = null
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
            val qrWidth = bitMatrix.width
            val qrHeight = bitMatrix.height

            // Calculer la hauteur du label
            val labelHeight = if (label != null) (size * 0.12f).toInt() else 0
            val totalHeight = qrHeight + labelHeight

            val bitmap = Bitmap.createBitmap(qrWidth, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Fond blanc
            canvas.drawColor(Color.WHITE)

            // Dessiner le QR code
            for (x in 0 until qrWidth) {
                for (y in 0 until qrHeight) {
                    if (bitMatrix[x, y]) {
                        canvas.drawPoint(x.toFloat(), y.toFloat(), Paint().apply {
                            color = Color.BLACK
                            strokeWidth = 1f
                        })
                    }
                }
            }

            // Dessiner chaque pixel du QR code
            val qrPaint = Paint().apply { color = Color.BLACK }
            for (x in 0 until qrWidth) {
                for (y in 0 until qrHeight) {
                    if (bitMatrix[x, y]) {
                        canvas.drawRect(
                            x.toFloat(), y.toFloat(),
                            (x + 1).toFloat(), (y + 1).toFloat(),
                            qrPaint
                        )
                    }
                }
            }

            // Dessiner le label si présent
            if (label != null) {
                val textPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = size * 0.08f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val xPos = qrWidth / 2f
                val yPos = qrHeight + (labelHeight * 0.7f)
                canvas.drawText(label, xPos, yPos, textPaint)
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
     * @param label Le texte à afficher sous le QR code (optionnel)
     * @return Le Bitmap contenant le QR code généré, converti en ImageBitmap pour Compose
     */
    @Composable
    fun rememberQrCodeBitmap(
        content: String,
        size: Dp = 200.dp,
        padding: Int = 2,
        label: String? = null
    ): androidx.compose.ui.graphics.ImageBitmap? {
        val density = LocalDensity.current
        val sizeInPx = with(density) { size.toPx().toInt() }

        return remember(content, sizeInPx, padding, label) {
            generateQrCodeBitmapWithLabel(content, sizeInPx, padding, label)?.asImageBitmap()
        }
    }

    /**
     * Fonction Composable pour générer un QR code avec ID de batterie
     * @param content Le contenu à encoder dans le QR code
     * @param batteryId L'ID de la batterie pour générer le label SF-XXX
     * @param size La taille du QR code en dp
     * @param padding Le padding autour du QR code
     * @return Le Bitmap contenant le QR code avec le label SF-XXX
     */
    @Composable
    fun rememberQrCodeBitmapWithBatteryId(
        content: String,
        batteryId: Long,
        size: Dp = 200.dp,
        padding: Int = 2
    ): androidx.compose.ui.graphics.ImageBitmap? {
        val label = generateShortId(batteryId)
        return rememberQrCodeBitmap(content, size, padding, label)
    }
}