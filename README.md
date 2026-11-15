# Team 7 - Child Welfare & Foster Care Tracking System

CS 3354 Software Engineering · Fall 2026 · UTD

---

## Scope

| Status | Use Case |
|--------|----------|
| **Implemented (working)** | UC-1 Log In & Access Records, UC-2 Manage Placement, UC-5 Monitor Compliance |
| **Design only** | UC-3 Search & View Case History, UC-4 Schedule & Record Visit |

---

## Requirements

- **Java 17 or later** (tested on Java 25)
- Maven is not required — Maven Wrapper (`mvnw`) is included

---

## Running the app

```bash
# Mac / Linux
cd app
./mvnw spring-boot:run

# Windows
cd app
mvnw.cmd spring-boot:run
```

Open http://localhost:8080 in a browser (redirects to the sign-in page).  
When you see `Started FosterCareApp` in the terminal, the server is ready.

---

## Demo accounts (password: `demo`)

| Username | Role | Landing page |
|----------|------|--------------|
| `m.garcia` | Caseworker | My Work · deadline alerts · visit schedule |
| `k.brown` | Supervisor | Compliance dashboard (all caseworkers / filtered) |
| `admin` | Administrator | Nightly scheduler page · H2 console |

---

## Demo walkthrough

### UC-1 — Sign-in and access control

1. Go to http://localhost:8080 — you are redirected to the sign-in page.
2. Sign in as `m.garcia` with the wrong password (e.g. `wrong`) → error message, stays on sign-in page (FR-1).
3. Sign in as each demo account to verify role-based routing (FR-1):
   - `m.garcia` → **My Work** (caseworker dashboard)
   - `k.brown` → **Compliance** (supervisor dashboard)
   - `admin` → **Nightly Scheduler** (admin page)
4. While signed in as `m.garcia`, open http://localhost:8080/admin/scheduler → **403 Access Denied** (FR-2).
5. While signed in as `m.garcia`, open http://localhost:8080/caseworker/cases/C-1066 → **403 Access Denied** — C-1066 belongs to another caseworker (FR-2).

### UC-2 — Noah placement change scenario

1. Sign in as `m.garcia` / `demo`.
2. **My Work** → click **C-1088 Noah K.**
3. **End placement** → submit with an empty reason field → error: *"Enter a reason for the placement change."* (FR-4).
4. Enter a reason → confirm discharge from Patel Home (FR-4).
5. **New placement** → select **Rivera Family Home** → rejected: home is full (FR-5).
6. Select **Johnson Home** → placement saved, now 3 of 4 beds occupied (FR-3).
7. **Timeline** → verify both events were logged automatically (FR-6).

### UC-5 — Compliance monitoring scenario

1. Sign in as `admin` / `demo`.
2. **Nightly deadline check** → click **Run check now** (FR-11).
   - Expected: 3 alerts sent, 4 items flagged overdue across all caseworkers.
3. Sign in as `m.garcia` → **My Work** → verify 3 deadline alerts appear (FR-11).
4. Click the **Compliance** tab → verify overdue items list (FR-12).
5. Sign in as `k.brown` → **Compliance** → review per-caseworker on-track rate bars (FR-12).
6. Select **Maria Garcia** in the filter dropdown → verify overdue items list (3 items) (FR-12).

---

## Running tests

```bash
cd app
./mvnw test
```

**39 tests** (8 PlacementServiceTest + 9 ComplianceSchedulerServiceTest + 22 NavControllerTest) — all pass.

---

## Troubleshooting

**Check your Java version** — Java 17 or later is required:

```bash
java -version
```

**Port 8080 already in use** — another process is holding the port.

- Mac / Linux: `kill $(lsof -t -i:8080)` then restart.
- Windows: open Task Manager → Details tab → end any `java.exe` process, then restart.

**Data resets on restart** — all sample data is re-created from scratch on every startup.
To repeat a scenario from the beginning, stop the app (`Ctrl+C`) and run it again.

---

## For developers

See [docs/UC5_DEVELOPER_GUIDE.md](docs/UC5_DEVELOPER_GUIDE.md) for a detailed walkthrough of the
UC-5 compliance monitoring implementation (data model, service logic, endpoints, templates, and tests).

---

## Notes

- **Database**: Deliverable 1 specifies PostgreSQL, but H2 in-memory DB is used here for zero-setup TA convenience.
- **Sample data**: Reset on every startup. Deadline and visit dates are computed relative to the current date, so compliance rates are always meaningful regardless of when the app is run.
- **H2 console**: http://localhost:8080/h2-console — accessible by `admin` only. JDBC URL: `jdbc:h2:mem:fostercare`
