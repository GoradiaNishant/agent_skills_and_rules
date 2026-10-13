# Reference 11 — Utils, Helpers, Extensions & Constants

This reference defines the standard patterns for all utility classes, extension
functions, helper objects, and constants in a Kotlin/Compose Android project.

**Core rule:** Search before creating. Reuse before duplicating. Never scatter
helper logic across feature packages — place it in the appropriate `core/` location.

---

## Standard Directory Structure

```
app/src/main/java/<package>/
└── core/
    ├── constants/
    │   └── AppConstants.kt          ← static app-wide constants
    ├── extensions/
    │   ├── StringExtensions.kt      ← String utility extensions
    │   ├── DateExtensions.kt        ← Date/time formatting extensions
    │   ├── ContextExtensions.kt     ← Context/Activity/Fragment helpers
    │   ├── FlowExtensions.kt        ← Coroutine Flow extensions
    │   ├── ModifierExtensions.kt    ← Compose Modifier helpers (if needed)
    │   └── NumberExtensions.kt      ← Int/Long/Double formatting helpers
    ├── utils/
    │   ├── DateTimeUtils.kt         ← Date/time parsing and formatting logic
    │   ├── CoroutineUtils.kt        ← Coroutine scopes, dispatchers, helpers
    │   └── ValidationUtils.kt       ← Input validation functions
    └── res/
        └── strings/
            └── strings.xml          ← ALL user-visible strings (localisation)
```

Adjust to the actual project's package structure. Do not create this directory
tree for a trivial app — only create what is actually needed.

---

## 1. AppConstants

Centralise all static values that are referenced across the app.

```kotlin
// core/constants/AppConstants.kt
object AppConstants {

    // API / Network
    const val API_TIMEOUT_SECONDS = 30L
    const val API_PAGE_SIZE = 20

    // Pagination
    const val INITIAL_PAGE = 1
    const val PREFETCH_DISTANCE = 5

    // Local storage keys (DataStore / EncryptedPreferences)
    const val KEY_AUTH_TOKEN = "auth_token"
    const val KEY_REFRESH_TOKEN = "refresh_token"
    const val KEY_USER_ID = "user_id"
    const val KEY_THEME = "theme_preference"
    const val KEY_LANGUAGE = "language_preference"

    // Date/time formats (use only in DateTimeUtils — not scattered around)
    const val FORMAT_DATE_DISPLAY = "dd MMM yyyy"
    const val FORMAT_TIME_DISPLAY = "hh:mm a"
    const val FORMAT_DATETIME_DISPLAY = "dd MMM yyyy, hh:mm a"
    const val FORMAT_DATE_API = "yyyy-MM-dd"
    const val FORMAT_DATETIME_API = "yyyy-MM-dd'T'HH:mm:ss'Z'"

    // Debounce
    const val DEBOUNCE_SEARCH_MS = 400L

    // Validation
    const val MIN_PASSWORD_LENGTH = 8
    const val MAX_PASSWORD_LENGTH = 64
    const val MAX_NAME_LENGTH = 100

    // Other project-wide values
    // Add here — never hardcode values inside feature code
}
```

**Rules:**
- Never hardcode URLs, API keys, tokens, or credentials here. Use `BuildConfig` + `local.properties`.
- Never scatter `const val` declarations inside ViewModel, Repository, or UI files.
- Keep constants organised with comments by category.

---

## 2. String Extensions

