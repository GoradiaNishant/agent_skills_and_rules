# Reference 14 — Concurrency, Isolates & Native Interoperability Guide

## 1. The Dart Concurrency Model & Event Loop

Dart is fundamentally **single-threaded by default** with an event-driven concurrency model. Understanding the Dart Event Loop is essential for eliminating UI freezes.

```text
┌────────────────────────────────────────────────────────┐
│                   Dart Event Loop                      │
│                                                        │
│   ┌──────────────────────┐   ┌─────────────────────┐   │
│   │   Microtask Queue    │   │     Event Queue     │   │
│   │ (scheduleMicrotask)  │   │  (I/O, Timers, UI)  │   │
│   └──────────┬───────────┘   └──────────┬──────────┘   │
│              │                          │              │
│              ▼                          ▼              │
│   Highest priority tasks     Standard async events     │
└────────────────────────────────────────────────────────┘
```

### Microtask Queue vs. Event Queue:
- **Microtask Queue**: Executed with the highest priority. The event loop empties the *entire* microtask queue before picking up the next item from the Event Queue.
  - Used for internal framework actions and short asynchronous updates (`scheduleMicrotask()`).
  - **Warning**: Scheduling too many microtasks starves the Event Queue, freezing user touch gestures and rendering frames!
- **Event Queue**: Handles external events:
  - User interactions (tap, swipe, scroll).
  - I/O events (network responses, file access).
  - Timers (`Timer`, `Future.delayed`).
  - Platform messages from native Android/iOS channels.

---

## 2. Asynchronous I/O vs. CPU-Heavy Isolates

A common junior/mid-level mistake is assuming that `async` and `await` run work on a background thread.

> **Crucial Rule**: `async` / `await` does **NOT** run code on another thread. It simply pauses execution until an asynchronous operation yields a result, while keeping the thread unblocked for other events. Heavy synchronous computations inside an `async` function still block the main isolate and cause frame drops!

### When to Use Async I/O vs. Isolates:

| Workload | Recommended Approach | Why |
|---|---|---|
| **Network Requests (Dio/HTTP)** | `async` / `await` | I/O wait is non-blocking at the OS level; no heavy CPU calculation. |
| **Database Queries (drift/sqflite)** | `async` / `await` | Native database drivers handle thread pooling natively. |
| **Large JSON Decoding (>100KB)** | **Isolate (`Isolate.run`)** | `jsonDecode()` is a synchronous, CPU-intensive tree traversal that blocks the UI isolate. |
| **Image Compression / Resizing** | **Isolate (`compute`)** | Heavy pixel manipulation stutters animations if run on the main isolate. |
| **Cryptography / Hashing** | **Isolate (`Isolate.run`)** | CPU-intensive mathematical operations. |

### Production Pattern: Offloading with `Isolate.run`
```dart
import 'dart:convert';

class LargeDataParser {
  static Future<List<TransactionModel>> parseTransactions(String rawJson) {
    return Isolate.run(() {
      final List<dynamic> jsonList = jsonDecode(rawJson) as List<dynamic>;
      return jsonList
          .map((item) => TransactionModel.fromJson(item as Map<String, dynamic>))
          .toList();
    });
  }
}
```

---

## 3. Native Platform Interoperability

When communicating with native Android (Kotlin/Java) or iOS (Swift/Objective-C), select between **Platform Channels** and **Dart FFI**:

### Comparison: Platform Channels vs. Dart FFI

| Feature | Platform Channels | Dart FFI (`dart:ffi`) |
|---|---|---|
| **Underlying Mechanism** | Asynchronous IPC via binary message serialization (StandardMessageCodec). | Direct C-ABI dynamic library call in process memory. |
| **Performance / Overhead** | Moderate serialization/deserialization overhead; async event loop delay. | **Zero overhead**; direct memory pointer access, near-native C speed. |
| **Language Target** | Kotlin, Java, Swift, Objective-C. | C, C++, Rust (via `extern "C"`). |
| **Suitability** | OS APIs (Battery, Bluetooth, Permissions, Push notifications, Camera). | High-performance computing, audio processing, machine learning (TensorFlow Lite), encryption. |

---

## 4. Platform Channels Architecture

Flutter provides three distinct channel types:

1. **`MethodChannel` (Request / Response)**:
   - Asynchronous 1-to-1 method invocation.
   - Example: Fetching battery level or triggering a native dialog.
   ```dart
   const platform = MethodChannel('com.example.app/battery');

   Future<int> getBatteryLevel() async {
     try {
       final int result = await platform.invokeMethod('getBatteryLevel');
       return result;
     } on PlatformException catch (e) {
       throw Failure(message: e.message ?? 'Unknown battery error');
     }
   }
   ```

2. **`EventChannel` (Continuous Streaming)**:
   - Exposes a continuous native data stream to Dart as a `Stream<T>`.
   - Example: Gyroscope, accelerometer updates, step counters, network connectivity changes.
   ```dart
   const eventChannel = EventChannel('com.example.app/sensor_stream');

   Stream<SensorData> get sensorStream {
     return eventChannel
         .receiveBroadcastStream()
         .map((event) => SensorData.fromMap(event as Map<dynamic, dynamic>));
   }
   ```

3. **`BasicMessageChannel` (Custom Codec Messaging)**:
   - Bidirectional exchange of semi-structured messages using customizable codecs (`StringCodec`, `BinaryCodec`, `JSONMessageCodec`).

---

## 5. Dart FFI (`dart:ffi`) Capabilities & Boundaries

Dart FFI allows calling native C-compatible libraries directly.

### What FFI Can Call Directly:
- **C functions**: Native dynamic libraries (`.so` on Android, `.dylib`/framework on iOS).
- **C++**: Via `extern "C"` wrappers that prevent C++ name mangling.
- **Rust**: By compiling with `#[no_mangle] pub extern "C" fn ...`.

### What FFI CANNOT Call Directly:
- **Arbitrary Kotlin / Java JVM methods**: FFI cannot inspect the JVM heap directly without going through JNI (Java Native Interface) within a C wrapper.
- **Arbitrary Swift classes/methods**: Swift's high-level runtime metadata and vtables are not exposed via C ABI unless explicitly marked `@_cdecl` or wrapped in C-compatible headers.
