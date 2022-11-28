package me.darknet.assembler.exceptions;

import me.darknet.assembler.parser.Location;

public class AssemblerException extends Exception {
	private final Location where;

	public AssemblerException(Exception e, Location where) {
		super(e);
		this.where = where;
	}

	public AssemblerException(String message, Location where) {
		super(message);
		this.where = where;
	}

	public AssemblerException(String message, Location where, Throwable cause) {
		super(message, cause);
		this.where = where;
	}

	public Location getLocation() {
		return where;
	}

	public String describe() {
		return "[" + where + "]" + " " + getMessage();
	}

}
