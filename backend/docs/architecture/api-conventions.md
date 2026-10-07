# MIT TECH KERNEL Backend - REST API Conventions

## Base URL

```
/api/v1
```

All endpoints are versioned. Breaking changes require a new version (`/api/v2/`).

---

## Resource Naming

| Convention | Example |
|------------|---------|
| Plural, kebab-case for collections | `/api/v1/users`, `/api/v1/events`, `/api/v1/domain-leads` |
| Singular for single resource by ID | `/api/v1/users/{userId}`, `/api/v1/events/{eventId}` |
| Nested for sub-resources | `/api/v1/events/{eventId}/registrations`, `/api/v1/domains/{domainId}/members` |
| Actions as sub-resources (not verbs in URL) | `/api/v1/registrations/{id}/cancel`, `/api/v1/events/{id}/publish` |
| Filter/search as query parameters | `/api/v1/events?domain=ai-ml&status=upcoming` |

### Resource Name Mapping

| Module | Base Path | Resources |
|--------|-----------|-----------|
| auth | `/api/v1/auth` | `login`, `logout`, `refresh`, `password/reset`, `password/change`, `mfa` |
| user | `/api/v1/users` | `users`, `users/{id}`, `users/{id}/profile`, `users/{id}/roles` |
| member | `/api/v1/members` | `members`, `members/{id}`, `members/applications`, `members/applications/{id}` |
| domain | `/api/v1/domains` | `domains`, `domains/{id}`, `domains/{id}/leads`, `domains/{id}/members` |
| event | `/api/v1/events` | `events`, `events/{id}`, `events/{id}/speakers`, `events/{id}/resources` |
| registration | `/api/v1/registrations` | `registrations`, `registrations/{id}`, `registrations/{id}/cancel`, `registrations/{id}/attendance` |
| foundation | `/api/v1/foundation` | `programs`, `programs/{id}`, `programs/{id}/enrollments`, `programs/{id}/phases`, `programs/{id}/tasks`, `programs/{id}/submissions` |
| leaderboard | `/api/v1/leaderboards` | `leaderboards`, `leaderboards/{id}`, `leaderboards/{id}/entries`, `leaderboards/{id}/entries/{userId}/rank` |

---

## HTTP Methods

| Method | Use Case | Idempotent | Safe |
|--------|----------|------------|------|
| GET | Retrieve resource(s) | Yes | Yes |
| POST | Create resource, or complex action | No | No |
| PUT | Full resource replacement | Yes | No |
| PATCH | Partial resource update | Yes* | No |
| DELETE | Delete resource | Yes | No |

*PATCH is idempotent when applying the same patch multiple times yields same result.

### Method Conventions by Operation

| Operation | Method | Path |
|-----------|--------|------|
| List resources | GET | `/resources` |
| Get single resource | GET | `/resources/{id}` |
| Create resource | POST | `/resources` |
| Full update | PUT | `/resources/{id}` |
| Partial update | PATCH | `/resources/{id}` |
| Delete resource | DELETE | `/resources/{id}` |
| Custom action | POST | `/resources/{id}/action-name` |
| Sub-resource list | GET | `/resources/{id}/sub-resources` |
| Sub-resource create | POST | `/resources/{id}/sub-resources` |

---

## Request/Response DTO Conventions

### Naming

| Type | Suffix | Example |
|------|--------|---------|
| Request (create) | `CreateRequest` | `CreateUserRequest` |
| Request (update) | `UpdateRequest` | `UpdateUserRequest` |
| Request (custom action) | `{Action}Request` | `CancelRegistrationRequest` |
| Response (single) | `Response` | `UserResponse` |
| Response (list item) | `ListItemResponse` / `Response` | `UserListItemResponse` |
| Response (page) | `PageResponse<T>` | `PageResponse<UserResponse>` |

### Request DTOs

```java
// Use records for immutability
public record CreateUserRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 2, max = 100) String fullName,
    @NotBlank @Pattern(regexp = "^[0-9]{10}$") String studentId,
    @NotNull Role defaultRole
) {}

// Validation annotations on fields
// No business logic in DTOs
// No JPA annotations
```

### Response DTOs

```java
// Use records
public record UserResponse(
    Long id,
    String email,
    String fullName,
    String studentId,
    List<Role> roles,
    Boolean active,
    Instant createdAt,
    Instant updatedAt
) {}

// No setters, no JPA annotations
// Only fields safe for API exposure
// No sensitive data (passwords, tokens, internal IDs)
```

### Wrapper Response

All API responses use a standard wrapper:

