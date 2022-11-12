package me.darknet.assembler;

import me.darknet.assembler.parser.Location;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.NumberGroup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NumberTests {
	@Test
	public void testInt() {
		NumberGroup n = number("10");
		assertEquals(10, n.asInt());
		assertEquals(10.0D, n.asDouble());
		assertEquals(10.0F, n.asFloat());
		assertEquals(10L, n.asLong());
		assertFalse(n.isWide());
		assertFalse(n.isFloat());
	}

	@Test
	public void testLong() {
		long l = 51253419024L;
		NumberGroup n = number("51253419024L");
		assertEquals((int) l, n.asInt());
		assertEquals((double) l, n.asDouble());
		assertEquals((float) l, n.asFloat());
		assertEquals(l, n.asLong());
		assertTrue(n.isWide());
		assertFalse(n.isFloat());
	}

	@Test
	public void testHexInt() {
		NumberGroup n = number("0x10");
		assertEquals(0x10, n.asInt());
		assertEquals(0x10L, n.asLong());
		assertFalse(n.isWide());
		assertFalse(n.isFloat());
	}

	@Test
	public void testFloat() {
		NumberGroup n = number("3.14f");
		assertEquals(3.14, n.asFloat(), 0.00001);
		assertEquals(3, n.asInt());
		assertFalse(n.isWide());
		assertTrue(n.isFloat());
	}

	@Test
	public void testDouble() {
		NumberGroup n = number("3.14159265359");
		assertEquals(3.14159265359, n.asDouble());
		assertEquals(3, n.asInt());
		assertTrue(n.isWide());
		assertTrue(n.isFloat());
	}

	private static NumberGroup number(String text) {
		return new NumberGroup(new Token(
				text,
				new Location(0, 0, "", 0),
				Token.TokenType.NUMBER));
	}
}
