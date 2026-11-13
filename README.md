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

Then open http://localhost:8080 in a browser.

---

## Demo accounts (password: `demo`)

| Username | Role | Landing page |
|----------|------|--------------|
| `m.garcia` | Caseworker | My cases list · deadline alerts · visit schedule |
| `k.brown` | Supervisor | Compliance dashboard (all caseworkers / filtered) |
| `admin` | Administrator | Nightly scheduler page · H2 console |

---

## Demo walkthrough

### UC-2 — Noah placement change scenario

1. Log in as `m.garcia` / `demo`
2. **My cases** → click C-1088 Noah K.
3. **End placement** → enter a reason → confirm Patel Home discharge
4. **New placement** → select Johnson Home → verify event auto-logged in the case timeline

### UC-5 — Compliance monitoring scenario

1. Log in as `admin` / `demo`
2. **Nightly deadline check** → click **Run check now**
   - Expected: 3 alerts sent, 4 items flagged overdue across all caseworkers
3. Log in as `m.garcia` → **My work** → verify 3 deadline alerts appear
4. Log in as `k.brown` → **Compliance dashboard** → review per-caseworker on-track rate bars
5. Select **Maria Garcia** in the filter dropdown → verify overdue items list (3 items)

---

## Running tests

```bash
cd app
./mvnw test
```

**35 tests** (8 PlacementServiceTest + 9 ComplianceSchedulerServiceTest + 18 NavControllerTest) — all pass.

---

## For developers

See [docs/UC5_DEVELOPER_GUIDE.md](docs/UC5_DEVELOPER_GUIDE.md) for a detailed walkthrough of the
UC-5 compliance monitoring implementation (data model, service logic, endpoints, templates, and tests).

---

## Notes

- **Database**: Deliverable 1 specifies PostgreSQL, but H2 in-memory DB is used here for zero-setup TA convenience.
- **Sample data**: Reset on every startup. Deadline and visit dates are computed relative to the current date, so compliance rates are always meaningful regardless of when the app is run.
- **H2 console**: http://localhost:8080/h2-console — accessible by `admin` only. JDBC URL: `jdbc:h2:mem:fostercare`
