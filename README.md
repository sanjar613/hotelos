# HotelOS — Real-Time Hotel Management System

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    HotelOS Microservices                        │
│                                                                 │
│  ┌──────────────┐    ┌──────────────────────────────────────┐  │
│  │  Reception   │    │         RabbitMQ Broker               │  │
│  │   :8081      │───▶│  hotelos.exchange (Topic Exchange)    │  │
│  └──────────────┘    │                                       │  │
│                      │  room.vacated ─────────▶ Housekeeping │  │
│  ┌──────────────┐    │  room.status.changed ──▶ Dashboard   │  │
│  │Housekeeping  │───▶│  order.status.changed ─▶ Dashboard   │  │
│  │   :8082      │    │  maintenance.created ──▶ Dashboard   │  │
│  └──────────────┘    │  room.service.charge ──▶ Reception   │  │
│                      └──────────────────────────────────────┘  │
│  ┌──────────────┐                                               │
│  │ Room Service │         WebSocket (/topic/rooms)             │
│  │   :8083      │──────────────────────────────────▶ Dashboard │
│  └──────────────┘         WebSocket (/topic/orders)            │
│                                                                 │
│  ┌──────────────┐         WebSocket (/topic/maintenance)       │
│  │ Maintenance  │──────────────────────────────────▶ Dashboard │
│  │   :8084      │                                               │
│  └──────────────┘                                               │
│                                                                 │
│  ┌──────────────┐                                               │
│  │  Dashboard   │  ← Browser UI with live WebSocket updates    │
│  │   :8080      │                                               │
│  └──────────────┘                                               │
└─────────────────────────────────────────────────────────────────┘
```

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 17 |
| Gradle | 8.6 |
| PostgreSQL | 14+ |
| RabbitMQ | 3.12+ |

## Setup — Step by Step

### 1. Create PostgreSQL Databases

```sql
CREATE DATABASE hotelos_reception;
CREATE DATABASE hotelos_housekeeping;
CREATE DATABASE hotelos_roomservice;
CREATE DATABASE hotelos_maintenance;
```

All use: username=`postgres`, password=`12345`

### 2. Install & Start RabbitMQ

**macOS:** `brew install rabbitmq && brew services start rabbitmq`  
**Ubuntu:** `sudo apt install rabbitmq-server && sudo systemctl start rabbitmq-server`  
**Windows:** Download from https://www.rabbitmq.com/download.html

RabbitMQ Management UI: http://localhost:15672 (guest/guest)

### 3. Build All Services

```bash
cd hotelos
./gradlew build -x test
```

### 4. Start All Services (in separate terminals)

```bash
# Terminal 1 — Reception Service (port 8081)
./gradlew :reception-service:bootRun

# Terminal 2 — Housekeeping Service (port 8082)  
./gradlew :housekeeping-service:bootRun

# Terminal 3 — Room Service (port 8083)
./gradlew :room-service:bootRun

# Terminal 4 — Maintenance Service (port 8084)
./gradlew :maintenance-service:bootRun

# Terminal 5 — Dashboard (port 8080)
./gradlew :dashboard:bootRun
```

### 5. Open Dashboard

Navigate to: **http://localhost:8080**

Login: `admin` / `hotelos2024`

---

## API Reference

### Reception Service (port 8081)

All endpoints require HTTP Basic Auth: `admin:hotelos2024`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/reception/checkin` | Check in a guest |
| POST | `/api/reception/checkout/{roomNumber}` | Check out, calculate bill |
| GET  | `/api/reception/rooms` | All room statuses |

**Check-in example:**
```bash
curl -X POST http://localhost:8081/api/reception/checkin \
  -u admin:hotelos2024 \
  -H "Content-Type: application/json" \
  -d '{
    "guestName": "Alice Smith",
    "requestedRoomType": "DOUBLE",
    "preferredFloor": 2,
    "checkInDate": "2024-06-01",
    "discountPercent": 0
  }'
```

**Check-out example (TS-02):**
```bash
curl -X POST http://localhost:8081/api/reception/checkout/204 \
  -u admin:hotelos2024
```

### Housekeeping Service (port 8082)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET  | `/api/housekeeping/queue` | Dirty rooms needing cleaning |
| GET  | `/api/housekeeping/rooms` | All rooms with HK status |
| POST | `/api/housekeeping/rooms/{room}/start-cleaning` | Begin cleaning (TS-03) |
| POST | `/api/housekeeping/rooms/{room}/mark-clean` | Mark clean (TS-03) |

**Start cleaning (TS-03):**
```bash
curl -X POST http://localhost:8082/api/housekeeping/rooms/204/start-cleaning \
  -H "Content-Type: application/json" \
  -d '{"housekeeper": "Maria"}'
```

