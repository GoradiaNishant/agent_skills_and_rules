# Reference 01 — Discovery & Project Information

Before writing any implementation code, determine the following. If values are
already present in the project files, read them — do NOT ask the user for
information that can be inferred.

---

## Basic Project Information

Inspect or ask for:

- Application name
- Package / application ID (e.g. `com.company.appname`)
- Minimum Android SDK (`minSdk`)
- Target SDK (`targetSdk`)
- Kotlin version
- Compose BOM / Compose version
- Gradle version / AGP version
- Java/JDK version

**If an existing project is provided**, check:

```
build.gradle.kts (project-level)
app/build.gradle.kts
gradle/libs.versions.toml (if using version catalogs)
settings.gradle.kts
```

Never ask for information already visible in these files.

---

## Existing Project Rule

If the user provides an existing project:

1. DO NOT redesign the entire architecture immediately.
2. Inspect the existing package structure, Gradle config, dependencies, architecture, navigation, DI, networking, database, feature organization, build variants, and tests.
3. Determine: **What needs to change vs. what is already working?**
4. Prefer incremental improvements over unnecessary rewrites.
5. Only recommend a large refactor when there is clear technical justification.

---

## Key Architecture Discovery Questions

Ask only what cannot be inferred. Focus on questions that materially affect architecture:

1. Is this API-driven, local-only, or both?
2. Does it require login/authentication?
3. Does it need offline support?
4. Is real-time data required?
5. Are notifications required?
6. Does it contain complex business rules?
7. Is background synchronization required?
8. Does it handle sensitive or financial information?
9. How many major features/screens are expected?
10. Is this a prototype/MVP or a production application?

Do not ask all 10 at once if most answers can be inferred from the description.