```java
// Success
{
  "timestamp": "2026-01-15T10:30:00Z",
  "status": 200,
  "success": true,
  "data": { ... },
  "path": "/api/v1/users/123"
}

// Error
{
  "timestamp": "2026-01-15T10:30:00Z",
  "status": 404,
  "success": false,
  "error": "USER_NOT_FOUND",
  "message": "User with id 123 not found",
  "path": "/api/v1/users/123",
  "validationErrors": null
}

// Page response
{
  "timestamp": "2026-01-15T10:30:00Z",
  "status": 200,
  "success": true,
  "data": {
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "first": true,
    "last": false
  },
  "path": "/api/v1/users"
}
```

---

## Validation Conventions

### Bean Validation Annotations

| Constraint | Use For |
|------------|---------|
| `@NotNull` | Required non-primitive fields |
| `@NotBlank` | Required strings (trimmed) |
| `@NotEmpty` | Required collections/arrays |
| `@Size(min, max)` | String/collection length |
| `@Min`, `@Max` | Numeric ranges |
| `@Positive`, `@PositiveOrZero` | Positive numbers |
| `@Email` | Email format |
| `@Pattern(regexp)` | Custom regex |
| `@Past`, `@Future` | Date/timestamp validation |
| `@Valid` | Nested object validation |

### Custom Validators

For cross-field validation or complex rules:

