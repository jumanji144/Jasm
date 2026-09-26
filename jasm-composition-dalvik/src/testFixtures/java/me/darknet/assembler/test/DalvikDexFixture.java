package me.darknet.assembler.test;

import me.darknet.assembler.backend.dalvik.io.DalvikDexIO;
import me.darknet.assembler.backend.dalvik.printer.DalvikClassPrinter;
import me.darknet.assembler.printer.PrintContext;
import me.darknet.dex.tree.DexFile;
import me.darknet.dex.tree.definitions.ClassDefinition;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared read, write, print, and round-trip helpers for Dalvik tests.
 */
public final class DalvikDexFixture {
	private DalvikDexFixture() {}

	/**
	 * Reads a standalone DEX file through the production adapter.
	 *
	 * @param bytes
	 * 		Complete DEX file bytes.
	 *
	 * @return Decoded DEX tree.
	 *
	 * @throws IOException
	 * 		If the DEX file cannot be decoded.
	 */
	public static DexFile readDex(byte[] bytes) throws IOException {
		return DalvikDexIO.read(bytes);
	}

	/**
	 * Writes a DEX tree through the production adapter.
	 *
	 * @param dexFile
	 * 		DEX tree to encode.
	 *
	 * @return Complete encoded DEX file bytes.
	 *
	 * @throws IOException
	 * 		If the DEX tree cannot be encoded.
	 */
	public static byte[] writeDex(DexFile dexFile) throws IOException {
		return DalvikDexIO.write(dexFile);
	}

	/**
	 * Builds and writes a DEX file containing the supplied classes.
	 *
	 * @param definitions
	 * 		Class definitions in output order.
	 * @param version
	 * 		DEX version to encode.
	 *
	 * @return Complete encoded DEX file bytes.
	 *
	 * @throws IOException
	 * 		If the DEX tree cannot be encoded.
	 */
	public static byte[] writeDex(List<ClassDefinition> definitions, int version) throws IOException {
		return writeDex(new DexFile(version, List.copyOf(definitions)));
	}

	/**
	 * Prints one class using the canonical Dalvik printer.
	 *
	 * @param definition
	 * 		Class definition to print.
	 *
	 * @return Dalvik source text.
	 */
	public static String print(ClassDefinition definition) {
		PrintContext<?> context = new PrintContext<>("\t");
		new DalvikClassPrinter(definition).print(context);
		return context.toString();
	}

	/**
	 * Prints all classes in DEX definition order.
	 *
	 * @param dexFile
	 * 		DEX tree to print.
	 *
	 * @return Printed source keyed by internal class name.
	 */
	public static Map<String, String> printAll(DexFile dexFile) {
		Map<String, String> output = new LinkedHashMap<>();
		for (ClassDefinition definition : dexFile.definitions()) {
			output.put(definition.getType().internalName(), print(definition));
		}
		return output;
	}
}
