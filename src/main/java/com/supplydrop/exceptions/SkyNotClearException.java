package com.supplydrop.exceptions;

import org.bukkit.Location;

public class SkyNotClearException extends Exception {

    private final Location location;

    public SkyNotClearException(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }
}
