# Reference 05 — Implementation Rules

Once architecture is approved or sufficiently inferred, implement in this order.

---

## Implementation Order

1. Create the minimum required project structure
2. Configure Gradle dependencies (only what is justified)
3. Configure DI (Hilt modules) if needed
4. Configure networking (Retrofit/OkHttp) if needed
5. Configure database (Room) if needed
6. Implement domain models
7. Implement data layer (DTOs, APIs, DAOs, repositories)
8. Implement domain layer (UseCases) if required
9. Implement ViewModels with UI state
10. Implement Compose UI screens
11. Implement navigation
12. Add error / loading / empty states to every screen
13. Add tests for critical logic — only if explicitly requested
14. Configure release shrinking, ProGuard keep rules, and resource preservation if needed
15. Verify compilation
16. Fix warnings and errors
17. Provide final architecture summary

Do not create placeholder layers with no implementation purpose.
Do not create empty test classes unless tests are requested.

---

## Build Verification

After implementation, run:

```bash
./gradlew assembleDebug
```

For release configuration, R8 minification, or ProGuard rule changes:
```bash
./gradlew assembleRelease
# Or test minified debug variant:
./gradlew assembleMinifiedDebug
```

And optionally:
```bash
./gradlew test
./gradlew lint
```

Do not claim the project works unless it has been verified.
If build verification cannot be performed, explicitly state that.

---

## Final Output — Required Sections

After implementation, provide:

### 1. Architecture Summary
What architecture was selected and why.

### 2. Project Structure
Show the actual final directory tree.

### 3. Dependencies
List only dependencies that were actually added.

### 4. Key Decisions
Explain important decisions:
- Why DI?
- Why UseCases?
- Why Repository?
- Why Room / DataStore?
- Why WorkManager?
- Why WebSocket?
- Why or why not Clean Architecture?

### 5. Verification Report
- Build result (pass/fail)
- Tests (if run)
- Lint (if run)
- Known issues or limitations

---

## Migration Rule

If the existing architecture is imperfect but functional:

```
Existing code
  ↓
Identify architectural problem
  ↓
Fix problem incrementally
  ↓
Keep working functionality
```

Do NOT rewrite everything unless there is clear technical justification.
