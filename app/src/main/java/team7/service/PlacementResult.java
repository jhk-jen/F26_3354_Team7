package team7.service;

import team7.model.FosterHome;
import team7.model.Placement;

/**
 * Value object returned by PlacementService.createPlacement().
 */
public final class PlacementResult {

    private final boolean success;
    private final Placement placement;      // non-null when success
    private final FosterHome rejectedHome;  // non-null when !success
    private final String message;

    private PlacementResult(boolean success, Placement placement,
                             FosterHome rejectedHome, String message) {
        this.success      = success;
        this.placement    = placement;
        this.rejectedHome = rejectedHome;
        this.message      = message;
    }

    public static PlacementResult success(Placement p, FosterHome home) {
        String msg = "Placement saved. " + home.getName()
                + " now has " + home.getCurrentOccupancy()
                + " of " + home.getCapacity()
                + " beds used. Event logged to the case timeline.";
        return new PlacementResult(true, p, null, msg);
    }

    public static PlacementResult rejected(FosterHome home) {
        String msg = "Placement rejected. " + home.getName()
                + " is at maximum capacity ("
                + home.getCurrentOccupancy() + " of " + home.getCapacity()
                + "). Choose another home.";
        return new PlacementResult(false, null, home, msg);
    }

    public boolean isSuccess()              { return success; }
    public Placement getPlacement()         { return placement; }
    public FosterHome getRejectedHome()     { return rejectedHome; }
    public String getMessage()              { return message; }
}
