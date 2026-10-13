---
name: flutter-ui-standards
description: >-
  Flutter UI standards, widget composition, design system tokens, theming,
  responsive layouts, const optimization, and localization best practices.
---

# Flutter UI & Design Standards Skill

You are an expert Flutter UI and Design System architect.

## 1. Core Principles

1. **Rich Aesthetics & Premium Feel**: Wow the user with curated color schemes, subtle micro-animations, smooth transitions, and modern typography. Avoid raw unstyled widgets.
2. **Const Widget Optimization**: Always mark widget constructors with `const` whenever parameters are compile-time constants. This prevents unnecessary rebuilds.
3. **Class-Based Widget Extraction**: Extract widgets as separate `StatelessWidget` / `StatefulWidget` classes rather than helper functions (`Widget _buildItem()`). Helper functions break Flutter's element tree diffing and optimization.
4. **Theme Tokens & Design System**:
   - Access colors and styles through `Theme.of(context).colorScheme` and `Theme.of(context).textTheme`.
   - Define custom design tokens using `ThemeExtension<MyTokens>`.
   - Never hardcode raw hex colors (`Color(0xFF123456)`) in screen widgets.
5. **Localization & User-Visible Strings**:
   - All user-facing text must use `AppLocalizations.of(context)`. Never write raw English literals directly in widgets.
   - For dynamic text from data layers, use `UiText`.

---

## 2. Production Patterns

### 1. Dedicated Widget Extraction
```dart
// ✅ Correct: Separate widget class with const constructor
class ProductCard extends StatelessWidget {
  final Product product;
  final VoidCallback onTap;

  const ProductCard({
    super.key,
    required this.product,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      child: ListTile(
        title: Text(product.name, style: theme.textTheme.titleMedium),
        onTap: onTap,
      ),
    );
  }
}
```

### 2. Custom Theme Tokens via ThemeExtension
```dart
class AppSpacing extends ThemeExtension<AppSpacing> {
  final double s;
  final double m;
  final double l;

  const AppSpacing({this.s = 8.0, this.m = 16.0, this.l = 24.0});

  @override
  AppSpacing copyWith({double? s, double? m, double? l}) =>
      AppSpacing(s: s ?? this.s, m: m ?? this.m, l: l ?? this.l);

  @override
  AppSpacing lerp(ThemeExtension<AppSpacing>? other, double t) => this;
}
```
