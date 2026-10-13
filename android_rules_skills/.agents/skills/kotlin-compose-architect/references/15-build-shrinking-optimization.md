# Reference 15 — Build Shrinking, Minification & Optimization

Comprehensive guide for R8 code shrinking, resource shrinking, ProGuard configuration, Compose compiler optimization, and app size reduction.

---

## 1. R8 Shrinker vs. Legacy ProGuard

Android Gradle Plugin (AGP) utilizes **R8** as the default shrinker and optimizer. R8 combines four distinct operations into a single compilation phase:

```
Source Code / Bytecode
        ↓
┌───────────────────────────────────────────────┐
│                     R8                        │
│ 1. Tree Shaking (Code Shrinking)             │
│ 2. Optimization (Inlining, Devirtualization) │
│ 3. Obfuscation (Renaming to a/b/c)            │
│ 4. Desugaring (Java 8+ language features)     │
└───────────────────────────────────────────────┘
        ↓
    DEX Files (Smaller, Faster, Secure)
```

- **Tree Shaking (Code Shrinking)**: Detects and removes unreachable classes, fields, and methods from your app and library dependencies.
- **Optimization**: Inspects code to inline small methods, eliminate unused arguments, propagate constants, and merge single-implementation interfaces.
- **Obfuscation**: Shortens class, method, and field names (e.g., `UserRepositoryImpl` → `a.b.c`), decreasing DEX size and preventing reverse-engineering.
- **Resource Shrinking**: Removes unused drawables, layouts, and strings after R8 strips dead code.

---

## 2. Gradle Build Configuration (`build.gradle.kts`)

### Release Configuration
Always use `proguard-android-optimize.txt` rather than `proguard-android.txt` to enable aggressive optimization passes:

```kotlin
android {
    buildTypes {
        release {
            // Enable R8 code shrinking and obfuscation
            isMinifyEnabled = true

            // Enable resource shrinking (requires isMinifyEnabled = true)
            isShrinkResources = true

            // Proguard configuration files
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig = signingConfigs.getByName("release")
        }

        // Dedicated variant for testing R8 rules without production signing
        create("minifiedDebug") {
            initWith(getByName("debug"))
            matchingFallbacks += listOf("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}
```

---

## 3. Essential ProGuard & R8 Rules (`proguard-rules.pro`)

### A. Data Models & JSON Serialization
Prevent R8 from renaming or stripping fields parsed via reflection or Kotlinx Serialization:

```proguard
# === Kotlinx Serialization ===
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt

# Keep serializer companion objects and generated serializers
-keepclassmembers class * {
    companion object;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation,allowshrinking class * {
    <fields>;
}

# === Moshi / Gson ===
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
    @com.squareup.moshi.JsonQualifier <fields>;
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.JsonClass public <init>(...);
}

# === Universal Model Annotation ===
# Annotate critical DTOs with @androidx.annotation.Keep
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
```

### B. Networking: Retrofit & OkHttp
```proguard
# === Retrofit ===
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}

# Preserve parameter names for reflection-based adapters
-keepattributes Signature, InnerClasses, EnclosingMethod

# === OkHttp ===
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
```

### C. Local Database: Room
```proguard
# === Room Database ===
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.TypeConverter { *; }
```

### D. Navigation & Type-Safe Routes
```proguard
# === Navigation 2.8+ Type-Safe Route Objects / Classes ===
-keepclasseswithmembers @kotlinx.serialization.Serializable class * {
    <fields>;
    <init>(...);
}
```

### E. Crashlytics & Stack Trace Line Numbers
Preserve line numbers and file names so Google Play Console and Crashlytics can symbolicate obfuscated stack traces with `mapping.txt`:

```proguard
# Preserve source file attributes and line number table
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Preserve Kotlin metadata for reflection and debugging
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
```

### F. Stripping Logging in Production
Strip standard `android.util.Log` statements during R8 bytecode optimization:

```proguard
# Strip logging calls in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}
```
*Note: If using Timber, do not install `Timber.DebugTree()` in production builds instead of relying on bytecode stripping.*

---

## 4. Resource Shrinking & Strict Mode

Resource shrinking works in tandem with code shrinking. Once R8 removes unused classes, resource shrinking removes unreferenced drawables, XML layouts, strings, and raw assets.

### Dynamic Resource Handling (`keep.xml`)
If the codebase accesses resources dynamically via reflection or `Resources.getIdentifier()`:

```kotlin
// Example dynamic access that R8 cannot trace automatically
val resId = context.resources.getIdentifier("flag_$countryCode", "drawable", context.packageName)
```

Create `res/raw/keep.xml` to protect those resources from being stripped:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools"
    tools:keep="@drawable/flag_*,@string/error_code_*"
    tools:discard="@layout/unused_legacy_layout"
    tools:shrinkMode="strict" />
