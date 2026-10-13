# Reference 13 — API & Data Layer Patterns

## Table of Contents
- [NetworkResult Architecture](#networkresult-architecture)
- [Retrofit Service Definition](#retrofit-service-definition)
- [Safe DTO & Serialization](#safe-dto--serialization)
- [DTO-to-Domain Mapping](#dto-to-domain-mapping)
- [Repository Implementation](#repository-implementation)
- [Auth Interceptor & Token Refresh](#auth-interceptor--token-refresh)
- [Canonical File Map](#canonical-file-map)

---

## NetworkResult Architecture

Represent all remote network operations through a typed sealed hierarchy:

```kotlin
// core/network/NetworkResult.kt
sealed interface NetworkResult<out T> {
    data class Success<out T>(val data: T, val message: UiText? = null) : NetworkResult<T>
    data class Error(val message: UiText, val code: Int? = null) : NetworkResult<Nothing>
    data class Exception(val throwable: Throwable) : NetworkResult<Nothing>
}

inline fun <T> NetworkResult<T>.onSuccess(action: (value: T) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Success) action(data)
    return this
}

inline fun <T> NetworkResult<T>.onError(action: (message: UiText, code: Int?) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Error) action(message, code)
    return this
}
```

---

## Retrofit Service Definition

```kotlin
// data/remote/api/ProductApi.kt
interface ProductApi {

    @GET("v1/products")
    suspend fun getProducts(
        @Query("page") page: Int,
        @Query("limit") limit: Int = AppConstants.API_PAGE_SIZE,
        @Query("search") query: String? = null
    ): Response<ApiResponse<List<ProductDto>>>

    @GET("v1/products/{id}")
    suspend fun getProductDetail(
        @Path("id") id: String
    ): Response<ApiResponse<ProductDetailDto>>

    @POST("v1/products")
    suspend fun createProduct(
        @Body request: CreateProductRequestDto
    ): Response<ApiResponse<ProductDto>>
}
```

---

## Safe DTO & Serialization

### Resilient JSON Serializer Configuration
Always configure the JSON serializer (Kotlinx Serialization or Moshi) with schema-evolution resilience so backend changes never crash the client:

```kotlin
// core/di/NetworkModule.kt
@Provides
@Singleton
fun provideJson(): Json = Json {
    ignoreUnknownKeys = true    // CRITICAL: prevents crash when backend adds new fields
    coerceInputValues = true    // CRITICAL: prevents crash if null is returned for default values
    isLenient = true            // Relaxed parser for lenient formatting (e.g. unquoted values)
    explicitNulls = false       // Tolerates omitted properties without deserialization errors
}
```

### Safe DTO Structure
```kotlin
// data/remote/dto/ApiResponse.kt
@Serializable
data class ApiResponse<T>(
    @SerialName("status") val status: Boolean? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: T? = null,
    @SerialName("meta") val meta: PaginationMetaDto? = null
)

@Serializable
data class PaginationMetaDto(
    @SerialName("current_page") val currentPage: Int? = null,
    @SerialName("total_pages") val totalPages: Int? = null,
    @SerialName("total_count") val totalCount: Int? = null
)

@Serializable
data class ProductDto(
    @SerialName("id") val id: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("price") val price: Double? = null,
    @SerialName("thumbnail") val thumbnail: String? = null,
    @SerialName("is_available") val isAvailable: Boolean? = null
)
```

**Hard Rules for DTOs & Schema Changes:**
1. **Every DTO field must be nullable (`?`) with `= null` default**: Prevents parsing crashes if backend fields are omitted, renamed, or null.
2. **Never use Enums directly in DTOs**: Keep as `String?` in DTO, and map to a domain enum with a safe `UNKNOWN` fallback.
3. **Never expose DTOs directly to Jetpack Compose UI**: Always map to clean domain models.

---

## DTO-to-Domain Mapping

```kotlin
// data/mapper/ProductMapper.kt
fun ProductDto.toDomain(): Product {
    return Product(
        id = id.orEmpty(),
        title = title.orEmpty().ifBlank { "-" },
        price = price ?: 0.0,
        thumbnailUrl = thumbnail.orEmpty(),
        isAvailable = isAvailable ?: false
    )
}

fun List<ProductDto>?.toDomain(): List<Product> =
    this?.map { it.toDomain() } ?: emptyList()
```

---

## Repository Implementation (Crash-Resilient with Messenger & UiText)

```kotlin
// data/repository/ProductRepositoryImpl.kt
class ProductRepositoryImpl @Inject constructor(
    private val api: ProductApi,
    private val messenger: Messenger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ProductRepository {

    override suspend fun getProducts(page: Int, query: String?): NetworkResult<List<Product>> =
        withContext(ioDispatcher) {
            try {
                val response = api.getProducts(page = page, query = query)
                if (response.isSuccessful) {
                    val body = response.body()
                    val domainItems = body?.data.toDomain()
                    NetworkResult.Success(domainItems, message = body?.message?.let { UiText.str(it) })
                } else {
                    val rawError = response.errorBody()?.string()
                    val apiMessage = parseErrorMessage(rawError)
                    val errorText = UiText.from(apiMessage, R.string.error_generic)
                    messenger.deliver(Message.error(errorText))
                    NetworkResult.Error(errorText, code = response.code())
                }
            } catch (e: SerializationException) {
                // Catch schema changes, unexpected types, or malformed JSON gracefully
                val errorText = UiText.res(R.string.error_response_malformed)
                messenger.deliver(Message.error(errorText))
                NetworkResult.Error(errorText, code = -1)
            } catch (e: IOException) {
                // Offline, DNS, or socket timeout failure
                val errorText = UiText.res(R.string.no_connectivity_message)
                messenger.deliver(Message.error(errorText))
                NetworkResult.Error(errorText, code = null)
            } catch (e: Exception) {
                if (e is CancellationException) throw e // Never swallow coroutine cancellation
                val errorText = UiText.from(e.localizedMessage, R.string.error_generic)
                messenger.deliver(Message.error(errorText))
                NetworkResult.Exception(e)
            }
        }
}
```

---

## Auth Interceptor & Token Refresh

For complete thread-safe token refresh coordination, loop guards, session termination, and global UI messaging, see [`android-network-session-management`](file:///Users/firdos/StudioProjects/agent_skills_and_rules/android_rules_skills/.agents/skills/android-network-session-management/SKILL.md).

```kotlin
// core/network/AuthInterceptor.kt
class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokenProvider.getAccessToken()

        val authenticatedRequest = if (!token.isNullOrBlank()) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }

        return chain.proceed(authenticatedRequest)
    }
}
```

---

## Canonical File Map
- **NetworkResult**: `core/network/NetworkResult.kt`
- **Network Module**: `core/di/NetworkModule.kt`
- **Auth & Session Management**: See `android-network-session-management` skill
  - `core/network/auth/TokenProvider.kt`
  - `core/network/interceptor/AuthInterceptor.kt`
  - `core/network/auth/TokenAuthenticator.kt`
  - `core/session/SessionManager.kt`
  - `core/ui/message/UiMessageDispatcher.kt`
- **Retrofit APIs**: `data/remote/api/<Feature>Api.kt`
- **Response DTOs**: `data/remote/dto/<Feature>Dto.kt`
- **Mappers**: `data/mapper/<Feature>Mapper.kt`
- **Repository Interface**: `domain/repository/<Feature>Repository.kt`
- **Repository Impl**: `data/repository/<Feature>RepositoryImpl.kt`
