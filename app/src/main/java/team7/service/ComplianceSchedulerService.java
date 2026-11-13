package team7.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import team7.model.Alert;
import team7.model.Deadline;
import team7.model.Role;
import team7.model.User;
import team7.model.Visit;
import team7.repository.AlertRepository;
import team7.repository.CaseRepository;
import team7.repository.DeadlineRepository;
import team7.repository.UserRepository;
import team7.repository.VisitRepository;

@Service
public class ComplianceSchedulerService {

    private static final Set<Long> ALERT_DAYS = Set.of(7L, 1L, 0L);
    private static final DateTimeFormatter MDY = DateTimeFormatter.ofPattern("M/d/yyyy");
    private static final DateTimeFormatter MD  = DateTimeFormatter.ofPattern("M/d");

    private final DeadlineRepository deadlineRepo;
    private final AlertRepository    alertRepo;
    private final VisitRepository    visitRepo;
    private final CaseRepository     caseRepo;
    private final UserRepository     userRepo;
    private final SchedulerState     schedulerState;

    public ComplianceSchedulerService(DeadlineRepository deadlineRepo,
                                       AlertRepository alertRepo,
                                       VisitRepository visitRepo,
                                       CaseRepository caseRepo,
                                       UserRepository userRepo,
                                       SchedulerState schedulerState) {
        this.deadlineRepo   = deadlineRepo;
        this.alertRepo      = alertRepo;
        this.visitRepo      = visitRepo;
        this.caseRepo       = caseRepo;
        this.userRepo       = userRepo;
        this.schedulerState = schedulerState;
    }

    // ── Scheduled nightly run ─────────────────────────────────────────────────

    @Scheduled(cron = "0 0 0 * * ?", zone = "America/Chicago")
    public void scheduledCheck() {
        checkDeadlines();
    }

    // ── Core: checkDeadlines() ────────────────────────────────────────────────

    @Transactional
    public SchedulerRunResult checkDeadlines() {
        List<Deadline> all = deadlineRepo.findAll();
        List<DeadlineCheckResult> results = new ArrayList<>();
        int alertsSent = 0;
        int overdueCount = 0;
        int noAlertCount = 0;

        for (Deadline d : all) {
            String outcome;
            String outcomeClass;

            if (d.isMet()) {
                String metStr = d.getMetDate() != null
                        ? "Met on " + d.getMetDate().format(MD)
                        : "Met";
                outcome = "No alert (" + metStr.toLowerCase() + ")";
                outcomeClass = "ok";
            } else if (d.isOverdue()) {
                overdueCount++;
                outcome = "Flagged overdue";
                outcomeClass = "danger";
            } else {
                long days = d.daysRemaining();
                if (ALERT_DAYS.contains(days)) {
                    // duplicate prevention
                    if (!alertRepo.existsByDeadlineIdAndDaysBeforeDue(d.getId(), (int) days)) {
                        Alert alert = new Alert(d, d.getAssignedTo(), LocalDateTime.now(), (int) days);
                        alertRepo.save(alert);
                        alertsSent++;
                    }
                    if (days == 0) {
                        outcome = "Alert sent (due today)";
                        outcomeClass = "danger";
                    } else if (days == 1) {
                        outcome = "Alert sent";
                        outcomeClass = "warn";
                    } else {
                        outcome = "Alert sent";
                        outcomeClass = "info";
                    }
                } else {
                    noAlertCount++;
                    outcome = "No alert";
                    outcomeClass = "neutral";
                }
            }
            results.add(new DeadlineCheckResult(d, outcome, outcomeClass));
        }

        SchedulerRunResult result = new SchedulerRunResult(
                LocalDateTime.now(), alertsSent, overdueCount, noAlertCount, all.size(), results);
        schedulerState.setLastResult(result);
        return result;
    }

    // ── calculateOnTrackRate() ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public double calculateOnTrackRate(String username) {
        List<Deadline> deadlines = deadlineRepo.findByAssignedToUsernameAndMetFalse(username);
        List<Visit>    visits    = visitRepo.findByCaseRecordAssignedCaseworkerUsernameOrderByScheduledDateAsc(username);

        // De-duplicate: skip visit-type deadlines whose case already has an OVERDUE/MISSED visit
        List<Deadline> effectiveDl = effectiveDeadlines(deadlines, visits);

        int total = effectiveDl.size() + visits.size();
        if (total == 0) return 100.0;

        long overdue = effectiveDl.stream().filter(Deadline::isOverdue).count()
                + visits.stream().filter(v ->
                        Visit.OVERDUE.equals(v.getStatus()) || Visit.MISSED.equals(v.getStatus())).count();

