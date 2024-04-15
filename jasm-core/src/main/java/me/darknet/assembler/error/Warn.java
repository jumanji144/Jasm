package me.darknet.assembler.error;

import me.darknet.assembler.util.Location;

public class Warn extends Error {
    public Warn(String message, Location location) {
        super(message, location);
    }
}
