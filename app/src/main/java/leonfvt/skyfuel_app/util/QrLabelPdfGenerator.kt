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
 * Supporte des tailles d'étiquettes différentes par batterie.
 * Utilise un algorithme shelf-packing pour minimiser le gaspillage de papier
 * lorsque les étiquettes ont des tailles mixtes.
 */
@Singleton
class QrLabelPdfGenerator @Inject constructor() {

    data class LabelDimensions(
        val widthMm: Float,
        val heightMm: Float
    ) {
        val qrSizeMm: Float get() = (widthMm * 0.65f).coerceAtMost(heightMm * 0.5f)
    }

    enum class LabelSize(
        val widthMm: Float,
        val heightMm: Float,
        val displayName: String
    ) {
        SMALL(20f, 25f, "S"),
        MEDIUM(25f, 35f, "M"),
        LARGE(35f, 45f, "L");

        fun toDimensions() = LabelDimensions(widthMm, heightMm)
    }

    /**
     * Une étiquette à placer : batterie + dimensions spécifiques
     */
    data class LabelEntry(
        val battery: Battery,
        val dimensions: LabelDimensions
    )

    companion object {
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val MM_TO_POINTS = 2.835f
        private const val MARGIN_MM = 10f
        private const val GAP_MM = 2f
    }

