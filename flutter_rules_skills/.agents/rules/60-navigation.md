# Navigation & Routing Rules

@../skills/flutter-network-session-management/SKILL.md

## Declarative Routing & Navigation Governance

1. **Routing Framework**:
   - Use declarative routing via `go_router`.
   - Maintain route path constants centrally in `AppRoutes`.
2. **Session & Auth Redirection Guards**:
   - Use `go_router`'s `redirect` callback to observe `SessionManager`.
   - Automatically redirect unauthenticated or session-expired users to `/login`.
   - Prevent authenticated users from accessing auth screens (`/login`, `/signup`).
3. **Deep Linking & Backstack Safety**:
   - Configure URL schemes and universal links in Android Manifest and iOS `Info.plist`.
   - Validate deep link parameters before navigating.
4. **Parameter Safety**:
   - Prefer passing primitive entity IDs (e.g. `productId`) via path/query parameters rather than passing large mutable model objects in `extra`.
   - Allow screens to fetch their own fresh state based on the ID.
