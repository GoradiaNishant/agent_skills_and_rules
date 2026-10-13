# Flutter UI Architecture & Performance Rules

1. **Const Constructors**: Always use `const` widget constructors for static subtrees to avoid unnecessary rebuild cycles.
2. **Dedicated Widget Classes**: Extract reusable components as separate `StatelessWidget` classes with `Key` parameters. Never write large helper functions returning `Widget`.
3. **Responsive Sizing**: Use `LayoutBuilder`, `MediaQuery.sizeOf(context)`, and flexible widgets (`Flexible`, `Expanded`). Avoid hardcoded absolute screen dimensions.
4. **Design Tokens**: Centralize colors, font styles, and spacings in `ThemeData` and custom `ThemeExtension` classes.
5. **No Logic in Build**: Never perform database queries, API calls, or side effects inside `build()`.
