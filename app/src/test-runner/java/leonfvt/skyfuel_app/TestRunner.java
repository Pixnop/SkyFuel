package leonfvt.skyfuel_app;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/**
 * Simple test runner to execute unit tests directly
 * without requiring the complete Android app to compile.
 */
public class TestRunner {
    public static void main(String[] args) {
        Result result = JUnitCore.runClasses(
            leonfvt.skyfuel_app.domain.model.BatteryTest.class
        );
        
        for (Failure failure : result.getFailures()) {
            System.out.println(failure.toString());
        }
        
        if (result.wasSuccessful()) {
            System.out.println("All tests passed successfully!");
        } else {
            System.out.println("Tests failed!");
        }
    }
}