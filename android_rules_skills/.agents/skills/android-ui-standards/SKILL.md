---
name: android-ui-standards
description: "Compose UI tokens, components, and layout architecture"
trigger: always_on
alwaysApply: true
globs:
  - "**/ui/**"
---

# Android Compose UI & Code Standardization Skill

## 1. Objective
Maintain 100% strict adherence to Android, Material 3, and Jetpack Compose conventions across all UI components, screens, composables, and layouts.

---

## 2. Core UI Standards & Hard Rules

### 1. Typography Standard (`MaterialTheme.typography`)
- **ALWAYS** use typography tokens from `MaterialTheme.typography`:
  ```kotlin
  Text(
      text = stringResource(R.string.title_profile),
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurface
  )
  ```
- **NEVER** hardcode inline font sizes, e.g. `fontSize = 16.sp`, `fontSize = 14.sp` directly in feature screen composables.
- Map custom typography needs through the central design system typography definition (`core/designsystem/theme/Type.kt`).

### 2. Spacing & Dimensions Standard (`dp` Tokens)
- **ALWAYS** use standard spacing increments:
  - Micro: `4.dp`
  - Small: `8.dp`
  - Medium: `16.dp`
  - Large: `24.dp`
  - Extra Large: `32.dp`
- **ALWAYS** use `Spacer(modifier = Modifier.height(16.dp))` or `Arrangement.spacedBy(16.dp)` for spacing between layout items.
- **NEVER** use arbitrary magic numbers (e.g. `13.dp`, `27.dp`, `19.dp`) for layout spacing.
- **NEVER** use hardcoded screen widths/heights (e.g. `Modifier.width(360.dp)`, `Modifier.height(720.dp)`). Use adaptive modifiers: `fillMaxWidth()`, `weight()`, `wrapContentHeight()`.

### 3. Padding & Window Insets Standard
- **ALWAYS** handle screen edge padding consistently:
  - Standard screen horizontal padding: `16.dp` (compact) or `24.dp`–`32.dp` (expanded)
  - Card inner padding: `16.dp`
  - Form spacing: `16.dp`
- In `Scaffold`, **ALWAYS** consume `PaddingValues` inside the content lambda:
  ```kotlin
  Scaffold(
      topBar = { AppTopBar(title = stringResource(R.string.title_home)) }
  ) { paddingValues ->
      Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          // Content
      }
  }
  ```
- For Lazy lists, **ALWAYS** use `contentPadding = PaddingValues(...)` instead of adding padding to individual items or the outer container.

### 4. Color System Standard (`MaterialTheme.colorScheme`)
- **ALWAYS** use semantic color tokens from `MaterialTheme.colorScheme`:
  - `MaterialTheme.colorScheme.primary`, `onPrimary`, `primaryContainer`, `onPrimaryContainer`
  - `MaterialTheme.colorScheme.surface`, `onSurface`, `surfaceVariant`, `onSurfaceVariant`
  - `MaterialTheme.colorScheme.background`, `onBackground`, `outline`, `error`
- **NEVER** hardcode hex colors (e.g. `Color(0xFF2196F3)`) or RGB literals in Composables. All colors must be declared in `Color.kt` / `Theme.kt` and consumed via the theme.
- Ensure all screens support dynamic color and both Light and Dark themes seamlessly.

### 5. Icons & Asset Resources Standard
- **ALWAYS** check `core/designsystem/components/` and `AppImages` / `R.drawable` before adding an icon.
- Use `Icon(painter = painterResource(id = R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.cd_back))` with tinting from `MaterialTheme.colorScheme`.
- **ALWAYS** provide a localized `contentDescription` for accessibility, or set `contentDescription = null` ONLY when the icon is purely decorative.
- For remote network images, use Coil (`AsyncImage`) with placeholder and error fallbacks.

### 6. User-Visible Text & Localization (`strings.xml`)
- **ALWAYS** extract user-visible strings into `res/values/strings.xml`.
- **ALWAYS** use `stringResource(R.string.<id>)` or `pluralStringResource(...)` in Compose.
- For dynamic messages and API responses originating from data or domain layers, **ALWAYS** model text with `UiText` (e.g. `UiText.from(apiMsg, R.string.fallback)` and resolve via `uiText.asString()`) rather than hardcoding `String` literals.
- **NEVER** write raw strings like `Text("Settings")` or concatenate strings in Compose code. Use format arguments (`%s`, `%d`).

---

## 3. Checklist for Code Review / Refactoring
When reviewing or writing any Compose screen or component:
- [ ] Are all text widgets styled with `MaterialTheme.typography` without hardcoded `.sp` values?
- [ ] Are all spacing gaps implemented via standard `dp` tokens or `Arrangement.spacedBy()`?
- [ ] Is `Scaffold`'s `paddingValues` correctly applied without clipping?
- [ ] Are all colors referenced from `MaterialTheme.colorScheme` without hardcoded `Color(...)` literals?
- [ ] Do Lazy lists utilize `contentPadding` and stable item keys (`key = { ... }`)?
- [ ] Are all icons accessible with proper `contentDescription`?
- [ ] Are all strings sourced from `strings.xml` via `stringResource()`?
- [ ] Does the screen adapt properly across Compact, Medium, and Expanded window sizes?
- [ ] Has `./gradlew assembleDebug` and `./gradlew lint` been run cleanly?
