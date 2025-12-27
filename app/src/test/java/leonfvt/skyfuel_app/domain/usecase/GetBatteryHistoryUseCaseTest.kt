package leonfvt.skyfuel_app.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import leonfvt.skyfuel_app.domain.model.BatteryEventType
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.model.BatteryType
import leonfvt.skyfuel_app.domain.model.Result
import leonfvt.skyfuel_app.repository.FakeBatteryRepository
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class GetBatteryHistoryUseCaseTest {

    private lateinit var getBatteryHistoryUseCase: GetBatteryHistoryUseCase
    private lateinit var addBatteryUseCase: AddBatteryUseCase
    private lateinit var updateBatteryStatusUseCase: UpdateBatteryStatusUseCase
    private lateinit var recordVoltageReadingUseCase: RecordVoltageReadingUseCase
    private lateinit var fakeBatteryRepository: FakeBatteryRepository

    @Before
    fun setUp() {
        fakeBatteryRepository = FakeBatteryRepository()
        getBatteryHistoryUseCase = GetBatteryHistoryUseCase(fakeBatteryRepository)
        addBatteryUseCase = AddBatteryUseCase(fakeBatteryRepository)
        updateBatteryStatusUseCase = UpdateBatteryStatusUseCase(fakeBatteryRepository)
        recordVoltageReadingUseCase = RecordVoltageReadingUseCase(fakeBatteryRepository)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBatteryHistory returns empty list for non-existent battery`() = runTest {
        // When
        val history = getBatteryHistoryUseCase(999L).first()

        // Then
        assertTrue("Should return empty list for non-existent battery", history.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBatteryHistory returns initial entry after battery creation`() = runTest {
        // Given - add a battery
        val result = addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "SN001",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000
        )
        val batteryId = (result as Result.Success).data

        // When
        val history = getBatteryHistoryUseCase(batteryId).first()

        // Then
        assertEquals("Should have 1 initial entry", 1, history.size)
        assertEquals("Entry should be for our battery", batteryId, history[0].batteryId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBatteryHistory includes status change events`() = runTest {
        // Given - add a battery and change its status
        val result = addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "SN001",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000
        )
        val batteryId = (result as Result.Success).data
        updateBatteryStatusUseCase(batteryId, BatteryStatus.DISCHARGED, "After flight")

        // When
        val history = getBatteryHistoryUseCase(batteryId).first()

        // Then
        assertTrue("Should have at least 2 entries", history.size >= 2)
        val statusChangeEvents = history.filter { it.eventType == BatteryEventType.STATUS_CHANGE }
        assertTrue("Should have status change events", statusChangeEvents.isNotEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBatteryHistory includes voltage reading events`() = runTest {
        // Given - add a battery and record a voltage reading
        val result = addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "SN001",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000
        )
        val batteryId = (result as Result.Success).data
        recordVoltageReadingUseCase(batteryId, 15.2f, "Cell check")

        // When
        val history = getBatteryHistoryUseCase(batteryId).first()

        // Then
        val voltageEvents = history.filter { it.eventType == BatteryEventType.VOLTAGE_READING }
        assertEquals("Should have 1 voltage reading event", 1, voltageEvents.size)
        assertEquals("Voltage should be recorded", 15.2f, voltageEvents[0].voltage)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBatteryHistory returns events in order`() = runTest {
        // Given - add a battery with multiple events
        val result = addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "SN001",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000
        )
        val batteryId = (result as Result.Success).data

        // Add multiple events
        updateBatteryStatusUseCase(batteryId, BatteryStatus.DISCHARGED, "Event 1")
        recordVoltageReadingUseCase(batteryId, 14.8f, "Event 2")
        updateBatteryStatusUseCase(batteryId, BatteryStatus.CHARGED, "Event 3")

        // When
        val history = getBatteryHistoryUseCase(batteryId).first()

        // Then
        assertTrue("Should have at least 4 entries", history.size >= 4)
        // Events should have chronological timestamps (or at least not be out of order)
        for (i in 0 until history.size - 1) {
            assertTrue(
                "Events should be in chronological order",
                history[i].timestamp <= history[i + 1].timestamp
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBatteryHistory preserves notes in events`() = runTest {
        // Given
        val result = addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "SN001",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000
        )
        val batteryId = (result as Result.Success).data
        updateBatteryStatusUseCase(batteryId, BatteryStatus.STORAGE, "Storing for winter")

        // When
        val history = getBatteryHistoryUseCase(batteryId).first()

        // Then
        val storageEvent = history.find { it.notes.contains("Storing for winter") }
        assertNotNull("Should find event with our note", storageEvent)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBatteryHistory is isolated per battery`() = runTest {
        // Given - add two batteries with different histories
        val result1 = addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "SN001",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000
        )
        val battery1Id = (result1 as Result.Success).data

        val result2 = addBatteryUseCase(
            brand = "Autel",
            model = "EVO II",
            serialNumber = "SN002",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 6000
        )
        val battery2Id = (result2 as Result.Success).data

        // Add events to battery 1 only
        updateBatteryStatusUseCase(battery1Id, BatteryStatus.DISCHARGED, "Battery 1 event")
        recordVoltageReadingUseCase(battery1Id, 15.0f, "Battery 1 voltage")

        // When
        val history1 = getBatteryHistoryUseCase(battery1Id).first()
        val history2 = getBatteryHistoryUseCase(battery2Id).first()

        // Then
        assertTrue("Battery 1 should have more events", history1.size > history2.size)
        assertTrue("Battery 1 events should reference battery 1", history1.all { it.batteryId == battery1Id })
        assertTrue("Battery 2 events should reference battery 2", history2.all { it.batteryId == battery2Id })
    }
}
