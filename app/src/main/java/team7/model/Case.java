package team7.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Represents a child welfare case.
 * Entity name "FosterCase" avoids the JPQL reserved word "CASE".
 * Table name "cases" matches the SQL convention requested.
 */
@Entity(name = "FosterCase")
@Table(name = "cases")
public class Case {

    /** e.g. "C-1088" */
    @Id
    @Column(length = 20)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "caseworker_id", nullable = false)
    private User assignedCaseworker;

    /** "Open" or "Closed" */
    @Column(nullable = false, length = 20)
    private String status;

    /**
     * Optional UI tag override (e.g. "Placement change needed").
     * When set, PlacementService shows this as the highest-priority status tag.
     */
    @Column(length = 100)
    private String statusFlag;

    public Case() {}

    public Case(String id, Child child, User assignedCaseworker, String status) {
        this.id = id;
        this.child = child;
        this.assignedCaseworker = assignedCaseworker;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Child getChild() { return child; }
    public void setChild(Child child) { this.child = child; }
    public User getAssignedCaseworker() { return assignedCaseworker; }
    public void setAssignedCaseworker(User assignedCaseworker) { this.assignedCaseworker = assignedCaseworker; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStatusFlag() { return statusFlag; }
    public void setStatusFlag(String statusFlag) { this.statusFlag = statusFlag; }
}
