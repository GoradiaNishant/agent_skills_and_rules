---
name: ui-first-mock-api
description: >-
  Specialized skill for building mobile and frontend user interfaces before backend APIs are ready.
  Enforces contract-first draft design, repository interface abstraction, fake data sources with
  delay and error simulation, DTO vs Domain model separation, complete UI state modeling
  (Loading, Content, Empty, Error), DI-based swapping, and API_ASSUMPTIONS.md documentation.
---

# UI-First Development with Mock APIs Skill

You are an expert mobile and frontend architect specializing in **UI-First Development** when backend services are under construction, incomplete, or unavailable.

---

## 1. Core Principle

> **Never block UI development on backend readiness, and never hardcode mock data inside UI screens.**

Decouple the UI completely from the data source using a **Repository Interface**:

```text
UI (Screen) → ViewModel (StateFlow) → Repository (Interface)
                                             ├── FakeRepository (Active now: local fixtures + delay)
                                             └── RemoteRepository (Active later: Retrofit/Ktor/API)
```

When the real API is deployed, you swap the implementation via a single DI binding or configuration flag. **The UI and ViewModel do not change.**

---

## 2. Mandatory 9-Step Workflow

When tasked with implementing features whose backend is not yet ready, execute these 9 steps in order:

```text
1. Draft Contract ──► 2. Architecture ──► 3. Two-Layer Models ──► 4. 4-State UI
         │
         ▼
5. Fake Data Source ──► 6. Swapping Mechanism ──► 7. Build Screen UI
         │
         ▼
8. ViewModel Tests ──► 9. Backend Integration & Verification
```

### Step 1: Draft the API Contract Early
- Request or draft an OpenAPI/Swagger spec, Postman collection, or JSON schema.
- Define endpoints, HTTP methods, path/query parameters, request payloads, response bodies, and error formats.
- Log every assumption immediately in `API_ASSUMPTIONS.md`.

### Step 2: Establish the Architecture & Directory Structure
- Follow feature-based Clean Architecture or MVVM.
- Organize files cleanly:
  ```text
  feature/<feature_name>/
  ├── data/
  │   ├── fake/
  │   │   ├── Fake<Feature>Repository.kt
  │   │   └── <feature>_mock.json
  │   ├── remote/
  │   │   ├── api/<Feature>Api.kt
  │   │   ├── dto/<Feature>ResponseDto.kt
  │   │   └── <Feature>RemoteDataSource.kt
  │   ├── mapper/<Feature>Mapper.kt
  │   └── repository/<Feature>RepositoryImpl.kt
  ├── domain/
  │   ├── model/<Feature>.kt
  │   └── repository/<Feature>Repository.kt
  ├── presentation/
  │   ├── <Feature>ViewModel.kt
  │   ├── <Feature>UiState.kt
  │   ├── <Feature>UiEvent.kt
  │   ├── <Feature>Route.kt
  │   └── <Feature>Screen.kt
  └── di/<Feature>DataModule.kt
  ```

### Step 3: Define Models in Two Layers
- **Data Transfer Objects (DTOs)** in `data/remote/dto/`:
  - Exactly match the draft JSON response structure.
  - Use Kotlinx Serialization (`@Serializable`) or Moshi.
  - Make uncertain fields nullable (`String?`, `Int?`) and supply safe defaults to prevent parsing breakage.
- **Domain Models** in `domain/model/`:
  - Clean Kotlin data classes containing only fields needed by business logic and UI.
- **Mappers** in `data/mapper/`:
  - Pure extension functions converting DTOs into domain models:
    ```kotlin
    fun UserDto.toDomain(): User = User(
        id = id,
        name = name.orEmpty(),
        email = email.orEmpty(),
        isActive = status?.equals("ACTIVE", ignoreCase = true) == true
    )
    ```

### Step 4: Model Every Screen State Explicitly
A screen must handle all four primary states. Do not design only the "happy path":
```kotlin
sealed interface ScreenUiState<out T> {
    data object Loading : ScreenUiState<Nothing>
    data class Success<T>(val data: T) : ScreenUiState<T>
    data object Empty : ScreenUiState<Nothing>
    data class Error(val message: String, val canRetry: Boolean = true) : ScreenUiState<Nothing>
}
```
Every Screen Composable must explicitly provide:
1. **Loading**: Shimmer placeholder, skeleton, or progress indicator.
2. **Success**: Rendered content using design system components.
3. **Empty**: Descriptive icon, empty-state message, and call-to-action button.
4. **Error**: User-friendly message with a "Retry" button linked to ViewModel intent.

