# Flutter Security & Sensitive Data Handling Guide

## 1. Credentials & Token Storage
- **Never** store access tokens, refresh tokens, or user credentials in plain `SharedPreferences`.
- Use `flutter_secure_storage` which utilizes Android Keystore (AES encryption) and iOS Keychain.

## 2. Environment Variables & API Keys
- Never commit hardcoded production API keys or secrets to git.
- Pass compile-time variables using `--dart-define` or `--dart-define-from-file`:
  ```dart
  const apiKey = String.fromEnvironment('API_KEY');
  ```

## 3. Network Security & Obfuscation
- Enforce HTTPS and TLS 1.3.
- Redact `Authorization`, `Cookie`, and bearer tokens from all logs via `DataSanitizer`.
- Use release build obfuscation (`flutter build appbundle --obfuscate --split-debug-info=/<symbols>`).