```kotlin
// core/extensions/StringExtensions.kt

/**
 * Returns null if the string is null, blank, or equals "null" (API safety).
 */
fun String?.orNullIfEmpty(): String? =
    if (isNullOrBlank() || this == "null") null else this

/**
 * Returns the fallback string if this string is null or blank.
 */
fun String?.orDefault(default: String = "-"): String =
    if (isNullOrBlank()) default else this!!

/**
 * Capitalises the first letter of each word.
 */
fun String.toTitleCase(): String =
    split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercaseChar() }
    }

/**
 * Masks a string for display (e.g., email, phone).
 * Example: "user@email.com" → "us**@email.com"
 */
fun String.maskEmail(): String {
    val parts = split("@")
    if (parts.size != 2) return this
    val name = parts[0]
    val masked = name.take(2) + "*".repeat((name.length - 2).coerceAtLeast(0))
    return "$masked@${parts[1]}"
}

/**
 * Safe integer parsing — returns null instead of throwing.
 */
fun String?.toIntOrNull(): Int? = this?.let {
    try { it.trim().toInt() } catch (e: NumberFormatException) { null }
}

/**
 * Removes all non-digit characters from a string.
 */
fun String.digitsOnly(): String = filter { it.isDigit() }

/**
 * Returns true if the string is a valid email format.
 */
fun String.isValidEmail(): Boolean =
    android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()

/**
 * Returns true if the string is a valid phone number (digits, optional +, 7-15 chars).
 */
fun String.isValidPhone(): Boolean =
    matches(Regex("^\\+?[0-9]{7,15}$"))
```

**Rules:**
- All user-visible strings must come from `strings.xml` — never hardcode them here.
- Extension functions are for transformation/validation logic only, not for UI text.

---

## 3. Date & Time Utils and Extensions

Keep all date/time logic in one place. Never scatter `SimpleDateFormat` or
`DateTimeFormatter` calls across feature code.

```kotlin
// core/utils/DateTimeUtils.kt
import java.text.SimpleDateFormat
import java.util.*

object DateTimeUtils {

    // Use thread-local formatters if performance matters on high-frequency calls
    private fun formatter(pattern: String, locale: Locale = Locale.getDefault()) =
        SimpleDateFormat(pattern, locale)

    /**
     * Parses an ISO-8601 API date string to a Date object.
     * Returns null if parsing fails.
     */
    fun parseApiDate(dateString: String?): Date? {
        if (dateString.isNullOrBlank()) return null
        return try {
            formatter(AppConstants.FORMAT_DATETIME_API, Locale.US).parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parses a date-only API string (yyyy-MM-dd).
     */
    fun parseApiDateOnly(dateString: String?): Date? {
        if (dateString.isNullOrBlank()) return null
        return try {
            formatter(AppConstants.FORMAT_DATE_API, Locale.US).parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formats a Date to a display-friendly date string.
     * Example: "25 Sep 2025"
     */
    fun formatDisplayDate(date: Date?): String =
        date?.let { formatter(AppConstants.FORMAT_DATE_DISPLAY).format(it) } ?: "-"

    /**
     * Formats a Date to a display-friendly time string.
     * Example: "10:30 AM"
     */
    fun formatDisplayTime(date: Date?): String =
        date?.let { formatter(AppConstants.FORMAT_TIME_DISPLAY).format(it) } ?: "-"

    /**
     * Formats a Date to a full datetime display string.
     * Example: "25 Sep 2025, 10:30 AM"
     */
    fun formatDisplayDateTime(date: Date?): String =
        date?.let { formatter(AppConstants.FORMAT_DATETIME_DISPLAY).format(it) } ?: "-"

    /**
     * Formats a Date to API format (yyyy-MM-dd'T'HH:mm:ss'Z').
     */
    fun formatApiDateTime(date: Date?): String =
        date?.let { formatter(AppConstants.FORMAT_DATETIME_API, Locale.US).format(it) } ?: ""

    /**
     * Returns today's date with time set to 00:00:00.
     */
    fun today(): Date = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    /**
     * Returns true if the given date is in the past.
     */
    fun isPast(date: Date): Boolean = date.before(Date())

    /**
     * Returns relative time label ("Today", "Yesterday", "25 Sep 2025").
     */
    fun relativeDate(date: Date?, context: android.content.Context): String {
        if (date == null) return "-"
        val today = today()
        val yesterday = Calendar.getInstance().apply {
            time = today
            add(Calendar.DAY_OF_YEAR, -1)
        }.time
        return when {
            date >= today -> context.getString(R.string.label_today)
            date >= yesterday -> context.getString(R.string.label_yesterday)
            else -> formatDisplayDate(date)
        }
    }
}
```

