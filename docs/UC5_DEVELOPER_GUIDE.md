# UC-5 Monitor Compliance — Developer Guide

CS 3354 Team 7 · Child Welfare & Foster Care Tracking System

---

## Table of Contents

1. [Overview](#1-overview)
2. [Data Model](#2-data-model)
3. [Repository Layer](#3-repository-layer)
4. [Service Layer](#4-service-layer)
5. [Controller Endpoints](#5-controller-endpoints)
6. [Templates](#6-templates)
7. [Security Configuration](#7-security-configuration)
8. [Sample Data](#8-sample-data)
9. [Running and Testing](#9-running-and-testing)

---

## 1. Overview

UC-5 (Monitor Compliance) lets caseworkers track upcoming and overdue deadlines, supervisors review
compliance across all caseworkers, and administrators trigger the nightly compliance check on demand.

The core mechanism is a scheduled job (`ComplianceSchedulerService`) that runs at midnight CST.
It inspects every open deadline, generates in-system alerts for items due in 7 days, 1 day, or today,
and flags any items that are already past due. Results are stored in a singleton (`SchedulerState`) so
dashboards can display them without re-running the check.

### Involved roles

| Role | Landing page | Compliance surface |
|------|--------------|--------------------|
| Caseworker | `/caseworker/home` | Alerts + overdue deadlines + visit schedule |
| Supervisor | `/supervisor/dashboard` | Per-caseworker on-track rates; filterable overdue list |
| Administrator | `/admin/scheduler` | Last run summary; manual trigger button |

---

## 2. Data Model

All entity classes live in `src/main/java/team7/model/`.

### 2.1 `Deadline`

Represents one compliance task assigned to a caseworker for a specific case.

| Field | Type | Notes |
|-------|------|-------|
| `id` | `Long` | Auto-generated PK |
| `description` | `String` | E.g. "Court report", "Service plan review" |
| `caseRecord` | `Case` (eager) | The case this deadline belongs to |
| `assignedTo` | `User` (eager) | Caseworker responsible |
| `dueDate` | `LocalDate` | Compliance deadline |
| `met` | `boolean` | `true` once the caseworker marks it complete |
| `metDate` | `LocalDate` | Null until marked met |

Key methods:

```java
boolean isOverdue()      // !met && LocalDate.now().isAfter(dueDate)
long    daysRemaining()  // ChronoUnit.DAYS.between(now, dueDate) — negative when overdue
```

### 2.2 `Alert`

An in-system notification created by the scheduler when a deadline crosses an alert threshold.
No email or push notification is sent.

| Field | Type | Notes |
|-------|------|-------|
| `id` | `Long` | Auto-generated PK |
| `deadline` | `Deadline` (eager) | The deadline this alert refers to |
| `recipient` | `User` (eager) | Caseworker who receives the alert |
| `sentAt` | `LocalDateTime` | Timestamp of creation |
| `daysBeforeDue` | `int` | Must be 7, 1, or 0 |
| `read` | `boolean` | `false` until the caseworker dismisses it |

### 2.3 `Visit`

A scheduled or completed field visit by a caseworker.

| Field | Type | Notes |
|-------|------|-------|
| `id` | `Long` | Auto-generated PK |
| `caseRecord` | `Case` (eager) | Case the visit belongs to |
| `scheduledDate` | `LocalDate` | Planned visit date |
| `visitType` | `String` | E.g. "Home visit", "School visit" |
| `status` | `String` | `SCHEDULED`, `COMPLETED`, `MISSED`, `OVERDUE` |
| `notes` | `String` | Optional; up to 1 000 characters |

Status constants are defined as `public static final String` fields on the entity.

```java
boolean isOverdue()           // status.equals(SCHEDULED) && now.isAfter(scheduledDate)
void    markCompleted(notes)  // sets status = COMPLETED
void    markMissed()          // sets status = MISSED
```

### 2.4 `Case`

Parent record linking a child to a caseworker. Many `Deadline` and `Visit` records reference a case.

| Field | Type | Notes |
|-------|------|-------|
| `id` | `String` | Business key, e.g. "C-1088" |
| `child` | `Child` | The child in care |
| `assignedCaseworker` | `User` | Responsible caseworker |
| `status` | `String` | "Open" or "Closed" |

### 2.5 `User`

| Field | Type | Notes |
|-------|------|-------|
| `id` | `Long` | Auto-generated PK |
| `username` | `String` | Unique; used for Spring Security authentication |
| `passwordHash` | `String` | BCrypt-encoded |
| `displayName` | `String` | Full name shown in the UI |
| `role` | `Role` | `CASEWORKER`, `SUPERVISOR`, or `ADMINISTRATOR` |

---

## 3. Repository Layer

All repositories extend `JpaRepository` and live in `src/main/java/team7/repository/`.

### 3.1 `DeadlineRepository`

```java
List<Deadline> findByMetFalse();
List<Deadline> findByAssignedToUsernameAndMetFalse(String username);
```

The scheduler calls `findByMetFalse()` to retrieve all open deadlines.
The caseworker dashboard calls the second method.

### 3.2 `AlertRepository`

```java
List<Alert> findByRecipientUsernameAndReadFalseOrderBySentAtDesc(String username);
boolean     existsByDeadlineIdAndDaysBeforeDue(Long deadlineId, int daysBeforeDue);
```

`existsByDeadlineIdAndDaysBeforeDue` is the duplicate-prevention guard: before creating a new alert,
the scheduler checks whether one has already been sent for the same deadline at the same threshold.

### 3.3 `VisitRepository`

```java
List<Visit> findByCaseRecordAssignedCaseworkerUsernameOrderByScheduledDateAsc(String username);
```

Returns all visits for a caseworker sorted by date, used by both the home and compliance pages.

### 3.4 `UserRepository`

```java
Optional<User> findByUsername(String username);
List<User>     findByRole(Role role);
```

`findByRole(Role.CASEWORKER)` powers the supervisor's per-caseworker statistics table.

---

## 4. Service Layer

### 4.1 `ComplianceSchedulerService`

Located at `src/main/java/team7/service/ComplianceSchedulerService.java`.
This is the central compliance class. All other compliance queries and calculations go through it.

#### 4.1.1 Scheduled job

```java
@Scheduled(cron = "0 0 0 * * ?", zone = "America/Chicago")
public void scheduledCheck() {
    SchedulerRunResult result = checkDeadlines();
    schedulerState.setLastRun(result);
}
```

The same method is callable manually from `AdminController`.

#### 4.1.2 `checkDeadlines()`

```
for each open deadline (met == false):
    if now > dueDate        → flag as "Overdue" (no new alert)
    if daysRemaining in {0, 1, 7}:
        if no existing alert for (deadline, daysBeforeDue):
            create and save Alert
    else                    → no action
```

Returns a `SchedulerRunResult` with per-deadline outcomes and aggregate counts.

#### 4.1.3 `calculateOnTrackRate(String username)`

```
effectiveItems = effectiveDeadlines(deadlines, visits) + visits
rate = (effectiveItems.size() - overdueCount) * 100.0 / effectiveItems.size()
```

Defaults to 100 % when a caseworker has no items.

#### 4.1.4 `effectiveDeadlines(deadlines, visits)` — deduplication

If a case already has an `OVERDUE` or `MISSED` visit, any deadline whose description contains
"visit" (case-insensitive) for the same case is excluded from the count.
This prevents double-counting the same compliance failure.

#### 4.1.5 Supervisor-facing methods

| Method | Purpose |
|--------|---------|
| `getOverallStats()` | Aggregate on-track rate, overdue count, etc. for all caseworkers |
| `getAllCaseworkerStats()` | Per-caseworker `CaseworkerStat` list for the supervisor table |
| `filterByCaseworker(username)` | Detailed overdue items list for one caseworker |

### 4.2 Supporting value objects

| Class | Purpose |
|-------|---------|
| `SchedulerRunResult` | Immutable result of one scheduler run (counts + per-deadline outcomes) |
| `DeadlineCheckResult` | Outcome and CSS class for a single deadline check |
| `OverdueItem` | Display-ready overdue item (title, detail, tag, type) for supervisor view |
| `CaseworkerStat` | Per-caseworker metrics including `getBarWidthStyle()` for the progress bar |
| `CaseView` | Read-only case row with derived tag for the caseworker "My cases" list |

### 4.3 `SchedulerState`

A plain `@Component` singleton that holds the last `SchedulerRunResult` in memory.
Data is lost on restart; the sample data seeder does **not** pre-populate it.

```java
@Component
public class SchedulerState {
    private SchedulerRunResult lastRun;   // null until first run
    // getters and setters
}
```

> **Note for tests**: because `SchedulerState` is a simple POJO bean (no Mockito mocking needed),
> inject the real bean in `@SpringBootTest` tests. Mocking it on Java 25 is unreliable.

---

## 5. Controller Endpoints

All controllers extend `ControllerSupport`, which provides a `@ModelAttribute` that injects
`username`, `displayName`, and `role` into every model automatically.

### 5.1 Caseworker — `CaseworkerController`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `GET /caseworker/home` | — | Home dashboard: alerts, overdue deadlines, upcoming visits, case list |
| `GET /caseworker/compliance` | — | Dedicated compliance tab with the same alert and visit data |

Both endpoints load:

```java
model.addAttribute("alerts",           schedulerService.getAlertsForUser(username));
model.addAttribute("overdueDeadlines", schedulerService.getOverdueDeadlinesForUser(username));
model.addAttribute("visits",           visitRepo.findByCaseRecordAssignedCaseworkerUsernameOrderByScheduledDateAsc(username));
```

**Ownership enforcement**: Every case-specific endpoint (`/cases/{id}`, `/cases/{id}/timeline`,
`/cases/{id}/end-placement`, `/cases/{id}/new-placement`) calls `requireOwner(c, auth)`, which
throws `AccessDeniedException` if the authenticated user is not the case's assigned caseworker.
Spring Security converts this to a `403 Forbidden` response.

### 5.2 Supervisor — `SupervisorController`

| Endpoint | Method | Query param | Description |
|----------|--------|-------------|-------------|
| `GET /supervisor/dashboard` | — | — | Overall compliance view |
| `GET /supervisor/dashboard` | — | `caseworker=<username>` | Filtered view for one caseworker |

**Overall view** model attributes:

```java
model.addAttribute("stats",           schedulerService.getOverallStats());
model.addAttribute("caseworkerStats", schedulerService.getAllCaseworkerStats());
model.addAttribute("lastRun",         schedulerState.getLastRun());
```

**Filtered view** adds:

```java
model.addAttribute("selectedCaseworker", user);
model.addAttribute("onTrackRate",        rate);
model.addAttribute("onTrackRateRaw",     rate);
model.addAttribute("rateClass",          rateClass);  // "ok", "warn", or "danger"
model.addAttribute("overdueItems",       schedulerService.filterByCaseworker(username));
model.addAttribute("missedVisits",       missedVisitCount);
```

### 5.3 Admin — `AdminController`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `GET  /admin/scheduler` | — | Displays last scheduler run result |
| `POST /admin/run-check` | — | Manually triggers `checkDeadlines()`; redirects with flash message |

Manual trigger code:

```java
SchedulerRunResult result = schedulerService.checkDeadlines();
schedulerState.setLastRun(result);
ra.addFlashAttribute("message", "Check complete: " + result.getAlertsSent() + " alerts sent, "
    + result.getOverdueCount() + " items flagged overdue.");
return "redirect:/admin/scheduler";
```

---

## 6. Templates

All templates use Thymeleaf 3.1 and are in `src/main/resources/templates/`.

| Template | Route | Key model attributes |
|----------|-------|---------------------|
| `caseworker/home.html` | `/caseworker/home` | `alerts`, `overdueDeadlines`, `visits`, `cases` |
| `caseworker/compliance.html` | `/caseworker/compliance` | `alerts`, `overdueDeadlines`, `visits` |
| `supervisor/dashboard.html` | `/supervisor/dashboard` | `stats`, `caseworkerStats`, `lastRun`, (filtered: `selectedCaseworker`, `overdueItems`, …) |
| `admin/scheduler.html` | `/admin/scheduler` | `result`, `hasRun` |

### Thymeleaf `th:style` note

Expressions inside `th:style` **must** be wrapped in `${}`:

```html
<!-- Correct -->
<span th:style="${'width:' + stat.onTrackRateRaw + '%'}"></span>

<!-- Broken — Thymeleaf ParseException, partial page render -->
<span th:style="'width:' + stat.onTrackRateRaw + '%'"></span>
```

### En-dash in string literals

Thymeleaf/SpEL does **not** interpret `\u2013` Unicode escapes (that is a Java source-code feature).
Use the literal `–` character in templates:

```html
<!-- Correct -->
th:text="${d.description + ' – ' + d.caseRecord.id}"

<!-- Broken — renders the five characters \, u, 2, 0, 1, 3 -->
th:text="${d.description + ' \u2013 ' + d.caseRecord.id}"
```

---

## 7. Security Configuration

`SecurityConfig` (`src/main/java/team7/config/SecurityConfig.java`) defines role-based access:

```
/caseworker/**  → ROLE_CASEWORKER
/supervisor/**  → ROLE_SUPERVISOR
/admin/**       → ROLE_ADMINISTRATOR
/h2-console/**  → ROLE_ADMINISTRATOR
```

An `AccessDeniedException` thrown in a controller (e.g., the ownership check) results in a
`403 Forbidden` response via `accessDeniedPage("/403")`.

Authentication uses BCrypt-encoded passwords. Credentials are seeded at startup (see Section 8).

---

## 8. Sample Data

`DataLoader` (`src/main/java/team7/DataLoader.java`) runs on startup and creates all data fresh.
All passwords are `demo`, BCrypt-encoded at seed time.

### Demo accounts

| Username | Role | Purpose |
|----------|------|---------|
| `m.garcia` | Caseworker | Primary caseworker with active cases and alerts |
| `k.brown` | Supervisor | Compliance dashboard and filtered views |
| `admin` | Administrator | Scheduler page and H2 console |
| `d.lee`, `p.shah`, `t.becker`, `a.lopez`, `c.ward` | Caseworker | Additional caseworkers for supervisor table |

### Deadlines seeded (relative to today's date)

| Description | Case | Due | Expected outcome |
|-------------|------|-----|-----------------|
| Court report | C-1042 | today | Alert: "Due today" |
| Service plan review | C-1077 | +1 day | Alert: "1 day" |
| Medical exam record | C-1103 | +7 days | Alert: "7 days" |
| School enrollment form | C-1088 | +5 days | No alert (not in threshold) |
| Court report | C-1066 | +7 days | No alert (already met) |
| Permanency review | C-1059 | −3 days | Flagged overdue |
| Home visit deadline | C-1088 | −4 days | Flagged overdue (deduped against visit) |

### Visits seeded

| Case | Type | Status | Scheduled |
|------|------|--------|-----------|
| C-1088 Noah | Home visit | OVERDUE | −4 days |
| C-1092 Ava | School visit | MISSED | −1 day |
| C-1042 Jordan | Home visit | SCHEDULED | +2 days |
| C-1077 Liam | Home visit | SCHEDULED | +6 days |
| C-1103 Mia | Home visit | COMPLETED | −7 days |

---

## 9. Running and Testing

### Start the app

```bash
cd app
./mvnw spring-boot:run      # Mac / Linux
mvnw.cmd spring-boot:run    # Windows
```

Open `http://localhost:8080`.

### Run tests

```bash
cd app
./mvnw test
```

Expected: **35 tests pass** — 8 `PlacementServiceTest`, 9 `ComplianceSchedulerServiceTest`,
18 `NavControllerTest`.

`NavControllerTest` includes a `CaseworkerOwnership` nested class with 5 tests that verify the
403 access-denied behavior for cross-caseworker case access.

### H2 console

`http://localhost:8080/h2-console` — accessible by `admin` only.

JDBC URL: `jdbc:h2:mem:fostercare`  
Username: `sa`  
Password: (empty)

### Trigger the compliance check manually

1. Log in as `admin` / `demo`
2. Navigate to **Nightly scheduler** (`/admin/scheduler`)
3. Click **Run check now**

The page shows per-deadline outcomes and aggregate counts. The supervisor dashboard and caseworker
compliance pages reflect the new alerts immediately.
