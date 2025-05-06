package leonfvt.skyfuel_app.domain.service

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.model.BatteryType
import leonfvt.skyfuel_app.domain.model.QrCodeData
import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.time.LocalDate

@ExperimentalCoroutinesApi
class QrCodeServiceTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var batteryRepository: BatteryRepository

    private lateinit var qrCodeService: QrCodeService

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
        qrCodeService = QrCodeService(context, batteryRepository)
    }

    @Test
    fun `decodeQrCode correctly decodes valid QR string`() {
        // Arrange
        val validQrString = "SKYFUEL::BATTERY::123::1617295200000::1::brand=TestBrand,model=TestModel"

        // Act
        val result = qrCodeService.decodeQrCode(validQrString)

        // Assert
        assertNotNull(result)
        assertEquals("123", result?.entityId)
    }

    @Test
    fun `decodeQrCode returns null for invalid QR string`() {
        // Arrange
        val invalidQrString = "BATTERY_123_SERIALNUMBER"

        // Act
        val result = qrCodeService.decodeQrCode(invalidQrString)

        // Assert
        assertNull(result)
    }

    @Test
    fun `generateNewQrCodeId creates string with expected format`() {
        // Act
        val qrCodeId = qrCodeService.generateNewQrCodeId(testBattery)

        // Assert
        assertTrue(qrCodeId.startsWith("BATTERY_${testBattery.id}_${testBattery.serialNumber}"))
    }

    @Test
    fun `processScannedQrCode returns success for valid battery QR code`() = runTest {
        // Arrange
        val qrData = QrCodeData.forBattery(
            batteryId = testBattery.id,
            serialNumber = testBattery.serialNumber,
            brand = testBattery.brand,
            model = testBattery.model
        )
        val qrContent = qrData.encode()
        
        `when`(batteryRepository.getBatteryById(testBattery.id)).thenReturn(testBattery)

        // Act
        val result = qrCodeService.processScannedQrCode(qrContent)

        // Assert
        assertTrue(result.first) // Success should be true
        assertTrue(result.second.contains(testBattery.brand)) // Message should contain battery info
    }

    @Test
    fun `processScannedQrCode returns failure for non-existent battery`() = runTest {
        // Arrange
        val qrData = QrCodeData.forBattery(
            batteryId = 999L, // Non-existent ID
            serialNumber = "NONEXISTENT",
            brand = "Unknown",
            model = "Unknown"
        )
        val qrContent = qrData.encode()
        
        `when`(batteryRepository.getBatteryById(999L)).thenReturn(null)

        // Act
        val result = qrCodeService.processScannedQrCode(qrContent)

        // Assert
        assertFalse(result.first) // Success should be false
        assertTrue(result.second.contains("non trouvée")) // Message should indicate battery not found
    }

    @Test
    fun `processScannedQrCode returns failure for invalid QR code`() = runTest {
        // Arrange
        val invalidQrContent = "BATTERY_123_SERIALNUMBER" // Old format

        // Act
        val result = qrCodeService.processScannedQrCode(invalidQrContent)

        // Assert
        assertFalse(result.first) // Success should be false
        assertTrue(result.second.contains("invalide")) // Message should indicate invalid QR code
    }

    @Test
    fun `getBatteryFromQrCode returns battery for valid QR code`() = runTest {
        // Arrange
        val qrData = QrCodeData.forBattery(
            batteryId = testBattery.id,
            serialNumber = testBattery.serialNumber
        )
        val qrContent = qrData.encode()
        
        `when`(batteryRepository.getBatteryById(testBattery.id)).thenReturn(testBattery)

        // Act
        val result = qrCodeService.getBatteryFromQrCode(qrContent)

        // Assert
        assertNotNull(result)
        assertEquals(testBattery.id, result?.id)
    }

    @Test
    fun `getBatteryFromQrCode returns null for invalid QR code`() = runTest {
        // Arrange
        val invalidQrContent = "BATTERY_123_SERIALNUMBER" // Old format

        // Act
        val result = qrCodeService.getBatteryFromQrCode(invalidQrContent)

        // Assert
        assertNull(result)
    }
}