```

- `tools:keep`: Glob pattern of resources to retain.
- `tools:discard`: Forcefully removes resources even if referenced in code.
- `tools:shrinkMode="strict"`: Only keeps resources explicitly referenced in code or listed in `tools:keep` (disables safe-mode heuristic that keeps all resources matching string tokens in code).

---

## 5. Jetpack Compose Compiler Optimizations

### A. Strong Skipping Mode
Strong skipping mode enables Composables with unstable parameters to be skipped during recomposition if their instances have not changed:

```kotlin
// build.gradle.kts (Kotlin 2.0+ with Compose Compiler Gradle Plugin)
composeCompiler {
    enableStrongSkippingMode = true
}
```

### B. Compose Stability Configuration File
For external or third-party classes that Compose treats as unstable:

```kotlin
// build.gradle.kts
composeCompiler {
    stabilityConfigurationFile = rootProject.file("compose-stability.conf")
}
```

In `compose-stability.conf`:
```text
// Treat external models or JDK collections as stable
java.time.Instant
java.time.LocalDate
com.example.external.sdk.*
```

### C. Compose Compiler Metrics & Reports
Analyze recomposition costs and class stability:

```kotlin
composeCompiler {
    metricsDestination = layout.buildDirectory.dir("compose_metrics")
    reportsDestination = layout.buildDirectory.dir("compose_reports")
}
```
Run `./gradlew assembleRelease` and inspect `*-classes.txt` and `*-composables.txt` to identify:
- **Restartable vs. Skippable composables**.
- **Unstable classes** triggering unnecessary recompositions.

### D. Compose Runtime Optimization Rules
1. **Defer State Reads to Layout/Draw**: Use lambda modifiers (`Modifier.offset { IntOffset(x, y) }`) instead of direct values (`Modifier.offset(x.dp, y.dp)`) to skip recomposition when state changes frequently.
2. **Use DerivedStateOf**: Wrap calculations derived from fast-changing state (e.g. `LazyListState.firstVisibleItemIndex > 0`).
3. **Use Immutable Collections**: Prefer `ImmutableList<T>` / `PersistentList<T>` from `kotlinx.collections.immutable` over standard `List<T>`.

---

## 6. App Size & Delivery Optimizations

### A. Android App Bundle (AAB) & Splits
Always produce an AAB (`./gradlew bundleRelease`) for Google Play distribution:
- Delivers only the target device's screen density (xhdpi, xxhdpi, etc.).
- Delivers only the target device's CPU architecture (ABI).
- Delivers only matching language resources.

### B. ABI Filters
Prevent bundling unnecessary native architectures in universal APKs:

```kotlin
android {
    defaultConfig {
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }
}
```

### C. Language / Resource Configurations
Strip unused localization strings from third-party libraries (e.g. Play Services, AppCompat):

```kotlin
android {
    defaultConfig {
        // Only bundle specified languages
        resourceConfigurations += listOf("en", "es", "ar")
    }
}
```

### D. Image Asset Formats
- **Icons & Shapes**: Always use Android Vector Drawables (`<vector>`).
- **Photographic / Complex Images**: Convert PNG and JPEG to **WebP** (lossy for photos, lossless for UI graphics).
- **Avoid Unused Densities**: If a vector drawable can be used, eliminate mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi PNG duplicates.

---

## 7. Baseline Profiles & Startup Optimization

### What are Baseline Profiles?
Baseline Profiles instruct the Android Runtime (ART) ahead of time (AOT) to compile critical bytecode paths into machine code before the user launches the app, eliminating JIT compilation pauses and reducing cold startup time by 20–40%.

### Integration Setup
```kotlin
// In build.gradle.kts
plugins {
    id("androidx.baselineprofile")
}

dependencies {
    baselineProfile(project(":baselineprofile"))
}
```

Generate baseline profiles using Macrobenchmark tests targeting:
1. Application cold startup (`startActivityAndWait()`).
2. Primary critical user journeys (e.g., Home feed scroll, product detail navigation).

---

## 8. Debugging R8 & ProGuard Issues

When minification is enabled, runtime issues typically manifest as:
- `ClassNotFoundException` / `NoClassDefFoundError` (class stripped by tree shaking).
- `NoSuchMethodException` (reflection cannot find stripped constructor or method).
- `NullPointerException` (JSON serialization fields stripped or renamed).

### Diagnostic Artifacts
R8 outputs mapping and usage diagnostics under `app/build/outputs/mapping/<variant>/`:
1. `mapping.txt`: Translates obfuscated class/method names back to original names. (Upload to Play Console / Crashlytics).
2. `usage.txt`: Exhaustive list of all code removed by R8. Search this file to check if a required class was stripped.
3. `seeds.txt`: Exhaustive list of all classes and members retained according to your `-keep` rules.
4. `configuration.txt`: Combined ProGuard rules from all project modules and AAR dependencies.

### Verification Command
Always verify minified builds before shipping:
```bash
./gradlew assembleRelease
# Or test locally with:
./gradlew assembleMinifiedDebug
```
