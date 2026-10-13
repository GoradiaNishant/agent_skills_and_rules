# Reference 09 — Domain-Agnostic Architecture Rules

This skill applies to **any Android application domain**. Do not assume apps
are limited to CRUD, social media, e-commerce, or stock markets.

The same architecture decision process must work for:
- Medical / Healthcare
- Fitness & Wearables
- Hardware synchronization (BLE, USB, NFC)
- IoT
- Productivity & Utility tools
- Scientific & Engineering
- Industrial & Logistics
- Transportation
- Monitoring & Telemetry
- Communication (Chat, VoIP)
- Media (Audio, Video, Camera)
- Location-based
- Offline-first
- Enterprise
- Education
- Financial
- Multi-domain combinations

---

## Domain Detection

Before selecting architecture, identify the project's primary and secondary domains.

```
Primary Domain:   Healthcare
Secondary:        Bluetooth, Wearable sync, Offline data, Authentication
```

```
Primary Domain:   Fitness
Secondary:        Wearable integration, Health data, Background sync, Notifications
```

Do not force the project into a predefined category.

---

## Domain Capability Matrix

Instead of asking: *"Which architecture template?"*
Ask: *"What capabilities does this application require?"*

### Data Sources
REST API · GraphQL · WebSocket · BLE · Bluetooth Classic · USB · NFC · Wi-Fi ·
Local database · File system · Sensors · Camera · Microphone · GPS ·
External SDK · Cloud storage · Device storage

### Communication
Internet · Bluetooth · BLE · NFC · USB · Serial · Wi-Fi · WebSocket ·
MQTT · Push notifications · Local device communication

### State Complexity
Simple UI state · Session state · Persistent state · Offline state ·
Device state · Connection state · Sync state · Background state · Real-time state

### Processing
Basic CRUD · Business rules · Calculations · Data transformation ·
File processing · Image/audio/video processing · Sensor processing ·
Scientific calculations · Financial calculations · Encryption · Hardware protocols

### Background
None · Periodic sync · Continuous synchronization · Background location ·
Background Bluetooth · Background audio · Push handling · Long-running processing

### Security Level
Normal · Authentication · Sensitive personal data · Medical data ·
Financial data · Device credentials · Encryption keys · Certificates ·
Biometric auth · Secure hardware storage

### Reliability
Normal · Offline support · Retry · Reconnection · Data synchronization ·
Conflict resolution · Transaction safety · Recovery after process death ·
Recovery after device reboot

---

## Architecture Derivation Flow

```
Project Requirements
        ↓
Domain(s)
        ↓
Capabilities
        ↓
Complexity Level
        ↓
Architecture
```

A medical app with BLE + offline + API sync requires more infrastructure than
a calculator that needs only Compose + ViewModel.

---

## Capability → Architecture Component Mapping

| Capability | Likely Component |
|-----------|-----------------|
| REST API | Retrofit + OkHttp |
| GraphQL | GraphQL client |
| Local persistence | Room |
| Preferences | DataStore |
| Authentication | Auth repo + secure storage |
| Complex business rules | Domain layer + UseCases |
| Simple business rules | ViewModel / domain functions |
| Real-time data | WebSocket / SSE / MQTT |
| BLE | Hardware abstraction + BleManager |
| USB / Serial | Device abstraction |
| Sensors | Sensor service / repository |
| Background sync | WorkManager |
| Continuous hardware | Foreground service (if justified) |
| Audio / video | Media3 |
| Maps | Maps SDK |
| Camera | CameraX |
| Files | Storage abstraction |
| Notifications | Notification layer |
| Secure secrets | Secure storage / server-side |
| Large lists | Paging |
| Offline-first | Local source of truth + sync |

This table is **guidance, not a mandatory dependency list**.

---

## Architecture Complexity Levels

### Level 1 — Simple
Local-only, few screens, minimal business logic, no complex data layer.
```
ui/
viewmodel/
utils/
```

