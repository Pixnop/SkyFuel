package leonfvt.skyfuel_app.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.model.BatteryType
import leonfvt.skyfuel_app.domain.model.Result
import leonfvt.skyfuel_app.repository.FakeBatteryRepository
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate

/**
 * Tests pour AddBatteryUseCase.
 *
 * Vérifie que le use case retourne correctement des Result.Success
 * ou Result.Error selon les cas.
 */
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
    fun `addBattery with valid data returns Success with id and adds battery to repository`() = runTest {
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
        val result = addBatteryUseCase(
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
        assertTrue(result.isSuccess)
        val batteryId = (result as Result.Success).data
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
            assertEquals(BatteryStatus.CHARGED, it.status)
            assertEquals(0, it.cycleCount)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `addBattery with empty brand returns Error`() = runTest {
        // When
        val result = addBatteryUseCase(
            brand = "",
            model = "Mavic 3",
            serialNumber = "SN123456789",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000
        )

        // Then
        assertTrue(result.isError)
        assertTrue((result as Result.Error).message.contains("marque"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `addBattery with empty model returns Error`() = runTest {
        // When
        val result = addBatteryUseCase(
            brand = "DJI",
            model = "",
            serialNumber = "SN123456789",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000
        )

        // Then
        assertTrue(result.isError)
        assertTrue((result as Result.Error).message.contains("modèle"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `addBattery with empty serialNumber returns Error`() = runTest {
        // When
        val result = addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000
        )

        // Then
        assertTrue(result.isError)
        assertTrue((result as Result.Error).message.contains("série"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `addBattery with zero cells returns Error`() = runTest {
        // When
        val result = addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "SN123456789",
            type = BatteryType.LIPO,
            cells = 0,
            capacity = 5000
        )

        // Then
        assertTrue(result.isError)
        assertTrue((result as Result.Error).message.contains("cellules"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `addBattery with negative cells returns Error`() = runTest {
        // When
        val result = addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "SN123456789",
            type = BatteryType.LIPO,
            cells = -1,
            capacity = 5000
        )

        // Then
        assertTrue(result.isError)
        assertTrue((result as Result.Error).message.contains("cellules"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `addBattery with zero capacity returns Error`() = runTest {
        // When
        val result = addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "SN123456789",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 0
        )

        // Then
        assertTrue(result.isError)
        assertTrue((result as Result.Error).message.contains("capacité"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `addBattery with negative capacity returns Error`() = runTest {
        // When
        val result = addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "SN123456789",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = -100
        )

        // Then
        assertTrue(result.isError)
        assertTrue((result as Result.Error).message.contains("capacité"))
    }
}