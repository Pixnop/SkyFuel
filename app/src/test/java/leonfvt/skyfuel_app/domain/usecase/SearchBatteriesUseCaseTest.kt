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

class SearchBatteriesUseCaseTest {

    private lateinit var searchBatteriesUseCase: SearchBatteriesUseCase
    private lateinit var addBatteryUseCase: AddBatteryUseCase
    private lateinit var fakeBatteryRepository: FakeBatteryRepository

    @Before
    fun setUp() {
        fakeBatteryRepository = FakeBatteryRepository()
        searchBatteriesUseCase = SearchBatteriesUseCase(fakeBatteryRepository)
        addBatteryUseCase = AddBatteryUseCase(fakeBatteryRepository)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `searchBatteries returns empty list when no batteries exist`() = runTest {
        // When
        val results = searchBatteriesUseCase("DJI").first()

        // Then
        assertTrue("Should return empty list", results.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `searchBatteries finds battery by brand`() = runTest {
        // Given
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
            model = "EVO II",
            serialNumber = "SN002",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 6000
        )

        // When
        val results = searchBatteriesUseCase("DJI").first()

        // Then
        assertEquals("Should find 1 battery", 1, results.size)
        assertEquals("Should be DJI battery", "DJI", results[0].brand)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `searchBatteries finds battery by model`() = runTest {
        // Given
        addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "SN001",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000
        )
        addBatteryUseCase(
            brand = "DJI",
            model = "Mini 3 Pro",
            serialNumber = "SN002",
            type = BatteryType.LIPO,
            cells = 3,
            capacity = 3000
        )

        // When
        val results = searchBatteriesUseCase("Mavic").first()

        // Then
        assertEquals("Should find 1 battery", 1, results.size)
        assertEquals("Should be Mavic 3 model", "Mavic 3", results[0].model)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `searchBatteries finds battery by serial number`() = runTest {
        // Given
        addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "ABC123XYZ",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000
        )
        addBatteryUseCase(
            brand = "Autel",
            model = "EVO II",
            serialNumber = "DEF456UVW",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 6000
        )

        // When
        val results = searchBatteriesUseCase("ABC123").first()

        // Then
        assertEquals("Should find 1 battery", 1, results.size)
        assertEquals("Should be the battery with matching serial", "ABC123XYZ", results[0].serialNumber)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `searchBatteries is case insensitive`() = runTest {
        // Given
        addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "SN001",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000
        )

        // When - search with different cases
        val resultsLower = searchBatteriesUseCase("dji").first()
        val resultsUpper = searchBatteriesUseCase("DJI").first()
        val resultsMixed = searchBatteriesUseCase("Dji").first()

        // Then
        assertEquals("Lowercase should find battery", 1, resultsLower.size)
        assertEquals("Uppercase should find battery", 1, resultsUpper.size)
        assertEquals("Mixed case should find battery", 1, resultsMixed.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `searchBatteries returns multiple matching batteries`() = runTest {
        // Given - multiple DJI batteries
        addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "SN001",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000
        )
        addBatteryUseCase(
            brand = "DJI",
            model = "Mini 3 Pro",
            serialNumber = "SN002",
            type = BatteryType.LIPO,
            cells = 3,
            capacity = 3000
        )
        addBatteryUseCase(
            brand = "Autel",
            model = "EVO II",
            serialNumber = "SN003",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 6000
        )

        // When
        val results = searchBatteriesUseCase("DJI").first()

        // Then
        assertEquals("Should find 2 DJI batteries", 2, results.size)
        assertTrue("All results should be DJI", results.all { it.brand == "DJI" })
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `searchBatteries returns empty for non-matching query`() = runTest {
        // Given
        addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3",
            serialNumber = "SN001",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000
        )

        // When
        val results = searchBatteriesUseCase("Parrot").first()

        // Then
        assertTrue("Should return empty list for non-matching query", results.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `searchBatteries with partial match finds batteries`() = runTest {
        // Given
        addBatteryUseCase(
            brand = "DJI",
            model = "Mavic 3 Enterprise",
            serialNumber = "ENT001",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000
        )

        // When - partial model name
        val results = searchBatteriesUseCase("Enterprise").first()

        // Then
        assertEquals("Should find battery with partial match", 1, results.size)
    }
}
