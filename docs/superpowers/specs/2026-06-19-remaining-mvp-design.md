# Remaining MVP Features — Design Spec

**Date:** 2026-06-19
**Branch:** DEV
**Status:** Approved

---

## Scope

Four remaining features to complete MVP v1.0:

| # | Feature | Priority | Module | Est. |
|---|---------|:--------:|--------|:----:|
| 1 | Ticket Configuration (Custom Fields + SLA Rules) | P3 | Admin | 2h |
| 2 | System Monitoring Dashboard | P3 | Admin | 2h |
| 3 | WebSocket Notifications | P4 | User + Agent | 3h |
| 4 | Timeout Alerts | P4 | Agent | 1.5h |

---

## Feature 1: Ticket Configuration

### Data Model (V9 Migration)

**`ticket_field_config`** — custom field definitions

| Column | Type | Purpose |
|--------|------|---------|
| id | BIGINT PK | Auto increment |
| name | VARCHAR(100) | Display name, e.g. "Environment" |
| field_key | VARCHAR(50) UNIQUE | Machine key, e.g. "environment" |
| field_type | VARCHAR(20) NOT NULL DEFAULT 'TEXT' | TEXT / SINGLE_SELECT / NUMBER / DATE |
| options | JSON NULLABLE | For SELECT: `{"items": ["prod","staging","dev"]}` |
| required | TINYINT DEFAULT 0 | Is this field required? |
| active | TINYINT DEFAULT 1 | Soft enable/disable |
| display_order | INT DEFAULT 0 | Sort order for rendering |
| created_by / created_date / last_modified_by / last_modified_date | BIGINT | Standard audit columns |

**`sla_config`** — priority-SLA bindings

| Column | Type | Purpose |
|--------|------|---------|
| id | BIGINT PK | Auto increment |
| priority | VARCHAR(20) UNIQUE | LOW / MEDIUM / HIGH / URGENT |
| response_hours | INT | First response time limit in hours |
| resolution_hours | INT | Resolution time limit in hours |
| active | TINYINT DEFAULT 1 | Soft enable/disable |
| created_by / created_date / last_modified_by / last_modified_date | BIGINT | Standard audit columns |

**`ticket` table alteration**

```sql
ALTER TABLE ticket ADD COLUMN custom_fields JSON DEFAULT NULL;
```

### Default Seeds

| Priority | Response (h) | Resolution (h) |
|----------|:----------:|:------------:|
| URGENT | 1 | 4 |
| HIGH | 4 | 24 |
| MEDIUM | 8 | 48 |
| LOW | 24 | 96 |

Custom fields: `environment` (SINGLE_SELECT, ["Production","Staging","Development"]), `version` (TEXT).

### API Endpoints

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| GET | `/api/admin/config/fields` | ADMIN | List all field definitions (sorted by display_order) |
| POST | `/api/admin/config/fields` | ADMIN | Create a field |
| PUT | `/api/admin/config/fields/{id}` | ADMIN | Update a field (name, type, options, required, active) |
| DELETE | `/api/admin/config/fields/{id}` | ADMIN | Delete a field |
| PUT | `/api/admin/config/fields/reorder` | ADMIN | Batch update display_order `[{id, displayOrder}]` |
| GET | `/api/admin/config/sla` | ADMIN | List all SLA rules (sorted by priority severity) |
| PUT | `/api/admin/config/sla/{id}` | ADMIN | Update SLA rule (response_hours, resolution_hours) |

### Frontend: ConfigView.vue

- Route: `/admin/config`, admin-only guard
- Tabs: "Custom Fields" / "SLA Rules" (`aria-selected`, bold active + purple underline, 150ms transition)
- Custom Fields tab: sortable table with drag handles ⠿ (44×44px), toggle switches, **mini preview card** showing how the field renders in ticket form
- SLA Rules tab: inline editable table, confirmation dialog before save (`ElMessageBox.confirm`), "Reset to Defaults" button
- Empty state: "No custom fields configured. Add your first field above." with create button
- Loading: skeleton rows with shimmer animation
- Success/error feedback: `ElMessage` on every mutation

### Validation (per CLAUDE.md validation spec)

| Field | Level | Rules |
|-------|:-----:|-------|
| `name` | 🟡 Standard | Non-empty, max 100 chars |
| `field_key` | 🟡 Standard | Non-empty, max 50 chars, unique, regex `^[a-z][a-z0-9_]*$` |
| `field_type` | 🟡 Standard | Must be in TEXT/SINGLE_SELECT/NUMBER/DATE |
| `options` | 🟢 Loose | Valid JSON if provided, max 20 items for SELECT |
| `response_hours` | 🟡 Standard | 1–720 (1h to 30 days) |
| `resolution_hours` | 🟡 Standard | 1–1440 (1h to 60 days), must be > response_hours |