    /**
     * Génère un PDF avec des étiquettes de tailles mixtes, agencées pour minimiser le gaspillage.
     *
     * @param context Context Android
     * @param entries Liste d'étiquettes (batterie + taille individuelle)
     * @return Fichier PDF ou null en cas d'erreur
     */
    fun generateLabelSheet(
        context: Context,
        entries: List<LabelEntry>
    ): File? {
        if (entries.isEmpty()) return null

        return try {
            val pdfDocument = PdfDocument()
            val margin = (MARGIN_MM * MM_TO_POINTS).toInt()
            val gap = (GAP_MM * MM_TO_POINTS).toInt()
            val usableW = PAGE_WIDTH - 2 * margin
            val usableH = PAGE_HEIGHT - 2 * margin

            // Shelf-packing : trier par hauteur décroissante pour optimiser
            val sortedEntries = entries.sortedByDescending { it.dimensions.heightMm }

            val pages = packLabels(sortedEntries, usableW, usableH, margin, gap)

            pages.forEachIndexed { pageIdx, placements ->
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIdx + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)

                for (placement in placements) {
                    drawLabel(
                        canvas, placement.entry.battery,
                        placement.x, placement.y,
                        placement.w, placement.h,
                        placement.entry.dimensions
                    )
                }

                // Pied de page
                val footerPaint = Paint().apply {
                    color = Color.LTGRAY
                    textSize = 7f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(
                    "SkyFuel — Page ${pageIdx + 1}/${pages.size} — ${LocalDate.now()}",
                    PAGE_WIDTH / 2f, PAGE_HEIGHT - 10f, footerPaint
                )

                pdfDocument.finishPage(page)
            }

            val outputDir = File(context.cacheDir, "labels")
            if (!outputDir.exists()) outputDir.mkdirs()

            val fileName = "skyfuel_labels_${entries.size}x_${LocalDate.now()}.pdf"
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
     * Rétro-compatibilité : taille unique pour toutes les batteries
     */
    fun generateLabelSheet(
        context: Context,
        batteries: List<Battery>,
        dimensions: LabelDimensions = LabelSize.MEDIUM.toDimensions(),
        copies: Int = 1
    ): File? {
        val entries = batteries.flatMap { battery ->
            List(copies) { LabelEntry(battery, dimensions) }
        }
        return generateLabelSheet(context, entries)
    }

    // ─── Skyline bin-packing algorithm ───
    // Optimise le placement pour minimiser le gaspillage de papier.
    // Utilise un "skyline" (ligne d'horizon) pour tracker l'espace disponible
    // et placer les petites étiquettes dans les trous laissés par les grandes.

    private data class Placement(
        val entry: LabelEntry,
        val x: Float, val y: Float,
        val w: Float, val h: Float
    )

    private data class SkylineSegment(val x: Float, val y: Float, val width: Float)

    /**
     * Skyline bin-packing : place les étiquettes en remplissant les espaces vides.
     * Trie par hauteur décroissante puis par largeur décroissante pour optimiser.
     * Quand une étiquette est placée, le skyline est mis à jour.
     */
    private fun packLabels(
        entries: List<LabelEntry>,
        usableW: Int, usableH: Int,
        margin: Int, gap: Int
    ): List<List<Placement>> {
        val pages = mutableListOf<MutableList<Placement>>()
        val remaining = entries.toMutableList()

        while (remaining.isNotEmpty()) {
            val pagePlacements = mutableListOf<Placement>()
            // Skyline = liste de segments (x, y, largeur) représentant le bord supérieur occupé
            val skyline = mutableListOf(SkylineSegment(0f, 0f, usableW.toFloat()))

            val toPlace = remaining.toMutableList()

            for (entry in toPlace) {
                val labelW = entry.dimensions.widthMm * MM_TO_POINTS + gap
                val labelH = entry.dimensions.heightMm * MM_TO_POINTS + gap

                // Chercher le meilleur segment du skyline pour cette étiquette
                val bestFit = findBestPosition(skyline, labelW, labelH, usableW.toFloat(), usableH.toFloat())

                if (bestFit != null) {
                    val (x, y) = bestFit
                    pagePlacements.add(
                        Placement(entry, margin + x, margin + y, labelW - gap, labelH - gap)
                    )
                    updateSkyline(skyline, x, y + labelH, labelW)
                    remaining.remove(entry)
                }
            }

            // Si rien n'a pu être placé (ne devrait pas arriver), forcer une nouvelle page
            if (pagePlacements.isEmpty() && remaining.isNotEmpty()) {
                val entry = remaining.removeFirst()
                val labelW = entry.dimensions.widthMm * MM_TO_POINTS
                val labelH = entry.dimensions.heightMm * MM_TO_POINTS
                pagePlacements.add(Placement(entry, margin.toFloat(), margin.toFloat(), labelW, labelH))
            }

            pages.add(pagePlacements)
        }

        return pages
    }

    /**
     * Trouve la meilleure position sur le skyline pour une étiquette de dimensions données.
     * Stratégie "Bottom-Left" : cherche la position la plus basse, puis la plus à gauche.
     */
    private fun findBestPosition(
        skyline: List<SkylineSegment>,
        labelW: Float, labelH: Float,
        maxW: Float, maxH: Float
    ): Pair<Float, Float>? {
        var bestX = -1f
        var bestY = Float.MAX_VALUE

        for (i in skyline.indices) {
            val seg = skyline[i]

            // L'étiquette dépasse à droite ?
            if (seg.x + labelW > maxW) continue

            // Calculer la hauteur max du skyline sous cette étiquette
            var maxSkyY = seg.y
            var coveredWidth = 0f
            for (j in i until skyline.size) {
                val s = skyline[j]
                if (s.x >= seg.x + labelW) break
                maxSkyY = maxOf(maxSkyY, s.y)
                coveredWidth = s.x + s.width - seg.x
                if (coveredWidth >= labelW) break
            }

            // L'étiquette dépasse en bas ?
            if (maxSkyY + labelH > maxH) continue

            // Meilleure position (la plus basse, puis la plus à gauche)
            if (maxSkyY < bestY || (maxSkyY == bestY && seg.x < bestX)) {
                bestY = maxSkyY
                bestX = seg.x
            }
        }

        return if (bestX >= 0) Pair(bestX, bestY) else null
    }

    /**
     * Met à jour le skyline après placement d'une étiquette.
     */
    private fun updateSkyline(skyline: MutableList<SkylineSegment>, x: Float, newY: Float, width: Float) {
        val newSegments = mutableListOf<SkylineSegment>()

        for (seg in skyline) {
            val segEnd = seg.x + seg.width
            val labelEnd = x + width

            if (segEnd <= x || seg.x >= labelEnd) {
                // Segment complètement en dehors → garder tel quel
                newSegments.add(seg)
            } else {
                // Segment chevauche la zone de placement
                if (seg.x < x) {
                    // Partie gauche non couverte
                    newSegments.add(SkylineSegment(seg.x, seg.y, x - seg.x))
                }
                if (segEnd > labelEnd) {
                    // Partie droite non couverte
                    newSegments.add(SkylineSegment(labelEnd, seg.y, segEnd - labelEnd))
                }
            }
        }

        // Ajouter le nouveau segment pour l'étiquette placée
        newSegments.add(SkylineSegment(x, newY, width))

        // Trier par x et fusionner les segments adjacents de même hauteur
        newSegments.sortBy { it.x }
        skyline.clear()
        for (seg in newSegments) {
            val last = skyline.lastOrNull()
            if (last != null && last.x + last.width == seg.x && last.y == seg.y) {
                skyline[skyline.lastIndex] = SkylineSegment(last.x, last.y, last.width + seg.width)
            } else {
                skyline.add(seg)
            }
        }
    }

    // ─── Label drawing ───

    private fun drawLabel(
        canvas: Canvas,
        battery: Battery,
        x: Float, y: Float,
        w: Float, h: Float,
        dims: LabelDimensions
    ) {
        // Bordure pointillée de découpe
        val borderPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(3f, 3f), 0f)
        }
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 4f, 4f, borderPaint)

