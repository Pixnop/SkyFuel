package leonfvt.skyfuel_app.domain.usecase

import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import leonfvt.skyfuel_app.domain.service.DataExportService
import leonfvt.skyfuel_app.domain.service.ExportFormat
import leonfvt.skyfuel_app.domain.service.ImportResult as ServiceImportResult
import javax.inject.Inject

/**
 * UseCase pour importer les données
 */
class ImportDataUseCase @Inject constructor(
    private val repository: BatteryRepository,
    private val exportService: DataExportService
) {
    /**
     * Importe les données depuis le contenu fourni
     * @param content Le contenu du fichier
     * @param format Le format du fichier
     * @param replaceExisting Si true, supprime les données existantes avant l'import
     */
    suspend operator fun invoke(
        content: String,
        format: ExportFormat,
        replaceExisting: Boolean = false
    ): ImportDataResult {
        return try {
            val parseResult = when (format) {
                ExportFormat.JSON -> exportService.importFromJson(content)
                ExportFormat.CSV -> exportService.importFromCsv(content)
            }
            
            when (parseResult) {
                is ServiceImportResult.Success -> {
                    if (replaceExisting) {
                        repository.deleteAllBatteries()
                    }
                    
                    var importedCount = 0
                    var skippedCount = 0
                    
                    parseResult.batteries.forEach { battery ->
                        // Vérifier si une batterie avec le même numéro de série existe
                        val existing = repository.getBatteryBySerialNumber(battery.serialNumber)
                        if (existing == null) {
                            repository.insertBattery(battery)
                            importedCount++
                        } else {
                            skippedCount++
                        }
                    }
                    
                    ImportDataResult.Success(
                        importedCount = importedCount,
                        skippedCount = skippedCount,
                        totalInFile = parseResult.batteries.size
                    )
                }
                is ServiceImportResult.Error -> {
                    ImportDataResult.Error(parseResult.message)
                }
            }
        } catch (e: Exception) {
            ImportDataResult.Error("Erreur lors de l'import: ${e.message}")
        }
    }
}

sealed class ImportDataResult {
    data class Success(
        val importedCount: Int,
        val skippedCount: Int,
        val totalInFile: Int
    ) : ImportDataResult()
    
    data class Error(val message: String) : ImportDataResult()
}
