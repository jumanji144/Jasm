package me.darknet.assembler.test;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Utilities for reading binary test samples from the filesystem.
 */
public final class BinarySampleFixture {
	private static final Path TEST_RESOURCES = Path.of("src", "test", "resources", "samples");
	private static final Path JASM_SAMPLES = TEST_RESOURCES.resolve("jasm");
	private static final Path ILLEGAL_JASM_SAMPLES = TEST_RESOURCES.resolve("jasm-illegal");
	private static final Path BINARY_SAMPLES = TEST_RESOURCES.resolve("binary");

	private BinarySampleFixture() {}

	/**
	 * Reads a JVM assembly sample with the given name from the test resources.
	 *
	 * @param name
	 * 		The name of the sample file to read, relative to the {@code "samples/jasm"} or {@code "samples/jasm-illegal"} directory.
	 *
	 * @return A {@link JvmTextSample} representing the sample file.
	 */
	public static JvmTextSample jvmSample(String name) {
		Path candidate = JASM_SAMPLES.resolve(name);
		if (!candidate.toFile().exists()) {
			candidate = ILLEGAL_JASM_SAMPLES.resolve(name);
		}
		return JvmTextSample.from(candidate);
	}

	/**
	 * Reads a JVM class file sample with the given name from the test resources.
	 *
	 * @param name
	 * 		The name of the binary sample file to read, relative to the {@code "samples/binary"} directory.
	 *
	 * @return A {@link BinarySample} representing the sample file.
	 */
	public static BinarySample binarySample(String name) {
		return BinarySample.from(BINARY_SAMPLES.resolve(name));
	}

	/**
	 * @return List of all JVM assembly samples available in the test resources, including both valid and illegal samples.
	 *
	 * @see #validJvmSamples()
	 */
	public static List<JvmTextSample> allJvmSamples() {
		BiPredicate<Path, BasicFileAttributes> filter = (path, attributes) ->
				attributes.isRegularFile() && path.toString().endsWith(".jasm");
		return SampleSourceFixture.listFiles(Path.of("src"), 25, filter).stream()
				.map(JvmTextSample::from)
				.toList();
	}

	/**
	 * @return List of valid JVM assembly samples available in the test resources.
	 *
	 * @see #allJvmSamples()
	 */
	public static List<JvmTextSample> validJvmSamples() {
		return allJvmSamples().stream()
				.filter(sample -> !sample.path().toString().contains("illegal"))
				.toList();
	}

	/**
	 * Assembly source sample for JVM tests.
	 *
	 * @param path
	 * 		The path to the sample file.
	 * @param name
	 * 		The name of the sample, for display purposes.
	 */
	public record JvmTextSample(Path path, String name) {
		/**
		 * @param path
		 * 		The path to the sample file, which must exist.
		 *
		 * @return Sample representing the file at the given path.
		 */
		public static JvmTextSample from(Path path) {
			return new JvmTextSample(SampleSourceFixture.requireExisting(path), path.getFileName().toString());
		}

		/**
		 * @return The contents of the sample file as a String.
		 */
		public String read() {
			return SampleSourceFixture.readText(path);
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/**
	 * Binary class file sample for JVM tests.
	 *
	 * @param path
	 * 		The path to the sample file.
	 * @param name
	 * 		The name of the sample, for display purposes.
	 */
	public record BinarySample(Path path, String name) {
		/**
		 * @param path
		 * 		The path to the sample file, which must exist.
		 *
		 * @return Sample representing the file at the given path.
		 */
		public static BinarySample from(Path path) {
			return new BinarySample(SampleSourceFixture.requireExisting(path), path.getFileName().toString());
		}

		/**
		 * @return The contents of the sample file as a byte array.
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
