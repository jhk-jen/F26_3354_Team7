package team7;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import team7.model.Alert;
import team7.model.Case;
import team7.model.CaseEvent;
import team7.model.Child;
import team7.model.Deadline;
import team7.model.FosterHome;
import team7.model.Placement;
import team7.model.Role;
import team7.model.User;
import team7.model.Visit;
import team7.repository.AlertRepository;
import team7.repository.CaseEventRepository;
import team7.repository.CaseRepository;
import team7.repository.ChildRepository;
import team7.repository.DeadlineRepository;
import team7.repository.FosterHomeRepository;
import team7.repository.PlacementRepository;
import team7.repository.UserRepository;
import team7.repository.VisitRepository;

/**
 * Loads synthetic sample data on startup.
 * All dates are relative to LocalDate.now() so the app works on any run date.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository       userRepo;
    private final ChildRepository      childRepo;
    private final CaseRepository       caseRepo;
    private final FosterHomeRepository homeRepo;
    private final PlacementRepository  placementRepo;
    private final CaseEventRepository  eventRepo;
    private final VisitRepository      visitRepo;
    private final DeadlineRepository   deadlineRepo;
    private final AlertRepository      alertRepo;
    private final PasswordEncoder      encoder;

    public DataLoader(UserRepository userRepo, ChildRepository childRepo,
                      CaseRepository caseRepo, FosterHomeRepository homeRepo,
                      PlacementRepository placementRepo, CaseEventRepository eventRepo,
                      VisitRepository visitRepo, DeadlineRepository deadlineRepo,
                      AlertRepository alertRepo, PasswordEncoder encoder) {
        this.userRepo     = userRepo;
        this.childRepo    = childRepo;
        this.caseRepo     = caseRepo;
        this.homeRepo     = homeRepo;
        this.placementRepo = placementRepo;
        this.eventRepo    = eventRepo;
        this.visitRepo    = visitRepo;
        this.deadlineRepo = deadlineRepo;
        this.alertRepo    = alertRepo;
        this.encoder      = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        LocalDate today = LocalDate.now();
        String pw = encoder.encode("demo");

        // ── Users ──────────────────────────────────────────────────────────────
        User maria   = userRepo.save(new User("m.garcia", pw, "Maria Garcia",   Role.CASEWORKER));
        User karen   = userRepo.save(new User("k.brown",  pw, "Karen Brown",    Role.SUPERVISOR));
        User admin   = userRepo.save(new User("admin",    pw, "System Admin",   Role.ADMINISTRATOR));
        User david   = userRepo.save(new User("d.lee",    pw, "David Lee",      Role.CASEWORKER));
        User priya   = userRepo.save(new User("p.shah",   pw, "Priya Shah",     Role.CASEWORKER));
        User tom     = userRepo.save(new User("t.becker", pw, "Tom Becker",     Role.CASEWORKER));
        User ana     = userRepo.save(new User("a.lopez",  pw, "Ana Lopez",      Role.CASEWORKER));
        User chris   = userRepo.save(new User("c.ward",   pw, "Chris Ward",     Role.CASEWORKER));

        // ── Children ───────────────────────────────────────────────────────────
        Child noah   = childRepo.save(new Child("2231", "Noah K."));
        Child ava    = childRepo.save(new Child("2245", "Ava R."));
        Child jordan = childRepo.save(new Child("2142", "Jordan P."));
        Child liam   = childRepo.save(new Child("2198", "Liam T."));
        Child mia    = childRepo.save(new Child("2260", "Mia S."));
        Child eli    = childRepo.save(new Child("2175", "Eli M."));   // C-1059
        Child zoe    = childRepo.save(new Child("2188", "Zoe C."));   // C-1066

        // ── Cases ──────────────────────────────────────────────────────────────
        Case c1088 = new Case("C-1088", noah,   maria, "Open");
        c1088.setStatusFlag("Placement change needed");
        caseRepo.save(c1088);
        Case c1092 = caseRepo.save(new Case("C-1092", ava,    maria, "Open"));
        Case c1042 = caseRepo.save(new Case("C-1042", jordan, maria, "Open"));
        Case c1077 = caseRepo.save(new Case("C-1077", liam,   maria, "Open"));
        Case c1103 = caseRepo.save(new Case("C-1103", mia,    maria, "Open"));
        Case c1059 = caseRepo.save(new Case("C-1059", eli,    maria, "Open"));
        Case c1066 = caseRepo.save(new Case("C-1066", zoe,    david, "Open"));

        // ── Foster Homes ───────────────────────────────────────────────────────
        // currentOccupancy accounts for all active placements seeded below.
        // Patel Home: Noah is there → 1 of 4 (we show it as 3/4 to suggest
        //   other children not in the sample, but keep occupancy consistent).
        FosterHome patel    = homeRepo.save(new FosterHome("Patel Home",          "Irving, TX",     4, 3));
        FosterHome johnson  = homeRepo.save(new FosterHome("Johnson Home",         "Richardson, TX", 4, 2));
        FosterHome nguyen   = homeRepo.save(new FosterHome("Nguyen Family Home",   "Plano, TX",      3, 2));
        FosterHome rivera   = homeRepo.save(new FosterHome("Rivera Family Home",   "Garland, TX",    4, 4));
        FosterHome smith    = homeRepo.save(new FosterHome("Smith Home",           "Dallas, TX",     2, 2));

        // ── Placements ─────────────────────────────────────────────────────────
        // Noah at Patel Home — active (the "placement change needed" scenario)
        LocalDate noahStart = LocalDate.of(2025, 3, 14);
        Placement p1 = placementRepo.save(new Placement(c1088, patel, noahStart));

        // Ava at Nguyen
        Placement p2 = placementRepo.save(new Placement(c1092, nguyen, today.minusDays(200)));

        // Jordan at Johnson
        Placement p3 = placementRepo.save(new Placement(c1042, johnson, today.minusDays(300)));

        // Liam at Smith
        Placement p4 = placementRepo.save(new Placement(c1077, smith, today.minusDays(150)));

        // Mia at Rivera
        Placement p5 = placementRepo.save(new Placement(c1103, rivera, today.minusDays(100)));

        // Eli at Johnson (past, ended)
        Placement p6 = new Placement(c1059, johnson, today.minusDays(730));
        p6.setEndDate(today.minusDays(400));
        p6.setChangeReason("Reunified with family");
        p6.setActive(false);
        placementRepo.save(p6);

        // Zoe at Patel (past, ended)
        Placement p7 = new Placement(c1066, patel, today.minusDays(500));
        p7.setEndDate(today.minusDays(200));
        p7.setChangeReason("Court order: transfer to relative placement");
        p7.setActive(false);
        placementRepo.save(p7);

        // ── Case Events ────────────────────────────────────────────────────────
        // C-1088 Noah
        eventRepo.save(new CaseEvent(c1088,
            LocalDateTime.of(noahStart, LocalTime.of(11, 22)),
            "K. Brown (Supervisor)", "Placement started: Patel Home"));
        eventRepo.save(new CaseEvent(c1088,
            today.minusDays(35).atTime(14, 5),
            "M. Garcia", "Home visit completed, notes added"));
        eventRepo.save(new CaseEvent(c1088,
            today.minusDays(70).atTime(10, 30),
            "M. Garcia", "Service plan review completed"));

        // C-1092 Ava
        eventRepo.save(new CaseEvent(c1092,
            today.minusDays(200).atTime(9, 0),
            "M. Garcia", "Placement started: Nguyen Family Home"));
        eventRepo.save(new CaseEvent(c1092,
            today.minusDays(1).atTime(15, 0),
            "M. Garcia", "Scheduled school visit – caseworker unable to attend (Missed)"));

        // C-1042 Jordan
        eventRepo.save(new CaseEvent(c1042,
            today.minusDays(300).atTime(8, 45),
            "M. Garcia", "Placement started: Johnson Home"));
        eventRepo.save(new CaseEvent(c1042,
            today.minusDays(14).atTime(13, 20),
            "M. Garcia", "Home visit completed"));

        // C-1077 Liam
        eventRepo.save(new CaseEvent(c1077,
            today.minusDays(150).atTime(11, 0),
            "M. Garcia", "Placement started: Smith Home"));

        // C-1103 Mia
        eventRepo.save(new CaseEvent(c1103,
            today.minusDays(100).atTime(9, 30),
            "M. Garcia", "Placement started: Rivera Family Home"));
        eventRepo.save(new CaseEvent(c1103,
            today.minusDays(7).atTime(14, 0),
            "M. Garcia", "Home visit completed, notes added"));

        // C-1059 Eli
        eventRepo.save(new CaseEvent(c1059,
            today.minusDays(730).atTime(10, 0),
            "M. Garcia", "Placement started: Johnson Home"));
        eventRepo.save(new CaseEvent(c1059,
            today.minusDays(400).atTime(16, 0),
            "M. Garcia", "Placement ended: Johnson Home (reason: Reunified with family)"));

        // C-1066 Zoe
        eventRepo.save(new CaseEvent(c1066,
            today.minusDays(500).atTime(9, 0),
            "D. Lee", "Placement started: Patel Home"));
        eventRepo.save(new CaseEvent(c1066,
            today.minusDays(200).atTime(11, 0),
            "D. Lee", "Placement ended: Patel Home (reason: Court order: transfer to relative placement)"));

        // ── Visits ─────────────────────────────────────────────────────────────
        // Noah C-1088: home visit overdue (-4 days) — matches UC-5 scheduler
        visitRepo.save(new Visit(c1088, today.minusDays(4), "Home visit", Visit.OVERDUE));

        // Ava C-1092: school visit missed (-1 day)
        visitRepo.save(new Visit(c1092, today.minusDays(1), "School visit", Visit.MISSED));

        // Jordan C-1042: home visit scheduled (+2 days)
        visitRepo.save(new Visit(c1042, today.plusDays(2), "Home visit", Visit.SCHEDULED));

        // Liam C-1077: home visit scheduled (+6 days)
        visitRepo.save(new Visit(c1077, today.plusDays(6), "Home visit", Visit.SCHEDULED));

        // Mia C-1103: home visit completed (-7 days)
        Visit miaVisit = new Visit(c1103, today.minusDays(7), "Home visit", Visit.COMPLETED);
        miaVisit.addNotes("Child is adjusting well. No concerns.");
        visitRepo.save(miaVisit);

        // Eli C-1059: completed long ago
        Visit eliVisit = new Visit(c1059, today.minusDays(420), "Home visit", Visit.COMPLETED);
        eliVisit.addNotes("Final visit before reunification.");
        visitRepo.save(eliVisit);

        // Zoe C-1066 (David Lee)
        visitRepo.save(new Visit(c1066, today.plusDays(5), "Home visit", Visit.SCHEDULED));
        visitRepo.save(new Visit(c1066, today.minusDays(14), "School visit", Visit.COMPLETED));

        // Extra visits for other caseworkers (compliance dashboard data)
        visitRepo.save(new Visit(c1042, today.minusDays(30), "Home visit", Visit.COMPLETED));
        visitRepo.save(new Visit(c1077, today.minusDays(20), "School visit", Visit.COMPLETED));
        visitRepo.save(new Visit(c1088, today.minusDays(65), "Home visit", Visit.COMPLETED));

        // ── Deadlines ──────────────────────────────────────────────────────────
        // These 7 rows match the UC-5 Nightly Scheduler table exactly.
        // All dates relative to today so the TA can run the app on any date.

        // 1. Court report C-1042 — due today (0 days) → alert
        deadlineRepo.save(new Deadline("Court report", c1042, maria, today));

        // 2. Service plan review C-1077 — due tomorrow (+1 day) → alert
        deadlineRepo.save(new Deadline("Service plan review", c1077, maria, today.plusDays(1)));

        // 3. Medical exam record C-1103 — due in 7 days → alert
        deadlineRepo.save(new Deadline("Medical exam record", c1103, maria, today.plusDays(7)));

        // 4. School enrollment form C-1088 — due in 5 days → no alert (not 7/1/0)
        deadlineRepo.save(new Deadline("School enrollment form", c1088, maria, today.plusDays(5)));

        // 5. Court report C-1066 — due in 7 days but already MET yesterday → no alert
        Deadline d5 = new Deadline("Court report", c1066, david, today.plusDays(7));
        d5.setMet(true);
        d5.setMetDate(today.minusDays(1));
        deadlineRepo.save(d5);

        // 6. Permanency review C-1059 — overdue by 3 days (-3)
        deadlineRepo.save(new Deadline("Permanency review", c1059, maria, today.minusDays(3)));

        // 7. Home visit deadline C-1088 — overdue by 4 days (-4)
        deadlineRepo.save(new Deadline("Home visit (Noah K.)", c1088, maria, today.minusDays(4)));

        // ── Extra data for Tom Becker (t.becker) for supervisor dashboard ─────
        Child sam   = childRepo.save(new Child("2301", "Sam W."));
        Child ruby  = childRepo.save(new Child("2302", "Ruby H."));
        Child oscar = childRepo.save(new Child("2303", "Oscar T."));
        Case  tb01  = caseRepo.save(new Case("C-1110", sam,   tom, "Open"));
        Case  tb02  = caseRepo.save(new Case("C-1111", ruby,  tom, "Open"));
        Case  tb03  = caseRepo.save(new Case("C-1112", oscar, tom, "Open"));
        FosterHome cedar = homeRepo.save(new FosterHome("Cedar Home", "Mesquite, TX", 3, 2));
        placementRepo.save(new Placement(tb01, cedar, today.minusDays(80)));
        // Tom's deadlines: 2 overdue, 1 on-track → triggers "Below target"
        deadlineRepo.save(new Deadline("Service plan review", tb01, tom, today.minusDays(5)));
        deadlineRepo.save(new Deadline("Court report",        tb02, tom, today.minusDays(2)));
        deadlineRepo.save(new Deadline("Medical exam",        tb03, tom, today.plusDays(10)));
        // Tom's visits
        visitRepo.save(new Visit(tb01, today.minusDays(6), "Home visit", Visit.OVERDUE));
        visitRepo.save(new Visit(tb02, today.plusDays(3),  "Home visit", Visit.SCHEDULED));

        // ── Extra data for Priya Shah (p.shah) ────────────────────────────────
        Child kai   = childRepo.save(new Child("2305", "Kai N."));
        Case  ps01  = caseRepo.save(new Case("C-1115", kai, priya, "Open"));
        placementRepo.save(new Placement(ps01, johnson, today.minusDays(50)));
        deadlineRepo.save(new Deadline("Service plan review", ps01, priya, today.plusDays(14)));
        visitRepo.save(new Visit(ps01, today.plusDays(4), "Home visit", Visit.SCHEDULED));

        // ── Extra data for Ana Lopez (a.lopez) ────────────────────────────────
        Child leo = childRepo.save(new Child("2310", "Leo B."));
        Case al01 = caseRepo.save(new Case("C-1120", leo, ana, "Open"));
        placementRepo.save(new Placement(al01, nguyen, today.minusDays(60)));
        deadlineRepo.save(new Deadline("Court report", al01, ana, today.plusDays(20)));
        visitRepo.save(new Visit(al01, today.minusDays(10), "Home visit", Visit.COMPLETED));

        // ── Extra data for Chris Ward (c.ward) ────────────────────────────────
        Child ivy = childRepo.save(new Child("2315", "Ivy C."));
        Case cw01 = caseRepo.save(new Case("C-1125", ivy, chris, "Open"));
        deadlineRepo.save(new Deadline("Service plan review", cw01, chris, today.plusDays(30)));
        visitRepo.save(new Visit(cw01, today.plusDays(7), "Home visit", Visit.SCHEDULED));

        // ── Alerts ─────────────────────────────────────────────────────────────
        // 0 alerts on startup — ComplianceScheduler creates them on demand.

        System.out.println("✔ Sample data loaded. Users: m.garcia, k.brown, admin, d.lee, p.shah, t.becker, a.lopez, c.ward (password: demo)");
    }
}
