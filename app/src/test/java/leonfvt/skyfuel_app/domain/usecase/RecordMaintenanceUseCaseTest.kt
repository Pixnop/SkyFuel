package leonfvt.skyfuel_app.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import leonfvt.skyfuel_app.domain.model.BatteryEventType
import leonfvt.skyfuel_app.domain.model.BatteryType
import leonfvt.skyfuel_app.domain.model.Result
import leonfvt.skyfuel_app.repository.FakeBatteryRepository
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class RecordMaintenanceUseCaseTest {

    private lateinit var recordMaintenanceUseCase: RecordMaintenanceUseCase
    private lateinit var addBatteryUseCase: AddBatteryUseCase
    private lateinit var getBatteryHistoryUseCase: GetBatteryHistoryUseCase
    private lateinit var fakeBatteryRepository: FakeBatteryRepository

    private var testBatteryId: Long = 0

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() = runTest {
        fakeBatteryRepository = FakeBatteryRepository()
        recordMaintenanceUseCase = RecordMaintenanceUseCase(fakeBatteryRepository)
        addBatteryUseCase = AddBatteryUseCase(fakeBatteryRepository)
        getBatteryHistoryUseCase = GetBatteryHistoryUseCase(fakeBatteryRepository)

        // Create a test battery
        val result = addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "SN001",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000
        )
        testBatteryId = (result as Result.Success).data
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `recordMaintenance creates history entry`() = runTest {
        // When
        recordMaintenanceUseCase(testBatteryId, "Changed battery contacts")

        // Then
        val history = getBatteryHistoryUseCase(testBatteryId).first()
        val maintenanceEntry = history.find { it.eventType == BatteryEventType.MAINTENANCE }

        assertNotNull("Should have a maintenance entry", maintenanceEntry)
        assertTrue(
            "Maintenance entry should contain description",
            maintenanceEntry?.notes?.contains("Changed battery contacts") ?: false
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test(expected = IllegalArgumentException::class)
    fun `recordMaintenance with empty description throws exception`() = runTest {
        // When - trying to record with empty description
        recordMaintenanceUseCase(testBatteryId, "")

        // Then - should throw IllegalArgumentException
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test(expected = IllegalArgumentException::class)
    fun `recordMaintenance with blank description throws exception`() = runTest {
        // When - trying to record with blank description
        recordMaintenanceUseCase(testBatteryId, "   ")

        // Then - should throw IllegalArgumentException
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `recordMaintenance multiple times creates separate entries`() = runTest {
        // When - record multiple maintenance events
        recordMaintenanceUseCase(testBatteryId, "Cleaned contacts")
        recordMaintenanceUseCase(testBatteryId, "Checked cell balance")
        recordMaintenanceUseCase(testBatteryId, "Visual inspection")

        // Then
        val history = getBatteryHistoryUseCase(testBatteryId).first()
        val maintenanceEntries = history.filter { it.eventType == BatteryEventType.MAINTENANCE }

        assertEquals("Should have 3 maintenance entries", 3, maintenanceEntries.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `recordMaintenance preserves description content`() = runTest {
        // Given - a detailed maintenance description
        val detailedDescription = "Full maintenance: cleaned contacts, checked cell voltage (3.85V/cell), updated firmware to v1.2.3"

        // When
        recordMaintenanceUseCase(testBatteryId, detailedDescription)

        // Then
        val history = getBatteryHistoryUseCase(testBatteryId).first()
        val maintenanceEntry = history.find { it.eventType == BatteryEventType.MAINTENANCE }

        assertNotNull("Should have maintenance entry", maintenanceEntry)
        assertEquals("Description should be preserved", detailedDescription, maintenanceEntry?.notes)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `recordMaintenance for different batteries are isolated`() = runTest {
        // Given - add another battery
        val result2 = addBatteryUseCase(
            brand = "Autel",
            model = "EVO II",
            serialNumber = "SN002",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 6000
        )
        val battery2Id = (result2 as Result.Success).data

        // When - record maintenance on first battery only
        recordMaintenanceUseCase(testBatteryId, "Maintenance for battery 1")

        // Then - second battery should not have the maintenance entry
        val history1 = getBatteryHistoryUseCase(testBatteryId).first()
        val history2 = getBatteryHistoryUseCase(battery2Id).first()

        val maintenanceEntries1 = history1.filter { it.eventType == BatteryEventType.MAINTENANCE }
        val maintenanceEntries2 = history2.filter { it.eventType == BatteryEventType.MAINTENANCE }

        assertEquals("Battery 1 should have 1 maintenance entry", 1, maintenanceEntries1.size)
        assertEquals("Battery 2 should have 0 maintenance entries", 0, maintenanceEntries2.size)
    }
}
