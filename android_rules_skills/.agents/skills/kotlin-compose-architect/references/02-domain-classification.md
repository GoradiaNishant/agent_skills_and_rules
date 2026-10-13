# Reference 02 — Domain Classification

Classify the application into one or more categories to determine which
infrastructure and patterns are likely required. Then remove anything that
is not actually needed by the specific project.

---

## A. Simple CRUD / API Application

Examples: news app, blog, public content, basic catalog.

**Typical needs:**
- REST API (GET/POST/PUT/DELETE)
- Authentication if required
- Pagination, search/filter
- Basic caching
- Loading / error / empty states

Architecture should remain relatively lightweight.

---

## B. Social / Public Engagement Application

Examples: community app, social feed, comments, likes, user profiles, notifications.

**Evaluate:**
- Authentication + user profile
- Feed with pagination
- Media upload
- Push notifications
- Local caching
- Optimistic UI where appropriate

Use cases become useful when business rules are non-trivial.

---

## C. E-Commerce Application

**Evaluate:**
- Auth, product catalog, search, categories
- Cart, wishlist, orders, payments, address management
- Order tracking, coupons, inventory, notifications

**Consider:**
- Repository abstraction
- Use cases for business operations (PlaceOrderUseCase, etc.)
- Local database + remote API
- Payment abstraction, error mapping, session handling

---

## D. Stock / Financial Market Application

If the project involves stocks, trading, portfolio, market data, watchlists, charts, orders, or financial transactions:

**Automatically evaluate:**
- REST API + WebSocket/streaming
- Authentication + secure token storage
- Local database + caching
- Offline/read-only fallback, pagination
- Real-time state updates + background sync
- Market/session state, time-sensitive data
- **Decimal-safe financial calculations** — use `BigDecimal`, NOT `Float`/`Double`
- Domain validation, error handling, retry policies
- Rate limiting, network state, data freshness
- Secure storage, potential biometric auth
- Notification infrastructure, observability/logging

Do NOT treat this as a financial app merely because it shows stock prices.
Evaluate actual features required.

---

## E. Real-Time Application

Examples: chat, live tracking, live sports, live market data, delivery tracking.

**Evaluate:**
- WebSocket / SSE / polling — choose the simplest suitable mechanism
- Connection lifecycle, reconnection, backoff, connection state
- Foreground/background behavior
- Local cache, event ordering, duplicate events, state synchronization

Do not use WebSockets merely because they are available.

---

## F. Offline-First Application

If the application must work without internet:

**Consider:**
- Room as local source of truth
- Sync mechanism + conflict resolution
- WorkManager for background sync
- Network monitoring, pending operations, retry strategy

Standard layer flow:
```
UI → ViewModel → UseCase (if required) → Repository → Local / Remote
```

---

## G. Media / Audio / Video Application

**Evaluate:**
- Media3 / ExoPlayer
- Background playback, media session, audio focus
- Notifications, caching, lifecycle, playback state

Only add these when actually required.

---

## H. Location / Maps Application

**Evaluate:**
- Location permissions (runtime)
- Google Maps or map provider
- Background location (only if explicitly required)
- Geocoding, location caching, battery impact, privacy

---

## I. Authentication-Heavy Application

**Evaluate:**
- Login, registration, social login
- Token management, refresh token, secure storage
- Session expiration, logout, multi-account
- Biometric authentication, deep links, email/OTP

Authentication logic must NOT live in Composables.

---

## Domain-Aware Automatic Decisions

### Stock Market
Likely: REST, WebSocket, Room, Secure Storage, Hilt, ViewModel, Repository, UseCases, financial precision, caching, background sync, notifications, error handling.

### Blog / Public Content
Likely: Retrofit, OkHttp, Serialization, Repository, ViewModel, Navigation, Pagination, image loading.
Probably NOT: WebSocket, Room, complex UseCase layer, background sync.

### Simple Utility App
Likely: Compose, ViewModel (if state requires it), DataStore (if persistence needed).
Probably NOT: Repository, UseCase, Room, Retrofit, WebSocket.

### Chat Application
Likely: Auth, REST, WebSocket, Room, Repository, UseCases (business logic), Notifications, background handling, connection management, Paging.

### E-Commerce
Likely: Auth, REST, Room/cache, Repository, UseCases, cart, orders, payments, notifications, secure storage, Paging.
