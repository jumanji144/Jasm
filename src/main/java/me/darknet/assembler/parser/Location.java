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
	public int position;

	public Location copy() {
		return new Location(line, column, source, position);
	}

	public Location sub(int column) {
		return new Location(line, this.column - column, source, position - column);
	}

	public int getStart() {
		return position;
	}

	@Override
	public String toString() {
		return source + ":" + line + ":" + column;
	}
}
