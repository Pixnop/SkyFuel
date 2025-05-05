package leonfvt.skyfuel_app.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import leonfvt.skyfuel_app.domain.model.BatteryEventType
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.model.BatteryType
import leonfvt.skyfuel_app.repository.FakeBatteryRepository
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class UpdateBatteryStatusUseCaseTest {

    private lateinit var updateBatteryStatusUseCase: UpdateBatteryStatusUseCase
    private lateinit var addBatteryUseCase: AddBatteryUseCase
    private lateinit var getBatteryDetailUseCase: GetBatteryDetailUseCase
    private lateinit var fakeBatteryRepository: FakeBatteryRepository
    
    private var testBatteryId: Long = 0
    
    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() = runTest {
        fakeBatteryRepository = FakeBatteryRepository()
        updateBatteryStatusUseCase = UpdateBatteryStatusUseCase(fakeBatteryRepository)
        addBatteryUseCase = AddBatteryUseCase(fakeBatteryRepository)
        getBatteryDetailUseCase = GetBatteryDetailUseCase(fakeBatteryRepository)
        
        // Clear any existing data
        fakeBatteryRepository.clearAll()
        
        // Create a test battery
        testBatteryId = addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "SN001",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000
        )
    }
    
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `updateBatteryStatus changes battery status successfully`() = runTest {
        // Given - a battery with default status (CHARGED)
        val initialBattery = getBatteryDetailUseCase(testBatteryId)
        assertNotNull("Test battery should exist", initialBattery)
        assertEquals("Initial status should be CHARGED", BatteryStatus.CHARGED, initialBattery?.status)
        
        // When - update the battery status to DISCHARGED
        updateBatteryStatusUseCase(testBatteryId, BatteryStatus.DISCHARGED)
        
        // Then - the status should be updated
        val updatedBattery = getBatteryDetailUseCase(testBatteryId)
        assertNotNull("Battery should still exist after update", updatedBattery)
        assertEquals("Status should be updated to DISCHARGED", BatteryStatus.DISCHARGED, updatedBattery?.status)
    }
    
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `updateBatteryStatus creates history entry with notes`() = runTest {
        // Given - a battery and a note for the status change
        val testNotes = "Test notes for status change"
        
        // When - update the battery status with notes
        updateBatteryStatusUseCase(
            batteryId = testBatteryId,
            newStatus = BatteryStatus.STORAGE,
            notes = testNotes
        )
        
        // Then - there should be a history entry for the status change with the notes
        val historyEntries = fakeBatteryRepository.getBatteryHistory(testBatteryId).first()
        
        // Should have at least 2 entries: initial "ADDED" entry and our status change
        assertTrue("Should have at least 2 history entries", historyEntries.size >= 2)
        
        // The latest entry should be our status change
        val statusChangeEntry = historyEntries.filter { it.eventType == BatteryEventType.STATUS_CHANGE }.lastOrNull()
        assertNotNull("Should have a STATUS_CHANGE entry", statusChangeEntry)
        statusChangeEntry?.let {
            assertTrue("Status change entry should contain our notes", it.notes.contains(testNotes))
        }
    }
    
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `updateBatteryStatus with same status still adds history entry`() = runTest {
        // Given - get the initial history entry count
        val initialHistoryEntries = fakeBatteryRepository.getBatteryHistory(testBatteryId).first()
        val initialCount = initialHistoryEntries.size
        
        // When - update with the same status (CHARGED)
        updateBatteryStatusUseCase(testBatteryId, BatteryStatus.CHARGED, "Same status update")
        
        // Then - there should still be a new history entry
        val updatedHistoryEntries = fakeBatteryRepository.getBatteryHistory(testBatteryId).first()
        assertEquals("Should have one more history entry", initialCount + 1, updatedHistoryEntries.size)
    }
    
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `updateBatteryStatus to OUT_OF_SERVICE marks battery appropriately`() = runTest {
        // When - update the battery status to OUT_OF_SERVICE
        updateBatteryStatusUseCase(testBatteryId, BatteryStatus.OUT_OF_SERVICE, "End of life")
        
        // Then - the status should be updated to OUT_OF_SERVICE
        val updatedBattery = getBatteryDetailUseCase(testBatteryId)
        assertNotNull("Battery should still exist", updatedBattery)
        assertEquals("Status should be OUT_OF_SERVICE", BatteryStatus.OUT_OF_SERVICE, updatedBattery?.status)
    }
    
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `updateBatteryStatus with non-existent ID doesn't throw exception`() = runTest {
        // Given - a non-existent battery ID
        val nonExistentId = 999L
        
        try {
            // When - update a non-existent battery
            updateBatteryStatusUseCase(nonExistentId, BatteryStatus.DISCHARGED)
            
            // Then - no exception should be thrown
            // If we reach here, the test passes
            assertTrue(true)
        } catch (e: Exception) {
            fail("Should not throw exception for non-existent battery ID: ${e.message}")
        }
    }
    
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `multiple status updates create correct history entries`() = runTest {
        // When - perform multiple status updates
        updateBatteryStatusUseCase(testBatteryId, BatteryStatus.DISCHARGED, "Discharged after flight")
        updateBatteryStatusUseCase(testBatteryId, BatteryStatus.CHARGED, "Recharged for next flight")
        updateBatteryStatusUseCase(testBatteryId, BatteryStatus.STORAGE, "Storing for long trip")
        
        // Then - there should be history entries for each update
        val historyEntries = fakeBatteryRepository.getBatteryHistory(testBatteryId).first()
        
        // Filter to just STATUS_CHANGE events
        val statusChangeEntries = historyEntries.filter { it.eventType == BatteryEventType.STATUS_CHANGE }
        
        // Should have at least 4 entries: initial "ADDED" entry and our 3 status changes
        assertTrue("Should have at least 4 status change entries", statusChangeEntries.size >= 4)
        
        // Check the notes contain our messages
        val notes = statusChangeEntries.map { it.notes }
        assertTrue("Should contain 'Discharged after flight'", notes.any { it.contains("Discharged after flight") })
        assertTrue("Should contain 'Recharged for next flight'", notes.any { it.contains("Recharged for next flight") })
        assertTrue("Should contain 'Storing for long trip'", notes.any { it.contains("Storing for long trip") })
    }
}