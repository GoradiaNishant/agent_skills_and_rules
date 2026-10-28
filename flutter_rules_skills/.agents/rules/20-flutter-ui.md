---
name: 20-flutter-ui
description: "Stateless/Stateful widgets, Design System tokens, and responsive layouts"
trigger: always_on
alwaysApply: true
globs:
  - "**/presentation/**"
  - "**/views/**"
  - "**/screens/**"
---

# Flutter UI Rules

@../skills/flutter-ui-standards/SKILL.md
@../skills/flutter-architect/references/10-flutter-ui-rules.md

## Widget Construction & Aesthetics Governance

1. **Rendering Pipeline & Tree Architecture**:
   - Understand Flutter's 3-tier hierarchy:
     - **Widget Tree**: Lightweight, immutable blueprints/configurations.
     - **Element Tree**: Mutable nodes managing tree lifecycle, identity, and connecting Widgets to RenderObjects.
     - **RenderObject Tree**: Heavyweight nodes handling layout constraints, sizing, positioning, and painting.
   - Distinguish **Rebuild** vs **Relayout** vs **Repaint**: Rebuilding a widget only re-runs `build()`; it does not force relayout or repaint if layout constraints and render properties remain unchanged.

2. **Strict Class-Based Extraction (No Helper Methods Returning Widgets)**:
   - **Always** extract widget subtrees into dedicated `StatelessWidget` or `StatefulWidget` classes.
   - **Never** write helper methods returning widgets (e.g. `Widget _buildUserRow() { ... }`):
     - Helper methods share the parent's `BuildContext` and force the entire subtree to rebuild whenever the parent updates.
     - Separate widget classes create distinct `Element` boundaries, allowing Flutter's framework to skip rebuilding when parameters are unchanged.

3. **Const Optimization & Canonical Memory Reuse**:
   - Mark widget constructor invocations with `const` wherever properties are compile-time constants.
   - Using `const` short-circuits subtree rebuilding: Flutter reuses the exact same Element instance and avoids re-calling `build()`.

4. **Image Decode Optimization (`cacheWidth` / `cacheHeight`)**:
   - When displaying network or asset images in lists or grids, always specify `cacheWidth` or `cacheHeight` (e.g. `ResizeImage` or `Image.network(..., cacheWidth: 240)`).
   - This hints the image decoder to downsample images into target display resolution before uploading textures to the GPU, preventing out-of-memory crashes and scrolling stutter.

5. **Theme Tokens**: Use `Theme.of(context).colorScheme` and `Theme.of(context).textTheme`. Never hardcode raw hex colors or ad-hoc text styles in screens.
6. **Localization**: Never hardcode English strings. Access localized text through `AppLocalizations.of(context)` and `UiText`.
7. **Aesthetics & Premium Feel**: Wow the user with curated harmonious palettes, subtle animations, and responsive padding.

