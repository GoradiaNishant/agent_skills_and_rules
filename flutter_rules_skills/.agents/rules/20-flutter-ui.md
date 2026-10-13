# Flutter UI Rules

@../skills/flutter-ui-standards/SKILL.md
@../skills/flutter-architect/references/10-flutter-ui-rules.md

## Widget Construction & Aesthetics Governance

1. **Const Optimization**: Mark widget invocations with `const` wherever properties are compile-time constants.
2. **Class-Based Extraction**: Always extract subtrees as separate `StatelessWidget` / `StatefulWidget` classes rather than helper methods returning `Widget`.
3. **Theme Tokens**: Use `Theme.of(context).colorScheme` and `Theme.of(context).textTheme`. Never hardcode raw hex colors or ad-hoc text styles in screens.
4. **Localization**: Never hardcode English strings. Access localized text through `AppLocalizations.of(context)` and `UiText`.
5. **Aesthetics & Premium Feel**: Wow the user with curated harmonious palettes, subtle animations, and responsive padding.
