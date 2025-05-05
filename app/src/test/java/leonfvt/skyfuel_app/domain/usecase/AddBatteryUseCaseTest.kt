package leonfvt.skyfuel_app.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.model.BatteryType
import leonfvt.skyfuel_app.repository.FakeBatteryRepository
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate

class AddBatteryUseCaseTest {

    private lateinit var addBatteryUseCase: AddBatteryUseCase
    private lateinit var fakeBatteryRepository: FakeBatteryRepository

    @Before
    fun setUp() {
        fakeBatteryRepository = FakeBatteryRepository()
        addBatteryUseCase = AddBatteryUseCase(fakeBatteryRepository)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `addBattery with valid data returns id and adds battery to repository`() = runTest {
        // Given
        val brand = "DJI"
        val model = "Mavic 3"
        val serialNumber = "SN123456789"
        val type = BatteryType.LIPO
        val cells = 4
        val capacity = 5000
        val purchaseDate = LocalDate.now().minusDays(30)
        val notes = "Test notes"

        // When
        val batteryId = addBatteryUseCase(
            brand = brand,
            model = model,
            serialNumber = serialNumber,
            type = type,
            cells = cells,
            capacity = capacity,
            purchaseDate = purchaseDate,
            notes = notes
        )

        // Then
        assertTrue(batteryId > 0)
        val savedBattery = fakeBatteryRepository.getBatteryById(batteryId)
        assertNotNull(savedBattery)
        savedBattery?.let {
            assertEquals(brand, it.brand)
            assertEquals(model, it.model)
            assertEquals(serialNumber, it.serialNumber)
            assertEquals(type, it.type)
            assertEquals(cells, it.cells)
            assertEquals(capacity, it.capacity)
            assertEquals(purchaseDate, it.purchaseDate)
            assertEquals(notes, it.notes)
            assertEquals(BatteryStatus.CHARGED, it.status) // Default status should be CHARGED
            assertEquals(0, it.cycleCount) // Default cycle count should be 0
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test(expected = IllegalArgumentException::class)
    fun `addBattery with empty brand throws IllegalArgumentException`() = runTest {
        addBatteryUseCase(
            brand = "",
            model = "Mavic 3",
            serialNumber = "SN123456789",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test(expected = IllegalArgumentException::class)
    fun `addBattery with empty model throws IllegalArgumentException`() = runTest {
        addBatteryUseCase(
            brand = "DJI",
            model = "",
            serialNumber = "SN123456789",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test(expected = IllegalArgumentException::class)
    fun `addBattery with empty serialNumber throws IllegalArgumentException`() = runTest {
        addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test(expected = IllegalArgumentException::class)
    fun `addBattery with zero cells throws IllegalArgumentException`() = runTest {
        addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "SN123456789",
            type = BatteryType.LIPO,
            cells = 0,
            capacity = 5000
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test(expected = IllegalArgumentException::class)
    fun `addBattery with negative cells throws IllegalArgumentException`() = runTest {
        addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "SN123456789",
            type = BatteryType.LIPO,
            cells = -1,
            capacity = 5000
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test(expected = IllegalArgumentException::class)
    fun `addBattery with zero capacity throws IllegalArgumentException`() = runTest {
        addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "SN123456789",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 0
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test(expected = IllegalArgumentException::class)
    fun `addBattery with negative capacity throws IllegalArgumentException`() = runTest {
        addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "SN123456789",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = -100
        )
    }
}