```kotlin
// core/extensions/DateExtensions.kt

/**
 * Convenience extension to format a Date directly.
 */
fun Date?.toDisplayDate(): String = DateTimeUtils.formatDisplayDate(this)
fun Date?.toDisplayTime(): String = DateTimeUtils.formatDisplayTime(this)
fun Date?.toDisplayDateTime(): String = DateTimeUtils.formatDisplayDateTime(this)
fun Date?.toApiDateTime(): String = DateTimeUtils.formatApiDateTime(this)

/**
 * Parses an API date string directly on a String.
 */
fun String?.parseApiDate(): Date? = DateTimeUtils.parseApiDate(this)
fun String?.parseApiDateOnly(): Date? = DateTimeUtils.parseApiDateOnly(this)
```

**Rules:**
- Do not use deprecated `java.util.Date` APIs for new arithmetic — use `Calendar` or `java.time` on API 26+.
- On API 26+ projects, prefer `java.time` (`LocalDate`, `LocalDateTime`, `ZonedDateTime`) over `SimpleDateFormat`.
- Never use `SimpleDateFormat` without specifying a `Locale` — always pass `Locale.US` for API formats and `Locale.getDefault()` for display formats.
- All display strings such as "Today", "Yesterday" must come from `strings.xml`.

---

## 4. Coroutine Utils & Extensions

```kotlin
// core/utils/CoroutineUtils.kt
import kotlinx.coroutines.*

/**
 * Launches a coroutine that catches and logs exceptions without crashing.
 * Use for fire-and-forget operations where failures should not surface to the user.
 */
fun CoroutineScope.launchSafe(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    onError: (Throwable) -> Unit = {},
    block: suspend CoroutineScope.() -> Unit
): Job = launch(dispatcher + CoroutineExceptionHandler { _, throwable ->
    onError(throwable)
}) {
    block()
}

/**
 * Executes a suspending block and returns a Result<T>, never throwing.
 */
suspend fun <T> runSuspendCatching(block: suspend () -> T): Result<T> =
    try { Result.success(block()) } catch (e: CancellationException) { throw e } catch (e: Throwable) { Result.failure(e) }
```

```kotlin
// core/extensions/FlowExtensions.kt
import kotlinx.coroutines.flow.*

/**
 * Emits items only after the given debounce period.
 * Standard usage for search input.
 */
fun <T> Flow<T>.debounceSearch(): Flow<T> =
    debounce(AppConstants.DEBOUNCE_SEARCH_MS)

/**
 * Catches exceptions and maps them to null, allowing the flow to continue.
 */
fun <T> Flow<T>.catchToNull(): Flow<T?> = map<T, T?> { it }.catch { emit(null) }

/**
 * Retries on IO exceptions up to [times] times with [delayMs] between attempts.
 */
fun <T> Flow<T>.retryOnIoError(
    times: Int = 3,
    delayMs: Long = 1000L
): Flow<T> = retryWhen { cause, attempt ->
    if (cause is java.io.IOException && attempt < times) {
        kotlinx.coroutines.delay(delayMs)
        true
    } else false
}
```

**Rules:**
- Never swallow `CancellationException` — always rethrow it.
- `Dispatchers.IO` is for blocking I/O (network, file, database). Do not use it for CPU-heavy work — use `Dispatchers.Default`.
- Avoid `GlobalScope` — always use a scoped `CoroutineScope` (ViewModel, repository, service).

---

## 5. Context & Activity Extensions