### Step 5: Build the Fake Data Source
Create `Fake<Feature>Repository` implementing `<Feature>Repository`:
- **Simulate realistic network latency**: `delay(700)` or 500–1200ms to reveal race conditions and verify loading states.
- **Load fixtures**: From assets JSON files or in-code Kotlin data objects.
- **Simulate edge cases**: Provide a controllable switch to trigger edge cases on demand:
  ```kotlin
  class FakeCustomerRepository @Inject constructor() : CustomerRepository {
      var simulatedScenario: MockScenario = MockScenario.SUCCESS

      enum class MockScenario {
          SUCCESS,
          EMPTY,
          NETWORK_ERROR,
          SERVER_ERROR_500,
          UNAUTHORIZED_401,
          SLOW_NETWORK
      }

      override suspend fun getCustomers(): Result<List<Customer>> {
          val latency = if (simulatedScenario == MockScenario.SLOW_NETWORK) 3000L else 700L
          delay(latency)

          return when (simulatedScenario) {
              MockScenario.SUCCESS -> Result.success(FakeCustomerFixtures.customerList)
              MockScenario.EMPTY -> Result.success(emptyList())
              MockScenario.NETWORK_ERROR -> Result.failure(IOException("Simulated network timeout"))
              MockScenario.SERVER_ERROR_500 -> Result.failure(HttpException(500, "Internal Server Error"))
              MockScenario.UNAUTHORIZED_401 -> Result.failure(HttpException(401, "Session expired"))
              MockScenario.SLOW_NETWORK -> Result.success(FakeCustomerFixtures.customerList)
          }
      }
  }
  ```

### Step 6: Configure the Swapping Mechanism
Choose one of three swapping strategies:

#### Option A: Hilt DI Module Swapping (Recommended)
Use a build config flag or build flavor to bind either the fake or real repository:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class CustomerRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCustomerRepository(
        impl: CustomerRepositoryImpl // Swap with FakeCustomerRepository when mock is desired
    ): CustomerRepository
}
```
Or conditional provider via `BuildConfig.USE_MOCK_DATA`:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object CustomerRepositoryModule {

    @Provides
    @Singleton
    fun provideCustomerRepository(
        fakeRepo: FakeCustomerRepository,
        remoteRepo: CustomerRepositoryImpl
    ): CustomerRepository {
        return if (BuildConfig.USE_MOCK_DATA) fakeRepo else remoteRepo
    }
}
```

#### Option B: MockWebServer / Network Interceptor
Route OkHttp traffic to a local `MockWebServer` or inject an OkHttp `Interceptor` returning pre-packaged JSON responses for specific endpoints.
- **Benefit**: Exercises the full Retrofit, OkHttp, and JSON serialization stack early.

#### Option C: Build Flavors (`mock` vs `prod`)
```kotlin
flavorDimensions += "environment"
productFlavors {
    create("mock") {
        dimension = "environment"
        buildConfigField("Boolean", "USE_MOCK_DATA", "true")
    }
    create("prod") {
        dimension = "environment"
        buildConfigField("Boolean", "USE_MOCK_DATA", "false")
    }
}
```

### Step 7: Build UI Against the Fake Data
- Implement UI using reusable design system tokens (`MaterialTheme`, typography, colors, standard spacing).
- Ensure seamless transitions between Loading → Content, Empty, and Error states.
- Verify Pull-to-Refresh (`PullToRefreshBox`) and Retry interactions with the fake latency.

### Step 8: Write ViewModel Unit Tests
- Use the fake repository in unit tests without needing complex Mockito/MockK mocking:
  ```kotlin
  @Test
  fun `loadCustomers emits Success when fake returns items`() = runTest {
      val fakeRepo = FakeCustomerRepository().apply {
          simulatedScenario = FakeCustomerRepository.MockScenario.SUCCESS
      }
      val viewModel = CustomerViewModel(fakeRepo)
      
      viewModel.uiState.test {
          assertEquals(CustomerUiState.Loading, awaitItem())
          val successState = awaitItem() as CustomerUiState.Success
          assertEquals(3, successState.customers.size)
      }
  }
  ```

