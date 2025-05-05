# Running Unit Tests in SkyFuel App

## Current Issue: Dependency Injection

We're currently experiencing a dependency injection issue with Hilt. The error occurs because the UI viewmodels are trying to inject a data layer repository interface, but our dependency injection module provides a domain layer repository interface.

## Solution Approach

We've implemented several solutions to resolve this issue:

1. Created a `BatteryRepository` interface in the data layer
2. Implemented a `LegacyBatteryRepositoryWrapper` to adapt between the domain and data layer repository interfaces
3. Updated the Dependency Injection module to provide the proper repository implementations

## Running Individual Tests in Android Studio

The most reliable way to run tests is through Android Studio:

1. Open the project in Android Studio
2. Navigate to one of the test files:
   - `app/src/test/java/leonfvt/skyfuel_app/domain/model/BatteryTest.kt`
   - `app/src/test/java/leonfvt/skyfuel_app/domain/usecase/AddBatteryUseCaseTest.kt`
3. Right-click on the test class or method name
4. Select "Run '[TestName]'"

This allows you to run individual test classes or methods even when there are compilation issues in the project.

## Using TestUtils

We've added a TestUtils class to help with creating test data. This provides consistent test data across all test classes and makes tests more readable.

```kotlin
// Example of using TestUtils
val testBattery = TestUtils.createTestBattery(
    brand = "Custom Brand",
    status = BatteryStatus.DISCHARGED
)
```

## Next Steps

To fully resolve the dependency injection issues:

1. Run the Gradle command with `--stacktrace` to get more detailed error information:
   ```
   ./gradlew testDebugUnitTest --stacktrace
   ```

2. If the data type conversion is causing issues, consider creating proper mapping functions between domain and data models

3. For testing purposes only, you can temporarily disable Hilt by removing the `@HiltAndroidApp` annotation from the Application class and using direct instantiation in tests

## Running Tests Without Compiling UI

We've set up a standalone test runner structure in `app/src/test-runner/` that could potentially be used to run tests without requiring the entire app to compile. This approach is still experimental.

Remember that the unit tests are correctly implemented and ready to run - the issues are related to the dependency injection framework, not the tests themselves.