        val padding = 2f * MM_TO_POINTS
        val innerW = w - 2 * padding
        var curY = y + padding

        // === QR Code (centré) ===
        val qrSizePx = (dims.qrSizeMm * MM_TO_POINTS).toInt().coerceAtLeast(20)
        val qrBitmap = generateQrForLabel(battery, qrSizePx * 4)
        if (qrBitmap != null) {
            val qrX = x + (w - qrSizePx) / 2f
            canvas.drawBitmap(
                qrBitmap, null,
                Rect(qrX.toInt(), curY.toInt(), (qrX + qrSizePx).toInt(), (curY + qrSizePx).toInt()),
                null
            )
            curY += qrSizePx + 1.5f * MM_TO_POINTS
        }

        // === ID SF-XXX — gros, lisible ===
        val idFontSize = (w * 0.16f).coerceIn(8f, 24f)
        val idPaint = Paint().apply {
            color = Color.BLACK
            textSize = idFontSize
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val sfId = QrCodeGenerator.generateShortId(battery.id)
        canvas.drawText(sfId, x + w / 2f, curY, idPaint)
        curY += idFontSize * 1.1f

        // === Séparateur ===
        val sepPaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f }
        canvas.drawLine(x + padding, curY, x + w - padding, curY, sepPaint)
        curY += 1.5f * MM_TO_POINTS

        // === Nom (marque modèle) ===
        val nameFontSize = (w * 0.08f).coerceIn(5f, 12f)
        val namePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = nameFontSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val name = "${battery.brand} ${battery.model}"
        val maxChars = (innerW / (nameFontSize * 0.52f)).toInt().coerceAtLeast(5)
        val truncName = if (name.length > maxChars) name.take(maxChars - 1) + "\u2026" else name
        canvas.drawText(truncName, x + w / 2f, curY, namePaint)
        curY += nameFontSize + 0.8f * MM_TO_POINTS

        // === Specs ===
        val specFontSize = (w * 0.07f).coerceIn(4f, 10f)
        val specPaint = Paint().apply {
            color = Color.GRAY
            textSize = specFontSize
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("${battery.type.name} ${battery.cells}S ${battery.capacity}mAh", x + w / 2f, curY, specPaint)
    }

    private fun generateQrForLabel(battery: Battery, sizePx: Int): Bitmap? {
        return try {
            val qrData = QrCodeData.forBattery(
                batteryId = battery.id,
                serialNumber = battery.serialNumber,
                brand = battery.brand,
                model = battery.model
            )
            QrCodeGenerator.generateQrCodeBitmap(qrData.encode(), sizePx, 1)
        } catch (e: Exception) {
            null
        }
    }
}
