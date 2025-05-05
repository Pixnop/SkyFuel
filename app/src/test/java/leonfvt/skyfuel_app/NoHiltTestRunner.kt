package leonfvt.skyfuel_app

import org.junit.runner.RunWith
import org.junit.runners.Suite
import leonfvt.skyfuel_app.domain.model.BatteryTest
import leonfvt.skyfuel_app.domain.usecase.AddBatteryUseCaseTest
import leonfvt.skyfuel_app.domain.usecase.GetAllBatteriesUseCaseTest
import leonfvt.skyfuel_app.domain.usecase.GetBatteryDetailUseCaseTest
import leonfvt.skyfuel_app.domain.usecase.UpdateBatteryStatusUseCaseTest

/**
 * Test runner that runs all domain layer tests without relying on Hilt
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    BatteryTest::class,
    AddBatteryUseCaseTest::class,
    GetBatteryDetailUseCaseTest::class,
    GetAllBatteriesUseCaseTest::class,
    UpdateBatteryStatusUseCaseTest::class
)
class NoHiltDomainTestSuite