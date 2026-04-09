package leonfvt.skyfuel_app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.QrCodeData
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Générateur de planches d'étiquettes QR code au format PDF.
 *
 * Chaque étiquette contient :
 * - Le QR code de la batterie
 * - L'identifiant SF-XXX
 * - Le nom (marque + modèle)
 * - Les specs (type, capacité)
 *
 * Tailles d'étiquettes optimisées pour batteries de drone :
 * - SMALL  : 20x25mm — petites batteries racing (1100-1500mAh)
 * - MEDIUM : 25x30mm — batteries standard (3000-5000mAh)
 * - LARGE  : 30x40mm — grosses batteries (>5000mAh)
 *
 * Disposition sur page A4 en grille avec marges de découpe.
 */
@Singleton
class QrLabelPdfGenerator @Inject constructor() {

    enum class LabelSize(
        val widthMm: Float,
        val heightMm: Float,
        val qrSizeMm: Float,
        val displayName: String
    ) {
        SMALL(20f, 25f, 14f, "Petit (20×25mm)"),
        MEDIUM(25f, 35f, 18f, "Moyen (25×35mm)"),
        LARGE(35f, 45f, 25f, "Grand (35×45mm)")
    }

    companion object {
        // A4 en points (72 dpi)
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val MM_TO_POINTS = 2.835f // 1mm = 2.835 points (72dpi)
        private const val MARGIN_MM = 10f
    }

