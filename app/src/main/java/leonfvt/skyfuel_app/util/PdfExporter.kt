package leonfvt.skyfuel_app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryHistory
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utilitaire pour exporter les données de batteries en PDF
 */
@Singleton
class PdfExporter @Inject constructor() {
    
    companion object {
        private const val PAGE_WIDTH = 595 // A4 width in points
        private const val PAGE_HEIGHT = 842 // A4 height in points
        private const val MARGIN = 40f
        private const val LINE_HEIGHT = 20f
    }
    
    private val titlePaint = Paint().apply {
        color = Color.parseColor("#1976D2")
        textSize = 24f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    
    private val headerPaint = Paint().apply {
        color = Color.parseColor("#333333")
        textSize = 16f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    
    private val textPaint = Paint().apply {
        color = Color.parseColor("#666666")
        textSize = 12f
    }
    
    private val smallTextPaint = Paint().apply {
        color = Color.parseColor("#888888")
        textSize = 10f
    }
    
    private val linePaint = Paint().apply {
        color = Color.parseColor("#E0E0E0")
        strokeWidth = 1f
    }
    
    private val statusPaints = mapOf(
        BatteryStatus.CHARGED to Paint().apply {
            color = Color.parseColor("#4CAF50")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        },
        BatteryStatus.DISCHARGED to Paint().apply {
            color = Color.parseColor("#FF9800")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        },
        BatteryStatus.STORAGE to Paint().apply {
            color = Color.parseColor("#2196F3")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        },
        BatteryStatus.OUT_OF_SERVICE to Paint().apply {
            color = Color.parseColor("#F44336")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    )
    
    /**
     * Exporte une liste de batteries en PDF
     */
    fun exportBatteriesToPdf(
        context: Context,
        batteries: List<Battery>,
        fileName: String = "skyfuel_batteries_${LocalDate.now()}.pdf"
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            var yPosition = MARGIN
            
            // Titre
            canvas.drawText("SkyFuel - Rapport de Batteries", MARGIN, yPosition + 24f, titlePaint)
            yPosition += 40f
            
            // Date d'export
            val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            canvas.drawText(
                "Généré le ${LocalDateTime.now().format(dateFormatter)}", 
                MARGIN, 
                yPosition, 
                smallTextPaint
            )
            yPosition += 30f
            
            // Résumé
            val chargedCount = batteries.count { it.status == BatteryStatus.CHARGED }
            val dischargedCount = batteries.count { it.status == BatteryStatus.DISCHARGED }
            val storageCount = batteries.count { it.status == BatteryStatus.STORAGE }
            val outOfServiceCount = batteries.count { it.status == BatteryStatus.OUT_OF_SERVICE }
            
            canvas.drawText("Résumé: ${batteries.size} batteries", MARGIN, yPosition, headerPaint)
            yPosition += LINE_HEIGHT
            canvas.drawText("• Chargées: $chargedCount", MARGIN + 20f, yPosition, textPaint)
            yPosition += LINE_HEIGHT
            canvas.drawText("• Déchargées: $dischargedCount", MARGIN + 20f, yPosition, textPaint)
            yPosition += LINE_HEIGHT
            canvas.drawText("• En stockage: $storageCount", MARGIN + 20f, yPosition, textPaint)
            yPosition += LINE_HEIGHT
            canvas.drawText("• Hors service: $outOfServiceCount", MARGIN + 20f, yPosition, textPaint)
            yPosition += 40f
            
            // Ligne de séparation
            canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, linePaint)
            yPosition += 20f
            
            // Liste des batteries
            batteries.forEachIndexed { index, battery ->
                // Vérifier si on a besoin d'une nouvelle page
                if (yPosition > PAGE_HEIGHT - 150f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPosition = MARGIN
                }
                
                yPosition = drawBatteryCard(canvas, battery, yPosition)
                yPosition += 20f
            }
            
            pdfDocument.finishPage(page)
            
            // Sauvegarder le fichier
            val outputDir = File(context.cacheDir, "exports")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            
            val outputFile = File(outputDir, fileName)
            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            
            pdfDocument.close()
            
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Exporte une batterie détaillée avec son historique en PDF
     */
    fun exportBatteryDetailToPdf(
        context: Context,
        battery: Battery,
        history: List<BatteryHistory>,
        fileName: String = "skyfuel_battery_${battery.serialNumber}_${LocalDate.now()}.pdf"
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            var yPosition = MARGIN
            
            // Titre
            canvas.drawText("Fiche Batterie", MARGIN, yPosition + 24f, titlePaint)
            yPosition += 50f
            
            // Informations de la batterie
            canvas.drawText("${battery.brand} ${battery.model}", MARGIN, yPosition, headerPaint)
            yPosition += LINE_HEIGHT + 5f
            
            canvas.drawText("N° série: ${battery.serialNumber}", MARGIN, yPosition, textPaint)
            yPosition += LINE_HEIGHT
            
            val statusPaint = statusPaints[battery.status] ?: textPaint
            canvas.drawText("Statut: ${getStatusText(battery.status)}", MARGIN, yPosition, statusPaint)
            yPosition += LINE_HEIGHT + 10f
            
            // Caractéristiques techniques
            canvas.drawText("Caractéristiques", MARGIN, yPosition, headerPaint)
            yPosition += LINE_HEIGHT
            canvas.drawText("• Type: ${battery.type.name}", MARGIN + 20f, yPosition, textPaint)
            yPosition += LINE_HEIGHT
            canvas.drawText("• Cellules: ${battery.cells}S", MARGIN + 20f, yPosition, textPaint)
            yPosition += LINE_HEIGHT
            canvas.drawText("• Capacité: ${battery.capacity} mAh", MARGIN + 20f, yPosition, textPaint)
            yPosition += LINE_HEIGHT
            canvas.drawText("• Cycles: ${battery.cycleCount}", MARGIN + 20f, yPosition, textPaint)
            yPosition += LINE_HEIGHT
            canvas.drawText("• Santé: ${battery.getHealthPercentage()}%", MARGIN + 20f, yPosition, textPaint)
            yPosition += LINE_HEIGHT + 10f
            
            // Dates
            val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            canvas.drawText("Dates importantes", MARGIN, yPosition, headerPaint)
            yPosition += LINE_HEIGHT
            canvas.drawText("• Achat: ${battery.purchaseDate.format(dateFormatter)}", MARGIN + 20f, yPosition, textPaint)
            yPosition += LINE_HEIGHT
            battery.lastUseDate?.let {
                canvas.drawText("• Dernière utilisation: ${it.format(dateFormatter)}", MARGIN + 20f, yPosition, textPaint)
                yPosition += LINE_HEIGHT
            }
            battery.lastChargeDate?.let {
                canvas.drawText("• Dernière charge: ${it.format(dateFormatter)}", MARGIN + 20f, yPosition, textPaint)
                yPosition += LINE_HEIGHT
            }
            yPosition += 20f
            
            // Notes
            if (battery.notes.isNotBlank()) {
                canvas.drawText("Notes", MARGIN, yPosition, headerPaint)
                yPosition += LINE_HEIGHT
                
                // Diviser les notes en lignes si elles sont trop longues
                val maxCharsPerLine = 70
                val noteLines = battery.notes.chunked(maxCharsPerLine)
                noteLines.forEach { line ->
                    canvas.drawText(line, MARGIN + 20f, yPosition, textPaint)
                    yPosition += LINE_HEIGHT
                }
                yPosition += 10f
            }
            
            // Historique
            if (history.isNotEmpty()) {
                canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, linePaint)
                yPosition += 20f
                
                canvas.drawText("Historique (${history.size} événements)", MARGIN, yPosition, headerPaint)
                yPosition += LINE_HEIGHT + 5f
                
                val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                history.take(20).forEach { event ->
                    // Nouvelle page si nécessaire
                    if (yPosition > PAGE_HEIGHT - 60f) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        yPosition = MARGIN
                    }
                    
                    canvas.drawText(
                        "• ${event.timestamp.format(dateTimeFormatter)}: ${event.getDescription()}",
                        MARGIN + 20f,
                        yPosition,
                        smallTextPaint
                    )
                    yPosition += LINE_HEIGHT - 5f
                }
            }
            
            pdfDocument.finishPage(page)
            
            // Sauvegarder
            val outputDir = File(context.cacheDir, "exports")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            
            val outputFile = File(outputDir, fileName)
            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            
            pdfDocument.close()
            
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun drawBatteryCard(canvas: Canvas, battery: Battery, startY: Float): Float {
        var y = startY
        
        // Nom de la batterie
        canvas.drawText("${battery.brand} ${battery.model}", MARGIN, y, headerPaint)
        y += LINE_HEIGHT
        
        // Numéro de série et statut sur la même ligne
        canvas.drawText("S/N: ${battery.serialNumber}", MARGIN, y, textPaint)
        
        val statusPaint = statusPaints[battery.status] ?: textPaint
        val statusText = getStatusText(battery.status)
        canvas.drawText(statusText, PAGE_WIDTH - MARGIN - 100f, y, statusPaint)
        y += LINE_HEIGHT
        
        // Infos techniques
        canvas.drawText(
            "${battery.type.name} | ${battery.cells}S | ${battery.capacity}mAh | ${battery.cycleCount} cycles",
            MARGIN,
            y,
            smallTextPaint
        )
        y += LINE_HEIGHT
        
        // Santé
        canvas.drawText("Santé: ${battery.getHealthPercentage()}%", MARGIN, y, smallTextPaint)
        y += 5f
        
        // Ligne de séparation
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        
        return y
    }
    
    private fun getStatusText(status: BatteryStatus): String {
        return when (status) {
            BatteryStatus.CHARGED -> "Chargée"
            BatteryStatus.DISCHARGED -> "Déchargée"
            BatteryStatus.STORAGE -> "Stockage"
            BatteryStatus.OUT_OF_SERVICE -> "Hors service"
        }
    }
}
