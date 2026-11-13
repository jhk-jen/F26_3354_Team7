package team7.model;

import java.time.LocalDate;

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
 * Scheduled or completed visit (FR-8, FR-9, FR-10).
 * Methods: schedule, markCompleted, markMissed, addNotes, isOverdue.
 */
@Entity
@Table(name = "visits")
public class Visit {

    public static final String SCHEDULED  = "SCHEDULED";
    public static final String COMPLETED  = "COMPLETED";
    public static final String MISSED     = "MISSED";
    public static final String OVERDUE    = "OVERDUE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseRecord;

    @Column(nullable = false)
    private LocalDate scheduledDate;

    /** "Home visit", "School visit", etc. */
    @Column(length = 100)
    private String visitType;

    /** SCHEDULED, COMPLETED, MISSED, OVERDUE */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 1000)
    private String notes;

    public Visit() {}

    public Visit(Case caseRecord, LocalDate scheduledDate, String visitType, String status) {
        this.caseRecord = caseRecord;
        this.scheduledDate = scheduledDate;
        this.visitType = visitType;
        this.status = status;
    }

    public boolean isOverdue() {
        return SCHEDULED.equals(status) && LocalDate.now().isAfter(scheduledDate);
    }

    // schedule / markCompleted / markMissed / addNotes
    public void markCompleted(String notes) {
        this.status = COMPLETED;
        this.notes = notes;
    }

    public void markMissed() { this.status = MISSED; }

    public void addNotes(String notes) { this.notes = notes; }

    public Long getId() { return id; }
    public Case getCaseRecord() { return caseRecord; }
    public void setCaseRecord(Case caseRecord) { this.caseRecord = caseRecord; }
    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }
    public String getVisitType() { return visitType; }
    public void setVisitType(String visitType) { this.visitType = visitType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
