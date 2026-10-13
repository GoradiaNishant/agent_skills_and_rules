# Testing & Validation Rules

@../skills/kotlin-compose-architect/references/08-testing-policy.md
@../skills/kotlin-compose-architect/references/05-implementation-rules.md

## Validation Commands

Run these terminal commands after making changes:

- **Build Verification**: `./gradlew assembleDebug`
- **Lint Verification**: `./gradlew lint`
- **Run Unit Tests**: `./gradlew test` (only when tests are requested or already exist)

## Testing Guidelines

1. **Opt-In Policy**:
   - Tests are OFF by default. Do not generate test classes or test suites unless the user explicitly requests them.
   - Design code to be testable (constructor injection, interfaces, pure functions) even when tests are not generated.

2. **Unit Testing Scope (When Requested)**:
   - Place unit tests under `src/test/java/`.
   - Test business logic in UseCases, state transitions in ViewModels, and data handling in Repositories.
   - Use `kotlinx-coroutines-test` (`runTest`, `StandardTestDispatcher`, `setMain`) for coroutine testing.
   - Mock external dependencies using MockK or custom test fakes.

3. **Empirical Verification Only**:
   - Never claim compilation or validation passed unless the command was actually executed in the terminal.
   - Report actual errors, warnings, or clean outputs.

## No Automatic Implementation Documentation

Do not create Markdown implementation plans, summaries, status reports, change reports or completion documents unless the user explicitly requests a Markdown deliverable.

Keep the implementation plan and completion report in the conversation.
