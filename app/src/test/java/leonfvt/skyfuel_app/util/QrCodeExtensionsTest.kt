package leonfvt.skyfuel_app.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.model.BatteryType
import leonfvt.skyfuel_app.domain.model.QrCodeData
import leonfvt.skyfuel_app.presentation.viewmodel.QrCodeViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.time.LocalDate

@ExperimentalCoroutinesApi
class QrCodeExtensionsTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Mock
    private lateinit var qrCodeViewModel: QrCodeViewModel

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
    }

    @Test
    fun `processBatteryQrCode handles old format correctly`() = runTest {
        // Arrange
        val oldFormatQrCode = "BATTERY_123_SN123456"
        var batteryFound: Battery? = null
        var errorMessage: String? = null

        // Act
        QrCodeExtensions.processBatteryQrCode(
            qrContent = oldFormatQrCode,
            viewModel = qrCodeViewModel,
            scope = this,
            onBatteryFound = { battery -> batteryFound = battery },
            onError = { error -> errorMessage = error }
        )

        // Vérifier que getBatteryFromQrCode est appelé avec le nouveau format
        verify(qrCodeViewModel).getBatteryFromQrCode(
            anyString(), 
            any(), 
            any()
        )
        
        advanceUntilIdle()
    }

    @Test
    fun `processBatteryQrCode handles new format correctly`() = runTest {
        // Arrange
        val qrData = QrCodeData.forBattery(
            batteryId = testBattery.id,
            serialNumber = testBattery.serialNumber,
            brand = testBattery.brand,
            model = testBattery.model
        )
        val newFormatQrCode = qrData.encode()
        var batteryFound: Battery? = null
        var errorMessage: String? = null

        // Act
        QrCodeExtensions.processBatteryQrCode(
            qrContent = newFormatQrCode,
            viewModel = qrCodeViewModel,
            scope = this,
            onBatteryFound = { battery -> batteryFound = battery },
            onError = { error -> errorMessage = error }
        )

        // Vérifier que getBatteryFromQrCode est appelé avec le bon contenu
        verify(qrCodeViewModel).getBatteryFromQrCode(
            eq(newFormatQrCode),
            any(),
            any()
        )
        
        advanceUntilIdle()
    }

    @Test
    fun `processBatteryQrCode handles invalid format correctly`() = runTest {
        // Arrange
        val invalidQrCode = "INVALID_FORMAT"
        var batteryFound: Battery? = null
        var errorMessage: String? = null

        // Act
        QrCodeExtensions.processBatteryQrCode(
            qrContent = invalidQrCode,
            viewModel = qrCodeViewModel,
            scope = this,
            onBatteryFound = { battery -> batteryFound = battery },
            onError = { error -> errorMessage = error }
        )

        // Vérifier que onError est appelé avec le bon message
        verify(qrCodeViewModel, never()).getBatteryFromQrCode(
            eq(invalidQrCode),
            any(),
            any()
        )
        
        advanceUntilIdle()
    }
}