package me.darknet.assembler.test;

import me.darknet.assembler.printer.JvmClassPrinter;
import me.darknet.assembler.printer.PrintContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

/**
 * Utilities for disassembling JVM class bytecode back into JASM.
 */
public final class JvmDisassemblyFixture {
	private JvmDisassemblyFixture() {}

	/**
	 * @param raw
	 * 		The raw JVM class file to disassemble.
	 *
	 * @return Disassembled JASM source code representing the given JVM class file.
	 */
	public static String disassembleJvm(byte[] raw) {
		return disassembleJvm(raw, null);
	}

	/**
	 * @param raw
	 * 		The raw JVM class file to disassemble.
	 * @param contextConsumer
	 * 		An optional consumer to configure the print context used for disassembly, allowing customization of formatting and other options.
	 *
	 * @return Disassembled JASM source code representing the given JVM class file, formatted according to the provided print context configuration.
	 */
	public static String disassembleJvm(byte[] raw, Consumer<PrintContext<?>> contextConsumer) {
		try {
			JvmClassPrinter printer = new JvmClassPrinter(raw);
			PrintContext<?> context = new PrintContext<>("    ");
			if (contextConsumer != null)
				contextConsumer.accept(context);
			printer.print(context);
			return context.toString();
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}
}