```kotlin
// core/extensions/ContextExtensions.kt
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes

/**
 * Shows a short Toast message.
 */
fun Context.showToast(@StringRes messageRes: Int) =
    Toast.makeText(this, getString(messageRes), Toast.LENGTH_SHORT).show()

fun Context.showToast(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

/**
 * Opens a URL in the device browser.
 */
fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        // Handle unsupported scheme or no browser
    }
}

/**
 * Dials a phone number.
 */
fun Context.dialPhone(number: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
    startActivity(intent)
}

/**
 * Opens email client with pre-filled address.
 */
fun Context.sendEmail(email: String, subject: String = "") {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:$email")
        putExtra(Intent.EXTRA_SUBJECT, subject)
    }
    startActivity(intent)
}

/**
 * Returns true if a network connection is available.
 */
fun Context.isNetworkAvailable(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE)
            as android.net.ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

/**
 * Converts dp to pixels.
 */
fun Context.dpToPx(dp: Float): Int =
    (dp * resources.displayMetrics.density).toInt()
```

---

## 6. Number Extensions

```kotlin
// core/extensions/NumberExtensions.kt

/**
 * Formats a Double/Float as a price string with 2 decimal places.
 * Example: 1234.5 → "1,234.50"
 */
fun Double?.formatPrice(currency: String = ""): String {
    if (this == null) return "-"
    val formatted = String.format("%,.2f", this)
    return if (currency.isNotBlank()) "$currency $formatted" else formatted
}

/**
 * Returns a safe display value for a nullable Int.
 */
fun Int?.orZero(): Int = this ?: 0
fun Long?.orZero(): Long = this ?: 0L
fun Double?.orZero(): Double = this ?: 0.0

/**
 * Clamps a value between min and max.
 */
fun Int.clamp(min: Int, max: Int): Int = coerceIn(min, max)

/**
 * Converts minutes to "Xh Ym" format.
 * Example: 135 → "2h 15m"
 */
fun Int.toHoursAndMinutes(): String {
    val h = this / 60
    val m = this % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}
```

---

## 7. Validation Utils

```kotlin
// core/utils/ValidationUtils.kt

object ValidationUtils {

    fun isValidEmail(email: String): Boolean =
        email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    fun isValidPassword(password: String): Boolean =
        password.length >= AppConstants.MIN_PASSWORD_LENGTH

    fun isStrongPassword(password: String): Boolean =
        password.length >= AppConstants.MIN_PASSWORD_LENGTH &&
        password.any { it.isDigit() } &&
        password.any { it.isLetter() } &&
        password.any { !it.isLetterOrDigit() }

    fun isValidPhone(phone: String): Boolean =
        phone.matches(Regex("^\\+?[0-9]{7,15}$"))

    fun isValidName(name: String): Boolean =
        name.isNotBlank() && name.length <= AppConstants.MAX_NAME_LENGTH

    fun isValidUrl(url: String): Boolean =
        android.util.Patterns.WEB_URL.matcher(url).matches()

    /**
     * Returns a user-facing validation error string resource ID, or null if valid.
     * Use with stringResource() in Compose.
     */
    fun emailError(email: String): Int? = when {
        email.isBlank() -> R.string.error_email_required
        !isValidEmail(email) -> R.string.error_email_invalid
        else -> null
    }

    fun passwordError(password: String): Int? = when {
        password.isBlank() -> R.string.error_password_required
        !isValidPassword(password) -> R.string.error_password_too_short
        else -> null
    }
}
```

---

## 8. String Resources — Localisation Policy

**All user-visible strings must live in `res/values/strings.xml`.**

This includes: labels, messages, titles, error messages, button text, hints,
empty state text, status messages, accessibility descriptions, toast messages.

### Mandatory Naming Convention