---

## Feature 2: System Monitoring

### Data Sources (no new DB tables)

| Component | Data | Spring API |
|-----------|------|------------|
| Kafka | Queue depth, consumer lag per partition | `KafkaAdminClient.describeConsumerGroups()` |
| Redis | Hit rate, total keys, evictions, used memory | `RedisTemplate` + INFO command via Redisson `RTopic` or direct `JedisConnection.info()` |
| API | P50/P95/P99 response time, total requests | Custom `HandlerInterceptor` + in-memory ring buffer (last 60s, 300s, 3600s) |

### API Endpoints

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| GET | `/api/admin/monitor/overview` | ADMIN | Aggregated health summary (status per service) |
| GET | `/api/admin/monitor/kafka` | ADMIN | Kafka consumer group lag + queue depth |
| GET | `/api/admin/monitor/redis` | ADMIN | Redis hit ratio, keys count, evictions, memory |
| GET | `/api/admin/monitor/api` | ADMIN | API P50/P95/P99, request count |

### Redis Hit Rate via INFO Command

```java
// Redisson doesn't expose INFO directly; use Spring RedisTemplate
Properties info = redisTemplate.execute(
    (RedisCallback<Properties>) connection -> connection.serverCommands().info("stats"));
String hitRate = info.getProperty("keyspace_hits") / (hits + misses);
```

### API Latency Interceptor

```java
@Component
public class ApiMetricsInterceptor implements HandlerInterceptor {
    // Ring buffer: ConcurrentLinkedDeque<Long> last N latencies
    // On request complete: record duration, trim buffer to window size
    // GET /monitor/api → compute percentiles from buffer
}
```

### Frontend: MonitorView.vue

- Route: `/admin/monitor`, admin-only guard
- Top row: 3 status cards (Kafka / Redis / API) with health color (green 🟢 / yellow 🟡 / red 🔴) + text label
- Polling interval selector: 10s / 30s (default) / 60s / 120s dropdown, persisted to `localStorage`
- Pause/Resume button: stops polling, toggles icon ▶/⏸, `aria-label` dynamic
- Charts (ECharts):
  - Queue Depth: Streaming area chart (last 1h)
  - Cache Hit Rate: Bullet chart with 80% target line
  - API Response Time: Multi-line chart (P50/P95/P99) with 200ms threshold line
- Each chart has "View as Table" toggle (accessibility requirement)
- Loading: skeleton card with shimmer, replaced by data on arrival
- Status colors never used alone: always paired with text label
- Numeric values use Fira Code `tabular-nums` for alignment

---

## Feature 3: WebSocket Notifications

### Architecture

```
Kafka Event → NotificationService (in-process consumer)
    → SimpMessagingTemplate.convertAndSendToUser()
        → STOMP over WebSocket (/ws)
            → Browser (@stomp/stompjs + sockjs-client)
```

### Data Model (V10 Migration)

**`notification`**

| Column | Type | Purpose |
|--------|------|---------|
| id | BIGINT PK | Auto increment |
| user_id | BIGINT NOT NULL | Target user |
| type | VARCHAR(30) NOT NULL | TICKET_CREATED, TICKET_ASSIGNED, TICKET_REPLIED, TICKET_RESOLVED, TICKET_OVERDUE |
| ticket_id | BIGINT NULLABLE | Related ticket |
| title | VARCHAR(255) NOT NULL | Short summary |
| message | TEXT NULLABLE | Optional detail |
| is_read | TINYINT DEFAULT 0 | Read status |
| created_date | BIGINT NOT NULL | Timestamp |
| KEY `idx_notif_user_read` (`user_id`, `is_read`), KEY `idx_notif_date` (`created_date`) |

### Backend Components

| Component | Purpose |
|-----------|---------|
| `WebSocketConfig` | Register `/ws` STOMP endpoint, SockJS fallback, set `UserDestinationPrefix=/user` |
| `WebSocketAuthInterceptor` | Parse JWT from CONNECT headers, validate, set Principal |
| `NotificationService` | Listen Kafka events internally → build Notification entity → save to DB → push via `SimpMessagingTemplate.convertAndSendToUser(username, "/queue/notifications", dto)` |
| `NotificationController` | REST: GET list (paginated), PUT `{id}/read`, PUT `read-all`, GET `unread-count` |

### REST Endpoints

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| GET | `/api/notifications` | Authenticated | Paginated notification list (newest first) |
| GET | `/api/notifications/unread-count` | Authenticated | Current unread count |
| PUT | `/api/notifications/{id}/read` | Authenticated | Mark single as read |
| PUT | `/api/notifications/read-all` | Authenticated | Mark all as read |

### Redis Caching

- Key: `unread:count:{userId}` (String, incr on new, decr on read, set on read-all to 0)
- Fallback: COUNT query from DB if Redis key missing

