---
name: 50-local-storage
description: "Drift (Moor), Hive, and shared_preferences storage"
trigger: always_on
alwaysApply: true
globs:
  - "**/local/**"
  - "**/storage/**"
  - "**/database/**"
---

# Local Storage & Caching Rules

@../skills/flutter-architect/references/07-security.md

## Storage Selection & Governance

1. **Storage Mechanism Classification**:
   - **Sensitive Data & Auth Tokens**: MUST use `flutter_secure_storage` (backed by Android Keystore and iOS Keychain). Never use `shared_preferences` for secrets.
   - **Key-Value Settings**: Use `shared_preferences` only for non-sensitive preferences (e.g. theme mode, language selection, onboarding flags).
   - **Structured Offline Data / Relational Cache**: Use `sqflite`, `hive`, or `isar` with explicit entity serialization.
2. **Encrypted Caches**:
   - Local databases storing user medical, personal, or financial records must be encrypted using SQLCipher or Hive encrypted boxes.
3. **Session Invalidation**:
   - Upon session logout or expiration, purge all cached user data and clear credentials from `flutter_secure_storage`.
