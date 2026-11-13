package team7.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "children")
public class Child {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** System child ID, e.g. "2231" */
    @Column(nullable = false, unique = true, length = 20)
    private String childId;

    /** Display name, e.g. "Noah K." */
    @Column(nullable = false, length = 100)
    private String name;

    public Child() {}

    public Child(String childId, String name) {
        this.childId = childId;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getChildId() { return childId; }
    public void setChildId(String childId) { this.childId = childId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