```xml
<!-- res/values/strings.xml -->
<resources>

    <!-- App -->
    <string name="app_name">MyApp</string>

    <!-- Labels — prefix: label_ -->
    <string name="label_email">Email</string>
    <string name="label_password">Password</string>
    <string name="label_phone">Phone Number</string>
    <string name="label_name">Full Name</string>
    <string name="label_today">Today</string>
    <string name="label_yesterday">Yesterday</string>
    <string name="label_loading">Loading…</string>
    <string name="label_retry">Retry</string>
    <string name="label_cancel">Cancel</string>
    <string name="label_confirm">Confirm</string>
    <string name="label_save">Save</string>
    <string name="label_delete">Delete</string>
    <string name="label_edit">Edit</string>
    <string name="label_search">Search</string>
    <string name="label_filter">Filter</string>
    <string name="label_clear">Clear</string>
    <string name="label_close">Close</string>
    <string name="label_back">Back</string>
    <string name="label_next">Next</string>
    <string name="label_done">Done</string>
    <string name="label_submit">Submit</string>
    <string name="label_continue">Continue</string>
    <string name="label_skip">Skip</string>
    <string name="label_logout">Log Out</string>
    <string name="label_na">N/A</string>
    <string name="label_unknown">Unknown</string>
    <string name="label_or">or</string>

    <!-- Titles — prefix: title_ -->
    <string name="title_home">Home</string>
    <string name="title_profile">Profile</string>
    <string name="title_settings">Settings</string>

    <!-- Messages — prefix: msg_ -->
    <string name="msg_no_internet">No internet connection. Please check your network and try again.</string>
    <string name="msg_server_error">Something went wrong. Please try again later.</string>
    <string name="msg_timeout">The request timed out. Please try again.</string>
    <string name="msg_session_expired">Your session has expired. Please log in again.</string>
    <string name="msg_generic_error">An unexpected error occurred.</string>
    <string name="msg_success">Done successfully.</string>
    <string name="msg_logout_confirm">Are you sure you want to log out?</string>
    <string name="msg_delete_confirm">Are you sure you want to delete this?</string>
    <string name="msg_no_results">No results found.</string>
    <string name="msg_empty_list">Nothing to show here.</string>
    <string name="msg_coming_soon">Coming soon.</string>

    <!-- Errors — prefix: error_ -->
    <string name="error_email_required">Please enter your email address.</string>
    <string name="error_email_invalid">Please enter a valid email address.</string>
    <string name="error_password_required">Please enter your password.</string>
    <string name="error_password_too_short">Password must be at least 8 characters.</string>
    <string name="error_passwords_do_not_match">Passwords do not match.</string>
    <string name="error_field_required">This field is required.</string>
    <string name="error_invalid_phone">Please enter a valid phone number.</string>
    <string name="error_name_too_long">Name must be %d characters or fewer.</string>
    <string name="error_permission_denied">Permission denied. Please allow access in Settings.</string>
    <string name="error_bluetooth_disabled">Bluetooth is disabled. Please enable it to continue.</string>
    <string name="error_location_disabled">Location is disabled. Please enable it to continue.</string>
    <string name="error_device_not_found">Device not found. Please check that the device is powered on and nearby.</string>
    <string name="error_connection_failed">Connection failed. Please try again.</string>
    <string name="error_sync_failed">Synchronisation failed. Please try again.</string>

    <!-- Hints — prefix: hint_ -->
    <string name="hint_email">Enter your email</string>
    <string name="hint_password">Enter your password</string>
    <string name="hint_search">Search…</string>

    <!-- Accessibility — prefix: cd_ (content description) -->
    <string name="cd_back_button">Go back</string>
    <string name="cd_close_button">Close</string>
    <string name="cd_profile_image">Profile picture</string>
    <string name="cd_loading">Loading</string>

    <!-- Placeholders with format args -->
    <string name="label_items_count">%d items</string>
    <string name="msg_welcome_user">Welcome, %s!</string>
    <plurals name="label_item_count">
        <item quantity="one">%d item</item>
        <item quantity="other">%d items</item>
    </plurals>

</resources>
```

### Rules
- **Prefix all keys** with their semantic category (`label_`, `title_`, `msg_`, `error_`, `hint_`, `cd_`).
- **Never concatenate** string resources — use format args (`%s`, `%d`) or `getString(R.string.x, arg)`.
- **Never hardcode** user-visible text in Kotlin or Compose files.
- For language-specific overrides, create `res/values-<lang>/strings.xml` (e.g., `values-ar/`, `values-fr/`).
- RTL support: Add `android:supportsRtl="true"` in `AndroidManifest.xml` and test with `values-ar`.
- Use plurals for any count-based text.

---

## 9. Compose String Usage