**Mark clean:**
```bash
curl -X POST http://localhost:8082/api/housekeeping/rooms/204/mark-clean
```

### Room Service (port 8083)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/room-service/orders` | Place order (TS-04) |
| POST | `/api/room-service/orders/{id}/advance` | Advance state machine |
| GET  | `/api/room-service/orders/active` | Active orders |
| GET  | `/api/room-service/orders/room/{room}` | Orders for a room |

**Place order (TS-04):**
```bash
curl -X POST http://localhost:8083/api/room-service/orders \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "301",
    "items": ["2x Coffee", "Sandwich"],
    "totalAmount": 18.50
  }'
```

### Maintenance Service (port 8084)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/maintenance/issues` | Report issue (TS-05) |
| POST | `/api/maintenance/issues/{id}/resolve` | Resolve issue |
| GET  | `/api/maintenance/queue` | Priority queue (CRITICAL first) |
| GET  | `/api/maintenance/issues` | All issues |

**Report critical issue (TS-05):**
```bash
curl -X POST http://localhost:8084/api/maintenance/issues \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "115",
    "description": "Broken shower — water flooding bathroom",
    "urgency": "CRITICAL"
  }'
```

---

## Test Scenarios Walkthrough

### TS-01: Check-in with floor preference
```bash
curl -X POST http://localhost:8081/api/reception/checkin -u admin:hotelos2024 \
  -H "Content-Type: application/json" \
  -d '{"guestName":"John Doe","requestedRoomType":"DOUBLE","preferredFloor":2,"checkInDate":"2024-06-01","discountPercent":0}'
```

### TS-02 → TS-03: Full checkout + housekeeping flow
```bash
# 1. Check out guest
curl -X POST http://localhost:8081/api/reception/checkout/204 -u admin:hotelos2024

# 2. Housekeeping receives room.vacated event automatically (check queue)
curl http://localhost:8082/api/housekeeping/queue

# 3. Start cleaning
curl -X POST http://localhost:8082/api/housekeeping/rooms/204/start-cleaning \
  -H "Content-Type: application/json" -d '{"housekeeper":"Maria"}'

# 4. Mark clean — dashboard updates live via WebSocket
curl -X POST http://localhost:8082/api/housekeeping/rooms/204/mark-clean
```

### TS-04: Room service order lifecycle
```bash
ORDER=$(curl -s -X POST http://localhost:8083/api/room-service/orders \
  -H "Content-Type: application/json" \
  -d '{"roomNumber":"301","items":["2x Coffee","Sandwich"],"totalAmount":18.50}' | jq .id)
curl -X POST http://localhost:8083/api/room-service/orders/$ORDER/advance  # PREPARING
curl -X POST http://localhost:8083/api/room-service/orders/$ORDER/advance  # OUT_FOR_DELIVERY
curl -X POST http://localhost:8083/api/room-service/orders/$ORDER/advance  # DELIVERED
```

### TS-05: Critical maintenance issue
```bash
curl -X POST http://localhost:8084/api/maintenance/issues \
  -H "Content-Type: application/json" \
  -d '{"roomNumber":"115","description":"Broken shower","urgency":"CRITICAL"}'
# Verify it's at front of priority queue:
curl http://localhost:8084/api/maintenance/queue
```

### TS-06: Simultaneous check-in (no double booking)
The @Transactional annotation on checkIn() ensures atomic room locking.
Run two concurrent requests — they will receive different rooms.

### TS-07: No rooms available
```bash
curl -X POST http://localhost:8081/api/reception/checkin -u admin:hotelos2024 \
  -H "Content-Type: application/json" \
  -d '{"guestName":"Eve","requestedRoomType":"SUITE","checkInDate":"2024-06-01","discountPercent":0}'
# Returns informative message, no crash
```

### TS-08: Invalid room number
```bash
curl -X POST http://localhost:8081/api/reception/checkout/abc -u admin:hotelos2024
# Returns HTTP 400 with validation error, no crash
```

---

## Running Tests

```bash
# All tests
./gradlew test

# Single service
./gradlew :reception-service:test
./gradlew :housekeeping-service:test
./gradlew :room-service:test
./gradlew :maintenance-service:test

# Test report (HTML)
open reception-service/build/reports/tests/test/index.html
```

Tests use H2 in-memory database. RabbitMQ calls are mocked via Mockito.

---

## Git Log (for submission)

```bash
git log --oneline
```

Minimum 10 commits demonstrating development progression.

---

## Dashboard Credentials

| Username | Password | Role |
|----------|----------|------|
| admin | hotelos2024 | Full access |
| staff | hotel123 | Read-only |

