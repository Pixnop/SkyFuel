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

class AddBatteryNoteUseCaseTest {

    private lateinit var addBatteryNoteUseCase: AddBatteryNoteUseCase
    private lateinit var addBatteryUseCase: AddBatteryUseCase
    private lateinit var getBatteryHistoryUseCase: GetBatteryHistoryUseCase
    private lateinit var fakeBatteryRepository: FakeBatteryRepository

    private var testBatteryId: Long = 0

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() = runTest {
        fakeBatteryRepository = FakeBatteryRepository()
        addBatteryNoteUseCase = AddBatteryNoteUseCase(fakeBatteryRepository)
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
    fun `addBatteryNote creates history entry with note`() = runTest {
        // When
        addBatteryNoteUseCase(testBatteryId, "Test note for battery")

        // Then
        val history = getBatteryHistoryUseCase(testBatteryId).first()
        val noteEntry = history.find { it.eventType == BatteryEventType.NOTE_ADDED }

        assertNotNull("Should have a note entry", noteEntry)
        assertTrue("Note should contain our message", noteEntry?.notes?.contains("Test note for battery") ?: false)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test(expected = IllegalArgumentException::class)
    fun `addBatteryNote with empty note throws exception`() = runTest {
        // When - trying to add empty note
        addBatteryNoteUseCase(testBatteryId, "")

        // Then - should throw IllegalArgumentException
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test(expected = IllegalArgumentException::class)
    fun `addBatteryNote with blank note throws exception`() = runTest {
        // When - trying to add blank note (only spaces)
        addBatteryNoteUseCase(testBatteryId, "   ")

        // Then - should throw IllegalArgumentException
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `addBatteryNote multiple notes create separate entries`() = runTest {
        // When - add multiple notes
        addBatteryNoteUseCase(testBatteryId, "First note")
        addBatteryNoteUseCase(testBatteryId, "Second note")
        addBatteryNoteUseCase(testBatteryId, "Third note")

        // Then
        val history = getBatteryHistoryUseCase(testBatteryId).first()
        val noteEntries = history.filter { it.eventType == BatteryEventType.NOTE_ADDED }

        assertEquals("Should have 3 note entries", 3, noteEntries.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `addBatteryNote preserves note content`() = runTest {
        // Given - a note with special characters
        val specialNote = "Battery check: OK! Temperature: 25C, Voltage: 15.2V"

        // When
        addBatteryNoteUseCase(testBatteryId, specialNote)

        // Then
        val history = getBatteryHistoryUseCase(testBatteryId).first()
        val noteEntry = history.find { it.eventType == BatteryEventType.NOTE_ADDED }

        assertNotNull("Should have note entry", noteEntry)
        assertEquals("Note content should be preserved", specialNote, noteEntry?.notes)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `addBatteryNote for different batteries are isolated`() = runTest {
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

        // When - add note to first battery only
        addBatteryNoteUseCase(testBatteryId, "Note for battery 1")

        // Then - second battery should not have the note
        val history1 = getBatteryHistoryUseCase(testBatteryId).first()
        val history2 = getBatteryHistoryUseCase(battery2Id).first()

        val noteEntries1 = history1.filter { it.eventType == BatteryEventType.NOTE_ADDED }
        val noteEntries2 = history2.filter { it.eventType == BatteryEventType.NOTE_ADDED }

        assertEquals("Battery 1 should have 1 note", 1, noteEntries1.size)
        assertEquals("Battery 2 should have 0 notes", 0, noteEntries2.size)
    }
}
