package me.darknet.assembler;

import me.darknet.assembler.printer.PrintContext;
import me.darknet.assembler.printer.jvm.ClassPrinter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

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

		String a = "Hello, world!";
		char[] b = a.toCharArray();

		int[] c = new int[10];
		Object d = c;
		if(!(d instanceof char[])) {
			Arrays.stream(c).forEach(System.out::println);
		}
	}

	private static class InnerClass$InnerClass {
		public void test() {
			System.out.println("Hello, world!");
		}
	}

	public static void main(String[] args) throws IOException {
		InputStream thisClass = PrinterTest.class
				.getResourceAsStream("/me/darknet/assembler/PrinterTest.class");
		ClassPrinter printer = new ClassPrinter(thisClass);
		PrintContext<?> ctx = new PrintContext<>("\t");
		printer.print(ctx);
		String output = ctx.toString();
		System.out.println(output);
	}

}
