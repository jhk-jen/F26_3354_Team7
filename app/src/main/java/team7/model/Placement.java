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

@Entity
@Table(name = "placements")
public class Placement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseRecord;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "foster_home_id", nullable = false)
    private FosterHome fosterHome;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Column(length = 500)
    private String changeReason;

    @Column(nullable = false)
    private boolean active;

    public Placement() {}

    public Placement(Case caseRecord, FosterHome fosterHome, LocalDate startDate) {
        this.caseRecord = caseRecord;
        this.fosterHome = fosterHome;
        this.startDate = startDate;
        this.active = true;
    }

    public Long getId() { return id; }
    public Case getCaseRecord() { return caseRecord; }
    public void setCaseRecord(Case caseRecord) { this.caseRecord = caseRecord; }
    public FosterHome getFosterHome() { return fosterHome; }
    public void setFosterHome(FosterHome fosterHome) { this.fosterHome = fosterHome; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
