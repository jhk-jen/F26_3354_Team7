package team7.service;

import team7.model.Deadline;

/** One row in the scheduler results table. */
public class DeadlineCheckResult {
    private final Deadline deadline;
    private final String outcome;        // "Alert sent (due today)", "Flagged overdue", etc.
    private final String outcomeClass;   // "danger", "warn", "info", "ok", "neutral"

    public DeadlineCheckResult(Deadline deadline, String outcome, String outcomeClass) {
        this.deadline = deadline;
        this.outcome = outcome;
        this.outcomeClass = outcomeClass;
    }

    public Deadline getDeadline() { return deadline; }
    public String getOutcome() { return outcome; }
    public String getOutcomeClass() { return outcomeClass; }
}
