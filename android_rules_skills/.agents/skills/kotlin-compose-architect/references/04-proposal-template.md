# Reference 04 — Architecture Proposal Template

Before implementing a substantial project, provide a concise decision summary
in this format. Only include items that actually apply to the project.

---

## Required Pre-Implementation Response

```
Project Type:
[detected project type — e.g., "Medium-sized API-driven social app"]

Complexity:
Small / Medium / Large

Architecture:
[chosen architecture — e.g., "Clean Architecture + feature-based organization"]

Layers:
Presentation → Domain → Data   (or simpler if not needed)

DI:
[Yes — Hilt / No — reason]

Repository:
[Yes — reason / No — reason]

Domain Layer:
[Yes — reason / No — reason]

UseCases:
[Yes — which ones and why / No — reason]

Networking:
[Retrofit + OkHttp + Kotlin Serialization / None]

Local Storage:
[Room / DataStore / None — reason]

Caching:
[Yes / No]

Background Work:
[WorkManager / None]

Real-time:
[WebSocket / SSE / Polling / None]

Navigation:
[Navigation Compose / None]

Environment:
[dev / staging / prod / single environment]

Flavors:
[Yes / No]

Build Types:
[debug / release]

Security:
[required considerations — secure storage, HTTPS, etc.]

Tests:
Not generated unless explicitly requested.

Project Structure:
[show the actual folder tree]
```

---

## Example — Medium API-Driven Application

```
Project Type:
Medium-sized API-driven community application

Complexity:
Medium

Architecture:
Clean Architecture + feature-based organization

Layers:
Presentation → Domain → Data

DI:
Yes — Hilt (meaningful dependency graph with API, DB, repos)

Repository:
Yes — remote API + local Room cache

Domain Layer:
Yes — business rules for feed ranking, comment submission

UseCases:
Yes — SubmitCommentUseCase, FetchFeedUseCase, LoginUserUseCase

Networking:
Retrofit + OkHttp + Kotlin Serialization

Local Storage:
Room — offline cache for feed items

Caching:
Yes — Room as local source of truth

Background Work:
None initially

Real-time:
None initially (polling acceptable)

Navigation:
Navigation Compose

Environment:
dev / prod (two flavors)

Flavors:
Yes — dev, prod

Build Types:
debug, release

Security:
Secure token storage (EncryptedSharedPreferences), HTTPS enforced

Tests:
Not generated unless explicitly requested.

Project Structure:
app/
├── core/
│   ├── network/
│   ├── database/
│   └── designsystem/
├── data/
│   ├── remote/
│   ├── local/
│   ├── repository/
│   └── mapper/
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
├── feature/
│   ├── auth/
│   ├── feed/
│   ├── profile/
│   └── settings/
├── navigation/
└── MainActivity.kt
```

---

## Example — Small Utility Application

```
Project Type:
Simple single-screen calculator utility

Complexity:
Small

Architecture:
Single-module, UI + ViewModel only

DI:
No — no meaningful dependency graph

Repository:
No — no external data source

Domain Layer:
No

UseCases:
No

Networking:
None

Local Storage:
None

Navigation:
None (single screen)

Environment:
Single (debug/release build types only)

Tests:
Not generated unless explicitly requested.

Project Structure:
app/
├── ui/
│   ├── CalculatorScreen.kt
│   └── CalculatorViewModel.kt
├── theme/
└── MainActivity.kt
```
