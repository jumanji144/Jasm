package me.darknet.assembler.backend.dalvik.test;

import me.darknet.assembler.test.SampleSourceFixture;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Utilities for reading Dalvik JASM source samples.
 */
public final class DalvikJasmSampleFixture {
	private static final Path TEST_RESOURCES = Path.of("src", "test", "resources", "samples");
	private static final Path JASM_SAMPLES = TEST_RESOURCES.resolve("jasm");

	private DalvikJasmSampleFixture() {}

	/**
	 * Reads a Dalvik JASM source sample with the given name.
	 *
	 * @param name
	 * 		Sample file name relative to the JASM sample directory.
	 *
	 * @return Existing JASM sample descriptor.
	 */
	public static DalvikTextSample jasmSample(String name) {
		return DalvikTextSample.from(JASM_SAMPLES.resolve(name));
	}

	/**
	 * Finds all Dalvik JASM source samples in the module test resources.
	 *
	 * @return Dalvik JASM samples discovered below the module source directory.
	 */
	public static List<DalvikTextSample> allJasmSamples() {
		BiPredicate<Path, BasicFileAttributes> filter = (path, attributes) ->
				attributes.isRegularFile() && path.toString().endsWith(".jasm");
		return SampleSourceFixture.listFiles(JASM_SAMPLES, 1, filter).stream()
				.map(DalvikTextSample::from)
				.toList();
	}

	/**
	 * Dalvik JASM source sample descriptor.
	 *
	 * @param path
	 * 		Path to the sample file.
	 * @param name
	 * 		Display name for the sample.
	 */
	public record DalvikTextSample(Path path, String name) {
		/**
		 * Creates a descriptor for an existing sample path.
		 *
		 * @param path
		 * 		Path to the sample file.
		 *
		 * @return Descriptor for the existing sample.
		 */
		public static DalvikTextSample from(Path path) {
			return new DalvikTextSample(SampleSourceFixture.requireExisting(path), path.getFileName().toString());
		}

		/**
		 * @return Complete sample source text.
		 */
		public String read() {
			return SampleSourceFixture.readText(path);
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
