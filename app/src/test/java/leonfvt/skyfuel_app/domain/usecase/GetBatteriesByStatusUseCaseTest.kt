package leonfvt.skyfuel_app.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.model.BatteryType
import leonfvt.skyfuel_app.domain.model.Result
import leonfvt.skyfuel_app.repository.FakeBatteryRepository
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class GetBatteriesByStatusUseCaseTest {

    private lateinit var getBatteriesByStatusUseCase: GetBatteriesByStatusUseCase
    private lateinit var addBatteryUseCase: AddBatteryUseCase
    private lateinit var updateBatteryStatusUseCase: UpdateBatteryStatusUseCase
    private lateinit var fakeBatteryRepository: FakeBatteryRepository

    @Before
    fun setUp() {
        fakeBatteryRepository = FakeBatteryRepository()
        getBatteriesByStatusUseCase = GetBatteriesByStatusUseCase(fakeBatteryRepository)
        addBatteryUseCase = AddBatteryUseCase(fakeBatteryRepository)
        updateBatteryStatusUseCase = UpdateBatteryStatusUseCase(fakeBatteryRepository)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBatteriesByStatus returns empty list when no batteries exist`() = runTest {
        // When
        val batteries = getBatteriesByStatusUseCase(BatteryStatus.CHARGED).first()

        // Then
        assertTrue("Should return empty list", batteries.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBatteriesByStatus returns only batteries with specified status`() = runTest {
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

        // Update statuses: battery1 = CHARGED (default), battery2 = DISCHARGED, battery3 = STORAGE
        updateBatteryStatusUseCase(battery2Id, BatteryStatus.DISCHARGED)
        updateBatteryStatusUseCase(battery3Id, BatteryStatus.STORAGE)

        // When - get only CHARGED batteries
        val chargedBatteries = getBatteriesByStatusUseCase(BatteryStatus.CHARGED).first()

        // Then
        assertEquals("Should have 1 charged battery", 1, chargedBatteries.size)
        assertEquals("Should be the DJI battery", "DJI", chargedBatteries[0].brand)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBatteriesByStatus with null status returns all batteries`() = runTest {
        // Given - add multiple batteries
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

        // When - get all batteries (null status)
        val allBatteries = getBatteriesByStatusUseCase(null).first()

        // Then
        assertEquals("Should return all 2 batteries", 2, allBatteries.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBatteriesByStatus returns multiple batteries with same status`() = runTest {
        // Given - add batteries all with DISCHARGED status
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

        // Update both to DISCHARGED
        updateBatteryStatusUseCase(battery1Id, BatteryStatus.DISCHARGED)
        updateBatteryStatusUseCase(battery2Id, BatteryStatus.DISCHARGED)

        // When
        val dischargedBatteries = getBatteriesByStatusUseCase(BatteryStatus.DISCHARGED).first()

        // Then
        assertEquals("Should have 2 discharged batteries", 2, dischargedBatteries.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getBatteriesByStatus with OUT_OF_SERVICE status works correctly`() = runTest {
        // Given - add a battery and mark it out of service
        val result = addBatteryUseCase(
            brand = "Old",
            model = "Battery",
            serialNumber = "OLD001",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 3000
        )
        val batteryId = (result as Result.Success).data
        updateBatteryStatusUseCase(batteryId, BatteryStatus.OUT_OF_SERVICE, "End of life")

        // When
        val outOfServiceBatteries = getBatteriesByStatusUseCase(BatteryStatus.OUT_OF_SERVICE).first()

        // Then
        assertEquals("Should have 1 out of service battery", 1, outOfServiceBatteries.size)
        assertEquals("Should be the old battery", "Old", outOfServiceBatteries[0].brand)
    }
}
