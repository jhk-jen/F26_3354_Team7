package team7.service;

import org.springframework.stereotype.Component;

/** Spring singleton holding the most recent scheduler run result in memory. */
@Component
public class SchedulerState {
    private SchedulerRunResult lastResult;

    public SchedulerRunResult getLastResult() { return lastResult; }
    public void setLastResult(SchedulerRunResult r) { this.lastResult = r; }
    public boolean hasRun() { return lastResult != null; }
}
