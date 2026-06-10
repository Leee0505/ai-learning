# Phase 1 Design: Project Skeleton & Authentication System

> **Date:** 2026-06-10
> **Status:** Approved
> **Scope:** Docker Compose infrastructure, Spring Boot project skeleton, JWT authentication, Vue 3 login page

## 1. Overview

Phase 1 establishes the foundation for the Ticket Management System. It delivers a runnable development environment with MySQL + Redis in Docker, a Spring Boot backend with complete JWT-based authentication, and a Vue 3 frontend with a working login page.

### 1.1 What's In Scope

| Layer | Deliverable | Description |
|-------|------------|-------------|
| Infrastructure | Docker Compose | MySQL 8.0 + Redis 7 containers |
| Backend | Spring Boot 3.2 project | Single module, package-by-layer, MyBatis-Plus, Flyway |
| Database | 3 tables + seed data | `user`, `invite_token`, `audit_log` |
| Auth APIs | 7 endpoints | Register, Login, Logout, Refresh, Me, Invite, Accept-invite |
| Frontend | Vue 3 + Login page | Element Plus, Pinia, Axios interceptors, Router guard |

### 1.2 What's Out of Scope (deferred to later phases)

- Ticket CRUD, replies, attachments
- Agent workspace, internal notes, ticket assignment
- Admin dashboard, user management UI, SLA configuration
- Knowledge base, search (Elasticsearch), data export
- WebSocket notifications, email sending
- Production deployment (Dockerfile, Nginx, CI/CD)
- Kafka integration

---

## 2. Design Decisions

| # | Decision | Choice | Rationale |
|---|----------|--------|-----------|
| 1 | Backend project structure | Single module, package-by-layer | Simpler for 1-2 developers, faster IDE navigation |
| 2 | Database migration | Flyway | Best Spring Boot integration, pure SQL, simple versioning |
| 3 | ADMIN user creation | Flyway seed script (V2) | Deploy-time auto-creation, no manual DB access needed |
| 4 | AGENT user creation | Admin sends time-limited invitation link | Controlled access for internal staff, 48h expiry |
| 5 | USER creation | Open self-registration | Low friction for end users submitting tickets |
| 6 | Implementation order | Backend-first, frontend minimal | Fastest feedback loop; backend APIs verifiable via curl/Postman |
| 7 | Audit fields | `created_by`, `created_date`, `last_modified_by`, `last_modified_date` on all tables | Enterprise standard, enables full audit trail |
| 8 | Timestamp format | `BIGINT` (Unix milliseconds) | Timezone-safe; frontend formats per browser locale |
| 9 | Role table design | Single `user` table with `role` column | Simplest auth flow; 1:1 extension tables for role-specific fields later |
| 10 | Phone field | `VARCHAR(20) NULLABLE` on user table | Optional contact method for ticket follow-up |
| 11 | Field encryption | Only `password` bcrypt-hashed; `email`/`phone` plaintext | Email is a query key (login, invite, future search); phone may become a query dimension. PII encryption deferred to security hardening phase with hash-helper-column pattern |
| 12 | Password transmission | HTTPS/TLS (production), plaintext HTTP (dev) | Industry standard — TLS encrypts the entire HTTP payload. Client-side hashing adds complexity without meaningful security gain |
| 13 | Field length convention | Standardized: 50 (identifiers), 100 (email), 20 (phone), 255 (hashes), 64 (tokens), 45 (IPv6) | Consistent sizing across all tables; `detail` uses MySQL `JSON` type |
| 14 | Error code management | Centralized `ErrorCode` enum in `common/constant/ErrorCode.java` | Single source of truth; backend throws codes, frontend maps codes to messages; enables i18n by swapping message file |
| 15 | Login identifier | Username OR email (auto-detect: `@` = email, else = username) | UX improvement without API change; phone login deferred to future phase |

---

## 3. Data Model

### 3.0 Field Encryption Policy (MVP)

