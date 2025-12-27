package leonfvt.skyfuel_app.domain.usecase

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

class GetBatteryDetailUseCaseTest {

    private lateinit var getBatteryDetailUseCase: GetBatteryDetailUseCase
    private lateinit var addBatteryUseCase: AddBatteryUseCase
    private lateinit var fakeBatteryRepository: FakeBatteryRepository

    @Before
    fun setUp() {
        fakeBatteryRepository = FakeBatteryRepository()
        getBatteryDetailUseCase = GetBatteryDetailUseCase(fakeBatteryRepository)
        addBatteryUseCase = AddBatteryUseCase(fakeBatteryRepository)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBatteryDetail returns correct battery for valid id`() = runTest {
        // Given - add a test battery to the repository
        val result = addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "SN123456789",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000,
            purchaseDate = LocalDate.now().minusDays(30)
        )
        val testBatteryId = (result as Result.Success).data

        // When - get the battery details
        val retrievedBattery = getBatteryDetailUseCase(testBatteryId)

        // Then - verify battery details match what was added
        assertNotNull("Battery should not be null", retrievedBattery)
        retrievedBattery?.let {
            assertEquals(testBatteryId, it.id)
            assertEquals("DJI", it.brand)
            assertEquals("Mavic 3", it.model)
            assertEquals("SN123456789", it.serialNumber)
            assertEquals(BatteryType.LIPO, it.type)
            assertEquals(4, it.cells)
            assertEquals(5000, it.capacity)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBatteryDetail returns null for non-existent id`() = runTest {
        // Given - a non-existent battery ID
        val nonExistentId = 999L

        // When - try to get the battery details
        val retrievedBattery = getBatteryDetailUseCase(nonExistentId)

        // Then - should return null
        assertNull("Battery should be null for non-existent ID", retrievedBattery)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBatteryDetail returns null after battery is deleted`() = runTest {
        // Given - add a test battery to the repository
        val result = addBatteryUseCase(
            brand = "Test",
            model = "TestModel",
            serialNumber = "TEST123",
            type = BatteryType.LIPO,
            cells = 3,
            capacity = 3000
        )
        val testBatteryId = (result as Result.Success).data

        // Verify the battery exists
        val batteryBeforeDelete = getBatteryDetailUseCase(testBatteryId)
        assertNotNull("Battery should exist before deletion", batteryBeforeDelete)

        // Delete the battery
        batteryBeforeDelete?.let { fakeBatteryRepository.deleteBattery(it) }

        // When - try to get the battery details after deletion
        val batteryAfterDelete = getBatteryDetailUseCase(testBatteryId)

        // Then - should return null
        assertNull("Battery should be null after deletion", batteryAfterDelete)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBatteryDetail returns updated battery after battery is updated`() = runTest {
        // Given - add a test battery to the repository
        val result = addBatteryUseCase(
            brand = "Original",
            model = "OriginalModel",
            serialNumber = "ORIG123",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 4000
        )
        val testBatteryId = (result as Result.Success).data

        // Get the original battery
        val originalBattery = getBatteryDetailUseCase(testBatteryId)
        assertNotNull("Original battery should exist", originalBattery)

        // Update the battery
        originalBattery?.let {
            val updatedBattery = it.copy(
                brand = "Updated",
                model = "UpdatedModel"
            )
            fakeBatteryRepository.updateBattery(updatedBattery)
        }

        // When - get the battery details after update
        val retrievedBattery = getBatteryDetailUseCase(testBatteryId)

        // Then - should return updated battery
        assertNotNull("Updated battery should exist", retrievedBattery)
        retrievedBattery?.let {
            assertEquals(testBatteryId, it.id)
            assertEquals("Updated", it.brand)
            assertEquals("UpdatedModel", it.model)
            assertEquals("ORIG123", it.serialNumber) // Should remain unchanged
        }
    }
}
