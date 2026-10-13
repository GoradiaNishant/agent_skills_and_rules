# Background Execution & Scheduling Rules

@../skills/flutter-background-execution-scheduling/SKILL.md

## Background Workload Governance

1. **Main Isolate Protection**:
   - The main Flutter isolate is responsible for UI rendering (60/120fps).
   - CPU-bound operations exceeding 16ms (e.g. large JSON parsing >100KB, image processing, complex filtering) must be offloaded to a background isolate using `Isolate.run()` or `compute()`.
2. **Persistent Background Scheduling**:
   - For deferrable background work that must execute even when the app is closed, use `workmanager`.
   - All background entry-points must be top-level or static functions annotated with `@pragma('vm:entry-point')`.
   - Initialize `WidgetsFlutterBinding.ensureInitialized()` within background entry points before accessing plugins.
