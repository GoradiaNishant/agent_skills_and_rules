# Reference 07 — Security Rules

For applications handling sensitive information, evaluate the items in this
reference. Not every item applies to every project.

---

## Token Storage

Do not store sensitive tokens in plain SharedPreferences.

**Use:**
- `EncryptedSharedPreferences` (from AndroidX Security)
- `Keystore`-backed secure storage
- Biometric-protected storage where required

**Never use:**
- Plain `SharedPreferences` for access/refresh tokens
- `Bundle` extras for sensitive data passed between screens
- Logs that print token values

---

## Authentication Flow

If authentication exists:

```
Access Token
      ↓
Secure Storage
      ↓
OkHttp Interceptor (injects token into Authorization header)
      ↓
API
```

**Consider:**
- Access token + refresh token lifecycle
- 401 handling → trigger token refresh → retry original request
- Session restoration on app restart
- Logout (clear all tokens + local session data)
- Session expiration UX (redirect to login)

---

## Secrets — Never Commit

Never commit to source control:
- Passwords
- Private keys
- Access tokens
- Production secrets
- Private OAuth client secrets

**Use instead:**
- `local.properties` (Git-ignored)
- Gradle properties from CI/CD environment variables
- Secret management systems

---

## Public API Keys

Some client-side keys (Maps API key, Firebase config, public SDK IDs) must be
present in the application binary. These are NOT secrets, but should be:

- Restricted via provider-side restrictions (package + SHA-1)
- Environment-specific where needed
- Not confused with private server secrets

> A value embedded in the APK is inspectable. Never treat it as a secure secret.

---

## Network Security

- Enforce HTTPS — do not allow cleartext traffic in production
- Configure `network_security_config.xml` appropriately
- Do not disable certificate validation
- Use certificate pinning only where the threat model explicitly requires it

---

## ProGuard / R8

For release builds:
- Enable R8 shrinking and obfuscation
- Ensure proper keep rules for:
  - Retrofit/serialization models
  - Room entities
  - Hilt components
  - Any reflection-based libraries

---

## Logging

**Never log:**
- Passwords
- Access tokens
- Refresh tokens
- Private keys
- Sensitive personal information (PII)

**In production:**
- Disable or restrict verbose logs (use `BuildConfig.DEBUG` guards)
- Use a structured logging framework that supports log-level control

---

## Clipboard and Screenshot Risks

For sensitive screens (login, payment, OTP, account details):
- Consider disabling screenshots: `FLAG_SECURE`
- Be cautious about content that may be auto-captured by accessibility services

---

## Biometric Authentication

Evaluate biometric authentication when:
- The application handles financial transactions
- The application stores sensitive personal data
- The user requires quick re-authentication

Use `BiometricPrompt` from AndroidX Biometric.

---

## Security Checklist

Before implementation, verify:
- [ ] Authentication tokens stored securely
- [ ] HTTPS enforced, no cleartext in production
- [ ] Secrets not committed to VCS
- [ ] API keys restricted at provider side
- [ ] Logs do not expose sensitive data
- [ ] ProGuard/R8 configured for release
- [ ] 401 handling and session expiration handled
- [ ] Logout clears all local sensitive data
