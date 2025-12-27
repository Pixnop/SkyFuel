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

class RecordVoltageReadingUseCaseTest {

    private lateinit var recordVoltageReadingUseCase: RecordVoltageReadingUseCase
    private lateinit var addBatteryUseCase: AddBatteryUseCase
    private lateinit var getBatteryHistoryUseCase: GetBatteryHistoryUseCase
    private lateinit var fakeBatteryRepository: FakeBatteryRepository

    private var testBatteryId: Long = 0

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() = runTest {
        fakeBatteryRepository = FakeBatteryRepository()
        recordVoltageReadingUseCase = RecordVoltageReadingUseCase(fakeBatteryRepository)
        addBatteryUseCase = AddBatteryUseCase(fakeBatteryRepository)
        getBatteryHistoryUseCase = GetBatteryHistoryUseCase(fakeBatteryRepository)

        // Create a test battery for most tests
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
    fun `recordVoltageReading creates history entry with correct voltage`() = runTest {
        // When
        recordVoltageReadingUseCase(testBatteryId, 15.2f, "Test reading")

        // Then
        val history = getBatteryHistoryUseCase(testBatteryId).first()
        val voltageEntry = history.find { it.eventType == BatteryEventType.VOLTAGE_READING }

        assertNotNull("Should have voltage reading entry", voltageEntry)
        assertEquals("Voltage should be recorded", 15.2f, voltageEntry?.voltage)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `recordVoltageReading creates history entry with notes`() = runTest {
        // When
        recordVoltageReadingUseCase(testBatteryId, 14.8f, "Pre-flight check")

        // Then
        val history = getBatteryHistoryUseCase(testBatteryId).first()
        val voltageEntry = history.find { it.eventType == BatteryEventType.VOLTAGE_READING }

        assertNotNull("Should have voltage reading entry", voltageEntry)
        assertTrue("Notes should contain our message", voltageEntry?.notes?.contains("Pre-flight check") ?: false)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `recordVoltageReading with empty notes still works`() = runTest {
        // When
        recordVoltageReadingUseCase(testBatteryId, 16.0f, "")

        // Then
        val history = getBatteryHistoryUseCase(testBatteryId).first()
        val voltageEntry = history.find { it.eventType == BatteryEventType.VOLTAGE_READING }

        assertNotNull("Should have voltage reading entry even with empty notes", voltageEntry)
        assertEquals("Voltage should be recorded", 16.0f, voltageEntry?.voltage)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `recordVoltageReading with default notes parameter works`() = runTest {
        // When - using default notes parameter
        recordVoltageReadingUseCase(testBatteryId, 15.5f)

        // Then
        val history = getBatteryHistoryUseCase(testBatteryId).first()
        val voltageEntry = history.find { it.eventType == BatteryEventType.VOLTAGE_READING }

        assertNotNull("Should have voltage reading entry", voltageEntry)
        assertEquals("Voltage should be recorded", 15.5f, voltageEntry?.voltage)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `multiple voltage readings create separate history entries`() = runTest {
        // When - record multiple readings
        recordVoltageReadingUseCase(testBatteryId, 15.0f, "Reading 1")
        recordVoltageReadingUseCase(testBatteryId, 15.5f, "Reading 2")
        recordVoltageReadingUseCase(testBatteryId, 16.0f, "Reading 3")

        // Then
        val history = getBatteryHistoryUseCase(testBatteryId).first()
        val voltageEntries = history.filter { it.eventType == BatteryEventType.VOLTAGE_READING }

        assertEquals("Should have 3 voltage reading entries", 3, voltageEntries.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test(expected = IllegalArgumentException::class)
    fun `recordVoltageReading with zero voltage throws exception`() = runTest {
        // When - edge case: 0 voltage is invalid
        recordVoltageReadingUseCase(testBatteryId, 0f, "Zero voltage")

        // Then - should throw IllegalArgumentException
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test(expected = IllegalArgumentException::class)
    fun `recordVoltageReading with negative voltage throws exception`() = runTest {
        // When - invalid: negative voltage
        recordVoltageReadingUseCase(testBatteryId, -5.0f, "Negative voltage")

        // Then - should throw IllegalArgumentException
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `recordVoltageReading for different batteries are isolated`() = runTest {
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

        // When - record voltage for each battery
        recordVoltageReadingUseCase(testBatteryId, 15.0f, "Battery 1")
        recordVoltageReadingUseCase(battery2Id, 16.0f, "Battery 2")

        // Then - each battery has its own reading
        val history1 = getBatteryHistoryUseCase(testBatteryId).first()
        val history2 = getBatteryHistoryUseCase(battery2Id).first()

        val voltageEntries1 = history1.filter { it.eventType == BatteryEventType.VOLTAGE_READING }
        val voltageEntries2 = history2.filter { it.eventType == BatteryEventType.VOLTAGE_READING }

        assertEquals("Battery 1 should have 1 voltage entry", 1, voltageEntries1.size)
        assertEquals("Battery 2 should have 1 voltage entry", 1, voltageEntries2.size)
        assertEquals("Battery 1 voltage should be 15.0", 15.0f, voltageEntries1[0].voltage)
        assertEquals("Battery 2 voltage should be 16.0", 16.0f, voltageEntries2[0].voltage)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `recordVoltageReading with high precision voltage`() = runTest {
        // When - record a voltage with decimal precision
        recordVoltageReadingUseCase(testBatteryId, 15.234f, "Precise reading")

        // Then
        val history = getBatteryHistoryUseCase(testBatteryId).first()
        val voltageEntry = history.find { it.eventType == BatteryEventType.VOLTAGE_READING }

        assertNotNull("Should have voltage reading entry", voltageEntry)
        assertEquals("Voltage precision should be preserved", 15.234f, voltageEntry?.voltage ?: 0f, 0.001f)
    }
}
