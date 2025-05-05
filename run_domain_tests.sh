#!/bin/bash

echo "SkyFuel App - Domain Tests Runner"
echo "=================================="

# Set working directory to project root
cd "$(dirname "$0")"

# Create a separate test module to avoid UI compilation issues
echo "Creating standalone test module..."

# Create a temporary test directory
TEST_DIR="./domain-tests"
mkdir -p "$TEST_DIR"/src

# Copy domain model and tests files
echo "Copying test files..."

# Copy domain models
mkdir -p "$TEST_DIR"/src/domain/model
mkdir -p "$TEST_DIR"/src/domain/usecase
mkdir -p "$TEST_DIR"/src/domain/repository
mkdir -p "$TEST_DIR"/src/test/model
mkdir -p "$TEST_DIR"/src/test/usecase
mkdir -p "$TEST_DIR"/src/test/repository

# Copy our domain models
cp -r app/src/main/java/leonfvt/skyfuel_app/domain/model "$TEST_DIR"/src/domain/
cp -r app/src/main/java/leonfvt/skyfuel_app/domain/repository "$TEST_DIR"/src/domain/
cp -r app/src/main/java/leonfvt/skyfuel_app/domain/usecase "$TEST_DIR"/src/domain/

# Copy our test files
cp -r app/src/test/java/leonfvt/skyfuel_app/domain/model "$TEST_DIR"/src/test/
cp -r app/src/test/java/leonfvt/skyfuel_app/domain/usecase "$TEST_DIR"/src/test/
cp -r app/src/test/java/leonfvt/skyfuel_app/repository "$TEST_DIR"/src/test/

# Create a temporary pom.xml for dependencies
cat > "$TEST_DIR"/pom.xml << EOF
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>leonfvt.skyfuel_app</groupId>
    <artifactId>domain-tests</artifactId>
    <version>1.0-SNAPSHOT</version>
    
    <dependencies>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.13.2</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-stdlib</artifactId>
            <version>1.9.0</version>
        </dependency>
        <dependency>
            <groupId>org.jetbrains.kotlinx</groupId>
            <artifactId>kotlinx-coroutines-core</artifactId>
            <version>1.7.3</version>
        </dependency>
        <dependency>
            <groupId>org.jetbrains.kotlinx</groupId>
            <artifactId>kotlinx-coroutines-test</artifactId>
            <version>1.7.3</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
EOF

echo "Domain test files are ready for execution"
echo ""
echo "To run the tests, open the app in Android Studio and run individual test classes by:"
echo "1. Opening one of the test files in app/src/test/java/leonfvt/skyfuel_app/domain/"
echo "2. Right-clicking on the class name or test method"
echo "3. Selecting 'Run [TestName]'"
echo ""
echo "Specifically, try running:"
echo "- BatteryTest"
echo "- AddBatteryUseCaseTest"
echo "- GetBatteryDetailUseCaseTest"
echo ""
echo "For now, you can still use './gradlew test --tests \"leonfvt.skyfuel_app.domain.model.BatteryTest\"'"
echo "to run specific test classes, but this will require fixing all UI compilation issues first."