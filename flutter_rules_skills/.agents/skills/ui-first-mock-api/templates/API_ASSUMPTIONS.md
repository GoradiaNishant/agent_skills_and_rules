# API Assumptions Log

This document tracks all assumptions made about backend API contracts, endpoints, request formats, and response schemas while developing Flutter UI features prior to backend availability.

## Active Assumptions

| ID | Feature / Screen | Assumed Endpoint | Method | Assumed Request Body | Assumed Response DTO | Backend Ticket / Contact | Status |
|---|---|---|---|---|---|---|---|
| ASSUMP-001 | User Profile | `/api/v1/user/profile` | GET | None | `{"id": "str", "name": "str", "avatarUrl": "str?"}` | BACKEND-101 | Pending Verification |

## Verification Checklist

When the real backend API is ready:
- [ ] Cross-reference field types and nullability with the actual backend OpenAPI/Swagger specification.
- [ ] Verify HTTP status codes and error payload format match `NetworkResult.Error` expectations.
- [ ] Swap fake repository injection for real repository via DI (`@Injectable(as: ProfileRepository)`).
- [ ] Run integration tests against staging environment.
