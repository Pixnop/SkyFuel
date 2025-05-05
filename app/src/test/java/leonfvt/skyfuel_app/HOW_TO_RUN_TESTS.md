# How to Run the Domain Tests for SkyFuel App

While there are some compilation issues in the UI layer, we've implemented several approaches to run the domain layer tests.

## Option 1: Using Android Studio / IntelliJ IDEA

This is the easiest method:

1. Open the project in Android Studio
2. Navigate to one of the test classes:
   - `app/src/test/java/leonfvt/skyfuel_app/domain/model/BatteryTest.kt`
   - `app/src/test/java/leonfvt/skyfuel_app/domain/usecase/AddBatteryUseCaseTest.kt`
   - etc.
3. Right-click on the class name or a specific test method 
4. Select "Run 'TestClassName'" or "Run 'testMethodName'"

This approach works because Android Studio can run individual test classes without requiring the entire project to compile.

## Option 2: Using Test Suite

We've prepared a test suite to run all domain tests:

1. Open `app/src/test/java/leonfvt/skyfuel_app/RunDomainTests.kt`
2. Right-click and select "Run 'RunDomainTests'"

## Option 3: Command Line with Gradle

If you want to run the tests from the command line, you can try to run specific test classes:

```bash
./gradlew testDebugUnitTest --tests "leonfvt.skyfuel_app.domain.model.BatteryTest"
```

Note: This may still require the entire project to compile.

## Option 4: Using Custom Test Runner Script

We've provided a shell script that attempts to run the domain tests directly using JUnit:

```bash
# Make it executable if needed
chmod +x run_domain_tests.sh

# Run the script
./run_domain_tests.sh
```

## What's Been Tested

The implemented tests cover:

1. **Domain Models**:
   - `BatteryTest` - Tests age calculation, health percentage, charging reminders

2. **Use Cases**:
   - `AddBatteryUseCaseTest` - Input validation and battery creation
   - `GetBatteryDetailUseCaseTest` - Retrieving battery details
   - `GetAllBatteriesUseCaseTest` - List operations and filtering
   - `UpdateBatteryStatusUseCaseTest` - Status updates and history tracking

## Resolving UI Compilation Issues

The main compilation issues are related to the UI layer:

1. Missing imports in various UI files
2. Pull-to-refresh implementation issues
3. Material icon references

These issues are documented in:
- `app/src/test/java/leonfvt/skyfuel_app/FINAL_FIXES.md`
- `app/src/test/java/leonfvt/skyfuel_app/TestPlanExecution.md`

Once the UI compilation issues are resolved, all tests can be run with a simple:

```bash
./gradlew test
```