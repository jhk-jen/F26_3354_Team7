package team7.service;

import team7.model.Case;

/**
 * Read-only view model for one row in the "My cases" list.
 */
public final class CaseView {

    private final Case   caseRecord;
    private final String currentHomeName; // null = no active placement
    private final String tagClass;        // "danger" / "warn" / "info" / null
    private final String tagLabel;        // display text, or null

    public CaseView(Case caseRecord, String currentHomeName,
                    String tagClass, String tagLabel) {
        this.caseRecord      = caseRecord;
        this.currentHomeName = currentHomeName;
        this.tagClass        = tagClass;
        this.tagLabel        = tagLabel;
    }

    public Case   getCaseRecord()       { return caseRecord; }
    public String getCurrentHomeName()  { return currentHomeName; }
    public String getTagClass()         { return tagClass; }
    public String getTagLabel()         { return tagLabel; }
    public boolean hasTag()             { return tagClass != null; }
}
