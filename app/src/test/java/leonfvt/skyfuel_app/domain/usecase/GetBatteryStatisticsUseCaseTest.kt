package leonfvt.skyfuel_app.domain.usecase

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.model.BatteryType
import leonfvt.skyfuel_app.domain.model.Result
import leonfvt.skyfuel_app.repository.FakeBatteryRepository
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class GetBatteryStatisticsUseCaseTest {

    private lateinit var getBatteryStatisticsUseCase: GetBatteryStatisticsUseCase
    private lateinit var addBatteryUseCase: AddBatteryUseCase
    private lateinit var updateBatteryStatusUseCase: UpdateBatteryStatusUseCase
    private lateinit var fakeBatteryRepository: FakeBatteryRepository

    @Before
    fun setUp() {
        fakeBatteryRepository = FakeBatteryRepository()
        getBatteryStatisticsUseCase = GetBatteryStatisticsUseCase(fakeBatteryRepository)
        addBatteryUseCase = AddBatteryUseCase(fakeBatteryRepository)
        updateBatteryStatusUseCase = UpdateBatteryStatusUseCase(fakeBatteryRepository)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBatteryStatistics returns zeros when no batteries exist`() = runTest {
        // When
        val stats = getBatteryStatisticsUseCase()

        // Then
        assertEquals("Total count should be 0", 0, stats.totalCount)
        assertEquals("Charged count should be 0", 0, stats.chargedCount)
        assertEquals("Discharged count should be 0", 0, stats.dischargedCount)
        assertEquals("Storage count should be 0", 0, stats.storageCount)
        assertEquals("Out of service count should be 0", 0, stats.outOfServiceCount)
        assertEquals("Average cycle count should be 0", 0f, stats.averageCycleCount)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBatteryStatistics returns correct total count`() = runTest {
        // Given - add 3 batteries
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
        addBatteryUseCase(
            brand = "Parrot",
            model = "Anafi",
            serialNumber = "SN003",
            type = BatteryType.LIPO,
            cells = 3,
            capacity = 4000
        )

        // When
        val stats = getBatteryStatisticsUseCase()

        // Then
        assertEquals("Total count should be 3", 3, stats.totalCount)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBatteryStatistics returns correct counts by status`() = runTest {
        // Given - add batteries with different statuses
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

        val result3 = addBatteryUseCase(
            brand = "Parrot",
            model = "Anafi",
            serialNumber = "SN003",
            type = BatteryType.LIPO,
            cells = 3,
            capacity = 4000
        )
        val battery3Id = (result3 as Result.Success).data

        val result4 = addBatteryUseCase(
            brand = "Old",
            model = "Battery",
            serialNumber = "SN004",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 3000
        )
        val battery4Id = (result4 as Result.Success).data

        // Set different statuses:
        // Battery 1: CHARGED (default)
        // Battery 2: DISCHARGED
        updateBatteryStatusUseCase(battery2Id, BatteryStatus.DISCHARGED)
        // Battery 3: STORAGE
        updateBatteryStatusUseCase(battery3Id, BatteryStatus.STORAGE)
        // Battery 4: OUT_OF_SERVICE
        updateBatteryStatusUseCase(battery4Id, BatteryStatus.OUT_OF_SERVICE)

        // When
        val stats = getBatteryStatisticsUseCase()

        // Then
        assertEquals("Total should be 4", 4, stats.totalCount)
        assertEquals("Charged count should be 1", 1, stats.chargedCount)
        assertEquals("Discharged count should be 1", 1, stats.dischargedCount)
        assertEquals("Storage count should be 1", 1, stats.storageCount)
        assertEquals("Out of service count should be 1", 1, stats.outOfServiceCount)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBatteryStatistics returns zero average cycle count for new batteries`() = runTest {
        // Given - add batteries (new batteries have cycleCount = 0)
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
        val stats = getBatteryStatisticsUseCase()

        // Then
        assertEquals("Average cycle count should be 0 for new batteries", 0f, stats.averageCycleCount)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBatteryStatistics handles multiple batteries with same status`() = runTest {
        // Given - add 3 charged batteries
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
        addBatteryUseCase(
            brand = "Parrot",
            model = "Anafi",
            serialNumber = "SN003",
            type = BatteryType.LIPO,
            cells = 3,
            capacity = 4000
        )

        // When - all batteries are CHARGED by default
        val stats = getBatteryStatisticsUseCase()

        // Then
        assertEquals("Total should be 3", 3, stats.totalCount)
        assertEquals("Charged count should be 3", 3, stats.chargedCount)
        assertEquals("Discharged count should be 0", 0, stats.dischargedCount)
        assertEquals("Storage count should be 0", 0, stats.storageCount)
        assertEquals("Out of service count should be 0", 0, stats.outOfServiceCount)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBatteryStatistics updates when battery status changes`() = runTest {
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

        // Initial stats
        val initialStats = getBatteryStatisticsUseCase()
        assertEquals("Initial charged count should be 1", 1, initialStats.chargedCount)

        // When - change status
        updateBatteryStatusUseCase(batteryId, BatteryStatus.STORAGE)
        val updatedStats = getBatteryStatisticsUseCase()

        // Then
        assertEquals("Updated charged count should be 0", 0, updatedStats.chargedCount)
        assertEquals("Updated storage count should be 1", 1, updatedStats.storageCount)
    }
}
