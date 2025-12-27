package leonfvt.skyfuel_app.domain.usecase

import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import leonfvt.skyfuel_app.domain.service.DataExportService
import leonfvt.skyfuel_app.domain.service.ExportFormat
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * UseCase pour exporter les données
 */
class ExportDataUseCase @Inject constructor(
    private val repository: BatteryRepository,
    private val exportService: DataExportService
) {
    /**
     * Exporte toutes les données au format spécifié
     */
    suspend operator fun invoke(format: ExportFormat, includeHistory: Boolean = true): ExportResult {
        return try {
            val batteries = repository.getAllBatteries().first()
            val history = if (includeHistory) {
                repository.getAllHistory()
            } else {
                emptyList()
            }
            
            val content = when (format) {
                ExportFormat.JSON -> exportService.exportToJson(batteries, history)
                ExportFormat.CSV -> exportService.exportToCsv(batteries)
            }
            
            val fileName = exportService.generateExportFileName(format)
            
            ExportResult.Success(content, fileName, batteries.size)
        } catch (e: Exception) {
            ExportResult.Error("Erreur lors de l'export: ${e.message}")
        }
    }
}

sealed class ExportResult {
    data class Success(
        val content: String,
        val fileName: String,
        val batteryCount: Int
    ) : ExportResult()
    
    data class Error(val message: String) : ExportResult()
}
