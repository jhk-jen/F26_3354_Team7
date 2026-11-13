package team7.service;

import team7.model.User;

/** Per-caseworker compliance statistics for the supervisor dashboard. */
public class CaseworkerStat {
    private final User caseworker;
    private final int caseCount;
    private final double onTrackRate;
    private final int overdueCount;
    private final String rateClass;   // "ok", "warn", "danger"
    private final String rateLabel;   // "On target", "Needs attention", "Below target"

    public CaseworkerStat(User caseworker, int caseCount, double onTrackRate,
                           int overdueCount, String rateClass, String rateLabel) {
        this.caseworker = caseworker;
        this.caseCount = caseCount;
        this.onTrackRate = onTrackRate;
        this.overdueCount = overdueCount;
        this.rateClass = rateClass;
        this.rateLabel = rateLabel;
    }

    public User getCaseworker() { return caseworker; }
    public int getCaseCount() { return caseCount; }
    public double getOnTrackRate() { return onTrackRate; }
    public int getOverdueCount() { return overdueCount; }
    public String getRateClass() { return rateClass; }
    public String getRateLabel() { return rateLabel; }

    public String getRateFormatted() {
        return String.format("%.1f", onTrackRate);
    }
    public String getBarWidthStyle() {
        return "width:" + String.format("%.1f", onTrackRate) + "%";
    }
}