| Field | Protection | Method | Reason |
|-------|-----------|--------|--------|
| `password` | 🔴 Hashed | bcrypt (one-way) | Must never be reversible |
| `email` | 🟢 Plaintext | — | Login identifier + query key + unique constraint; encrypting would break `WHERE email = ?` lookups and index usage |
| `phone` | 🟢 Plaintext | — | Future query dimension; encryption deferred until hash-helper-column pattern is justified |
| All other fields | 🟢 Plaintext | — | No PII exposure |

> **Future PII hardening path:** When GDPR/compliance requires it, add `email_hash` (SHA-256) and `phone_hash` columns with indexes for lookups, then encrypt the original values with AES-256. This preserves queryability via the hash columns while protecting plaintext PII at rest.

### 3.1 Field Length Convention

| Usage | Length | Examples |
|-------|--------|---------|
| Short identifiers | `VARCHAR(50)` | username, role, action, target_type |
| Email | `VARCHAR(100)` | Accommodates longest valid email |
| Phone | `VARCHAR(20)` | Includes international prefix + separators |
| Hashes | `VARCHAR(255)` | bcrypt (60 chars) with margin |
| Tokens | `VARCHAR(64)` | UUID (36 chars) with margin |
| IP address | `VARCHAR(45)` | IPv6 maximum length |
| JSON / long text | `JSON` / `TEXT` | Native MySQL types for unstructured data |

### 3.2 Global Convention (Audit Fields)

All tables include four standard audit fields:

- `created_by BIGINT` — creator user ID (0 for system actions)
- `created_date BIGINT` — creation timestamp (Unix ms)
- `last_modified_by BIGINT` — last modifier user ID
- `last_modified_date BIGINT` — last modification timestamp (Unix ms)

All timestamps are stored as Unix milliseconds (`BIGINT`). The backend sends numeric timestamps; the frontend formats them according to the browser's timezone.

### 3.3 `user` Table

| Column | Type | Constraints | Description |
|--------|------|------------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Primary key |
| `username` | VARCHAR(50) | UNIQUE, NOT NULL | Login username |
| `email` | VARCHAR(100) | UNIQUE, NOT NULL | Email address |
| `phone` | VARCHAR(20) | NULLABLE | Phone number (optional) |
| `password` | VARCHAR(255) | NOT NULL | bcrypt encoded |
| `role` | VARCHAR(20) | NOT NULL | `ROLE_USER` / `ROLE_AGENT` / `ROLE_ADMIN` |
| `status` | TINYINT | DEFAULT 1 | 1=enabled, 0=disabled |
| `created_by` | BIGINT | DEFAULT 0 | Creator ID (0 for self-registration) |
| `created_date` | BIGINT | NOT NULL | Registration time (Unix ms) |
| `last_modified_by` | BIGINT | NULLABLE | Last modifier ID |
| `last_modified_date` | BIGINT | NULLABLE | Last modification time (Unix ms) |

**Indexes:** `idx_user_email` ON `email`, `idx_user_role` ON `role`

**Single-table rationale:** All three roles share the same core fields (username, email, password). A single table enables `loadUserByUsername()` in one query, supports role changes without data migration, and enforces global uniqueness on username/email naturally. When role-specific fields grow in future phases (e.g., agent skills, admin permissions), they will be added via 1:1 extension tables (`agent_profile`, `admin_profile`) without disrupting the auth flow.

### 3.4 `invite_token` Table

| Column | Type | Constraints | Description |
|--------|------|------------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Primary key |
| `token` | VARCHAR(64) | UNIQUE, NOT NULL | UUID-generated token |
| `email` | VARCHAR(100) | NOT NULL | Invited email address |
| `expires_at` | BIGINT | NOT NULL | Expiration time (Unix ms, 48h from creation) |
| `used` | TINYINT | DEFAULT 0 | 0=unused, 1=used |
| `created_by` | BIGINT | NOT NULL | Admin who created the invitation |
| `created_date` | BIGINT | NOT NULL | Creation time (Unix ms) |
| `last_modified_by` | BIGINT | NULLABLE | Last modifier ID |
| `last_modified_date` | BIGINT | NULLABLE | Last modification time (Unix ms) |

**Indexes:** `idx_invite_token` ON `token`, `idx_invite_email` ON `email`

### 3.5 `audit_log` Table

