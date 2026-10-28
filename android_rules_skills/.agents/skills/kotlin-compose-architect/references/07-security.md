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

## Network Security & Cleartext Traffic

- **Enforce HTTPS**: Production builds must never allow unencrypted HTTP traffic.
- **Cleartext Traffic Declaration**:
  - In `AndroidManifest.xml`, configure `<application android:usesCleartextTraffic="false" ...>`.
  - On Android 9 (API level 28) and above, cleartext network traffic is blocked by default.
  - If local development or local mock servers require HTTP, use a fine-grained `res/xml/network_security_config.xml` rather than enabling cleartext globally:
    ```xml
    <?xml version="1.0" encoding="utf-8"?>
    <network-security-config>
        <base-config cleartextTrafficPermitted="false" />
        <!-- Whitelist only local development IP/domains if strictly necessary -->
        <domain-config cleartextTrafficPermitted="true">
            <domain includeSubdomains="true">10.0.2.2</domain> <!-- Android Emulator localhost -->
        </domain-config>
    </network-security-config>
    ```
- **Do not disable certificate validation**: Never install custom all-trusting `X509TrustManager` or bypass hostname verification.
- Use certificate pinning only where the threat model explicitly requires it (via `network_security_config.xml` or OkHttp `CertificatePinner`).

---

## Android Manifest & OS Component Security

1. **Explicit `android:exported` (Mandatory Android 12+)**:
   - Every `<activity>`, `<service>`, `<receiver>`, or `<provider>` with an `<intent-filter>` MUST explicitly declare `android:exported="true"` or `android:exported="false"`.
   - Set `android:exported="false"` unless an external app or the Android OS system launcher specifically needs to launch that component.
   - For internal deep links or background services, keep `exported="false"` to prevent unauthorized external Intent injection.

2. **Activity Launch Modes & Task Affinity**:
   - Understand the task and backstack implications of launch modes:
     - `standard` (default): Creates a new Activity instance each time an Intent is routed.
     - `singleTop`: If an instance of the activity already exists at the top of the current task, Android routes the Intent to `onNewIntent()` rather than creating a duplicate.
     - `singleTask`: The activity is created at the root of a new task, or if an instance exists, all activities above it in the stack are cleared and the Intent is delivered to `onNewIntent()`.
     - `singleInstance`: The activity runs as the sole member of its own isolated task; any new activities launched from it run in separate tasks.
   - For standard Compose single-activity architectures, the root `MainActivity` is typically configured as `launchMode="singleTop"` or default with Compose `NavHost` managing the internal backstack.

3. **Screen Orientation Governance**:
   - Avoid hardcoding `android:screenOrientation="portrait"` globally unless strictly required by camera/AR features.
   - Hardcoding portrait breaks adaptive UI on foldables, tablets, ChromeOS, and multi-window mode. Prefer responsive layouts using Compose `WindowWidthSizeClass`.


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
