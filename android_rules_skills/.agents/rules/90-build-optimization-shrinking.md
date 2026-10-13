# Build Optimization, Shrinking & Minification Rules

@../skills/kotlin-compose-architect/references/15-build-shrinking-optimization.md
@../skills/kotlin-compose-architect/references/06-environment-config.md
@../skills/kotlin-compose-architect/references/07-security.md

## Core Shrinking & Optimization Conventions

1. **R8 Minification Enabled for Release**:
   - `isMinifyEnabled = true` must be enabled on all production release builds.
   - Always use `getDefaultProguardFile("proguard-android-optimize.txt")` for aggressive multi-pass code optimization.
   - Provide a local `minifiedDebug` build type when testing or debugging R8 and ProGuard rules without production signing.

2. **Resource Shrinking**:
   - `isShrinkResources = true` must be enabled on builds where `isMinifyEnabled = true`.
   - Protect dynamically referenced drawables or strings (e.g., accessed via `Resources.getIdentifier()`) by registering them in `res/raw/keep.xml` with `tools:shrinkMode="strict"`.

3. **Data Model & Serialization Retention**:
   - Never allow R8 to strip or rename serialized model fields.
   - For Kotlinx Serialization: ensure serializer classes, companion objects, and `@Serializable` types are retained.
   - For Moshi / Gson / reflection-based parsers: retain fields with `@Json`, `@SerializedName`, or annotate critical DTOs with `@Keep`.
   - Never use broad `-keep class * { *; }` wildcards; keep rules must be granular and scoped to specific packages or annotations.

4. **Crash Reporting & Symbolication**:
   - Retain line numbers and source file names in release builds using `-keepattributes SourceFile,LineNumberTable` and `-renamesourcefileattribute SourceFile`.
   - Ensure `mapping.txt` (under `app/build/outputs/mapping/<variant>/`) is preserved and uploaded to Crashlytics / Google Play Console for stack trace de-obfuscation.

5. **Release Log Stripping**:
   - Never ship verbose debug logs to production.
   - Strip `android.util.Log` statements in release builds using `-assumenosideeffects` or avoid installing debug logging trees (e.g. `Timber.DebugTree`) in production application classes.

6. **Jetpack Compose Performance Optimization**:
   - Enable Compose Strong Skipping Mode (`enableStrongSkippingMode = true`).
   - Eliminate unnecessary recompositions by using `@Immutable`, `@Stable`, or `ImmutableList` for collection parameters.
   - Defer reading fast-changing state to the layout or draw phase via lambda modifiers (`Modifier.offset { ... }`).
   - Use `derivedStateOf` for derived state calculations inside Composables.

7. **App Size & Asset Delivery**:
   - Distribute production builds as an Android App Bundle (AAB) to enable Google Play Dynamic Delivery (split APKs by ABI, density, and language).
   - Constrain universal packaging using `ndk.abiFilters` (e.g. `arm64-v8a`, `armeabi-v7a`).
   - Filter unused third-party library localizations via `resourceConfigurations += listOf("en", ...)`.
   - Prefer Android Vector Drawables over raster PNGs; convert photographic images to WebP.

8. **Baseline Profiles**:
   - Use the `androidx.baselineprofile` plugin to generate Ahead-Of-Time (AOT) compilation profiles for critical user journeys and app cold start.
