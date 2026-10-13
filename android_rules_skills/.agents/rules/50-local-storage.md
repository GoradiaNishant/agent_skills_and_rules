# Local Storage Rules

@../skills/kotlin-compose-architect/references/03-architecture-decisions.md
@../skills/kotlin-compose-architect/references/07-security.md

## Storage Conventions

1. **Room SQLite Database (`RoomDatabase`)**:
   - Relational data, offline caches, and queryable entities belong in Room (`data/local/database/`).
   - Tables defined using `@Entity(tableName = "...")`, DAOs defined using `@Dao` with Kotlin coroutines (`suspend fun` and `Flow<List<Entity>>`).
   - Define custom complex types with `@TypeConverters`.
   - Provide a singleton instance of the database and DAOs in Hilt's `DatabaseModule`.
   - Do NOT edit generated Room files (`*_Impl.kt`) manually.
   - Never introduce alternative database libraries (Realm, ObjectBox) without clear requirement justification.

2. **Key-Value Persistence (`DataStore`)**:
   - Use Jetpack DataStore (`Preferences DataStore` or `Proto DataStore`) for app preferences, theme choices, and lightweight flags (`data/local/datastore/`).
   - Expose stored values as reactive `Flow<T>`.
   - Never use legacy `android.content.SharedPreferences` for new features.

3. **Secure Storage (Tokens & Credentials)**:
   - Auth tokens, refresh tokens, and sensitive credentials must be encrypted using `EncryptedSharedPreferences` (from `androidx.security:security-crypto`) or stored using the Android Keystore.
   - Never persist sensitive authentication secrets in plaintext DataStore or standard Room tables.

4. **Session Clearing on Logout**:
   - Provide a repository method (e.g. `UserRepository.clearUserSession()`) to wipe sensitive Room tables and reset authentication DataStore keys upon logout.
   - Retain general device preferences (e.g. dark theme setting, language preference) during user logout.