        return (total - overdue) * 100.0 / total;
    }

    // ── filterByCaseworker() ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<OverdueItem> filterByCaseworker(String username) {
        List<OverdueItem> items = new ArrayList<>();

        List<Deadline> deadlines = deadlineRepo.findByAssignedToUsernameAndMetFalse(username);
        List<Visit>    visits    = visitRepo.findByCaseRecordAssignedCaseworkerUsernameOrderByScheduledDateAsc(username);

        // De-duplicate: skip visit-type deadlines covered by a visit record for the same case
        for (Deadline d : effectiveDeadlines(deadlines, visits)) {
            if (d.isOverdue()) {
                long days = Math.abs(d.daysRemaining());
                items.add(new OverdueItem(
                        d.getDescription() + " \u2013 " + d.getCaseRecord().getId(),
                        "Due " + d.getDueDate().format(MDY),
                        "Overdue " + days + " day" + (days == 1 ? "" : "s"),
                        "deadline"));
            }
        }

        for (Visit v : visits) {
            if (Visit.OVERDUE.equals(v.getStatus()) || Visit.MISSED.equals(v.getStatus())) {
                String title = v.getVisitType() + " \u2013 "
                        + v.getCaseRecord().getChild().getName()
                        + " (" + v.getCaseRecord().getId() + ")";
                String detail = "Scheduled " + v.getScheduledDate().format(MDY);
                String tag = Visit.MISSED.equals(v.getStatus())
                        ? "Missed"
                        : "Overdue since " + v.getScheduledDate().format(MD);
                items.add(new OverdueItem(title, detail, tag, "visit"));
            }
        }
        return items;
    }

    /**
     * Removes visit-type deadlines (description contains "visit", case-insensitive)
     * for any case that already has an OVERDUE or MISSED visit record.
     * Prevents double-counting when the same event appears as both a Deadline and a Visit.
     */
    private List<Deadline> effectiveDeadlines(List<Deadline> deadlines, List<Visit> visits) {
        Set<String> casesWithProblemVisit = visits.stream()
                .filter(v -> Visit.OVERDUE.equals(v.getStatus()) || Visit.MISSED.equals(v.getStatus()))
                .map(v -> v.getCaseRecord().getId())
                .collect(Collectors.toCollection(HashSet::new));

        return deadlines.stream()
                .filter(d -> !(d.getDescription().toLowerCase().contains("visit")
                               && casesWithProblemVisit.contains(d.getCaseRecord().getId())))
                .toList();
    }

    // ── Alerts for caseworker home ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Alert> getAlertsForUser(String username) {
        return alertRepo.findByRecipientUsernameAndReadFalseOrderBySentAtDesc(username);
    }

    // ── Overdue deadlines for caseworker home ─────────────────────────────────

    @Transactional(readOnly = true)
    public List<Deadline> getOverdueDeadlinesForUser(String username) {
        return deadlineRepo.findByAssignedToUsernameAndMetFalse(username).stream()
                .filter(Deadline::isOverdue).toList();
    }

    // ── Overall stats for supervisor dashboard cards ───────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getOverallStats() {
        List<User> caseworkers = userRepo.findByRole(Role.CASEWORKER);
        int totalItems   = 0;
        int totalOverdue = 0;
        int dueIn7Days   = 0;
        int missedVisits = 0;

        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);

        for (User cw : caseworkers) {
            List<Deadline> dl = deadlineRepo.findByAssignedToUsernameAndMetFalse(cw.getUsername());
            List<Visit>    vs = visitRepo.findByCaseRecordAssignedCaseworkerUsernameOrderByScheduledDateAsc(cw.getUsername());
            List<Deadline> eff = effectiveDeadlines(dl, vs);
            totalItems   += eff.size() + vs.size();
            totalOverdue += (int)(eff.stream().filter(Deadline::isOverdue).count()
                    + vs.stream().filter(v -> Visit.OVERDUE.equals(v.getStatus()) || Visit.MISSED.equals(v.getStatus())).count());
            dueIn7Days   += (int) eff.stream()
                    .filter(d -> !d.isOverdue() && d.daysRemaining() >= 0 && d.daysRemaining() <= 7).count();
            missedVisits += (int) vs.stream()
                    .filter(v -> Visit.MISSED.equals(v.getStatus()) && !v.getScheduledDate().isBefore(thirtyDaysAgo)).count();
        }

        double rate = totalItems > 0 ? (totalItems - totalOverdue) * 100.0 / totalItems : 100.0;
        double overdueRate = totalItems > 0 ? totalOverdue * 100.0 / totalItems : 0.0;

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("onTrackRate",    String.format("%.1f", rate));
        m.put("onTrackRateRaw", rate);
        m.put("onTrackCount",   totalItems - totalOverdue);
        m.put("totalItems",     totalItems);
        m.put("overdueRate",    String.format("%.1f", overdueRate));
        m.put("overdueCount",   totalOverdue);
        m.put("dueIn7Days",     dueIn7Days);
        m.put("missedVisits",   missedVisits);
        return m;
    }

    // ── Per-caseworker stats for supervisor table ─────────────────────────────

    @Transactional(readOnly = true)
    public List<CaseworkerStat> getAllCaseworkerStats() {
        List<User> caseworkers = userRepo.findByRole(Role.CASEWORKER);
        List<CaseworkerStat> stats = new ArrayList<>();
        for (User cw : caseworkers) {
            int caseCount = caseRepo.findByAssignedCaseworkerUsername(cw.getUsername()).size();
            double rate   = calculateOnTrackRate(cw.getUsername());

            List<Deadline> dl  = deadlineRepo.findByAssignedToUsernameAndMetFalse(cw.getUsername());
            List<Visit>    vs  = visitRepo.findByCaseRecordAssignedCaseworkerUsernameOrderByScheduledDateAsc(cw.getUsername());
            List<Deadline> eff = effectiveDeadlines(dl, vs);
            int overdue = (int)(eff.stream().filter(Deadline::isOverdue).count()
                    + vs.stream().filter(v -> Visit.OVERDUE.equals(v.getStatus()) || Visit.MISSED.equals(v.getStatus())).count());

            String rateClass, rateLabel;
            if (rate >= 90)      { rateClass = "ok";     rateLabel = "On target"; }
            else if (rate >= 70) { rateClass = "warn";   rateLabel = "Needs attention"; }
            else                 { rateClass = "danger"; rateLabel = "Below target"; }

            stats.add(new CaseworkerStat(cw, caseCount, rate, overdue, rateClass, rateLabel));
        }
        return stats;
    }
}