| Column | Type | Constraints | Description |
|--------|------|------------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Primary key |
| `user_id` | BIGINT | NOT NULL | Actor user ID |
| `action` | VARCHAR(50) | NOT NULL | Action type (LOGIN, LOGOUT, REGISTER, INVITE_AGENT, etc.) |
| `target_type` | VARCHAR(50) | NULLABLE | Target entity type (USER, INVITE_TOKEN) |
| `target_id` | BIGINT | NULLABLE | Target entity ID |
| `detail` | JSON | NULLABLE | Flexible detail payload |
| `ip_address` | VARCHAR(45) | NULLABLE | Client IP address |
| `created_by` | BIGINT | NOT NULL | Same as user_id (for unified audit queries) |
| `created_date` | BIGINT | NOT NULL | Event time (Unix ms) |
| `last_modified_by` | BIGINT | NULLABLE | Logs are typically immutable |
| `last_modified_date` | BIGINT | NULLABLE | Logs are typically immutable |

**Indexes:** `idx_audit_user` ON `user_id`, `idx_audit_action` ON `action`, `idx_audit_created_date` ON `created_date`

### 3.6 Flyway Scripts

**V1__init_schema.sql** — Creates all three tables with indexes.

**V2__seed_admin.sql** — Inserts default admin user:

```sql
INSERT INTO user (username, email, phone, password, role, status, created_by, created_date)
VALUES ('admin', 'admin@ticket.local', NULL,
        '$2a$10$...',  -- bcrypt hash of 'Admin@123'
        'ROLE_ADMIN', 1, 0, UNIX_TIMESTAMP(NOW()) * 1000);
```

