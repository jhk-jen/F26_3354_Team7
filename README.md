# Team 7 - Child Welfare & Foster Care Tracking System

CS 3354 Software Engineering · Fall 2026 · UTD

Team members: Vijaya Yeddu, Jon Montague, Sahana Kothuru, Hyunjung Kwon, Nikhil Sugumar, Shreya Pejavar, Phillip Tran

---

## Scope

| Status | Use Case |
|--------|----------|
| **Implemented (working)** | UC-1 Log In & Access Records, UC-2 Manage Placement, UC-5 Monitor Compliance |
| **Design only** | UC-3 Search & View Case History, UC-4 Schedule & Record Visit |

---

## Getting the code

**Option A — Git clone**

```bash
git clone https://github.com/jhk-jen/F26_3354_Team7.git
cd F26_3354_Team7
```

**Option B — Download ZIP**

1. Click **Code → Download ZIP** on the GitHub repository page.
2. Unzip the archive and open a terminal in the extracted folder.

---

## Install dependencies

1. Install **Java 17 or later** (tested on Java 25). Check with `java -version`. If needed, download it from https://adoptium.net
2. Maven is **not** required. The included Maven Wrapper (`mvnw`) downloads Maven and all project dependencies automatically on the first run (internet connection required).

---

## Configure environment variables

No environment variables are required. All defaults are set in `app/src/main/resources/application.properties`:

| Setting | Default |
|---------|---------|
| Server port | `8080` |
| Database | H2 in-memory (`jdbc:h2:mem:fostercare`), no install or password needed |

Optional: to use a different port (for example, if 8080 is busy), set `SERVER_PORT` before running, then open that port instead (e.g. http://localhost:9090):
- Mac / Linux: `SERVER_PORT=9090 ./mvnw spring-boot:run`
- Windows (PowerShell): `$env:SERVER_PORT=9090; .\mvnw.cmd spring-boot:run`

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

These are demo-only accounts. All data in the app is synthetic.

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
3. Sign in as each demo account to verify role-based routing (FR-2):
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
6. Select any home with space → placement saved (FR-3). For example, **Johnson Home** (2 of 4 → 3 of 4) or **Cedar Home** (2 of 3 → 3 of 3, now full; this is the one used in our demo video).
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

**`mvnw: Permission denied` (ZIP download only)** — the wrapper script loses its execute bit when unzipped on some systems.

```bash
# Fix once, then use ./mvnw normally
chmod +x mvnw

# Or run without fixing permissions
sh mvnw spring-boot:run
```

**Data resets on restart** — all sample data is re-created from scratch on every startup.
To repeat a scenario from the beginning, stop the app (`Ctrl+C`) and run it again.

---

## For developers

See [docs/UC5_DEVELOPER_GUIDE.md](docs/UC5_DEVELOPER_GUIDE.md) for a detailed walkthrough of the
UC-5 compliance monitoring implementation (data model, service logic, endpoints, templates, and tests).

---

## Deliverables

- Final report: [docs/Team\_07\_SE3354\_F26\_Final\_Project%20Report.pdf](docs/Team_07_SE3354_F26_Final_Project%20Report.pdf)
- Presentation slides: [docs/Team\_07\_SE3354\_F26\_Project\_Class\_Presentation.pdf](docs/Team_07_SE3354_F26_Project_Class_Presentation.pdf)
- Clickable static prototype: `prototype/index.html` (open the file directly in a browser; no server needed)
- Unit tests for the sample unit (createPlacement): `app/src/test/java/team7/service/PlacementServiceTest.java`
- Run only these tests: `cd app` then `./mvnw -Dtest=PlacementServiceTest test` (Windows: `mvnw.cmd -Dtest=PlacementServiceTest test`)

---

## Notes

- **Database**: Deliverable 1 specifies PostgreSQL, but H2 in-memory DB is used here for zero-setup TA convenience.
- **Sample data**: Reset on every startup. Deadline and visit dates are computed relative to the current date, so compliance rates are always meaningful regardless of when the app is run.
- **H2 console**: http://localhost:8080/h2-console — accessible by `admin` only. JDBC URL: `jdbc:h2:mem:fostercare`
