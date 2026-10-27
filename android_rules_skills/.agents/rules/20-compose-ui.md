---
name: 20-compose-ui
description: "Jetpack Compose UI rules, UDF, and preview standards"
trigger: always_on
alwaysApply: true
globs:
  - "**/*Screen.kt"
  - "**/*Route.kt"
  - "**/ui/**"
---

# Jetpack Compose UI Conventions

@../skills/kotlin-compose-architect/references/10-compose-ui-rules.md
@../skills/kotlin-compose-architect/references/11-utils-helpers-extensions.md

## UI Standards & Component Rules

1. **Screen & Route Architecture**:
   - Separate route from screen for all feature screens:
     - Route composable (`<Name>Route`): Collects `uiState` using `collectAsStateWithLifecycle()`, manages ViewModel lifecycle, and passes actions to `NavController`.
     - Screen composable (`<Name>Screen`): Pure stateless UI component accepting `state: <Name>UiState` and `onEvent: (<Name>Event) -> Unit` or explicit lambda callbacks.
   - Do NOT pass `NavController` or `ViewModel` deep down the Composable tree. Hoist state and pass event lambdas.
   - Top App Bar: Use Material 3 `TopAppBar` or project `CenterAlignedTopAppBar` inside `Scaffold`.

2. **Responsive & Adaptive Layouts (Window Size Classes)**:
   - Design for adaptive window sizes, not a single phone screen.
   - Use `WindowSizeClass` (`WindowWidthSizeClass.Compact`, `Medium`, `Expanded`) rather than device checks:
     - Compact: Standard single-column phone layout.
     - Medium / Expanded: Multi-column grids, List-Detail panes, or Navigation Rails.
   - Use adaptive layout modifiers (`Modifier.fillMaxWidth()`, `Modifier.weight()`, `Modifier.widthIn()`, `Modifier.heightIn()`) rather than fixed layout dimensions (e.g. avoid `width = 320.dp`).

3. **Design System & Material 3 Tokens**:
   - Always reference theme tokens from `MaterialTheme`:
     - Colors: `MaterialTheme.colorScheme.primary`, `onPrimary`, `surface`, `onSurface`, `surfaceVariant`, `error`.
     - Typography: `MaterialTheme.typography.titleMedium`, `bodyMedium`, `labelLarge`.
     - Shapes: `MaterialTheme.shapes.medium`, `MaterialTheme.shapes.large`.
   - Never hardcode inline hex colors (e.g. `Color(0xFFEAF8E6)`) or raw text sizes (`fontSize = 16.sp`).

4. **Spacing & Dimensions**:
   - Use standardized dp spacing tokens (`core/designsystem/theme/Spacing.kt` or `4.dp`, `8.dp`, `12.dp`, `16.dp`, `24.dp`, `32.dp`).
   - Use `Spacer(modifier = Modifier.height(16.dp))` or `Arrangement.spacedBy(12.dp)` for spacing gaps.
   - Handle system bars properly using `WindowInsets.safeDrawing`, `WindowInsets.systemBars`, or `Scaffold` `paddingValues`.

5. **Core Reusable UI Components**:
   - Search `core/designsystem/components/` before creating a new component.
   - Reuse existing buttons (`PrimaryButton`, `OutlinedAppButton`), text fields (`AppTextField`), app bars (`AppTopBar`), loading indicators, and empty states.
   - Render `EmptyStateView` or `ErrorStateView` with a retry callback for failed/empty states.

6. **Icons & Vector Resources**:
   - Centrally declare or reference drawables via `AppImages` or `R.drawable.*`.
   - Use `Icon(painter = painterResource(id = ...), contentDescription = stringResource(...))` for vector icons.
   - Always supply meaningful localized `contentDescription` for accessibility, or `null` only if purely decorative.

7. **Localization & User-Visible Text**:
   - Every user-visible string must be defined in `res/values/strings.xml` and referenced via `stringResource(R.string.<id>)`.
   - Never write raw string literals in Composables.

## Hard Restrictions

- Do not perform side effects, network calls, DB operations, or hardware operations during composition.
- Do not make backward writes (mutating state after reading it during composition).
- Do not create custom wrappers around basic Compose APIs that provide no added value (e.g. `MyText(text)`).
- Do not pass `NavController` into child list item composables or card widgets.
- Always use stable keys in `items()` for `LazyColumn`, `LazyRow`, and `LazyVerticalGrid`.
