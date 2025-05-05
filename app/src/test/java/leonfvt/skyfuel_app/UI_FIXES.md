# UI Compilation Issues Fix Guide

This document provides step-by-step instructions to fix the current UI compilation issues that are preventing the tests from running.

## Material Icons Issues

### 1. Add Material Icons Extended Dependency

We've already added this dependency to the `build.gradle.kts` file:

```kotlin
implementation(libs.androidx.material.icons.extended)
```

### 2. Fix Missing Icons

For each file using Material icons, make sure to import the correct icon sets:

#### BatteryActions.kt

Replace the current imports:

```kotlin
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.BatteryUnknown
import androidx.compose.material.icons.filled.ExpandMore
```

With:

```kotlin
// Add these imports at the top with other imports
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Battery1Bar
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Speed
```

Then update references in the code:

```kotlin
// Replace:
Icons.Default.BatteryChargingFull -> Icons.Default.Battery5Bar
Icons.Default.BatteryStd -> Icons.Default.Battery1Bar
Icons.Default.BatteryUnknown -> Icons.Default.BatteryAlert
```

#### BatteryHistoryList.kt

Add:

```kotlin
import androidx.compose.material.icons.filled.BoltOn
```

If `BoltOn` isn't available, replace with a similar icon like:

```kotlin
import androidx.compose.material.icons.filled.ElectricBolt
```

#### HomeScreen.kt and BatteryDetailScreen.kt

Replace:

```kotlin
import androidx.compose.material.icons.filled.Battery0Bar
```

With:

```kotlin
import androidx.compose.material.icons.filled.BatteryAlert
// Or any other suitable battery icon:
import androidx.compose.material.icons.filled.Battery1Bar
```

## PullRefresh Issues

### Fix HomeScreen.kt

1. First, make sure you have the correct dependency:

```kotlin
implementation(libs.androidx.compose.material.pullrefresh)
```

2. Add to `libs.versions.toml`:

```
pullrefresh = "1.0.0"
androidx-compose-material-pullrefresh = { module = "androidx.compose.material:material-pullrefresh", version.ref = "pullrefresh" }
```

3. Fix the PullRefreshIndicator implementation:

Replace:

```kotlin
PullRefreshIndicator(
    // Current implementation with missing parameters
)
```

With a proper implementation:

```kotlin
var refreshing by remember { mutableStateOf(false) }

PullRefreshBox(
    onRefresh = {
        refreshing = true
        onRefreshBatteries()
        refreshing = false
    },
    refreshing = refreshing
) {
    // Content
    LazyColumn {
        // ...
    }
}
```

## Other UI Issues

### Fix Missing Modifiers

1. For `background` in `BatteryDetailHeader.kt`:

```kotlin
import androidx.compose.foundation.background
```

2. For `size` in `FilterSection.kt`:

```kotlin
import androidx.compose.foundation.layout.size
```

### Fix Destructuring Issues in BatteryHistoryList.kt

Replace this kind of code:

```kotlin
val (icon, color) = when (eventType) {
    // ...
}
```

With explicit type declaration:

```kotlin
val iconAndColor: Pair<ImageVector, Color> = when (eventType) {
    // ...
}
val icon = iconAndColor.first
val color = iconAndColor.second
```

## Other Common UI Fixes

1. For any other missing modifiers, add:

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
```

2. For missing shape modifiers:

```kotlin
import androidx.compose.foundation.shape.*
```

3. For color utilities:

```kotlin
import androidx.compose.ui.graphics.Color
```

4. For text styling:

```kotlin
import androidx.compose.ui.text.style.*
```

## After Making Changes

After making these changes, try running the tests again:

```bash
./gradlew test
```

If you encounter specific errors in certain files, check the imports carefully and ensure all dependencies are correctly added to the build files.