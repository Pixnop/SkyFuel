package leonfvt.skyfuel_app.domain.model

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class BatteryExtendedTest {

    @Test
    fun `getAgeInDays returns correct age`() {
        // Given
        val purchaseDate = LocalDate.now().minusDays(30)
        val battery = Battery(
            brand = "Test",
            model = "Model",
            serialNumber = "SN123",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000,
            purchaseDate = purchaseDate
        )

        // When
        val ageInDays = battery.getAgeInDays()

        // Then
        assertEquals(30L, ageInDays)
    }

    @Test
    fun `getHealthPercentage for LIPO battery is calculated correctly`() {
        // Given
        val battery = Battery(
            brand = "Test",
            model = "Model",
            serialNumber = "SN123",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000,
            purchaseDate = LocalDate.now().minusYears(1),
            cycleCount = 100
        )

        // When
        val healthPercentage = battery.getHealthPercentage()

        // Then
        // Pour une batterie LIPO avec 100 cycles et 1 an d'âge, la santé devrait être:
        // 100 - (100 * 0.25 + 1 * 10) = 100 - (25 + 10) = 65%
        assertEquals(65, healthPercentage)
    }

    @Test
    fun `getHealthPercentage for LI_ION battery is calculated correctly`() {
        // Given
        val battery = Battery(
            brand = "Test",
            model = "Model",
            serialNumber = "SN123",
            type = BatteryType.LI_ION,
            cells = 4,
            capacity = 5000,
            purchaseDate = LocalDate.now().minusYears(1),
            cycleCount = 100
        )

        // When
        val healthPercentage = battery.getHealthPercentage()

        // Then
        // Pour une batterie LI_ION avec 100 cycles et 1 an d'âge, la santé devrait être:
        // 100 - (100 * 0.15 + 1 * 7) = 100 - (15 + 7) = 78%
        assertEquals(78, healthPercentage)
    }

    @Test
    fun `getHealthPercentage is capped at 100`() {
        // Given
        val battery = Battery(
            brand = "Test",
            model = "Model",
            serialNumber = "SN123",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000,
            purchaseDate = LocalDate.now(), // Date d'aujourd'hui
            cycleCount = 0 // Aucun cycle
        )

        // When
        val healthPercentage = battery.getHealthPercentage()

        // Then
        // Batterie neuve, la santé devrait être 100%
        assertEquals(100, healthPercentage)
    }

    @Test
    fun `getHealthPercentage is floored at 0`() {
        // Given
        val battery = Battery(
            brand = "Test",
            model = "Model",
            serialNumber = "SN123",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000,
            purchaseDate = LocalDate.now().minusYears(5), // 5 ans d'âge
            cycleCount = 400 // Nombre maximal de cycles pour une LIPO
        )

        // When
        val healthPercentage = battery.getHealthPercentage()

        // Then
        // Le résultat brut serait: 100 - (400 * 0.25 + 5 * 10) = 100 - (100 + 50) = -50%
        // Mais la méthode doit arrondir à 0%
        assertEquals(0, healthPercentage)
    }

    @Test
    fun `shouldBeCharged returns false for charged batteries`() {
        // Given
        val battery = Battery(
            brand = "Test",
            model = "Model",
            serialNumber = "SN123",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000,
            purchaseDate = LocalDate.now(),
            status = BatteryStatus.CHARGED
        )

        // When & Then
        assertFalse(battery.shouldBeCharged())
    }

    @Test
    fun `shouldBeCharged returns true for batteries discharged for more than 7 days`() {
        // Given
        val battery = Battery(
            brand = "Test",
            model = "Model",
            serialNumber = "SN123",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000,
            purchaseDate = LocalDate.now(),
            status = BatteryStatus.DISCHARGED,
            lastUseDate = LocalDate.now().minusDays(8) // 8 jours depuis la dernière utilisation
        )

        // When & Then
        assertTrue(battery.shouldBeCharged())
    }

    @Test
    fun `shouldBeCharged returns false for batteries discharged for less than 7 days`() {
        // Given
        val battery = Battery(
            brand = "Test",
            model = "Model",
            serialNumber = "SN123",
            type = BatteryType.LIPO,
            cells = 4,
            capacity = 5000,
            purchaseDate = LocalDate.now(),
            status = BatteryStatus.DISCHARGED,
            lastUseDate = LocalDate.now().minusDays(3) // 3 jours depuis la dernière utilisation
        )

        // When & Then
        assertFalse(battery.shouldBeCharged())
    }
}