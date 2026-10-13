# API Assumptions & Contract Specifications

## Feature: [Feature Name]
Date: [YYYY-MM-DD]
Status: Draft — Awaiting Backend Integration

---

## 1. Assumed Endpoints

| Method | Endpoint | Description | Auth Required | Status |
|--------|----------|-------------|---------------|--------|
| `GET`  | `/api/v1/[resource]` | Fetch list / collection | Yes | Draft |
| `GET`  | `/api/v1/[resource]/{id}` | Fetch resource detail | Yes | Draft |
| `POST` | `/api/v1/[resource]` | Create resource | Yes | Draft |
| `PUT`  | `/api/v1/[resource]/{id}` | Update resource | Yes | Draft |
| `DELETE` | `/api/v1/[resource]/{id}` | Delete resource | Yes | Draft |

---

## 2. Request & Response Payloads

### `GET /api/v1/[resource]`
**Query Parameters:**
- `page` (Int, optional, default: 1)
- `pageSize` (Int, optional, default: 20)
- `search` (String, optional)

**Expected 200 OK Response:**
```json
{
  "data": [
    {
      "id": "item_01",
      "title": "Example Title",
      "description": "Example description text",
      "status": "ACTIVE",
      "created_at": "2026-10-09T10:00:00Z"
    }
  ],
  "page": 1,
  "total_pages": 1,
  "total_count": 1
}
```

**DTO Fallbacks & Assumptions:**
- `status`: String mapped to domain enum; fallback to `UNKNOWN` if unrecognized.
- `created_at`: String parsed as ISO-8601 UTC.

---

## 3. Error Responses

**Expected 4xx / 5xx Error Body:**
```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "The requested item could not be found.",
  "errors": []
}
```

---

## 4. Backend Integration Verification Checklist

When backend APIs are ready, verify before flipping the mock switch:
- [ ] Confirm base URL and API gateway path (`/api/v1` vs `/v1`).
- [ ] Verify HTTP Authorization header scheme (`Bearer <token>`).
- [ ] Confirm date/time format across all timestamps.
- [ ] Compare field naming convention (`snake_case` vs `camelCase`).
- [ ] Verify HTTP status codes for errors (e.g. 401 for unauthorized, 403 for forbidden, 404 for not found).
- [ ] Verify pagination structure (page/size vs offset/limit vs cursor).
- [ ] Run live end-to-end flow with real network latency and verify Loading/Empty/Error UI states.
