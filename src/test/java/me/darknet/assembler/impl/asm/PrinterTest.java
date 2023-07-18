package me.darknet.assembler.impl.asm;

import me.darknet.assembler.ASTProcessorTest;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.assembler.printer.impl.asm.ASMClassPrinter;

import java.io.IOException;
import java.io.StringWriter;

public class PrinterTest {

	public static final synchronized void test(String test) {
		System.out.println("Hello, world!");
		switch (test) {
			case "test": {
				System.out.println("Hello, world!");
				break;
			}
			case "sg": {
				System.out.println("\u8153\u8157\u8135\u1183\\u1128");
				break;
			}
			default: {
				System.out.println("諯你好！");
				break;
			}
		}
	}

	private static class InnerClass$InnerClass {
		public void test() {
			System.out.println("Hello, world!");
		}
	}

	public static void main(String[] args) throws IOException {
		byte[] thisClass = PrinterTest.class
				.getResourceAsStream("/me/darknet/assembler/impl/asm/PrinterTest.class")
				.readAllBytes();
		ASMClassPrinter printer = new ASMClassPrinter(thisClass);
		StringWriter sw = new StringWriter();
		PrintContext<?> ctx = new PrintContext<>("\t", sw);
		printer.print(ctx);
		String output = sw.toString();
		ASTProcessorTest.parseString(output, (result) -> {
			return;
		});
		System.out.println(output);
	}

}
