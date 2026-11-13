package team7.service;

import java.time.LocalDateTime;
import java.util.List;

/** Full result of one ComplianceScheduler run. */
public class SchedulerRunResult {
    private final LocalDateTime ranAt;
    private final int alertsSent;
    private final int overdueCount;
    private final int noAlertCount;
    private final int totalChecked;
    private final List<DeadlineCheckResult> details;

    public SchedulerRunResult(LocalDateTime ranAt, int alertsSent, int overdueCount,
                               int noAlertCount, int totalChecked,
                               List<DeadlineCheckResult> details) {
        this.ranAt = ranAt;
        this.alertsSent = alertsSent;
        this.overdueCount = overdueCount;
        this.noAlertCount = noAlertCount;
        this.totalChecked = totalChecked;
        this.details = details;
    }

    public LocalDateTime getRanAt() { return ranAt; }
    public int getAlertsSent() { return alertsSent; }
    public int getOverdueCount() { return overdueCount; }
    public int getNoAlertCount() { return noAlertCount; }
    public int getTotalChecked() { return totalChecked; }
    public List<DeadlineCheckResult> getDetails() { return details; }
}
