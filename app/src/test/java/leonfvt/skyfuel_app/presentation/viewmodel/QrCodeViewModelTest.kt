package leonfvt.skyfuel_app.presentation.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.model.BatteryType
import leonfvt.skyfuel_app.domain.model.QrCodeData
import leonfvt.skyfuel_app.domain.service.QrCodeService
import leonfvt.skyfuel_app.util.CoroutineTestRule
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.time.LocalDate

@ExperimentalCoroutinesApi
class QrCodeViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Mock
    private lateinit var qrCodeService: QrCodeService

    private lateinit var viewModel: QrCodeViewModel

    private val testBattery = Battery(
        id = 123,
        brand = "TestBrand",
        model = "TestModel",
        serialNumber = "SN123456",
        type = BatteryType.LIPO,
        cells = 4,
        capacity = 5000,
        purchaseDate = LocalDate.now().minusDays(30),
        status = BatteryStatus.CHARGED,
        cycleCount = 10,
        notes = "Test battery"
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        viewModel = QrCodeViewModel(qrCodeService)
    }

    @Test
    fun `processScannedQrCode updates state correctly on success`() = runTest {
        // Arrange
        val testQrContent = "test_qr_content"
        val testDispatcher = StandardTestDispatcher(testScheduler)
        
        `when`(qrCodeService.processScannedQrCode(testQrContent)).thenReturn(
            Pair(true, "Batterie trouvée: TestBrand TestModel")
        )

        // Act
        viewModel.processScannedQrCode(testQrContent)
        advanceUntilIdle() // Wait for all coroutines to complete

        // Assert
        val state = viewModel.state.first()
        assertTrue(state.successMessage != null)
        assertEquals(testQrContent, state.scanResult)
        assertFalse(state.isProcessingQrCode)
        assertNull(state.errorMessage)
    }

    @Test
    fun `processScannedQrCode updates state correctly on failure`() = runTest {
        // Arrange
        val testQrContent = "invalid_qr_content"
        val errorMessage = "QR code invalide ou non reconnu"
        
        `when`(qrCodeService.processScannedQrCode(testQrContent)).thenReturn(
            Pair(false, errorMessage)
        )

        // Act
        viewModel.processScannedQrCode(testQrContent)
        advanceUntilIdle() // Wait for all coroutines to complete

        // Assert
        val state = viewModel.state.first()
        assertEquals(errorMessage, state.errorMessage)
        assertNull(state.successMessage)
        assertNull(state.scanResult)
        assertFalse(state.isProcessingQrCode)
    }

    @Test
    fun `resetScanState clears all state values`() = runTest {
        // Arrange - Set up state with values
        viewModel.processScannedQrCode("test_qr_content")
        advanceUntilIdle() // Ensure state is updated

        // Act
        viewModel.resetScanState()

        // Assert
        val state = viewModel.state.first()
        assertFalse(state.isProcessingQrCode)
        assertNull(state.scanResult)
        assertNull(state.errorMessage)
        assertNull(state.successMessage)
    }

    @Test
    fun `getBatteryFromQrCode calls callback with battery when found`() = runTest {
        // Arrange
        val testQrContent = "test_qr_content"
        var foundBattery: Battery? = null
        var notFoundCalled = false
        
        `when`(qrCodeService.getBatteryFromQrCode(testQrContent)).thenReturn(testBattery)

        // Act
        viewModel.getBatteryFromQrCode(
            testQrContent,
            onBatteryFound = { foundBattery = it },
            onBatteryNotFound = { notFoundCalled = true }
        )
        advanceUntilIdle() // Wait for all coroutines to complete

        // Assert
        assertNotNull(foundBattery)
        assertEquals(testBattery.id, foundBattery?.id)
        assertFalse(notFoundCalled)
    }

    @Test
    fun `getBatteryFromQrCode calls notFound callback when battery not found`() = runTest {
        // Arrange
        val testQrContent = "test_qr_content"
        var foundBattery: Battery? = null
        var notFoundMessage: String? = null
        
        `when`(qrCodeService.getBatteryFromQrCode(testQrContent)).thenReturn(null)

        // Act
        viewModel.getBatteryFromQrCode(
            testQrContent,
            onBatteryFound = { foundBattery = it },
            onBatteryNotFound = { notFoundMessage = it }
        )
        advanceUntilIdle() // Wait for all coroutines to complete

        // Assert
        assertNull(foundBattery)
        assertNotNull(notFoundMessage)
        assertTrue(notFoundMessage?.contains("non trouvée") == true)
    }
}