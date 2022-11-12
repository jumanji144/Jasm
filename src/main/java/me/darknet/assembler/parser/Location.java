package me.darknet.assembler.parser;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Location {
	private int line;
	private int column;
	private String source;
	private int position;

	public Location copy() {
		return new Location(line, column, source, position);
	}

	public Location sub(int column) {
		return new Location(line, this.column - column, source, position - column);
	}

	public Location add(int column) {
		return new Location(line, this.column + column, source, position + column);
	}

	public void advance() {
		column++;
	}

	public void advanceNewLine() {
		line++;
		column = 1;
	}

	public int getStart() {
		return position;
	}

	@Override
	public String toString() {
		return source + ":" + line + ":" + column;
	}


}
