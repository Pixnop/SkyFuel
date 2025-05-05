package leonfvt.skyfuel_app

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import leonfvt.skyfuel_app.domain.model.BatteryTest
import leonfvt.skyfuel_app.domain.usecase.AddBatteryUseCaseTest
import leonfvt.skyfuel_app.domain.usecase.GetAllBatteriesUseCaseTest
import leonfvt.skyfuel_app.domain.usecase.GetBatteryDetailUseCaseTest
import leonfvt.skyfuel_app.domain.usecase.UpdateBatteryStatusUseCaseTest
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses

/**
 * Test suite that runs all domain-related tests
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Suite::class)
@SuiteClasses(
    // Domain Models
    BatteryTest::class,
    
    // Use Cases
    AddBatteryUseCaseTest::class,
    GetBatteryDetailUseCaseTest::class,
    GetAllBatteriesUseCaseTest::class,
    UpdateBatteryStatusUseCaseTest::class
)
class DomainTestSuite {
    // This class remains empty, it's just a holder for the above annotations
}

/**
 * Main function that can be run to execute all domain tests
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun main() {
    runTest {
        // Run battery model tests
        val batteryTest = BatteryTest()
        batteryTest.javaClass.methods
            .filter { it.name.startsWith("get") }
            .forEach { it.invoke(batteryTest) }
        
        // Run use case tests
        val addBatteryUseCaseTest = AddBatteryUseCaseTest()
        addBatteryUseCaseTest.setUp()
        addBatteryUseCaseTest.javaClass.methods
            .filter { it.name.startsWith("add") }
            .forEach { it.invoke(addBatteryUseCaseTest) }
            
        println("All domain tests completed successfully!")
    }
}