package leonfvt.skyfuel_app.domain.service

import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryHistory
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.model.BatteryType
import leonfvt.skyfuel_app.domain.model.BatteryEventType
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Format d'export supporté
 */
enum class ExportFormat {
    JSON,
    CSV
}

/**
 * Service pour l'export et l'import de données
 */
@Singleton
class DataExportService @Inject constructor() {
    
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    
    /**
     * Exporte les batteries au format JSON
     */
    fun exportToJson(
        batteries: List<Battery>,
        history: List<BatteryHistory> = emptyList(),
        appVersion: String = "1.0"
    ): String {
        val root = JSONObject().apply {
            put("exportDate", LocalDateTime.now().format(dateTimeFormatter))
            put("appVersion", appVersion)
            put("batteries", JSONArray().apply {
                batteries.forEach { put(batteryToJson(it)) }
            })
            put("history", JSONArray().apply {
                history.forEach { put(historyToJson(it)) }
            })
        }
        return root.toString(2)
    }
    
    /**
     * Exporte les batteries au format CSV
     */
    fun exportToCsv(batteries: List<Battery>): String {
        val header = "id,brand,model,serialNumber,type,cells,capacity,purchaseDate,status,cycleCount,notes,lastUseDate,lastChargeDate,qrCodeId"
        val rows = batteries.map { battery ->
            listOf(
                battery.id.toString(),
                escapeCsv(battery.brand),
                escapeCsv(battery.model),
                escapeCsv(battery.serialNumber),
                battery.type.name,
                battery.cells.toString(),
                battery.capacity.toString(),
                battery.purchaseDate.format(dateFormatter),
                battery.status.name,
                battery.cycleCount.toString(),
                escapeCsv(battery.notes),
                battery.lastUseDate?.format(dateFormatter) ?: "",
                battery.lastChargeDate?.format(dateFormatter) ?: "",
                escapeCsv(battery.qrCodeId)
            ).joinToString(",")
        }
        return (listOf(header) + rows).joinToString("\n")
    }
    
    /**
     * Importe les batteries depuis JSON
     */
    fun importFromJson(jsonString: String): ImportResult {
        return try {
            val root = JSONObject(jsonString)
            val exportDate = root.optString("exportDate", "")
            
            val batteriesArray = root.getJSONArray("batteries")
            val batteries = mutableListOf<Battery>()
            for (i in 0 until batteriesArray.length()) {
                jsonToBattery(batteriesArray.getJSONObject(i))?.let { batteries.add(it) }
            }
            
            val historyArray = root.optJSONArray("history") ?: JSONArray()
            val history = mutableListOf<BatteryHistory>()
            for (i in 0 until historyArray.length()) {
                jsonToHistory(historyArray.getJSONObject(i))?.let { history.add(it) }
            }
            
            ImportResult.Success(batteries, history, exportDate)
        } catch (e: Exception) {
            ImportResult.Error("Erreur lors de l'import JSON: ${e.message}")
        }
    }
    
    /**
     * Importe les batteries depuis CSV
     */
    fun importFromCsv(csvString: String): ImportResult {
        return try {
            val lines = csvString.lines().filter { it.isNotBlank() }
            if (lines.size < 2) {
                return ImportResult.Error("Le fichier CSV est vide ou invalide")
            }
            
            val batteries = lines.drop(1).mapNotNull { line ->
                parseCsvLine(line)
            }
            
            ImportResult.Success(batteries, emptyList(), LocalDateTime.now().format(dateTimeFormatter))
        } catch (e: Exception) {
            ImportResult.Error("Erreur lors de l'import CSV: ${e.message}")
        }
    }
    
