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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Reads and writes Dalvik executable files through the DEX tree codec.
 */
public final class DalvikDexIO {
	private static final Pattern SECONDARY_DEX = Pattern.compile("^classes([0-9]+)\\.dex$");

	private DalvikDexIO() {}

	/**
	 * Reads one standalone DEX or all ordered DEX entries from an APK.
	 *
	 * @param source
	 * 		Input path ending in {@code .dex} or {@code .apk}.
	 *
	 * @return DEX files in deterministic input order.
	 *
	 * @throws IOException
	 * 		If the input cannot be read or contains no DEX entries.
	 */
	public static @NotNull List<@NotNull DexFile> read(@NotNull Path source) throws IOException {
		String name = source.getFileName().toString().toLowerCase();
		if (name.endsWith(".dex"))
			return List.of(DalvikDexIO.read(Files.readAllBytes(source)));
		if (name.endsWith(".apk"))
			return readApk(source);
		throw new IOException("Dalvik input must be a .dex file or .apk: " + source);
	}

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

	/**
	 * Reads all ordered DEX entries from an APK.
	 *
	 * @param source
	 * 		Input path ending in {@code .apk}.
	 *
	 * @return DEX files in deterministic input order.
	 *
	 * @throws IOException
	 * 		If the input cannot be read or contains no DEX entries.
	 */
	private static @NotNull List<@NotNull DexFile> readApk(@NotNull Path source) throws IOException {
		try (ZipFile zip = new ZipFile(source.toFile())) {
			List<ZipDexEntry> entries = new ArrayList<>();
			zip.stream().forEach(entry -> {
				if (entry.isDirectory())
					return;
				int index = dexIndex(entry);
				if (index >= 0)
					entries.add(new ZipDexEntry(index, entry));
			});
			if (entries.isEmpty()) {
				throw new IOException("APK contains no classes.dex entries: " + source);
			}

			entries.sort(Comparator.comparingInt(ZipDexEntry::index));
			Set<Integer> indexes = new HashSet<>();
			List<DexFile> files = new ArrayList<>(entries.size());
			for (ZipDexEntry entry : entries) {
				if (!indexes.add(entry.index()))
					throw new IOException("APK contains duplicate DEX index " + entry.index() + ": " + source);
				try (InputStream input = zip.getInputStream(entry.entry())) {
					files.add(DalvikDexIO.read(input));
				} catch (IOException exception) {
					throw new IOException("Failed to read APK DEX entry " + entry.entry().getName()
							+ ": " + exception.getMessage(), exception);
				}
			}
			return List.copyOf(files);
		}
	}

	/**
	 * Determines the numeric loading order of a DEX entry in an APK.
	 *
	 * @param entry
	 * 		ZIP entry to inspect.
	 *
	 * @return Numeric DEX index, or {@code -1} if the entry is not a DEX file.
	 */
	private static int dexIndex(ZipEntry entry) {
		String name = entry.getName();
		if ("classes.dex".equals(name))
			return 1;

		Matcher matcher = SECONDARY_DEX.matcher(name);
		if (matcher.matches()) {
			try {
				int index = Integer.parseInt(matcher.group(1));
				return index >= 2 ? index : -1;
			} catch (NumberFormatException ignored) {
				return -1;
			}
		}
		return -1;
	}

	/**
	 * Decodes a DEX file from an input source.
	 *
	 * @param input
	 * 		Input source containing one complete DEX file.
	 *
	 * @return Decoded DEX tree.
	 *
	 * @throws IOException
	 * 		If the input cannot be read or the DEX file cannot be decoded.
	 */
	private static @NotNull DexFile decode(@NotNull Input input) throws IOException {
		try {
			DexHeader header = DexHeader.CODEC.read(input);
			return DexFile.CODEC.map(header, header.map());
		} catch (IllegalArgumentException exception) {
			throw new IOException("Failed to map DEX: " + exception.getMessage(), exception);
		}
	}

	/**
	 * DEX archive entry paired with its numeric loading order.
	 *
	 * @param index
	 * 		Numeric DEX index used to order entries.
	 * @param entry
	 * 		ZIP entry containing the DEX file.
	 */
	private record ZipDexEntry(int index, ZipEntry entry) {}
}
