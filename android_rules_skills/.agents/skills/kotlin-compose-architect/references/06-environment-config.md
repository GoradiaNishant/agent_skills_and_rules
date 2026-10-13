# Reference 06 — Environment Configuration & Flavors

Environment configuration must be considered for every project but should only
be implemented when the project actually requires multiple environments.

---

## Decision Questions

Before configuring environments, determine:

1. Is there more than one backend/API environment?
2. Are API base URLs different per environment?
3. Are Firebase projects different per environment?
4. Are OAuth credentials different?
5. Are Google Maps/API keys different?
6. Are application IDs/package names different?
7. Are feature flags different?
8. Are logging levels different?
9. Are analytics/crash-reporting configs different?
10. Does QA/staging need a separate install alongside production?

If the answer is **no to all**, do not introduce flavor complexity.

---

## Build Types vs Product Flavors

### Build Types
Describe **how** the app is built:
```
debug          ← development, logging, no shrinking
minifiedDebug  ← local R8 verification with debug signing
release        ← optimized, minified, resource-shrunk, signed
```
*(For detailed shrinking and ProGuard configurations, see [15-build-shrinking-optimization.md](./15-build-shrinking-optimization.md))*


### Product Flavors
Describe **which environment** is targeted:
```
dev      ← development API, verbose logging
staging  ← QA API, test credentials
prod     ← production API, analytics, real credentials
```

Gradle generates combinations automatically:
```
devDebug / devRelease
stagingDebug / stagingRelease
prodDebug / prodRelease
```

---

## Recommended Flavor Strategy

Only create the flavors the project actually needs.

```kotlin
flavorDimensions += "environment"

productFlavors {
    create("dev") {
        dimension = "environment"
        applicationIdSuffix = ".dev"
        versionNameSuffix = "-dev"
    }
    create("staging") {
        dimension = "environment"
        applicationIdSuffix = ".staging"
        versionNameSuffix = "-staging"
    }
    create("prod") {
        dimension = "environment"
        // no suffix — clean production ID
    }
}
```

Do not add `staging` if the team has no staging environment.
Do not add any flavor if only one environment is needed.

---

## Environment-Specific Source Sets

```
app/src/
├── main/         ← shared code
├── dev/          ← dev-specific resources, google-services.json
├── staging/      ← staging-specific resources, google-services.json
└── prod/         ← prod-specific resources, google-services.json
```

Only create flavor source sets when they are actually needed.

---

## API Base URL Configuration

Do not hardcode environment URLs throughout the application.

Use centralized configuration via BuildConfig:

```kotlin
// In build.gradle.kts per flavor
buildConfigField("String", "API_BASE_URL", "\"https://api.dev.example.com/\"")
```

```kotlin
// Centralized usage
object EnvironmentConfig {
    val apiBaseUrl: String get() = BuildConfig.API_BASE_URL
}
```

---

## Application ID Strategy

If multiple environments must coexist on the same device:
```
com.company.app.dev      ← development build
com.company.app.staging  ← staging build
com.company.app          ← production build
```

If simultaneous installation is not required, do not change the application ID.

---

## App Name Per Environment

```xml
<!-- src/dev/res/values/strings.xml -->
<string name="app_name">MyApp Dev</string>

<!-- src/staging/res/values/strings.xml -->
<string name="app_name">MyApp Staging</string>

<!-- src/main/res/values/strings.xml -->
<string name="app_name">MyApp</string>
```

Do not expose environment labels in the production app name.

---

## Firebase / Google Services

Determine whether each environment uses:
- **Option A**: One shared Firebase project
- **Option B**: Separate Firebase projects (dev / staging / prod)

For production applications with materially different environments, separate projects are appropriate. Place the correct `google-services.json` in each flavor source set.

Do not create multiple Firebase projects automatically.

---

## OAuth / Social Login

For Google Sign-In, OAuth providers:
- Evaluate whether each environment needs different Client IDs, redirect URIs, SHA-1 registrations, or package names.
- Do NOT reuse production OAuth credentials for development unless the provider configuration explicitly supports it.

---

## Environment Summary (include in proposal)

```
Environment Setup

Environments:   dev / prod
Flavors:        dev, prod
Build Types:    debug, release

Application IDs:
  dev:  com.company.app.dev
  prod: com.company.app

API Config:     BuildConfig.API_BASE_URL
Secrets:        CI/local environment — NOT committed to source control
Firebase:       Shared project (or separate — specify which)
```

Only include items that actually apply.

---

## Build Commands

Document the commands for the configured flavors:

```bash
./gradlew assembleDevDebug
./gradlew assembleStagingDebug
./gradlew assembleProdRelease
```

Always make clear which variant is active when running in Android Studio.
