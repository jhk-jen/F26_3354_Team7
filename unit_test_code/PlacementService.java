package team7.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import team7.model.Case;
import team7.model.CaseEvent;
import team7.model.Deadline;
import team7.model.FosterHome;
import team7.model.Placement;
import team7.model.User;
import team7.model.Visit;
import team7.repository.CaseEventRepository;
import team7.repository.CaseRepository;
import team7.repository.DeadlineRepository;
import team7.repository.FosterHomeRepository;
import team7.repository.PlacementRepository;
import team7.repository.UserRepository;
import team7.repository.VisitRepository;

@Service
@Transactional
public class PlacementService {

    private final CaseRepository       caseRepo;
    private final PlacementRepository  placementRepo;
    private final FosterHomeRepository homeRepo;
    private final CaseEventRepository  eventRepo;
    private final UserRepository       userRepo;
    private final VisitRepository      visitRepo;
    private final DeadlineRepository   deadlineRepo;

    public PlacementService(CaseRepository caseRepo,
                             PlacementRepository placementRepo,
                             FosterHomeRepository homeRepo,
                             CaseEventRepository eventRepo,
                             UserRepository userRepo,
                             VisitRepository visitRepo,
                             DeadlineRepository deadlineRepo) {
        this.caseRepo     = caseRepo;
        this.placementRepo = placementRepo;
        this.homeRepo     = homeRepo;
        this.eventRepo    = eventRepo;
        this.userRepo     = userRepo;
        this.visitRepo    = visitRepo;
        this.deadlineRepo = deadlineRepo;
    }

