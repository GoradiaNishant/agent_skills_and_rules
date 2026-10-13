# Core Development Rules

@../skills/flutter-architect/SKILL.md

## Primary Instructions & Workflow Requirements

1. **Inspect Before Editing**: Inspect relevant existing files in the workspace before writing or modifying code.
2. **Follow Project Patterns**: Inspect at least one similar completed feature and strictly follow the nearest current project pattern.
3. **Reuse Existing Infrastructure**: Reuse existing Widgets, BLoCs/Cubits/Providers, UseCases, repositories, data sources, entities, validators, theme tokens, localization keys, navigation routes, and utilities.
4. **Search Prior to Abstracting**: Search for existing functionality before creating a new abstraction; never duplicate existing project logic.
5. **Architectural Consistency**: Do not introduce a different architecture, third-party state management library, or network client for a feature task.
6. **Minimal Dependencies**: Do not add a `pubspec.yaml` dependency unless explicitly requested or genuinely required.
7. **Strict Scope Control**: Do not modify unrelated files or perform broad refactoring outside the requested task scope.
8. **No Hardcoded Values**: Never hardcode API URLs, API keys, tokens, colors, font sizes, dimensions, or user-visible strings.
9. **Security First**: Never expose secrets, production credentials, passwords, keystores, certificates, or tokens in code, logs, tests, or documentation.
10. **Preserve Existing Behavior**: Ensure changes do not break existing functionality or introduce side effects.
11. **Standard Error Handling**: Use the existing project error handling mechanism (`NetworkResult<T>`, UI state error unions, localized user-friendly messages via `UiText`).
12. **Complete UI States**: Handle all relevant UI states: initial, loading, content, empty, validation error, initial error, retry, refresh, and pagination.
13. **Avoid Trivial Questions**: Do not ask unnecessary questions when the codebase provides a clear pattern.
14. **Post-Task Verification**: Run relevant build verification and static analysis (`flutter analyze`) and tests (`flutter test` if requested) after changes.
15. **Empirical Verification Only**: Never claim validation passed unless the command was actually executed in the terminal.
16. **Task Summary**: Report changed files, validation executed, assumptions made, and any unresolved issues in the conversation after completing work.
17. **Do Not Touch Generated Files**: Never manually edit generated code (`*.freezed.dart`, `*.g.dart`, `*.config.dart`).
18. **Skill Reference Usage**: Refer to `@../skills/flutter-architect/SKILL.md` for project development workflows and detailed reference mappings.
19. **Code Generation & Annotations**: Adhere to `@../skills/flutter-code-generation-annotations/SKILL.md` for Freezed immutability, JsonSerializable DTOs, and Injectable DI.
20. **Background Execution & Scheduling**: Adhere to `@../skills/flutter-background-execution-scheduling/SKILL.md` to select the simplest compliant mechanism (Isolates via `compute`/`Isolate.run` for heavy CPU tasks, Workmanager for persistent deferrable sync).
21. **Notification Architecture & Push Messaging**: Adhere to `@../skills/flutter-notifications-push-management/SKILL.md` for stable FCM message handling, background isolates, deep link routing, and permissions.
22. **App Monitoring, Observability & Global Error Handling**: Adhere to `@../skills/flutter-monitoring-observability-error-handling/SKILL.md` for single source of truth logging with execution timers, Firebase Crashlytics non-fatals with breadcrumbs, analytics tracking, and global error coordination via Messenger.

## Mandatory Implementation Plan Approval

Before every implementation, bug fix, refactor, API integration, UI change, navigation change, storage change or test change:

1. Inspect the current repository and related implementations before planning.
2. Create a structured Antigravity Implementation Plan artifact.
3. Stop and request user review.
4. Do not create or modify application files before approval.
5. Begin implementation only after the user presses `Proceed` or explicitly approves the plan.

If the user comments or provides feedback on the plan:
1. Read all comments.
2. Revise the Implementation Plan artifact.
3. Present the updated plan and request review again.
4. Do not implement until the revised plan is approved.

Do not interpret complete requirements, detailed prompt instructions, or the phrase "Prepared Implementation Plan" as approval to modify code. The plan is mandatory even for small tasks.

## No Automatic Implementation Documentation

Do not create Markdown implementation plans, summaries, status reports, change reports or completion documents unless the user explicitly requests a Markdown deliverable.

Keep the implementation plan and completion report in the conversation.