### Level 2 — Moderate
REST API, authentication, multiple features, some business logic, persistent state.
```
core/   data/   domain/   feature/   navigation/
```

### Level 3 — Complex
Multiple data sources, hardware, real-time, offline-first, complex business rules,
background processing, security requirements.
```
core/   data/   domain/   feature/   hardware/   sync/   navigation/
```

### Level 4 — Enterprise / Highly Complex
Multiple modules, multiple teams, large codebase, strong isolation requirements.
Consider **multi-module Gradle architecture** — but only when genuinely justified.
Do NOT introduce multi-module for a small application merely for scalability.

---

## Multi-Module Decision

Use Gradle modules only when there is a clear reason:
- Large codebase with multiple teams
- Independent feature development
- Build performance
- Strong feature isolation or reusable libraries
- Hardware SDK / domain isolation

Do NOT create `:core :data :domain :network :database :feature-home` for a tiny app.

---

## Domain-Specific Architecture

### Medical / Healthcare

Evaluate: patient data, medical records, vitals, appointments, reports, device measurements.

**Security must include:**
- Secure auth + token storage
- Encryption at rest for sensitive data
- No logging of medical information
- Screenshot protection where required
- Session expiration

**Offline:** Only add if required — local encryption, sync queue, conflict resolution.

**UseCases:** Use for clinical/business workflows with meaningful rules.
Do not create a UseCase for every getter.

---

### Hardware (BLE / USB / NFC / Wi-Fi)

Hardware communication must NOT be placed inside Composables.

```
Compose → ViewModel → UseCase (if business logic) → DeviceRepository → Hardware abstraction → BLE/USB/SDK
```

**For BLE specifically evaluate:**
Scan/Bluetooth/Location permissions · Service UUIDs · Characteristic UUIDs ·
Read/Write/Notify · MTU · Connection lifecycle · Reconnection · Timeout ·
GATT errors · Background restrictions · Battery impact · Data packet parsing

**Device State Machine:** Model connection lifecycle explicitly when complexity warrants it.

```
Disconnected → Scanning → Connecting → Connected → Synchronizing → Ready
```

Failure states: `ConnectionFailed · SyncFailed · DeviceUnavailable · PermissionDenied · Timeout`

Avoid scattered booleans (`isConnected`, `isConnecting`, `isSyncing`) that can produce contradictory states.

---

### Device Synchronization

Define clearly:
- Source of truth
- Sync direction (device → cloud, cloud → device, bidirectional)
- Sync frequency
- Conflict strategy
- Retry strategy
- Failure recovery

```
Device → Parser → Domain model → Room → WorkManager → Backend
```

Do not send raw hardware data from the UI layer.

---

### Tool / Utility Applications

Keep architecture lightweight unless complexity requires otherwise.
A simple tool may only need `Compose + ViewModel + Utility class`.

Do NOT automatically add Domain / UseCases / Repository / Room / Retrofit / Hilt
unless justified by actual requirements.

---

### Sensor Applications

Evaluate: sampling frequency, battery consumption, data volume, background behavior,
data processing, persistence, aggregation, real-time UI.

For high-frequency data:
```
Sensor → SensorManager → Processing → Repository → Database/State → UI
```

Do not update Compose directly from raw sensor callbacks at high frequency.

---

### Scientific / Engineering

Separate business/scientific calculations from UI. Do not embed them in Composables.
Choose appropriate numeric precision — do not blindly use `Double` when precision matters.

---

### IoT Applications

Evaluate: device discovery, device identity, connection, telemetry, commands,
device state, offline state, cloud sync, MQTT/WebSocket, background operation,
battery, security.

Maintain clear separation: Device communication → Domain → Cloud sync → UI.

---

### Financial Applications

Evaluate: decimal precision (use `BigDecimal`), transaction consistency,
authentication, secure storage, idempotency, retry safety, audit requirements,
offline restrictions, network security, session management.

Do NOT retry financial operations blindly — duplicate transactions are a serious error.

