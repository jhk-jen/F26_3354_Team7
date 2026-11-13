package team7.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

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
 * A compliance deadline (FR-11).
 * Business methods: isOverdue(), daysRemaining().
 */
@Entity
@Table(name = "deadlines")
public class Deadline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** e.g. "Court report", "Service plan review" */
    @Column(nullable = false, length = 200)
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseRecord;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_to_id", nullable = false)
    private User assignedTo;

    @Column(nullable = false)
    private LocalDate dueDate;

    /** True when the deadline has been met before its due date. */
    private boolean met;

    private LocalDate metDate;

    public Deadline() {}

    public Deadline(String description, Case caseRecord, User assignedTo, LocalDate dueDate) {
        this.description = description;
        this.caseRecord = caseRecord;
        this.assignedTo = assignedTo;
        this.dueDate = dueDate;
        this.met = false;
    }

    /** FR-11: past due and not met. */
    public boolean isOverdue() {
        return !met && LocalDate.now().isAfter(dueDate);
    }

    /** Signed days remaining (negative = overdue). */
    public long daysRemaining() {
        return ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }

    public Long getId() { return id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Case getCaseRecord() { return caseRecord; }
    public void setCaseRecord(Case caseRecord) { this.caseRecord = caseRecord; }
    public User getAssignedTo() { return assignedTo; }
    public void setAssignedTo(User assignedTo) { this.assignedTo = assignedTo; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public boolean isMet() { return met; }
    public void setMet(boolean met) { this.met = met; }
    public LocalDate getMetDate() { return metDate; }
    public void setMetDate(LocalDate metDate) { this.metDate = metDate; }
}