> ⚠️ Admin must change password on first login (enforced in a future phase; for MVP it's a documented requirement).

---

## 4. API Design

### 4.1 Unified Response Format

All API responses follow this structure:

```json
// Success
{ "code": 200, "message": "success", "data": { ... } }

// Business error
{ "code": 40001, "message": "username already exists", "data": null }

// Auth failure
{ "code": 40100, "message": "invalid credentials", "data": null }
```

**Error code ranges:**
- `200` — Success
- `400xx` — Validation / business errors
- `401xx` — Authentication / authorization errors
- `403xx` — Forbidden (insufficient role)
- `500xx` — Internal server errors

### 4.2 Endpoints

| # | Method | Path | Auth | Role | Request Body | Response `data` |
|---|--------|------|------|------|-------------|-----------------|
| 1 | POST | `/api/auth/register` | None | — | `{ username, email, password, phone? }` | `{ user, accessToken, refreshToken }` |
| 2 | POST | `/api/auth/login` | None | — | `{ login, password }` | `{ user, accessToken, refreshToken }` |
| 3 | POST | `/api/auth/logout` | JWT | Any | — | `null` |
| 4 | POST | `/api/auth/refresh` | Refresh | — | `{ refreshToken }` | `{ accessToken, refreshToken }` |
| 5 | GET | `/api/auth/me` | JWT | Any | — | `{ user }` |
| 6 | POST | `/api/auth/invite` | JWT | ADMIN | `{ email }` | `{ token, expiresAt }` |
| 7 | POST | `/api/auth/accept-invite` | None | — | `{ token, username, password }` | `{ user, accessToken, refreshToken }` |

### 4.3 Business Logic Details

**Register (POST /api/auth/register):**
- Validate username/email uniqueness
- bcrypt password before storage
- Assign `ROLE_USER` role
- Return tokens (auto-login after registration)
- Write audit log (action: REGISTER)

**Login (POST /api/auth/login):**
- Accept `login` field — backend auto-detects: contains `@` → query by `email`, otherwise → query by `username`
- Authenticate via Spring Security `AuthenticationManager`
- Generate Access Token (2h) + Refresh Token (7d)
- Store Refresh Token in Redis whitelist (key: `refresh:<userId>:<jti>`)
- Write audit log (action: LOGIN)

**Logout (POST /api/auth/logout):**
- Extract JWT from Authorization header
- Add Access Token to Redis blacklist with TTL = remaining expiration
- Remove Refresh Token from Redis whitelist
- Write audit log (action: LOGOUT)

**Refresh (POST /api/auth/refresh):**
- Validate Refresh Token signature and expiry
- Verify Refresh Token exists in Redis whitelist
- Remove old Refresh Token from whitelist
- Issue new Access Token + new Refresh Token (rotation)
- Add new Refresh Token to Redis whitelist

**Invite (POST /api/auth/invite):**
- Require `ROLE_ADMIN` authority
- Generate UUID token, store in `invite_token` table
- Set `expires_at` to now + 48 hours
- Write audit log (action: INVITE_AGENT, target: invite_token.id)

**Accept Invite (POST /api/auth/accept-invite):**
- Look up token in `invite_token` table
- Validate: token exists, not expired (`expires_at > now`), not used (`used = 0`)
- Create user with `ROLE_AGENT` role and provided username/password
- Mark token as used (`used = 1`)
- Return tokens (auto-login after accepting)
- Write audit log (action: ACCEPT_INVITE)

### 4.4 JWT Token Strategy

| Property | Access Token | Refresh Token |
|----------|-------------|---------------|
| Expiration | 2 hours | 7 days |
| Storage (client) | Pinia store (in-memory) | localStorage |
| Storage (server) | None (stateless verification) | Redis whitelist (`refresh:<userId>:<jti>`) |
| Payload | `{ sub (userId), role, iat, exp }` | `{ sub (userId), jti, iat, exp }` |
| On logout | Added to Redis blacklist (TTL = remaining life) | Removed from Redis whitelist |

**Security notes:**
- Access Token stored in Pinia (memory) only — not in localStorage — to prevent XSS exfiltration
- On page refresh, the Axios interceptor silently calls `/api/auth/refresh` to get a new Access Token
- Refresh Token rotation: each refresh call invalidates the old Refresh Token and issues a new one
- The `/api/auth/refresh` endpoint has no auth requirement — it authenticates via the Refresh Token itself
- Password is sent as plaintext in the HTTP request body over HTTPS/TLS in production — this is the industry standard (used by Google, GitHub, AWS). TLS encrypts the entire HTTP payload end-to-end. In local development, HTTP without TLS is acceptable.

### 4.5 Spring Security Configuration

```
SecurityFilterChain:
  /api/auth/register       → permitAll
  /api/auth/login          → permitAll
  /api/auth/refresh        → permitAll
  /api/auth/accept-invite  → permitAll
  /api/auth/invite         → hasRole('ROLE_ADMIN')
  /api/auth/**             → authenticated
  All other /**            → authenticated

JwtAuthenticationFilter:
  - Extracts JWT from Authorization: Bearer <token>
  - Validates signature and expiry
  - Checks Redis blacklist (key: "blacklist:<jti>")
  - Sets SecurityContextHolder with userId + role
  - Runs before UsernamePasswordAuthenticationFilter
```

### 4.6 Error Code Management

All error codes are defined in a single centralized enum — `com.ticket.common.constant.ErrorCode` — which is the single source of truth for the entire application.

```java
// common/constant/ErrorCode.java — Centralized error code registry
public enum ErrorCode {
    // 4xxxx Client Errors
    VALIDATION_ERROR(40000, "validation error"),
    USERNAME_ALREADY_EXISTS(40001, "username already exists"),
    EMAIL_ALREADY_EXISTS(40002, "email already exists"),
    INVITE_TOKEN_EXPIRED(40003, "invitation link has expired"),
    INVITE_TOKEN_USED(40004, "invitation link already used"),
    INVITE_TOKEN_NOT_FOUND(40005, "invitation link not found"),

    // 401xx Auth Errors
    INVALID_CREDENTIALS(40100, "invalid credentials"),
    TOKEN_EXPIRED(40101, "token expired"),
    TOKEN_BLACKLISTED(40102, "token has been revoked"),
    TOKEN_INVALID(40103, "token is invalid"),

    // 403xx Forbidden
    ACCESS_DENIED(40300, "access denied"),

    // 500xx Server Errors
    INTERNAL_ERROR(50000, "internal server error");

    private final int code;
    private final String defaultMessage;
    // getters...
}
```

**Usage pattern:**
- Backend exceptions carry an `ErrorCode` enum value, not a raw string
- `GlobalExceptionHandler` maps each exception to the corresponding `ErrorCode` and returns `{ code, message }` to the client
- Frontend maps error `code` to localized display text — no string matching, no fragile `message` comparison
- Adding a new error = adding one line to the enum + a new exception class (or reusing `BusinessException` with the new code)

### 4.7 Global Exception Handling

| Exception | HTTP Status | Code | Message |
|-----------|-------------|------|---------|
| `UsernameAlreadyExistsException` | 409 | 40001 | "username already exists" |
| `EmailAlreadyExistsException` | 409 | 40002 | "email already exists" |
| `BadCredentialsException` | 401 | 40100 | "invalid credentials" |
| `TokenExpiredException` | 401 | 40101 | "token expired" |
| `TokenBlacklistedException` | 401 | 40102 | "token has been revoked" |
| `InviteTokenExpiredException` | 400 | 40003 | "invitation link has expired" |
| `InviteTokenUsedException` | 400 | 40004 | "invitation link already used" |
| `AccessDeniedException` | 403 | 40300 | "access denied" |
| `MethodArgumentNotValidException` | 400 | 40000 | Field-level validation messages |
| `Exception` (fallback) | 500 | 50000 | "internal server error" |

---

## 5. Backend Project Structure

```
ticket-system/
├── docker-compose.yml
├── docker-volumes/               # Git-ignored
│   ├── mysql/
│   └── redis/
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/ticket/
│       ├── TicketApplication.java
│       ├── controller/
│       │   └── AuthController.java
│       ├── service/
│       │   ├── AuthService.java
│       │   └── impl/
│       │       └── AuthServiceImpl.java
│       ├── mapper/
│       │   ├── UserMapper.java
│       │   └── InviteTokenMapper.java
│       ├── entity/
│       │   ├── User.java
│       │   └── InviteToken.java
│       ├── dto/
│       │   ├── request/
│       │   │   ├── LoginRequest.java
│       │   │   ├── RegisterRequest.java
│       │   │   ├── RefreshRequest.java
│       │   │   ├── InviteRequest.java
│       │   │   └── AcceptInviteRequest.java
│       │   └── response/
│       │       ├── ApiResponse.java
│       │       ├── AuthResponse.java
│       │       └── UserResponse.java
│       ├── security/
│       │   ├── JwtTokenProvider.java
│       │   ├── JwtAuthenticationFilter.java
│       │   └── SecurityConfig.java
│       ├── config/
│       │   ├── RedisConfig.java
│       │   └── WebMvcConfig.java
│       └── common/
│           ├── constant/
│           │   └── ErrorCode.java
│           ├── exception/
│           │   ├── GlobalExceptionHandler.java
│           │   └── BusinessException.java
│           └── aspect/
│               └── AuditLogAspect.java
└── frontend/
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── main.js
        ├── App.vue
        ├── api/
        │   ├── request.js
        │   └── auth.js
        ├── router/
        │   └── index.js
        ├── stores/
        │   └── auth.js
        ├── views/
        │   └── LoginView.vue
        └── layouts/
            └── AuthLayout.vue
```

---

## 6. Frontend Design

### 6.1 Technology Stack

| Library | Version | Purpose |
|---------|---------|---------|
| Vue | 3.4+ | UI framework (Composition API + `<script setup>`) |
| Vite | 5.x | Build tool & dev server |
| TypeScript | 5.x | Type safety (strict mode) |
| Element Plus | 2.x | UI component library |
| Pinia | 2.x | State management |
| Vue Router | 4.x | Client-side routing |
| Axios | 1.x | HTTP client |

### 6.2 Phase 1 Scope

Only the **Login page** is built in Phase 1. Registration and invite-accept pages are added in later phases. The frontend is validated by logging in as admin (Flyway-seeded) and getting redirected to a placeholder dashboard.

### 6.3 Axios Interceptor Logic

```
Request Interceptor:
  1. Read accessToken from Pinia auth store
  2. If token exists, set Authorization: Bearer <token>
  3. Pass request through

Response Interceptor (error path):
  1. If response status != 401, reject as normal
  2. If 401 and not already retrying:
     a. Read refreshToken from localStorage
     b. POST /api/auth/refresh with refreshToken
     c. On success: update Pinia store with new tokens, retry original request
     d. On failure: clear auth state, redirect to /login
  3. If 401 and already retrying, reject
```

### 6.4 Router Guard

```
Navigation Guard (beforeEach):
  1. If route requires auth and no accessToken in store:
     a. Try silent refresh via refreshToken
     b. If refresh succeeds → proceed
     c. If refresh fails → redirect to /login
  2. If route is /login and user has valid accessToken → redirect to /
  3. Otherwise → proceed
```

---

## 7. Infrastructure

### 7.1 Docker Compose

```yaml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: rootpass
      MYSQL_DATABASE: ticket_db
      MYSQL_USER: ticket_user
      MYSQL_PASSWORD: ticket_pass
    ports:
      - "3306:3306"
    volumes:
      - ./docker-volumes/mysql:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      retries: 5

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - ./docker-volumes/redis:/data
    command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      retries: 5
```

### 7.2 Development Workflow

```
1. docker compose up -d              # Start MySQL + Redis
2. Run TicketApplication.java        # IDE: Spring Boot on :8080
3. cd frontend && npm run dev        # Vite dev server on :5173
4. Open http://localhost:5173        # Login page
```

`vite.config.js` proxies `/api` requests to `http://localhost:8080` to avoid CORS issues during development.

---

## 8. Testing Strategy

### 8.1 Backend

| Layer | Tool | What to Test |
|-------|------|-------------|
| Unit | JUnit 5 + Mockito | JWT token generation/validation, password encoding, service logic |
| Integration | Spring Boot Test + Testcontainers (or H2) | AuthController endpoints, Flyway migration correctness, Redis blacklist logic |
| Security | `@WithMockUser` | Role-based access: USER cannot invite, ADMIN can |

### 8.2 Frontend

| Scope | Tool | What to Test |
|-------|------|-------------|
| Component | Vitest + vue-test-utils | Login form validation, Pinia store actions |
| E2E | (Future phase) | Full login flow end-to-end |

### 8.3 Phase 1 Test Coverage Target

- Auth service layer: >90%
- JWT utility: 100%
- Auth controller integration: all 7 endpoints have at least one happy-path + one error-case test

---

## 9. Error Handling

### 9.1 Backend Error Flow

```
Controller
  → Service throws BusinessException
    → GlobalExceptionHandler catches
      → Maps to ApiResponse with appropriate code
        → Returns to client
```

Unhandled exceptions are caught by a fallback handler returning `{ code: 50000, message: "internal server error" }`.

### 9.2 Frontend Error Handling

- Axios response interceptor extracts `message` from error response body
- ElMessage (Element Plus) toast displays error to user
- Network errors (no response) display a generic "network error, please try again" message

---

## 10. Future Considerations (Out of Scope for Phase 1)

- **Email delivery**: The invite token is generated but email sending is deferred. In Phase 1, the admin copies the invite link manually (acceptable for MVP with 1-2 agents).
- **Password reset flow**: Not in Phase 1. Admin can create new invite if an agent loses access.
- **Account lockout after N failed attempts**: Phase 1 accepts unlimited login attempts. Will add in a security hardening phase.
- **Multi-factor authentication**: Not planned for MVP.
- **OAuth2 / SSO integration**: Not planned for MVP.
- **Production Docker images**: Backend + Frontend Dockerfiles and Nginx reverse proxy configuration deferred.

---

## 11. Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|-----------|
| JWT secret hardcoded in dev | Secrets leak to git | Use env variable with dev default, add to `.env.example` |
| Redis data loss on container restart | Blacklisted tokens lost, allowing re-use of logged-out tokens | Acceptable during development; production would use persistence + replication |
| Refresh Token in localStorage | Vulnerable to XSS | Use Content-Security-Policy headers; consider HttpOnly cookie in production |
| Flyway migration conflicts | Schema drift between developers | Use sequential version numbers; never modify applied migrations |
