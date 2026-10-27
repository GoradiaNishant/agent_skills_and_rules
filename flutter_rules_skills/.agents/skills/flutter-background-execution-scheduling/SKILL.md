---
name: flutter-background-execution-scheduling
description: "Isolate.run, Workmanager, and background tasks"
trigger: always_on
alwaysApply: true
globs:
  - "**/*.dart"
---

# Flutter Background Execution, Isolates & Scheduling Skill

You are an expert Flutter concurrency, background scheduling, and performance engineer.

## 1. Concurrency Model & Mechanism Selection

Choose the simplest compliant execution model based on the workload:

| Workload Type | Execution Context | Mechanism | When to Use |
|---|---|---|---|
| **Short Async I/O** | Main Isolate Event Loop | `async` / `await`, Streams | Network calls, file read/write, shared preferences |
| **Heavy Computation / CPU-Bound** | Separate Background Isolate | `Isolate.run()` or `compute()` | Large JSON parsing (>100KB), image resizing, crypto/hashing |
| **Persistent / Deferrable Sync** | Native Background Service | `workmanager` | Periodic sync when app is backgrounded/closed |
| **Location / Audio Streaming** | Native Foreground Service | Native platform channels or dedicated background plugins | Real-time GPS tracking, active media playback |

---

## 2. Production Patterns

### 1. Offloading Heavy CPU Work with `Isolate.run`
```dart
// data/parser/large_payload_parser.dart
class LargePayloadParser {
  static Future<List<Product>> parseProductsInIsolate(String rawJson) {
    return Isolate.run(() {
      final decoded = jsonDecode(rawJson) as List<dynamic>;
      return decoded.map((e) => Product.fromJson(e as Map<String, dynamic>)).toList();
    });
  }
}
```

### 2. Workmanager Background Task Entry Point
```dart
@pragma('vm:entry-point')
void callbackDispatcher() {
  Workmanager().executeTask((taskName, inputData) async {
    // 1. Initialize dependencies in isolated Dart context
    WidgetsFlutterBinding.ensureInitialized();
    // 2. Execute background synchronization
    return Future.value(true);
  });
}
```
