package me.darknet.assembler.parser;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Location {

    public int line;
    public int column;
    public String source;

    public String toString() {
        return source + ":" + line + ":" + column;
    }

    public Location copy() {
        return new Location(line, column, source);
    }

    public Location sub(int column) {
        return new Location(line, this.column - column, source);
    }

}
