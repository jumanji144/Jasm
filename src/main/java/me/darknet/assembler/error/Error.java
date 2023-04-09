package me.darknet.assembler.error;

import me.darknet.assembler.util.Location;

/**
 * Represents an error at a location.
 */
public class Error {

	private final String message;
	private final Location location;

	public Error(String message, Location location) {
		this.message = message;
		this.location = location;
	}

	public String getMessage() {
		return message;
	}

	public Location getLocation() {
		return location;
	}

}
