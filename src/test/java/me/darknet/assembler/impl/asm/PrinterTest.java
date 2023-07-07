package me.darknet.assembler.impl.asm;

import me.darknet.assembler.ASTProcessorTest;
import me.darknet.assembler.DeclarationParserTest;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.assembler.printer.impl.asm.ASMClassPrinter;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class PrinterTest {

	public static final synchronized void test(String test) {
		System.out.println("Hello, world!");
	}

	@Test
	public void testMethodPrinter() throws IOException {
		byte[] thisClass = PrinterTest.class
				.getResourceAsStream("/me/darknet/assembler/impl/asm/PrinterTest.class")
				.readAllBytes();
		ASMClassPrinter printer = new ASMClassPrinter(thisClass);
		PrintContext<?> ctx = new PrintContext<>("\t");
		printer.print(ctx);
		String output = ctx.toString();
		System.out.println(output);
		ASTProcessorTest.parseString(output, (result) -> {
			return;
		});
	}

}