---

### Real-Time Applications

Evaluate WebSocket / SSE / Polling / MQTT based on actual requirements.
Consider: connection state, reconnection, backoff, duplicate messages,
ordering, stale data, lifecycle, background behavior.

---

### Enterprise Applications

Evaluate: multiple roles, permissions, org/tenant, audit logging,
remote config, offline mode, sync, feature flags, environment separation.
Do not introduce enterprise architecture assumptions for a small internal tool.

---

## Permission Architecture

Request permissions **when the user reaches the feature that needs them**,
not blindly on app startup.

Create a permission abstraction only when multiple features share permission handling.

---

## Background Execution

First determine the actual requirement type:

| Requirement | Android Mechanism |
|-------------|------------------|
| Immediate foreground task | Coroutine |
| Periodic background task | WorkManager |
| Long-running task | WorkManager / Foreground Service |
| Continuous device connection | Foreground Service |
| Background location | Foreground Service |
| Background audio | Media3 + Foreground Service |
| Push-triggered task | BroadcastReceiver / push handler |

Choose based on actual Android behavior and OS restrictions, not preference.

---

## Multi-Domain Applications

Combine requirements instead of choosing one template.

Example — Healthcare + BLE + REST + Offline + Notifications:
```
Presentation
    ↓
Domain
    ↓
Repositories:  Remote API  |  Room  |  BLE Device
    ↓
WorkManager (sync)   +   Foreground Service (if continuous connection required)
```

Each component is only present if its capability is actually required.

---

## Domain Must NOT Dictate Unnecessary Dependencies

- A medical app does NOT automatically require: Room, WorkManager, BLE, WebSocket
- A hardware app does NOT automatically require: Retrofit, Room, WorkManager
- A stock app does NOT automatically require: WebSocket, Room

Validate the capability before adding any component.

---

## Adaptive Requirement Interview

For unfamiliar domains, ask targeted questions (not a fixed 20-question form).

### First pass (universal)
1. What does the application do?
2. Who uses it?
3. What are the primary user flows?
4. Is data local, remote, hardware-generated, or a combination?
5. Does the app require authentication?
6. Does it need offline functionality?
7. Does it need background operation?
8. Does it communicate with hardware?
9. Does it require real-time updates?
10. Does it handle sensitive data?
11. Are there multiple environments?
12. Is this MVP/prototype or production?
13. Are there regulatory/security requirements?

### Second pass (domain-specific)
After detecting the domain, ask only relevant follow-up questions.

**For BLE/Hardware detected:**
- Is sync one-way or two-way?
- Does the device stream continuously?
- Should syncing continue when backgrounded?
- Can multiple devices be connected?
- What happens if synchronization is interrupted?

**For simple utility detected:**
Do NOT ask about WebSocket, OAuth, Room, background sync — unless relevant.

---

## Architecture Evolution

Start with current requirements, not imaginary future ones.
Avoid decisions that make obvious future expansion unnecessarily difficult.

> **Simple now, extensible where reasonable.**

---

## Universal Decision Flow

```
User Request
     │
     ▼
Understand App
     │
     ▼
Detect Domain(s)
     │
     ▼
Identify Capabilities
     │
  Data?      Hardware?     Background?
  Network    BLE/USB       WorkManager
  Database   Sensors       Service
  Files      SDK           Push
     │
     ▼
Security Needs
     │
     ▼
Complexity Level  (1 → 2 → 3 → 4)
     │
     ▼
Architecture Choice
  Presentation → Domain → Data
  (only layers that are justified)
     │
     ▼
Environment Decision
     │
     ▼
Implementation → Build Verification
```

---

## Golden Rule

The architecture should feel like it was **designed for the project**,
not copied from a template.

Optimize for:
```
Correctness + Maintainability + Security + Testability + Scalability where justified
    MINUS
Unnecessary complexity
```

The final question before adding any architectural component:
> **"Does this application actually need this?"**