### Step 9: Transition When Real Backend is Ready
When the backend API is published:
1. **Compare JSON Contracts**: Compare live responses with your DTOs in `data/remote/dto/`. Adjust property names, nullabilities, and types.
2. **Implement Remote Data Source**: Finish Retrofit endpoint declarations and real repository implementation (`RemoteRepositoryImpl`).
3. **Configure Authentication**: Attach the OkHttp `AuthInterceptor` and token handling.
4. **Map Error Codes**: Map real backend HTTP error bodies to user-facing messages.
5. **Flip the Switch**: Change `BuildConfig.USE_MOCK_DATA = false` or update Hilt `@Binds`.
6. **Verify Live Edge Cases**: Test loading shimmer, empty screens, and real error states with the live backend.
7. **Retain Fakes**: Keep the fake implementations in `src/test/` or `src/debug/` for automated UI tests and fast Compose Previews.

---

## 3. Mandatory `API_ASSUMPTIONS.md` Format

Whenever developing in UI-First mock mode, you **must** maintain an `API_ASSUMPTIONS.md` document in the feature root or project root:

```markdown
# API Assumptions & Contract Specifications

## Feature: Customer Management
Date: 2026-10-09
Status: Awaiting Backend Verification

### 1. Endpoints Assumed

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| `GET`  | `/api/v1/customers` | Fetch paginated list of customers | Assumed |
| `GET`  | `/api/v1/customers/{id}` | Fetch customer detail | Assumed |
| `POST` | `/api/v1/customers` | Create new customer | Assumed |
| `DELETE` | `/api/v1/customers/{id}` | Delete customer | Assumed |

### 2. Request & Response Shapes

#### `GET /api/v1/customers`
**Expected Response (200 OK):**
```json
{
  "data": [
    {
      "id": "cust_123",
      "name": "Acme Corp",
      "email": "contact@acme.com",
      "status": "ACTIVE",
      "device_count": 5
    }
  ],
  "page": 1,
  "total_pages": 3
}
```

**Known Uncertainties & Fallbacks:**
- `device_count` may be omitted by backend in initial release → Defaulted to `0` in DTO.
- `status` enum string might be lowercase → Mapper uses case-insensitive normalization.

### 3. Error Contract Assumed
**Standard Error Body:**
```json
{
  "code": "CUSTOMER_NOT_FOUND",
  "message": "The requested customer could not be located."
}
```

### 4. Checklist for Backend Integration
- [ ] Verify endpoint base URL and versioning (`/api/v1` vs `/v1`).
- [ ] Verify auth header (`Authorization: Bearer <token>`).
- [ ] Confirm pagination query parameters (`?page=1&size=20` vs `?offset=0&limit=20`).
- [ ] Validate date formats (ISO-8601 UTC assumed).
```

---

## 4. Hard Rules & Best Practices

- **Never** put hardcoded mock objects or static lists inside Screen composables or ViewModels.
- **Never** perform JSON file parsing inside ViewModels or Screens.
- **Always** place mock JSON fixtures and fake repositories under a dedicated `fake/` or `mock/` package.
- **Always** include realistic artificial delay (500–1200ms) in fake repositories to ensure loading UI states are fully exercised.
- **Always** provide a way to simulate empty and error scenarios in fake repositories.
- **Always** use nullable fields with fallback defaults in DTOs to guard against unexpected backend schema differences.
- **Always** record every assumption in `API_ASSUMPTIONS.md` so integration requires verification, not rewrites.

---

## 5. Reusable User Prompt Template

When the user or another agent requests a UI-First implementation, use or recommend this prompt:

```text
I am building a [Android Kotlin + Compose / iOS / Web] feature called [Feature Name].
The backend API is NOT ready yet. I need to build the UI first and integrate the real API later with zero rework on UI screens.

Requirements:
1. Architecture: Clean Architecture / MVVM with feature-based packaging and DI.
2. Data Layer: Define a Repository interface with two implementations:
   - FakeRepository: Reads from local fixtures, includes 700ms simulated latency, and supports configurable error/empty scenarios.
   - RemoteRepository: Retrofit/API stub ready for backend integration.
   - Swapping between them must be controlled via a single DI binding or BuildConfig flag (USE_MOCK_DATA).
3. Models: Separate DTOs (matching draft JSON) from domain models, with mapper functions.
4. UI States: Explicitly model Loading, Content, Empty, and Error states, with retry support.
5. Create API_ASSUMPTIONS.md documenting all assumed endpoints, payloads, and uncertainties.

Screens & Flows to build: [Insert screen list or Figma descriptions].
Draft API Contract: [Insert draft endpoints/JSON, or propose a sensible draft].
```
