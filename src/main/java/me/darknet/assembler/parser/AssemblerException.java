package me.darknet.assembler.parser;

public class AssemblerException extends Exception {

    Location where;

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
        return "[" + where + "]" + ": " + getMessage();
    }

}
