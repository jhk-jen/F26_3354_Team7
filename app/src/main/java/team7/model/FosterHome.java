package team7.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "foster_homes")
public class FosterHome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String location;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private int currentOccupancy;

    public FosterHome() {}

    public FosterHome(String name, String location, int capacity, int currentOccupancy) {
        this.name = name;
        this.location = location;
        this.capacity = capacity;
        this.currentOccupancy = currentOccupancy;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public int getCurrentOccupancy() { return currentOccupancy; }
    public void setCurrentOccupancy(int currentOccupancy) { this.currentOccupancy = currentOccupancy; }

    public boolean isFull() { return currentOccupancy >= capacity; }
    public int getOpenBeds() { return Math.max(0, capacity - currentOccupancy); }

    /** Percentage of occupancy for capacity bar (0-100). */
    public int getOccupancyPercent() {
        if (capacity == 0) return 0;
        return (int) Math.round(currentOccupancy * 100.0 / capacity);
    }
}
