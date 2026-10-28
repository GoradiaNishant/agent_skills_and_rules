# Flutter UI Architecture & Rendering Performance Rules

## 1. The Flutter Three-Tree Rendering Architecture

To write high-performance Flutter interfaces, you must understand the distinction between the three parallel trees:

```text
┌────────────────────────────────────────────────────────┐
│                      Widget Tree                       │
│  - Lightweight, immutable blueprints of UI             │
│  - Recreated frequently during state updates           │
│  - Contains configuration properties (padding, color)  │
└───────────────────────────┬────────────────────────────┘
                            │ inflates / updates
                            ▼
┌────────────────────────────────────────────────────────┐
│                      Element Tree                      │
│  - Mutable instances holding structural tree location  │
│  - Manages component lifecycle and state persistence   │
│  - Compares old widget vs new widget: `canUpdate()`    │
└───────────────────────────┬────────────────────────────┘
                            │ mounts
                            ▼
┌────────────────────────────────────────────────────────┐
│                   RenderObject Tree                    │
│  - Heavyweight objects handling geometry & rendering   │
│  - Performs layout: constraints go down, sizes go up   │
│  - Handles compositing, hit testing, and painting      │
└────────────────────────────────────────────────────────┘
```

---

## 2. Rebuild vs. Relayout vs. Repaint

- **Rebuild (Widget Execution)**:
  - Invokes the `build(BuildContext context)` method to generate a new widget tree description.
  - Rebuilding a widget is relatively inexpensive, but rebuilding deep subtrees wastes CPU cycles and battery.
  - A widget rebuild does **NOT** automatically cause relayout or repaint if its Element and RenderObject determine that properties and layout dimensions are identical.
- **Relayout (Layout Phase)**:
  - Calculating element dimensions and offsets (`performLayout()`).
  - Occurs when an element's constraints change or its child intrinsic dimensions change.
  - Relayout boundaries (`LayoutBuilder`, fixed sizes) isolate layout passes so changes don't dirty the root.
- **Repaint (Paint Phase)**:
  - Painting pixels onto the canvas (`paint()`).
  - Use `RepaintBoundary` around fast-updating animations or complex vector paintings to isolate them onto a separate graphics layer.

---

## 3. Class-Based Widgets vs. Helper Functions Returning Widgets

**Hard Rule**: Always extract UI subtrees into dedicated `StatelessWidget` or `StatefulWidget` classes. **Never** write helper functions that return `Widget` (e.g. `Widget _buildItem(Item item)`).

### Why Helper Methods are an Anti-Pattern:
1. **No Element Boundary**: Helper methods share the parent's `BuildContext`. When the parent rebuilds, every widget created by helper methods is forced to rebuild from scratch.
2. **Cannot Use `const`**: Helper methods cannot be prefixed with `const`, preventing Flutter from canonicalizing and reusing widget configurations.
3. **No Granular Scoping**: Separate widget classes allow targeted rebuilding, independent animation controllers, and proper lifecycle management.

---

## 4. `const` Constructor Optimization

- Mark widget invocations with `const` wherever all constructor arguments are compile-time constants.
- Encountering a `const` constructor tells the framework that the widget's configuration cannot have changed since the last build, allowing Flutter to **skip re-evaluating the `build()` method** of that subtree entirely.

---

## 5. Frame Budgets & Eliminating Jank

- **Frame Budgets**:
  - **60 Hz displays**: ~16.6 milliseconds per frame.
  - **120 Hz displays**: ~8.3 milliseconds per frame.
- If synchronous Dart execution or GPU rasterization exceeds this window, a frame drop (**jank**) occurs, causing visual stuttering during scrolling or animations.
- **Never do heavy work in `build()`**:
  - No database queries, file reads, parsing, or sorting in `build()`.
  - Perform transformations in Cubits/BLoCs/Notifiers, not in presentation widgets.

---

## 6. Image Downsampling & Decode Optimization (`cacheWidth` / `cacheHeight`)

- Large images are the #1 cause of mobile app memory spikes and scroll stutter.
- An uncompressed 4000x3000 photo decoded into memory consumes:
  $$4000 \times 3000 \times 4 \text{ bytes (RGBA)} \approx 48 \text{ MB of RAM}$$
- **Always specify decode hints for thumbnails and list tiles**:
  ```dart
  Image.network(
    imageUrl,
    width: 80,
    height: 80,
    fit: BoxFit.cover,
    cacheWidth: 160, // 2x device pixel ratio for sharp rendering
    cacheHeight: 160,
    errorBuilder: (context, error, stackTrace) => const Icon(Icons.broken_image),
  )
  ```
- Alternatively, wrap image providers with `ResizeImage(NetworkImage(url), width: 160)`.

---

## 7. List Scrolling & Lazy Item Construction

- **Always use `ListView.builder` or `ListView.separated`** for unbounded, dynamic, or paginated datasets.
- **Never use `Column(children: items.map(...))`** or `ListView(children: [...])` for large datasets: they eagerly instantiate every child widget in memory simultaneously.
- **Provide explicit `ValueKey`s**:
  ```dart
  ListView.builder(
    itemCount: items.length,
    itemBuilder: (context, index) {
      final item = items[index];
      return UserListTile(
        key: ValueKey(item.id),
        user: item,
      );
    },
  )
  ```

