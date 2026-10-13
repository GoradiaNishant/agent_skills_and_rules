# Reference 14 — Local Storage Patterns

## Table of Contents
- [Storage Strategy Decision](#storage-strategy-decision)
- [Room Database Pattern](#room-database-pattern)
- [DataStore Preferences Pattern](#datastore-preferences-pattern)
- [Secure Encrypted Storage](#secure-encrypted-storage)
- [Session Clearing on Logout](#session-clearing-on-logout)
- [Canonical File Map](#canonical-file-map)

---

## Storage Strategy Decision

```
Relational, queryable, or offline cached entity data
→ Use Room SQLite (Entities, DAOs, RoomDatabase in data/local/database/).

Simple key-value preferences, UI settings, or flags
→ Use Jetpack DataStore Preferences (data/local/datastore/).

Auth tokens, refresh tokens, encryption keys, or sensitive credentials
→ Use EncryptedSharedPreferences / Android Keystore (core/security/).
```

---

## Room Database Pattern

### Entity
```kotlin
// data/local/database/entity/UserEntity.kt
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String?,
    val updatedAt: Long
)
```

### DAO
```kotlin
// data/local/database/dao/UserDao.kt
@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserById(userId: String): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun clearUsers()
}
```

### RoomDatabase
```kotlin
// data/local/database/AppDatabase.kt
@Database(
    entities = [UserEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}
```

---

## DataStore Preferences Pattern

```kotlin
// data/local/datastore/UserPreferencesDataStore.kt
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore by preferencesDataStore(name = "user_preferences")

    private object PreferencesKeys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_DARK_MODE] ?: false
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE] = enabled
        }
    }
}
```

---

## Secure Encrypted Storage

```kotlin
// core/security/EncryptedStorage.kt
class EncryptedStorage @Inject constructor(
    @ApplicationContext context: Context
) {
    private val sharedPreferences = EncryptedSharedPreferences.create(
        "secure_prefs",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveAccessToken(token: String) {
        sharedPreferences.edit().putString("access_token", token).apply()
    }

    fun getAccessToken(): String? =
        sharedPreferences.getString("access_token", null)

    fun clearAuthTokens() {
        sharedPreferences.edit().remove("access_token").remove("refresh_token").apply()
    }
}
```

---

## Session Clearing on Logout

```kotlin
// data/repository/UserRepositoryImpl.kt
override suspend fun logout() = withContext(ioDispatcher) {
    // 1. Wipe cached database entities
    appDatabase.userDao().clearUsers()

    // 2. Clear secure auth tokens
    encryptedStorage.clearAuthTokens()

    // 3. Keep persistent device settings (like dark mode, language) intact
}
```

---

## Canonical File Map
- **AppDatabase**: `data/local/database/AppDatabase.kt`
- **DAOs**: `data/local/database/dao/<Name>Dao.kt`
- **Entities**: `data/local/database/entity/<Name>Entity.kt`
- **Database Module**: `core/di/DatabaseModule.kt`
- **DataStore**: `data/local/datastore/<Name>DataStore.kt`
- **Encrypted Storage**: `core/security/EncryptedStorage.kt`