### Frontend

- `@stomp/stompjs` + `sockjs-client` (npm install)
- Pinia store `notifications.js`: `unreadCount`, `items[]`, `connect()`, `disconnect()`, `markRead()`, `markAllRead()`
- `NotificationBell.vue` component mounted in `AppLayout.vue` navbar (before Sign Out)
  - Bell SVG icon + red badge (unread count)
  - 44×44px touch target with `aria-label="{n} unread notifications"`
  - Click → Popover: notification list with timestamps, blue dot for unread, "Mark all read" header action, "View All" footer link
- Connect on mount, disconnect on unmount

### Notification Dispatch Rules

| Kafka Topic | Recipient | Condition |
|-------------|-----------|-----------|
| `ticket.created` | All AGENT + ADMIN roles | Broadcast to /topic/agents |
| `ticket.assigned` | The assigned agent | `/user/{userId}/queue/notifications` |
| `ticket_reply` event | Ticket creator (if replier is agent) | `/user/{userId}/queue/notifications` |
| `ticket_resolved` event | Ticket creator | `/user/{userId}/queue/notifications` |
| `ticket.overdue` | Assigned agent or all agents (if unassigned) | Depends |

---

## Feature 4: Timeout Alerts

### Detection (`@Scheduled`)

```java
@Scheduled(fixedDelay = 300_000) // Every 5 minutes
public void detectOverdueTickets() {
    List<SlaConfig> slas = slaConfigMapper.selectList(...);
    
    // 1. Unassigned tickets past response SLA
    List<Ticket> unassignedOverdue = ticketMapper.selectList(
        wrapper.eq("status", "OPEN")
               .isNull("assigned_to")
               .lt("created_date", now - responseHours * 3600_000));
    
    // 2. Assigned but unresolved past resolution SLA  
    List<Ticket> unresolvedOverdue = ticketMapper.selectList(
        wrapper.in("status", "OPEN", "IN_PROGRESS")
               .isNotNull("assigned_to")
               .lt("created_date", now - resolutionHours * 3600_000));
    
    // Publish events
    unassignedOverdue.forEach(t -> publisher.publishTicketOverdue(
        new TicketOverdueEvent(t, "UNASSIGNED_OVERDUE", overdueHours)));
    unresolvedOverdue.forEach(t -> publisher.publishTicketOverdue(
        new TicketOverdueEvent(t, "RESOLUTION_OVERDUE", overdueHours)));
}
```

### New Kafka Topic

- `ticket.overdue` — keyed by ticket ID, consumed by NotificationService

### Frontend

**AgentWorkbench.vue changes:**

- Top banner (collapsible): "⚠ Overdue Tickets: 2 unassigned · 1 unresolved → View All"
- Overdue tickets in queue rows: orange left border + ⏰ icon + relative time label ("2h overdue")
- Poll overdue list alongside existing data fetch (or via WebSocket push from overdue event)

### New REST Endpoint

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| GET | `/api/tickets/overdue` | AGENT/ADMIN | List overdue tickets for current agent/workbench view |

---

## Validation Summary (per CLAUDE.md spec)

All new API endpoints follow the validation strength matrix:

- 🔴 **Strict**: None applicable (no user identity fields)
- 🟡 **Standard**: `field_key`, `field_type`, `response_hours`, `resolution_hours`, notification `type`
- 🟢 **Loose**: `options` (JSON), `message` (notification body), custom field values
- ⚪ **Custom**: SLA hours validated against priority-specific ranges

DTO validation via `@Valid` + `jakarta.validation` annotations, unique checks in Service layer.

---

## UI/UX Checklist (from ui-ux-pro-max review)

- [x] Touch targets ≥ 44×44px on all interactive elements
- [x] `aria-selected` on tab controls
- [x] `aria-label` on icon-only buttons (bell, drag, pause, edit)
- [x] Drag handles visible (⠿ icon)
- [x] Confirmation dialog before SLA changes
- [x] Skeleton loading states (shimmer) for async content
- [x] Status colors paired with text labels (never color-only)
- [x] Charts with "View as Table" accessibility toggle
- [x] Pause button for real-time polling
- [x] Empty states with guidance text + action button
- [x] `cursor-pointer` on all clickable elements
- [x] 150-300ms transitions on interactive state changes
- [x] `prefers-reduced-motion` respected
- [x] Tab order matches visual order

---

## Implementation Order

1. V9 migration → ConfigController + ConfigService → ConfigView.vue
2. MonitorController + MonitorService → MonitorView.vue
3. V10 migration → WebSocket stack → NotificationService → NotificationBell.vue
4. @Scheduled task → ticket.overdue event → overdue detection → Workbench overlay

Each feature includes unit tests for all new service methods and controller endpoints per project CLAUDE.md requirement.