    /**
     * Génère un PDF de planches d'étiquettes QR code
     *
     * @param context Context Android
     * @param batteries Liste des batteries à imprimer
     * @param labelSize Taille des étiquettes
     * @param copies Nombre de copies par batterie
     * @return Fichier PDF ou null en cas d'erreur
     */
    fun generateLabelSheet(
        context: Context,
        batteries: List<Battery>,
        labelSize: LabelSize = LabelSize.MEDIUM,
        copies: Int = 1
    ): File? {
        if (batteries.isEmpty()) return null

        return try {
            val pdfDocument = PdfDocument()

            val labelW = (labelSize.widthMm * MM_TO_POINTS).toInt()
            val labelH = (labelSize.heightMm * MM_TO_POINTS).toInt()
            val margin = (MARGIN_MM * MM_TO_POINTS).toInt()
            val gap = (2f * MM_TO_POINTS).toInt() // 2mm entre étiquettes

            val usableW = PAGE_WIDTH - 2 * margin
            val usableH = PAGE_HEIGHT - 2 * margin

            val cols = (usableW + gap) / (labelW + gap)
            val rows = (usableH + gap) / (labelH + gap)
            val labelsPerPage = cols * rows

            // Générer la liste d'étiquettes (avec copies)
            val allLabels = batteries.flatMap { battery -> List(copies) { battery } }

            var pageNumber = 0
            var labelIndex = 0

            while (labelIndex < allLabels.size) {
                pageNumber++
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Fond blanc
                canvas.drawColor(Color.WHITE)

                // Dessiner les repères de coupe en coin
                drawCropMarks(canvas, margin, labelW, labelH, cols, rows, gap)

                // Dessiner les étiquettes
                for (row in 0 until rows) {
                    for (col in 0 until cols) {
                        if (labelIndex >= allLabels.size) break

                        val x = margin + col * (labelW + gap)
                        val y = margin + row * (labelH + gap)

                        drawLabel(canvas, allLabels[labelIndex], x.toFloat(), y.toFloat(), labelW.toFloat(), labelH.toFloat(), labelSize)
                        labelIndex++
                    }
                }

                // Pied de page
                val footerPaint = Paint().apply {
                    color = Color.LTGRAY
                    textSize = 7f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(
                    "SkyFuel — Page $pageNumber — ${LocalDate.now()}",
                    PAGE_WIDTH / 2f,
                    PAGE_HEIGHT - 10f,
                    footerPaint
                )

                pdfDocument.finishPage(page)
            }

            // Sauvegarder
            val outputDir = File(context.cacheDir, "labels")
            if (!outputDir.exists()) outputDir.mkdirs()

            val fileName = "skyfuel_labels_${labelSize.name.lowercase()}_${LocalDate.now()}.pdf"
            val outputFile = File(outputDir, fileName)
            FileOutputStream(outputFile).use { pdfDocument.writeTo(it) }
            pdfDocument.close()

            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Dessine une étiquette individuelle
     */
    private fun drawLabel(
        canvas: Canvas,
        battery: Battery,
        x: Float, y: Float,
        w: Float, h: Float,
        labelSize: LabelSize
    ) {
        // Bordure pointillée de découpe
        val borderPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(3f, 3f), 0f)
        }
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 4f, 4f, borderPaint)

        val padding = 3f * MM_TO_POINTS
        val innerW = w - 2 * padding
        val innerX = x + padding
        var curY = y + padding

        // QR Code
        val qrSizePx = (labelSize.qrSizeMm * MM_TO_POINTS).toInt()
        val qrBitmap = generateQrForLabel(battery, qrSizePx)
        if (qrBitmap != null) {
            val qrX = x + (w - qrSizePx) / 2f // Centré
            canvas.drawBitmap(qrBitmap, null, Rect(qrX.toInt(), curY.toInt(), (qrX + qrSizePx).toInt(), (curY + qrSizePx).toInt()), null)
            curY += qrSizePx + 2f * MM_TO_POINTS
        }

        // ID SF-XXX (gros, centré)
        val idPaint = Paint().apply {
            color = Color.BLACK
            textSize = when (labelSize) {
                LabelSize.SMALL -> 8f
                LabelSize.MEDIUM -> 10f
                LabelSize.LARGE -> 13f
            }
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val sfId = QrCodeGenerator.generateShortId(battery.id)
        canvas.drawText(sfId, x + w / 2f, curY, idPaint)
        curY += idPaint.textSize + 1f * MM_TO_POINTS

        // Nom (marque modèle) — tronqué si nécessaire
        val namePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = when (labelSize) {
                LabelSize.SMALL -> 5.5f
                LabelSize.MEDIUM -> 7f
                LabelSize.LARGE -> 9f
            }
            textAlign = Paint.Align.CENTER
        }
        val name = "${battery.brand} ${battery.model}"
        val maxChars = (innerW / (namePaint.textSize * 0.55f)).toInt()
        val truncatedName = if (name.length > maxChars) name.take(maxChars - 1) + "…" else name
        canvas.drawText(truncatedName, x + w / 2f, curY, namePaint)
        curY += namePaint.textSize + 0.5f * MM_TO_POINTS

        // Specs (type + capacité)
        val specPaint = Paint().apply {
            color = Color.GRAY
            textSize = when (labelSize) {
                LabelSize.SMALL -> 5f
                LabelSize.MEDIUM -> 6f
                LabelSize.LARGE -> 7.5f
            }
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${battery.type.name} ${battery.cells}S ${battery.capacity}mAh", x + w / 2f, curY, specPaint)
    }

    /**
     * Génère un QR code compact pour l'étiquette (sans label, petite taille)
     */
    private fun generateQrForLabel(battery: Battery, sizePx: Int): Bitmap? {
        return try {
            val qrData = QrCodeData.forBattery(
                batteryId = battery.id,
                serialNumber = battery.serialNumber,
                brand = battery.brand,
                model = battery.model
            )
            // QR code sans label, compact
            QrCodeGenerator.generateQrCodeBitmap(qrData.encode(), sizePx, 1)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Dessine les repères de coupe aux coins de la grille
     */
    private fun drawCropMarks(
        canvas: Canvas,
        margin: Int,
        labelW: Int, labelH: Int,
        cols: Int, rows: Int,
        gap: Int
    ) {
        val markPaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 0.3f
        }
        val markLen = 5f

        for (row in 0..rows) {
            for (col in 0..cols) {
                val cx = margin + col * (labelW + gap) - gap / 2f
                val cy = margin + row * (labelH + gap) - gap / 2f

                // Croix aux intersections
                if (col > 0 && col < cols && row > 0 && row < rows) {
                    canvas.drawLine(cx - markLen, cy, cx + markLen, cy, markPaint)
                    canvas.drawLine(cx, cy - markLen, cx, cy + markLen, markPaint)
                }
            }
        }
    }
}
