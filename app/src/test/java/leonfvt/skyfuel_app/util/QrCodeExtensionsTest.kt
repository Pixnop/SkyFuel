package leonfvt.skyfuel_app.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.model.BatteryType
import leonfvt.skyfuel_app.domain.model.QrCodeData
import leonfvt.skyfuel_app.domain.model.QrCodeEntityType
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@ExperimentalCoroutinesApi
class QrCodeExtensionsTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

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

    @Test
    fun `QrCodeData encode and decode works correctly`() {
        // Arrange
        val qrData = QrCodeData.forBattery(
            batteryId = testBattery.id,
            serialNumber = testBattery.serialNumber,
            brand = testBattery.brand,
            model = testBattery.model
        )

        // Act
        val encoded = qrData.encode()
        val decoded = QrCodeData.decode(encoded)

        // Assert
        assertNotNull(decoded)
        assertEquals(testBattery.id.toString(), decoded?.entityId)
    }

    @Test
    fun `QrCodeData decodes valid format correctly`() {
        // Arrange
        val validQrString = "SKYFUEL::BATTERY::123::1617295200000::1"

        // Act
        val result = QrCodeData.decode(validQrString)

        // Assert
        assertNotNull(result)
        assertEquals("123", result?.entityId)
        assertEquals(QrCodeEntityType.BATTERY, result?.entityType)
    }

    @Test
    fun `QrCodeData decodes format with metadata correctly`() {
        // Arrange
        val qrStringWithMetadata = "SKYFUEL::BATTERY::123::1617295200000::1::brand=Test,model=Model"

        // Act
        val result = QrCodeData.decode(qrStringWithMetadata)

        // Assert
        assertNotNull(result)
        assertEquals("123", result?.entityId)
        assertEquals("Test", result?.metadata?.get("brand"))
        assertEquals("Model", result?.metadata?.get("model"))
    }

    @Test
    fun `QrCodeData returns null for invalid format`() {
        // Arrange
        val invalidQrString = "BATTERY_123_SERIALNUMBER"

        // Act
        val result = QrCodeData.decode(invalidQrString)

        // Assert
        assertNull(result)
    }

    @Test
    fun `QrCodeData forBattery creates correct structure`() {
        // Arrange & Act
        val qrData = QrCodeData.forBattery(
            batteryId = 123L,
            serialNumber = "SN123",
            brand = "TestBrand",
            model = "TestModel"
        )

        // Assert
        assertEquals(QrCodeEntityType.BATTERY, qrData.entityType)
        assertEquals("123", qrData.entityId)
        assertEquals("TestBrand", qrData.metadata["brand"])
        assertEquals("TestModel", qrData.metadata["model"])
        assertEquals("SN123", qrData.metadata["sn"])
    }
}