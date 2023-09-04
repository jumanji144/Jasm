package me.darknet.assembler.parser;

import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Range;

public record Token(Range range, Location location, TokenType type, String content) {}