    // ── Read methods ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Case getCase(String caseId) {
        return caseRepo.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Case not found: " + caseId));
    }

    @Transactional(readOnly = true)
    public Optional<Placement> getCurrentPlacement(String caseId) {
        return placementRepo.findByCaseRecordIdAndActiveTrue(caseId);
    }

    @Transactional(readOnly = true)
    public List<CaseEvent> getTimeline(String caseId) {
        return eventRepo.findByCaseRecordIdOrderByTimestampDesc(caseId);
    }

    @Transactional(readOnly = true)
    public List<FosterHome> getAllHomes() {
        return homeRepo.findAll(Sort.by("name"));
    }

    /**
     * Returns enriched case views for the "My cases" list.
     * Status tags are computed from visits and deadlines (most urgent wins).
     */
    @Transactional(readOnly = true)
    public List<CaseView> getCaseViews(String caseworkerUsername) {
        List<Case> cases = caseRepo.findByAssignedCaseworkerUsername(caseworkerUsername);
        List<CaseView> views = new ArrayList<>();
        for (Case c : cases) {
            Optional<Placement> active = placementRepo.findByCaseRecordIdAndActiveTrue(c.getId());
            String homeName = active.map(p -> p.getFosterHome().getName()).orElse(null);
            String[] tag = computeTag(c, active.isEmpty());
            views.add(new CaseView(c, homeName, tag[0], tag[1]));
        }
        return views;
    }

    // ── Write methods ─────────────────────────────────────────────────────────

    /**
     * Ends the active placement for a case (FR-4).
     * Decrements FosterHome.currentOccupancy.
     * Logs a CaseEvent automatically (FR-6).
     *
     * @throws IllegalArgumentException if reason is blank
     * @throws IllegalStateException    if no active placement exists
     */
    public void endPlacement(String caseId, LocalDate endDate,
                              String reason, String actorUsername) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Enter a reason for the placement change.");
        }
        Placement p = placementRepo.findByCaseRecordIdAndActiveTrue(caseId)
                .orElseThrow(() -> new IllegalStateException(
                        "No active placement for case " + caseId));

        FosterHome home = p.getFosterHome();
        p.setEndDate(endDate);
        p.setChangeReason(reason.strip());
        p.setActive(false);
        placementRepo.save(p);

        // FR-4: decrement occupancy
        home.setCurrentOccupancy(Math.max(0, home.getCurrentOccupancy() - 1));
        homeRepo.save(home);

        // FR-6: automatic case event
        User actor = userRepo.findByUsername(actorUsername).orElseThrow();
        String desc = "Placement ended: " + home.getName()
                + " (reason: " + reason.strip().toLowerCase() + ")";
        eventRepo.save(new CaseEvent(p.getCaseRecord(), LocalDateTime.now(),
                actor.getShortName(), desc));
    }

    /**
     * Creates a new placement for a case (FR-3, FR-5).
     * Checks home capacity before saving.
     * Increments FosterHome.currentOccupancy on success.
     * Logs a CaseEvent automatically (FR-6).
     *
     * @return PlacementResult — success or rejection with message
     * @throws IllegalStateException if case already has an active placement
     */
    public PlacementResult createPlacement(String caseId, Long homeId,
                                            LocalDate startDate, String actorUsername) {
        if (placementRepo.findByCaseRecordIdAndActiveTrue(caseId).isPresent()) {
            throw new IllegalStateException(
                    "Case " + caseId + " already has an active placement. End it first.");
        }

        Case c = caseRepo.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Case not found: " + caseId));
        FosterHome home = homeRepo.findById(homeId)
                .orElseThrow(() -> new IllegalArgumentException("Foster home not found: " + homeId));

        // FR-5: reject if at capacity
        if (home.isFull()) {
            return PlacementResult.rejected(home);
        }

        // FR-3: create placement
        Placement p = new Placement(c, home, startDate);
        placementRepo.save(p);

        // increment occupancy
        home.setCurrentOccupancy(home.getCurrentOccupancy() + 1);
        homeRepo.save(home);

        // FR-6: automatic case event
        User actor = userRepo.findByUsername(actorUsername).orElseThrow();
        eventRepo.save(new CaseEvent(c, LocalDateTime.now(),
                actor.getShortName(), "Placement started: " + home.getName()));

        return PlacementResult.success(p, home);
    }

    @Transactional(readOnly = true)
    public List<Visit> getVisitsForCaseworker(String username) {
        return visitRepo.findByCaseRecordAssignedCaseworkerUsernameOrderByScheduledDateAsc(username);
    }

    /**
     * Returns a map of caseId → active Placement for all cases assigned to the caseworker.
     * Cases without an active placement are absent from the map.
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Placement> getCurrentPlacementsMap(String username) {
        List<Case> cases = caseRepo.findByAssignedCaseworkerUsername(username);
        java.util.Map<String, Placement> map = new java.util.LinkedHashMap<>();
        for (Case c : cases) {
            placementRepo.findByCaseRecordIdAndActiveTrue(c.getId())
                    .ifPresent(p -> map.put(c.getId(), p));
        }
        return map;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Returns [tagClass, tagLabel] for the most urgent status.
     * Priority: due-today deadline > missed visit > overdue visit >
     *           overdue deadline > 1-day deadline > upcoming deadline > needs placement.
     */
    private String[] computeTag(Case c, boolean noActivePlacement) {
        // 0. Manual override via statusFlag (highest priority)
        if (c.getStatusFlag() != null && !c.getStatusFlag().isBlank()) {
            return new String[]{"warn", c.getStatusFlag()};
        }

        List<Deadline> deadlines = deadlineRepo.findByCaseRecordIdAndMetFalse(c.getId());
        List<Visit>    visits    = visitRepo.findByCaseRecordIdOrderByScheduledDateDesc(c.getId());

        // 1. Deadline due today
        for (Deadline d : deadlines) {
            if (!d.isOverdue() && d.daysRemaining() == 0) {
                return new String[]{"danger", d.getDescription() + " due today"};
            }
        }
        // 2. Missed visit
        for (Visit v : visits) {
            if (Visit.MISSED.equals(v.getStatus())) {
                return new String[]{"danger", "Missed visit"};
            }
        }
        // 3. Overdue visit
        for (Visit v : visits) {
            if (Visit.OVERDUE.equals(v.getStatus())) {
                return new String[]{"danger", "Visit overdue"};
            }
        }
        // 4. Overdue deadline
        for (Deadline d : deadlines) {
            if (d.isOverdue()) {
                return new String[]{"danger", d.getDescription() + " overdue"};
            }
        }
        // 5. Deadline due in 1 day
        for (Deadline d : deadlines) {
            if (!d.isOverdue() && d.daysRemaining() == 1) {
                return new String[]{"warn", d.getDescription() + " due in 1 day"};
            }
        }
        // 6. Deadline due in 2–7 days
        for (Deadline d : deadlines) {
            long days = d.daysRemaining();
            if (!d.isOverdue() && days >= 2 && days <= 7) {
                return new String[]{"info", d.getDescription() + " due in " + days + " days"};
            }
        }
        // 7. No active placement
        if (noActivePlacement) {
            return new String[]{"warn", "Needs placement"};
        }
        return new String[]{null, null};
    }
}
