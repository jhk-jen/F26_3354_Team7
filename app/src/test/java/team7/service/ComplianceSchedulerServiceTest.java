package team7.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import team7.model.Alert;
import team7.model.Case;
import team7.model.Child;
import team7.model.Deadline;
import team7.model.Role;
import team7.model.User;
import team7.model.Visit;
import team7.repository.AlertRepository;
import team7.repository.CaseRepository;
import team7.repository.DeadlineRepository;
import team7.repository.UserRepository;
import team7.repository.VisitRepository;

@ExtendWith(MockitoExtension.class)
class ComplianceSchedulerServiceTest {

    @Mock DeadlineRepository deadlineRepo;
    @Mock AlertRepository    alertRepo;
    @Mock VisitRepository    visitRepo;
    @Mock CaseRepository     caseRepo;
    @Mock UserRepository     userRepo;

    // SchedulerState is a plain bean with no dependencies — use a real instance
    SchedulerState schedulerState = new SchedulerState();

    ComplianceSchedulerService service;

    private User maria;
    private Case c1042;

    @BeforeEach
    void setUp() {
        service = new ComplianceSchedulerService(
                deadlineRepo, alertRepo, visitRepo, caseRepo, userRepo, schedulerState);
        maria = new User("m.garcia", "hash", "Maria Garcia", Role.CASEWORKER);
        Child child = new Child("2142", "Jordan P.");
        c1042 = new Case("C-1042", child, maria, "Open");
    }

    private Deadline makeDeadline(long daysFromNow) {
        return new Deadline("Court report", c1042, maria, LocalDate.now().plusDays(daysFromNow));
    }

    // ── 1. 7-day deadline → alert sent ───────────────────────────────────────

