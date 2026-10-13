# List, Search & Pagination Patterns

@../skills/flutter-ui-standards/SKILL.md

## List & Scroll Performance Governance

1. **Lazy Item Construction**:
   - Always use `ListView.builder` or `ListView.separated` for dynamic lists.
   - Never use `Column(children: items.map(...))` or `ListView(children: [...])` for unbound or long lists, as they instantiate all children immediately in memory.
2. **Key Stabilization**:
   - Provide explicit, unique keys for dynamic list items (`key: ValueKey(item.id)`). This prevents widget reuse visual bugs during sorting or deletions.
3. **Pagination & Infinite Scrolling**:
   - Attach a `ScrollController` with threshold detection (e.g. `scrollPosition >= maxScrollExtent - 200`) to trigger next page fetches.
   - Guard against duplicate fetches with an `isLoadingMore` flag.
4. **Pull-to-Refresh**:
   - Wrap scrollable lists with `RefreshIndicator` and return a `Future` completing after data refresh.
5. **Complex Scrolling Layouts**:
   - Use `CustomScrollView` with `SliverAppBar`, `SliverToBoxAdapter`, and `SliverList.builder` for collapsible headers and multi-section layouts.