    /**
     * Génère un nom de fichier pour l'export
     */
    fun generateExportFileName(format: ExportFormat): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val extension = when (format) {
            ExportFormat.JSON -> "json"
            ExportFormat.CSV -> "csv"
        }
        return "skyfuel_backup_$timestamp.$extension"
    }
    
    // Conversion Battery -> JSONObject
    private fun batteryToJson(battery: Battery): JSONObject {
        return JSONObject().apply {
            put("id", battery.id)
            put("brand", battery.brand)
            put("model", battery.model)
            put("serialNumber", battery.serialNumber)
            put("type", battery.type.name)
            put("cells", battery.cells)
            put("capacity", battery.capacity)
            put("purchaseDate", battery.purchaseDate.format(dateFormatter))
            put("status", battery.status.name)
            put("cycleCount", battery.cycleCount)
            put("notes", battery.notes)
            put("lastUseDate", battery.lastUseDate?.format(dateFormatter))
            put("lastChargeDate", battery.lastChargeDate?.format(dateFormatter))
            put("qrCodeId", battery.qrCodeId)
        }
    }
    
    // Conversion BatteryHistory -> JSONObject
    private fun historyToJson(history: BatteryHistory): JSONObject {
        return JSONObject().apply {
            put("id", history.id)
            put("batteryId", history.batteryId)
            put("eventType", history.eventType.name)
            put("timestamp", history.timestamp.format(dateTimeFormatter))
            put("previousStatus", history.previousStatus?.name)
            put("newStatus", history.newStatus?.name)
            put("voltage", history.voltage)
            put("notes", history.notes)
            put("cycleNumber", history.cycleNumber)
        }
    }
    
    // Conversion JSONObject -> Battery
    private fun jsonToBattery(json: JSONObject): Battery? {
        return try {
            Battery(
                id = 0, // Nouvel ID lors de l'import
                brand = json.getString("brand"),
                model = json.getString("model"),
                serialNumber = json.getString("serialNumber"),
                type = BatteryType.valueOf(json.getString("type")),
                cells = json.getInt("cells"),
                capacity = json.getInt("capacity"),
                purchaseDate = LocalDate.parse(json.getString("purchaseDate"), dateFormatter),
                status = BatteryStatus.valueOf(json.getString("status")),
                cycleCount = json.getInt("cycleCount"),
                notes = json.optString("notes", ""),
                lastUseDate = json.optString("lastUseDate")?.takeIf { it.isNotBlank() && it != "null" }
                    ?.let { LocalDate.parse(it, dateFormatter) },
                lastChargeDate = json.optString("lastChargeDate")?.takeIf { it.isNotBlank() && it != "null" }
                    ?.let { LocalDate.parse(it, dateFormatter) },
                qrCodeId = json.optString("qrCodeId", "")
            )
        } catch (e: Exception) {
            null
        }
    }
    
    // Conversion JSONObject -> BatteryHistory
    private fun jsonToHistory(json: JSONObject): BatteryHistory? {
        return try {
            BatteryHistory(
                id = 0,
                batteryId = json.getLong("batteryId"),
                eventType = BatteryEventType.valueOf(json.getString("eventType")),
                timestamp = LocalDateTime.parse(json.getString("timestamp"), dateTimeFormatter),
                previousStatus = json.optString("previousStatus")?.takeIf { it.isNotBlank() && it != "null" }
                    ?.let { BatteryStatus.valueOf(it) },
                newStatus = json.optString("newStatus")?.takeIf { it.isNotBlank() && it != "null" }
                    ?.let { BatteryStatus.valueOf(it) },
                voltage = if (json.has("voltage") && !json.isNull("voltage")) 
                    json.getDouble("voltage").toFloat() else null,
                notes = json.optString("notes", ""),
                cycleNumber = if (json.has("cycleNumber") && !json.isNull("cycleNumber"))
                    json.getInt("cycleNumber") else null
            )
        } catch (e: Exception) {
            null
        }
    }
    
    // Parse une ligne CSV
    private fun parseCsvLine(line: String): Battery? {
        return try {
            val values = parseCsvValues(line)
            if (values.size < 14) return null
            
            Battery(
                id = 0,
                brand = values[1],
                model = values[2],
                serialNumber = values[3],
                type = BatteryType.valueOf(values[4]),
                cells = values[5].toInt(),
                capacity = values[6].toInt(),
                purchaseDate = LocalDate.parse(values[7], dateFormatter),
                status = BatteryStatus.valueOf(values[8]),
                cycleCount = values[9].toInt(),
                notes = values[10],
                lastUseDate = values[11].takeIf { it.isNotBlank() }?.let { LocalDate.parse(it, dateFormatter) },
                lastChargeDate = values[12].takeIf { it.isNotBlank() }?.let { LocalDate.parse(it, dateFormatter) },
                qrCodeId = values[13]
            )
        } catch (e: Exception) {
            null
        }
    }
    
    // Parse les valeurs CSV (gère les guillemets)
    private fun parseCsvValues(line: String): List<String> {
        val values = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        
        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    values.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        values.add(current.toString())
        
        return values
    }
    
    // Échappe les valeurs pour CSV
    private fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}

/**
 * Résultat d'un import
 */
sealed class ImportResult {
    data class Success(
        val batteries: List<Battery>,
        val history: List<BatteryHistory>,
        val exportDate: String
    ) : ImportResult()
    
    data class Error(val message: String) : ImportResult()
}