```java
@Constraint(validatedBy = RegistrationDatesValidator.class)
@Target({ TYPE, ANNOTATION_TYPE })
@Retention(RUNTIME)
public @interface ValidRegistrationDates {
    String message() default "Registration end must be after start";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

### Validation Error Response

```json
{
  "timestamp": "2026-01-15T10:30:00Z",
  "status": 400,
  "success": false,
  "error": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "path": "/api/v1/users",
  "validationErrors": [
    {
      "field": "email",
      "rejectedValue": "invalid-email",
      "message": "must be a valid email address"
    },
    {
      "field": "studentId",
      "rejectedValue": "abc",
      "message": "must match pattern ^[0-9]{10}$"
    }
  ]
}
```

---

## Pagination Conventions

### Request Parameters

| Parameter | Default | Max | Description |
|-----------|---------|-----|-------------|
| `page` | 0 | - | Zero-based page index |
| `size` | 20 | 100 | Page size |
| `sort` | `id,asc` | - | Sort field,direction (repeatable) |

### Example

```
GET /api/v1/events?page=0&size=20&sort=startDate,asc&sort=title,desc
```

### Response

```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8,
  "first": true,
  "last": false,
  "numberOfElements": 20,
  "sort": [
    {"direction": "ASC", "property": "startDate", "ignoreCase": false},
    {"direction": "DESC", "property": "title", "ignoreCase": false}
  ]
}
```

### Cursor-Based Pagination (for large datasets)

For leaderboards, activity feeds:

```
GET /api/v1/leaderboards/{id}/entries?cursor=eyJpZCI6MTIzfQ&size=50
```

Response includes `nextCursor` for next page.

---

## Filtering & Search Conventions

### Query Parameters

| Pattern | Example |
|---------|---------|
| Exact match | `?status=confirmed` |
| Multiple values (OR) | `?status=confirmed,pending` |
| Range | `?startDate=2026-01-01&endDate=2026-12-31` |
| Search (full-text) | `?search=workshop` |
| Boolean | `?active=true` |

### Reserved Parameters

Do not use these for custom filters:
- `page`, `size`, `sort` (pagination)
- `search` (full-text search)

---

## Error Response Conventions

### Standard Error Structure

```json
{
  "timestamp": "2026-01-15T10:30:00Z",
  "status": 404,
  "success": false,
  "error": "RESOURCE_NOT_FOUND",
  "message": "Event with id 999 not found",
  "path": "/api/v1/events/999",
  "validationErrors": null,
  "traceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

### Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `VALIDATION_FAILED` | 400 | Request validation failed |
| `BAD_REQUEST` | 400 | Malformed request |
| `UNAUTHORIZED` | 401 | Missing/invalid authentication |
| `FORBIDDEN` | 403 | Insufficient permissions |
| `RESOURCE_NOT_FOUND` | 404 | Resource does not exist |
| `CONFLICT` | 409 | Resource conflict (duplicate, state) |
| `CAPACITY_EXCEEDED` | 409 | Event/membership capacity full |
| `REGISTRATION_CLOSED` | 409 | Registration period ended |
| `WAITLIST_FULL` | 409 | Waitlist capacity reached |
| `UNPROCESSABLE_ENTITY` | 422 | Semantic validation failed |
| `INTERNAL_ERROR` | 500 | Unexpected server error |
| `SERVICE_UNAVAILABLE` | 503 | Downstream dependency unavailable |

### Domain-Specific Error Codes

| Module | Error Codes |
|--------|-------------|
| auth | `INVALID_CREDENTIALS`, `TOKEN_EXPIRED`, `TOKEN_REVOKED`, `ACCOUNT_LOCKED`, `MFA_REQUIRED` |
| user | `USER_NOT_FOUND`, `DUPLICATE_EMAIL`, `DUPLICATE_STUDENT_ID`, `INVALID_ROLE` |
| member | `MEMBER_NOT_FOUND`, `APPLICATION_NOT_FOUND`, `MEMBERSHIP_EXPIRED` |
| domain | `DOMAIN_NOT_FOUND`, `DOMAIN_LEAD_CONFLICT`, `LEAD_ASSIGNMENT_EXPIRED` |
| event | `EVENT_NOT_FOUND`, `EVENT_CAPACITY_EXCEEDED`, `EVENT_ALREADY_PUBLISHED`, `EVENT_CANCELLED` |
| registration | `REGISTRATION_NOT_FOUND`, `REGISTRATION_CLOSED`, `ALREADY_REGISTERED`, `WAITLIST_FULL`, `INELIGIBLE` |
| foundation | `PROGRAM_NOT_FOUND`, `ENROLLMENT_NOT_FOUND`, `TASK_NOT_FOUND`, `PHASE_NOT_COMPLETE` |
| leaderboard | `LEADERBOARD_NOT_FOUND`, `SCORE_CALCULATION_FAILED`, `INVALID_SCORE_SOURCE` |

---

## HTTP Status Codes

| Status | Use Case |
|--------|----------|
| 200 OK | Successful GET, PUT, PATCH, POST (action) |
| 201 Created | Successful POST (resource creation) - include `Location` header |
| 204 No Content | Successful DELETE, PUT/PATCH with no response body |
| 400 Bad Request | Validation failed, malformed JSON |
| 401 Unauthorized | Missing/invalid token |
| 403 Forbidden | Authenticated but insufficient permissions |
| 404 Not Found | Resource doesn't exist |
| 409 Conflict | Duplicate, state conflict, capacity |
| 422 Unprocessable Entity | Semantic validation (business rule) |
| 429 Too Many Requests | Rate limiting |
| 500 Internal Server Error | Unexpected error |
| 503 Service Unavailable | Downstream service down |

---

## Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| URL paths | kebab-case, plural | `/api/v1/domain-leads` |
| Query params | camelCase | `?domainId=123&startDate=2026-01-01` |
| Request/Response fields | camelCase | `fullName`, `createdAt` |
| Path variables | camelCase | `{userId}`, `{eventId}` |
| Enum values (JSON) | UPPER_SNAKE_CASE | `SUPER_ADMIN`, `CONFIRMED` |
| Headers | Kebab-Case | `X-Request-Id`, `Authorization` |

---

## Headers

### Request Headers

| Header | Required | Description |
|--------|----------|-------------|
| `Authorization` | Yes (protected) | `Bearer <jwt-token>` |
| `Content-Type` | For POST/PUT/PATCH | `application/json` |
| `Accept` | Optional | `application/json` |
| `X-Request-Id` | Optional | Client-generated trace ID |
| `Idempotency-Key` | For mutations | Client-generated UUID for idempotency |

### Response Headers

| Header | Description |
|--------|-------------|
| `Content-Type` | `application/json` |
| `Location` | On 201, URL of created resource |
| `X-Request-Id` | Echoed from request |
| `X-Total-Count` | For non-paginated lists |
| `ETag` | For cacheable GET responses |
| `Cache-Control` | Cache directives |

---

## Idempotency

For mutating operations (POST, PUT, PATCH, DELETE), clients SHOULD provide `Idempotency-Key` header.

Server stores key + response for 24 hours. Duplicate requests return cached response.

---

## Rate Limiting

| Tier | Limit |
|------|-------|
| Authenticated | 1000 req/min |
| Anonymous | 100 req/min |
| Auth endpoints | 10 req/min |

Response on limit exceeded: `429 Too Many Requests` with `Retry-After` header.

---

## API Documentation

OpenAPI 3.0 spec generated from code via `springdoc-openapi`.

- Available at: `/api/v1/swagger-ui.html`
- JSON spec at: `/api/v1/v3/api-docs`

Controllers must include:
- `@Operation(summary, description)`
- `@ApiResponse` for each expected status
- `@Parameter` for path/query params
- `@Schema` on DTO fields