package leonfvt.skyfuel_app.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryType
import leonfvt.skyfuel_app.domain.model.Result
import leonfvt.skyfuel_app.repository.FakeBatteryRepository
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate

class GetAllBatteriesUseCaseTest {

    private lateinit var getAllBatteriesUseCase: GetAllBatteriesUseCase
    private lateinit var addBatteryUseCase: AddBatteryUseCase
    private lateinit var fakeBatteryRepository: FakeBatteryRepository

    @Before
    fun setUp() {
        fakeBatteryRepository = FakeBatteryRepository()
        getAllBatteriesUseCase = GetAllBatteriesUseCase(fakeBatteryRepository)
        addBatteryUseCase = AddBatteryUseCase(fakeBatteryRepository)

        // Clear any existing data
        fakeBatteryRepository.clearAll()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getAllBatteries returns empty list when no batteries exist`() = runTest {
        // When - get all batteries
        val batteries = getAllBatteriesUseCase().first()

        // Then - should return an empty list
        assertTrue("Should return an empty list", batteries.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getAllBatteries returns list with all added batteries`() = runTest {
        // Given - add several test batteries
        addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "SN001",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000
        )

        addBatteryUseCase(
            brand = "Autel",
            model = "Evo II",
            serialNumber = "SN002",
            type = BatteryType.LI_ION,
            cells = 6,
            capacity = 7000
        )

        addBatteryUseCase(
            brand = "Parrot",
            model = "Anafi",
            serialNumber = "SN003",
            type = BatteryType.LIPO,
            cells = 3,
            capacity = 3000
        )

        // When - get all batteries
        val batteries = getAllBatteriesUseCase().first()

        // Then - should return all three batteries
        assertEquals("Should return 3 batteries", 3, batteries.size)

        // Verify battery data
        val brands = batteries.map { it.brand }.toSet()
        assertTrue(brands.contains("DJI"))
        assertTrue(brands.contains("Autel"))
        assertTrue(brands.contains("Parrot"))

        val models = batteries.map { it.model }.toSet()
        assertTrue(models.contains("Mavic 3"))
        assertTrue(models.contains("Evo II"))
        assertTrue(models.contains("Anafi"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getAllBatteries reflects changes after adding a new battery`() = runTest {
        // Given - initially no batteries
        val initialBatteries = getAllBatteriesUseCase().first()
        assertEquals("Should initially have 0 batteries", 0, initialBatteries.size)

        // When - add a new battery
        addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "SN001",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000
        )

        // Then - should now return 1 battery
        val updatedBatteries = getAllBatteriesUseCase().first()
        assertEquals("Should now have 1 battery", 1, updatedBatteries.size)
        assertEquals("DJI", updatedBatteries[0].brand)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getAllBatteries reflects changes after deleting a battery`() = runTest {
        // Given - add two batteries
        addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "SN001",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000
        )

        val batteryToDeleteResult = addBatteryUseCase(
            brand = "Autel",
            model = "Evo II",
            serialNumber = "SN002",
            type = BatteryType.LI_ION,
            cells = 6,
            capacity = 7000
        )
        val batteryToDeleteId = (batteryToDeleteResult as Result.Success).data

        // Verify we have 2 batteries
        val initialBatteries = getAllBatteriesUseCase().first()
        assertEquals("Should initially have 2 batteries", 2, initialBatteries.size)

        // When - delete one battery
        val batteryToDelete = fakeBatteryRepository.getBatteryById(batteryToDeleteId)
        assertNotNull("Battery to delete should exist", batteryToDelete)
        batteryToDelete?.let { fakeBatteryRepository.deleteBattery(it) }

        // Then - should now return 1 battery
        val updatedBatteries = getAllBatteriesUseCase().first()
        assertEquals("Should now have 1 battery", 1, updatedBatteries.size)
        assertEquals("DJI", updatedBatteries[0].brand)
        assertEquals("Mavic 3", updatedBatteries[0].model)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getAllBatteries reflects changes after updating a battery`() = runTest {
        // Given - add a battery
        val batteryResult = addBatteryUseCase(
            brand = "Original",
            model = "OriginalModel",
            serialNumber = "SN001",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000
        )
        val batteryId = (batteryResult as Result.Success).data

        // Verify initial state
        val initialBatteries = getAllBatteriesUseCase().first()
        assertEquals("Should have 1 battery", 1, initialBatteries.size)
        assertEquals("Original", initialBatteries[0].brand)

        // When - update the battery
        val batteryToUpdate = fakeBatteryRepository.getBatteryById(batteryId)
        assertNotNull("Battery to update should exist", batteryToUpdate)
        batteryToUpdate?.let {
            val updatedBattery = it.copy(brand = "Updated", model = "UpdatedModel")
            fakeBatteryRepository.updateBattery(updatedBattery)
        }

        // Then - should reflect the update
        val updatedBatteries = getAllBatteriesUseCase().first()
        assertEquals("Should still have 1 battery", 1, updatedBatteries.size)
        assertEquals("Updated", updatedBatteries[0].brand)
        assertEquals("UpdatedModel", updatedBatteries[0].model)
    }
}
