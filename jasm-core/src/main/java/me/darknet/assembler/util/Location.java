package me.darknet.assembler.util;

public class Location {

    private final int line, column;
    private final String source;

    public Location(int line, int column, String source) {
        this.line = line;
        this.column = column;
        this.source = source;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getSource() {
        return source;
    }

}