In Compose, always use:
```kotlin
Text(text = stringResource(R.string.label_email))
Text(text = stringResource(R.string.msg_welcome_user, userName))
Text(text = pluralStringResource(R.plurals.label_item_count, count, count))
```

Never:
```kotlin
Text(text = "Email")           // ❌ hardcoded
Text(text = "Welcome, $name")  // ❌ hardcoded
```

---

## 10. AppImages / AppIcons Constants

Keep all drawable/asset path references centralised.

```kotlin
// core/constants/AppImages.kt
object AppImages {
    // Drawables
    val icLogo = R.drawable.ic_logo
    val icPlaceholder = R.drawable.ic_placeholder
    val imgOnboarding1 = R.drawable.img_onboarding_1
    val imgOnboarding2 = R.drawable.img_onboarding_2
    // Add project assets here
}
```

Never reference `R.drawable.*` scattered across UI files — always go through `AppImages`.

---

## 11. Reuse-First Policy for Utils

Before creating any new utility function or extension, search:

1. `core/extensions/` — does an extension already exist?
2. `core/utils/` — does a utility class already provide this?
3. `core/constants/AppConstants` — is the value already defined?
4. Kotlin standard library — does Kotlin already provide this? (`takeIf`, `let`, `run`, `also`, `apply`, `coerceIn`, `filter`, `mapNotNull`, etc.)

**Do not create a utility if the Kotlin stdlib already provides it.**

Do not create:
```kotlin
fun <T> T?.isNotNull(): Boolean = this != null   // ❌ use != null directly
fun String.isNotNullOrEmpty(): Boolean = ...     // ❌ use isNotNullOrBlank()
```

---

## 12. Compose-Specific Modifier Extensions

Only create these when a pattern is genuinely repeated across multiple screens.

```kotlin
// core/extensions/ModifierExtensions.kt
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Applies standard card-corner rounding.
 */
fun Modifier.cardShape(): Modifier = clip(RoundedCornerShape(12.dp))

/**
 * Makes an element clickable without ripple (for non-interactive visual elements).
 */
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    clickable(
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
```

Do not create Modifier extensions for patterns used in only one place.

---

## 13. Utils Placement Decision

| What it is | Where it lives |
|-----------|---------------|
| App-wide static constants | `core/constants/AppConstants.kt` |
| Drawable/asset references | `core/constants/AppImages.kt` |
| String manipulation / validation extensions | `core/extensions/StringExtensions.kt` |
| Date parsing / formatting logic | `core/utils/DateTimeUtils.kt` |
| Date convenience extensions | `core/extensions/DateExtensions.kt` |
| Coroutine helpers and scope utilities | `core/utils/CoroutineUtils.kt` |
| Flow transformation extensions | `core/extensions/FlowExtensions.kt` |
| Context / Activity helpers | `core/extensions/ContextExtensions.kt` |
| Number formatting helpers | `core/extensions/NumberExtensions.kt` |
| Compose Modifier helpers | `core/extensions/ModifierExtensions.kt` |
| Input validation logic | `core/utils/ValidationUtils.kt` |
| All user-visible text | `res/values/strings.xml` |
| Feature-specific logic | Inside the feature package only |

---

## 14. Hard Rules

- **Never** hardcode user-visible text, error messages, labels, or hints in Kotlin or Compose files.
- **Never** create a `SimpleDateFormat` directly inside a ViewModel, Repository, or Composable.
- **Never** create a `Calendar` or date computation inside a Composable.
- **Never** scatter `const val` declarations across feature packages.
- **Never** duplicate an extension or utility that already exists in `core/`.
- **Never** add project-wide logic to a `companion object` inside a feature ViewModel.
- **Never** use `GlobalScope` for coroutines.
- **Never** swallow `CancellationException` in coroutine error handlers.
- **Never** pass raw string literals as `contentDescription` — use `stringResource()`.
- **Always** specify `Locale` when constructing `SimpleDateFormat`.
- **Always** use `stringResource()` in Compose for any user-visible text.
- **Always** add new strings to `strings.xml` with the correct prefix and semantic name.
