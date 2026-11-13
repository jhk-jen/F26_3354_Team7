package team7.service;

/** One overdue item for the supervisor filtered dashboard. */
public class OverdueItem {
    private final String title;    // e.g. "Permanency review – C-1059"
    private final String detail;   // e.g. "Due 11/07/2026"
    private final String tagLabel; // e.g. "Overdue 3 days", "Missed"
    private final String type;     // "deadline" or "visit"

    public OverdueItem(String title, String detail, String tagLabel, String type) {
        this.title = title;
        this.detail = detail;
        this.tagLabel = tagLabel;
        this.type = type;
    }

    public String getTitle() { return title; }
    public String getDetail() { return detail; }
    public String getTagLabel() { return tagLabel; }
    public String getType() { return type; }
}
