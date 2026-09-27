package me.darknet.assembler.backend.dalvik.test;

import me.darknet.assembler.test.SampleSourceFixture;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utilities for reading and composing Dalvik binary test samples.
 */
public final class DalvikSampleFixture {
	private static final Path BINARY_SAMPLES = resolveBinarySamples();

	private static Path resolveBinarySamples() {
		List<Path> candidates = List.of(
				Path.of("src", "test", "resources", "samples", "binary"),
				Path.of("..", "jasm-composition-dalvik", "src", "test", "resources", "samples", "binary"),
				Path.of("jasm-composition-dalvik", "src", "test", "resources", "samples", "binary")
		);
		return candidates.stream().filter(Files::isDirectory).findFirst().orElse(candidates.getFirst());
	}

	private DalvikSampleFixture() {}

	/**
	 * Reads a Dalvik binary sample with the given name.
	 *
	 * @param name
	 * 		Sample file name relative to the binary sample directory.
	 *
	 * @return Existing Dalvik sample descriptor.
	 */
	public static DexSample dexSample(String name) {
		return DexSample.from(BINARY_SAMPLES.resolve(name));
	}

	/**
	 * Finds all Dalvik binary samples in the module test resources.
	 *
	 * @return Dalvik samples discovered below the module source directory.
	 */
	public static List<DexSample> allDexSamples() {
		BiPredicate<Path, BasicFileAttributes> filter = (path, attributes) ->
				attributes.isRegularFile() && path.toString().endsWith(".sample");
		return SampleSourceFixture.listFiles(BINARY_SAMPLES, 1, filter).stream()
				.map(DexSample::from)
				.toList();
	}

	/**
	 * Writes a ZIP archive from deterministic entry bytes.
	 *
	 * @param directory
	 * 		Directory receiving the archive.
	 * @param name
	 * 		Archive file name.
	 * @param entries
	 * 		Archive entries keyed by their ZIP path.
	 *
	 * @return Path to the written archive.
	 */
	public static Path writeZip(Path directory, String name, Map<String, byte[]> entries) {
		try {
			Files.createDirectories(directory);
			Path archive = directory.resolve(name);
			try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
				entries.entrySet().stream()
						.sorted(Map.Entry.comparingByKey())
						.forEach(entry -> {
							try {
								output.putNextEntry(new ZipEntry(entry.getKey()));
								output.write(entry.getValue());
								output.closeEntry();
							} catch (IOException exception) {
								throw new UncheckedIOException(exception);
							}
						});
			}
			return archive;
		} catch (UncheckedIOException exception) {
			throw exception;
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	/**
	 * Binary Dalvik sample descriptor.
	 *
	 * @param path
	 * 		Path to the sample file.
	 * @param name
	 * 		Display name for the sample.
	 */
	public record DexSample(Path path, String name) {
		/**
		 * Creates a descriptor for an existing sample path.
		 *
		 * @param path
		 * 		Path to the sample file.
		 *
		 * @return Descriptor for the existing sample.
		 */
		public static DexSample from(Path path) {
			return new DexSample(SampleSourceFixture.requireExisting(path), path.getFileName().toString());
		}

		/**
		 * @return Complete sample bytes.
		 */
		public byte[] read() {
			return SampleSourceFixture.readBytes(path);
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
