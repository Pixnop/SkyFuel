package leonfvt.skyfuel_app

import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.model.BatteryType
import java.time.LocalDate

/**
 * Helper functions for setting up test data
 */
object TestUtils {
    
    /**
     * Creates a test battery with default values that can be overridden
     */
    fun createTestBattery(
        id: Long = 1L,
        brand: String = "DJI",
        model: String = "Mavic 3",
        serialNumber: String = "TEST123456",
        type: BatteryType = BatteryType.LIPO,
        cells: Int = 4,
        capacity: Int = 5000,
        purchaseDate: LocalDate = LocalDate.now().minusDays(30),
        status: BatteryStatus = BatteryStatus.CHARGED,
        cycleCount: Int = 0,
        notes: String = "Test notes",
        lastUseDate: LocalDate? = LocalDate.now().minusDays(5),
        lastChargeDate: LocalDate? = LocalDate.now().minusDays(2)
    ): Battery {
        return Battery(
            id = id,
            brand = brand,
            model = model,
            serialNumber = serialNumber,
            type = type,
            cells = cells,
            capacity = capacity,
            purchaseDate = purchaseDate,
            status = status,
            cycleCount = cycleCount,
            notes = notes,
            lastUseDate = lastUseDate,
            lastChargeDate = lastChargeDate
        )
    }
}