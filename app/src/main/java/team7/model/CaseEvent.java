package team7.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Automatic audit log entry for a case (FR-6).
 * Corresponds to the CaseEvent class in the design with logEvent().
 */
@Entity
@Table(name = "case_events")
public class CaseEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseRecord;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    /** Short actor name, e.g. "M. Garcia" */
    @Column(nullable = false, length = 100)
    private String actor;

    @Column(nullable = false, length = 500)
    private String description;

    public CaseEvent() {}

    public CaseEvent(Case caseRecord, LocalDateTime timestamp, String actor, String description) {
        this.caseRecord = caseRecord;
        this.timestamp = timestamp;
        this.actor = actor;
        this.description = description;
    }

    public Long getId() { return id; }
    public Case getCaseRecord() { return caseRecord; }
    public void setCaseRecord(Case caseRecord) { this.caseRecord = caseRecord; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