    @Test
    @DisplayName("checkDeadlines: 7-day deadline → alert sent")
    void checkDeadlines_7dayDeadline_sendsAlert() {
        Deadline d = makeDeadline(7);
        when(deadlineRepo.findAll()).thenReturn(List.of(d));
        when(alertRepo.existsByDeadlineIdAndDaysBeforeDue(any(), eq(7))).thenReturn(false);
        when(alertRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        SchedulerRunResult result = service.checkDeadlines();

        assertEquals(1, result.getAlertsSent());
        assertEquals("Alert sent", result.getDetails().get(0).getOutcome());
        assertEquals("info", result.getDetails().get(0).getOutcomeClass());
        verify(alertRepo).save(any(Alert.class));
    }

    // ── 2. 1-day deadline → alert sent ───────────────────────────────────────

    @Test
    @DisplayName("checkDeadlines: 1-day deadline → alert sent")
    void checkDeadlines_1dayDeadline_sendsAlert() {
        Deadline d = makeDeadline(1);
        when(deadlineRepo.findAll()).thenReturn(List.of(d));
        when(alertRepo.existsByDeadlineIdAndDaysBeforeDue(any(), eq(1))).thenReturn(false);
        when(alertRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        SchedulerRunResult result = service.checkDeadlines();

        assertEquals(1, result.getAlertsSent());
        assertEquals("warn", result.getDetails().get(0).getOutcomeClass());
    }

    // ── 3. Due-today deadline → alert sent ───────────────────────────────────

    @Test
    @DisplayName("checkDeadlines: due-today deadline → alert sent with 'due today'")
    void checkDeadlines_dueTodayDeadline_sendsAlert() {
        Deadline d = makeDeadline(0);
        when(deadlineRepo.findAll()).thenReturn(List.of(d));
        when(alertRepo.existsByDeadlineIdAndDaysBeforeDue(any(), eq(0))).thenReturn(false);
        when(alertRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        SchedulerRunResult result = service.checkDeadlines();

        assertEquals(1, result.getAlertsSent());
        assertTrue(result.getDetails().get(0).getOutcome().contains("due today"));
        assertEquals("danger", result.getDetails().get(0).getOutcomeClass());
    }

    // ── 4. 5-day deadline → no alert ─────────────────────────────────────────

    @Test
    @DisplayName("checkDeadlines: 5-day deadline → no alert (not 7/1/0)")
    void checkDeadlines_5dayDeadline_noAlert() {
        Deadline d = makeDeadline(5);
        when(deadlineRepo.findAll()).thenReturn(List.of(d));

        SchedulerRunResult result = service.checkDeadlines();

        assertEquals(0, result.getAlertsSent());
        assertEquals("No alert", result.getDetails().get(0).getOutcome());
        verify(alertRepo, never()).save(any());
    }

    // ── 5. Met deadline → no alert ────────────────────────────────────────────

    @Test
    @DisplayName("checkDeadlines: met deadline → no alert")
    void checkDeadlines_metDeadline_noAlert() {
        Deadline d = makeDeadline(7);
        d.setMet(true);
        d.setMetDate(LocalDate.now().minusDays(1));
        when(deadlineRepo.findAll()).thenReturn(List.of(d));

        SchedulerRunResult result = service.checkDeadlines();

        assertEquals(0, result.getAlertsSent());
        assertTrue(result.getDetails().get(0).getOutcome().startsWith("No alert"));
        verify(alertRepo, never()).save(any());
    }

    // ── 6. Overdue deadline → flagged, no alert ───────────────────────────────

    @Test
    @DisplayName("checkDeadlines: overdue deadline → flagged overdue, no alert sent")
    void checkDeadlines_overdueDeadline_flagged() {
        Deadline d = makeDeadline(-3);
        when(deadlineRepo.findAll()).thenReturn(List.of(d));

        SchedulerRunResult result = service.checkDeadlines();

        assertEquals(0, result.getAlertsSent());
        assertEquals(1, result.getOverdueCount());
        assertEquals("Flagged overdue", result.getDetails().get(0).getOutcome());
        assertEquals("danger", result.getDetails().get(0).getOutcomeClass());
        verify(alertRepo, never()).save(any());
    }

    // ── 7. Duplicate prevention ───────────────────────────────────────────────

    @Test
    @DisplayName("checkDeadlines: duplicate prevention — no second alert if already exists")
    void checkDeadlines_duplicatePrevention() {
        Deadline d = makeDeadline(7);
        when(deadlineRepo.findAll()).thenReturn(List.of(d));
        when(alertRepo.existsByDeadlineIdAndDaysBeforeDue(any(), eq(7))).thenReturn(true);

        SchedulerRunResult result = service.checkDeadlines();

        assertEquals(0, result.getAlertsSent(), "Should not send duplicate alert");
        verify(alertRepo, never()).save(any());
    }

    // ── 8. calculateOnTrackRate ───────────────────────────────────────────────

    @Test
    @DisplayName("calculateOnTrackRate: 1 overdue out of 4 items → 75.0%")
    void calculateOnTrackRate_oneOverdueFour_returns75() {
        Deadline overdue = makeDeadline(-3);
        Deadline ok1     = makeDeadline(5);
        Visit    v1 = new Visit(c1042, LocalDate.now().plusDays(2), "Home visit", Visit.SCHEDULED);
        Visit    v2 = new Visit(c1042, LocalDate.now().minusDays(10), "Home visit", Visit.COMPLETED);

        when(deadlineRepo.findByAssignedToUsernameAndMetFalse("m.garcia"))
                .thenReturn(List.of(overdue, ok1));
        when(visitRepo.findByCaseRecordAssignedCaseworkerUsernameOrderByScheduledDateAsc("m.garcia"))
                .thenReturn(List.of(v1, v2));

        double rate = service.calculateOnTrackRate("m.garcia");

        assertEquals(75.0, rate, 0.01);
    }

    // ── 9. filterByCaseworker ─────────────────────────────────────────────────

    @Test
    @DisplayName("filterByCaseworker: returns overdue deadline + missed visit")
    void filterByCaseworker_returnsOverdueItems() {
        Deadline overdue = makeDeadline(-3);
        Visit missed = new Visit(c1042, LocalDate.now().minusDays(1), "School visit", Visit.MISSED);

        when(deadlineRepo.findByAssignedToUsernameAndMetFalse("m.garcia"))
                .thenReturn(List.of(overdue));
        when(visitRepo.findByCaseRecordAssignedCaseworkerUsernameOrderByScheduledDateAsc("m.garcia"))
                .thenReturn(List.of(missed));

        List<OverdueItem> items = service.filterByCaseworker("m.garcia");

        assertEquals(2, items.size());
        assertTrue(items.stream().anyMatch(i -> "deadline".equals(i.getType())));
        assertTrue(items.stream().anyMatch(i -> "visit".equals(i.getType())));
    }
}
