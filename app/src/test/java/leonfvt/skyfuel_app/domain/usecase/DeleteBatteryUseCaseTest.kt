package leonfvt.skyfuel_app.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import leonfvt.skyfuel_app.domain.model.BatteryType
import leonfvt.skyfuel_app.domain.model.Result
import leonfvt.skyfuel_app.repository.FakeBatteryRepository
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class DeleteBatteryUseCaseTest {

    private lateinit var deleteBatteryUseCase: DeleteBatteryUseCase
    private lateinit var addBatteryUseCase: AddBatteryUseCase
    private lateinit var getBatteryDetailUseCase: GetBatteryDetailUseCase
    private lateinit var getAllBatteriesUseCase: GetAllBatteriesUseCase
    private lateinit var fakeBatteryRepository: FakeBatteryRepository

    @Before
    fun setUp() {
        fakeBatteryRepository = FakeBatteryRepository()
        deleteBatteryUseCase = DeleteBatteryUseCase(fakeBatteryRepository)
        addBatteryUseCase = AddBatteryUseCase(fakeBatteryRepository)
        getBatteryDetailUseCase = GetBatteryDetailUseCase(fakeBatteryRepository)
        getAllBatteriesUseCase = GetAllBatteriesUseCase(fakeBatteryRepository)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `deleteBattery removes battery from repository`() = runTest {
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
        val battery = getBatteryDetailUseCase(batteryId)
        assertNotNull("Battery should exist before deletion", battery)

        // When - delete the battery
        deleteBatteryUseCase(battery!!)

        // Then - battery should not exist
        val deletedBattery = getBatteryDetailUseCase(batteryId)
        assertNull("Battery should be null after deletion", deletedBattery)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `deleteBattery updates battery list flow`() = runTest {
        // Given - add two batteries
        val result1 = addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "SN001",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000
        )
        val result2 = addBatteryUseCase(
            brand = "Autel",
            model = "EVO II",
            serialNumber = "SN002",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 6000
        )

        val battery1Id = (result1 as Result.Success).data
        val battery1 = getBatteryDetailUseCase(battery1Id)!!

        // Verify initial count
        val initialList = getAllBatteriesUseCase().first()
        assertEquals("Should have 2 batteries initially", 2, initialList.size)

        // When - delete first battery
        deleteBatteryUseCase(battery1)

        // Then - list should have 1 battery
        val updatedList = getAllBatteriesUseCase().first()
        assertEquals("Should have 1 battery after deletion", 1, updatedList.size)
        assertEquals("Remaining battery should be Autel", "Autel", updatedList[0].brand)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `deleteBattery also removes battery history`() = runTest {
        // Given - add a battery and check it has history
        val result = addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "SN001",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000
        )
        val batteryId = (result as Result.Success).data
        val battery = getBatteryDetailUseCase(batteryId)!!

        // Verify history exists
        val historyBefore = fakeBatteryRepository.getBatteryHistory(batteryId).first()
        assertTrue("Should have history entries", historyBefore.isNotEmpty())

        // When - delete the battery
        deleteBatteryUseCase(battery)

        // Then - history should be empty
        val historyAfter = fakeBatteryRepository.getBatteryHistory(batteryId).first()
        assertTrue("History should be empty after deletion", historyAfter.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `deleteBattery with non-existent battery doesn't crash`() = runTest {
        // Given - a battery that was never added (with a fake ID)
        val result = addBatteryUseCase(
            brand = "Test",
            model = "Test",
            serialNumber = "TEST",
            type = BatteryType.LIPO,
            cells = 3,
            capacity = 3000
        )
        val batteryId = (result as Result.Success).data
        val battery = getBatteryDetailUseCase(batteryId)!!

        // Delete it first
        deleteBatteryUseCase(battery)

        // When - try to delete again (simulating non-existent)
        try {
            deleteBatteryUseCase(battery)
            // If we reach here, no exception was thrown
            assertTrue(true)
        } catch (e: Exception) {
            fail("Should not throw exception when deleting non-existent battery: ${e.message}")
        }
    }
}
