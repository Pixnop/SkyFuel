package leonfvt.skyfuel_app.domain.model

import org.junit.Assert.*
import org.junit.Test

class QrCodeDataTest {

    @Test
    fun `encode returns correctly formatted string`() {
        // Arrange
        val qrData = QrCodeData(
            entityType = QrCodeEntityType.BATTERY,
            entityId = "123",
            timestamp = 1617295200000, // 2021-04-01 12:00:00
            version = 1,
            metadata = mapOf("brand" to "TestBrand", "model" to "TestModel")
        )

        // Act
        val encoded = qrData.encode()

        // Assert
        assertTrue(encoded.startsWith("SKYFUEL::BATTERY::123::1617295200000::1"))
        assertTrue(encoded.contains("brand=TestBrand"))
        assertTrue(encoded.contains("model=TestModel"))
    }

    @Test
    fun `decode correctly parses valid QR code string`() {
        // Arrange
        val qrString = "SKYFUEL::BATTERY::123::1617295200000::1::brand=TestBrand,model=TestModel"

        // Act
        val decoded = QrCodeData.decode(qrString)

        // Assert
        assertNotNull(decoded)
        assertEquals(QrCodeEntityType.BATTERY, decoded?.entityType)
        assertEquals("123", decoded?.entityId)
        assertEquals(1617295200000, decoded?.timestamp)
        assertEquals(1, decoded?.version)
        assertEquals("TestBrand", decoded?.metadata?.get("brand"))
        assertEquals("TestModel", decoded?.metadata?.get("model"))
    }

    @Test
    fun `decode returns null for invalid format`() {
        // Arrange
        val invalidQrString = "BATTERY_123_SERIALNUMBER"

        // Act
        val decoded = QrCodeData.decode(invalidQrString)

        // Assert
        assertNull(decoded)
    }

    @Test
    fun `decode returns null when prefix is wrong`() {
        // Arrange
        val invalidQrString = "WRONG::BATTERY::123::1617295200000::1"

        // Act
        val decoded = QrCodeData.decode(invalidQrString)

        // Assert
        assertNull(decoded)
    }

    @Test
    fun `forBattery creates correct QrCodeData`() {
        // Arrange
        val batteryId = 123L
        val serialNumber = "TEST123"
        val brand = "TestBrand"
        val model = "TestModel"

        // Act
        val qrData = QrCodeData.forBattery(batteryId, serialNumber, brand, model)

        // Assert
        assertEquals(QrCodeEntityType.BATTERY, qrData.entityType)
        assertEquals("123", qrData.entityId)
        assertEquals(serialNumber, qrData.metadata["sn"])
        assertEquals(brand, qrData.metadata["brand"])
        assertEquals(model, qrData.metadata["model"])
    }
}