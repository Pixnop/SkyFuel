package leonfvt.skyfuel_app.domain.model

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class BatteryTest {

    @Test
    fun `getAgeInDays returns correct age for new battery`() {
        // Given - a battery purchased today
        val battery = createTestBattery(purchaseDate = LocalDate.now())
        
        // When - get age in days
        val ageInDays = battery.getAgeInDays()
        
        // Then - age should be 0
        assertEquals("Age should be 0 days for battery purchased today", 0, ageInDays)
    }
    
    @Test
    fun `getAgeInDays returns correct age for older battery`() {
        // Given - a battery purchased 30 days ago
        val purchaseDate = LocalDate.now().minusDays(30)
        val battery = createTestBattery(purchaseDate = purchaseDate)
        
        // When - get age in days
        val ageInDays = battery.getAgeInDays()
        
        // Then - age should be 30 days
        assertEquals("Age should be 30 days", 30, ageInDays)
    }
    
    @Test
    fun `shouldBeCharged returns false for charged battery`() {
        // Given - a charged battery
        val battery = createTestBattery(status = BatteryStatus.CHARGED)
        
        // When/Then - should not need charging
        assertFalse("Charged battery should not need charging", battery.shouldBeCharged())
    }
    
    @Test
    fun `shouldBeCharged returns false for recently discharged battery`() {
        // Given - a battery discharged 3 days ago
        val battery = createTestBattery(
            status = BatteryStatus.DISCHARGED,
            lastUseDate = LocalDate.now().minusDays(3)
        )
        
        // When/Then - should not need charging yet
        assertFalse("Recently discharged battery should not need charging yet", battery.shouldBeCharged())
    }
    
    @Test
    fun `shouldBeCharged returns true for battery discharged more than 7 days ago`() {
        // Given - a battery discharged 10 days ago
        val battery = createTestBattery(
            status = BatteryStatus.DISCHARGED,
            lastUseDate = LocalDate.now().minusDays(10)
        )
        
        // When/Then - should need charging
        assertTrue("Battery discharged 10 days ago should need charging", battery.shouldBeCharged())
    }
    
    @Test
    fun `shouldBeCharged returns false for discharged battery with no lastUseDate`() {
        // Given - a discharged battery with no last use date
        val battery = createTestBattery(
            status = BatteryStatus.DISCHARGED,
            lastUseDate = null
        )
        
        // When/Then - should not need charging (since we can't determine when it was discharged)
        assertFalse("Discharged battery without lastUseDate should not trigger charging", battery.shouldBeCharged())
    }
    
    @Test
    fun `getHealthPercentage returns 100 for new battery with no cycles`() {
        // Given - a brand new battery
        val battery = createTestBattery(
            purchaseDate = LocalDate.now(),
            cycleCount = 0
        )
        
        // When - get health percentage
        val health = battery.getHealthPercentage()
        
        // Then - health should be 100%
        assertEquals("New battery should have 100% health", 100, health)
    }
    
    @Test
    fun `getHealthPercentage decreases with cycle count`() {
        // Given - two batteries with different cycle counts
        val newBattery = createTestBattery(cycleCount = 0)
        val usedBattery = createTestBattery(cycleCount = 100)
        
        // When - get health percentages
        val newHealth = newBattery.getHealthPercentage()
        val usedHealth = usedBattery.getHealthPercentage()
        
        // Then - used battery should have lower health
        assertTrue("Used battery should have lower health", usedHealth < newHealth)
    }
    
    @Test
    fun `getHealthPercentage decreases with age`() {
        // Given - two batteries with same cycles but different ages
        val newBattery = createTestBattery(
            purchaseDate = LocalDate.now(),
            cycleCount = 10
        )
        val oldBattery = createTestBattery(
            purchaseDate = LocalDate.now().minus(2, ChronoUnit.YEARS),
            cycleCount = 10
        )
        
        // When - get health percentages
        val newHealth = newBattery.getHealthPercentage()
        val oldHealth = oldBattery.getHealthPercentage()
        
        // Then - older battery should have lower health
        assertTrue("Older battery should have lower health", oldHealth < newHealth)
    }
    
    @Test
    fun `getHealthPercentage differs by battery type`() {
        // Given - batteries of different types with same age and cycles
        val lipoBattery = createTestBattery(
            type = BatteryType.LIPO,
            cycleCount = 100
        )
        val lifeiBattery = createTestBattery(
            type = BatteryType.LIFE,
            cycleCount = 100
        )
        
        // When - get health percentages
        val lipoHealth = lipoBattery.getHealthPercentage()
        val lifeiHealth = lifeiBattery.getHealthPercentage()
        
        // Then - LiPo battery should degrade faster than LiFe battery
        assertTrue("LiPo battery should have lower health than LiFe battery", lipoHealth < lifeiHealth)
    }
    
    @Test
    fun `getHealthPercentage never returns negative value`() {
        // Given - a very old battery with many cycles
        val veryUsedBattery = createTestBattery(
            purchaseDate = LocalDate.now().minus(10, ChronoUnit.YEARS),
            cycleCount = 1000
        )
        
        // When - get health percentage
        val health = veryUsedBattery.getHealthPercentage()
        
        // Then - health should be at least 0%
        assertTrue("Health should never be negative", health >= 0)
    }
    
    @Test
    fun `getHealthPercentage never exceeds 100`() {
        // Given - a brand new battery of a durable type
        val durableBattery = createTestBattery(
            type = BatteryType.LIFE,
            purchaseDate = LocalDate.now(),
            cycleCount = 0
        )
        
        // When - get health percentage
        val health = durableBattery.getHealthPercentage()
        
        // Then - health should not exceed 100%
        assertTrue("Health should never exceed 100%", health <= 100)
    }
    
    /**
     * Helper method to create a test battery with default values
     */
    private fun createTestBattery(
        id: Long = 1L,
        brand: String = "Test Brand",
        model: String = "Test Model",
        serialNumber: String = "SN123456789",
        type: BatteryType = BatteryType.LIPO,
        cells: Int = 4,
        capacity: Int = 5000,
        purchaseDate: LocalDate = LocalDate.now().minusDays(60),
        status: BatteryStatus = BatteryStatus.CHARGED,
        cycleCount: Int = 0,
        notes: String = "",
        lastUseDate: LocalDate? = null,
        lastChargeDate: LocalDate? = null
    ): Battery {
        return Battery(
            id = id,
            brand = brand,
            model = model,
            serialNumber = serialNumber,
            type = type,
            cells = cells,
            capacity = capacity,
            purchaseDate = purchaseDate,
            status = status,
            cycleCount = cycleCount,
            notes = notes,
            lastUseDate = lastUseDate,
            lastChargeDate = lastChargeDate
        )
    }
}