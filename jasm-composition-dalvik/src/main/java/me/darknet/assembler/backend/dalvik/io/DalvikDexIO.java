package me.darknet.assembler.backend.dalvik.io;

import me.darknet.dex.file.DexHeader;
import me.darknet.dex.file.DexMapBuilder;
import me.darknet.dex.io.Input;
import me.darknet.dex.io.Output;
import me.darknet.dex.tree.DexFile;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads and writes standalone Dalvik executable files through the DEX tree codec.
 */
public final class DalvikDexIO {
	private DalvikDexIO() {}

	/**
	 * Reads a standalone DEX file from bytes.
	 *
	 * @param bytes
	 * 		Complete DEX file bytes.
	 *
	 * @return Decoded DEX tree.
	 *
	 * @throws IOException
	 * 		If the DEX file cannot be decoded.
	 */
	public static @NotNull DexFile read(byte @NotNull [] bytes) throws IOException {
		return decode(Input.wrap(bytes));
	}

	/**
	 * Reads a standalone DEX file from a stream.
	 *
	 * @param input
	 * 		Stream containing one complete DEX file. The stream remains open.
	 *
	 * @return Decoded DEX tree.
	 *
	 * @throws IOException
	 * 		If the stream or DEX file cannot be read.
	 */
	public static @NotNull DexFile read(@NotNull InputStream input) throws IOException {
		return read(input.readAllBytes());
	}

	/**
	 * Encodes a DEX tree as a standalone DEX file.
	 *
	 * @param dexFile
	 * 		DEX tree to encode.
	 *
	 * @return Complete encoded DEX file bytes.
	 *
	 * @throws IOException
	 * 		If the DEX tree cannot be encoded.
	 */
	public static byte @NotNull [] write(@NotNull DexFile dexFile) throws IOException {
		// Build the indexed DEX representation without changing the supplied tree.
		DexHeader header = DexFile.CODEC.unmap(dexFile, new DexMapBuilder());
		Output output = Output.wrap();
		DexHeader.CODEC.write(header, output);

		// Copy only the bytes written by the growing output implementation.
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		output.pipe(bytes);
		return bytes.toByteArray();
	}

	private static @NotNull DexFile decode(@NotNull Input input) throws IOException {
		try {
			DexHeader header = DexHeader.CODEC.read(input);
			return DexFile.CODEC.map(header, header.map());
		} catch (IllegalArgumentException exception) {
			throw new IOException("Failed to map DEX: " + exception.getMessage(), exception);
		}
	}
}
