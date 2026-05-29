package me.darknet.assembler.test;

import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Utilities for reading test samples from the filesystem.
 */
public final class SampleSourceFixture {
	private SampleSourceFixture() {}

	/**
	 * @param path
	 * 		The path to the text file to read.
	 *
	 * @return The contents of the file as a String.
	 */
	public static String readText(Path path) {
		try {
			return Files.readString(path);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/**
	 * @param path
	 * 		The path to the binary file to read.
	 *
	 * @return The contents of the file as a byte array.
	 */
	public static byte[] readBytes(Path path) {
		try {
			return Files.readAllBytes(path);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/**
	 * @param root
	 * 		The root directory to search for files.
	 * @param maxDepth
	 * 		The maximum depth to search for files.
	 * @param filter
	 * 		A filter to limit which files are yielded.
	 *
	 * @return A list of paths to files that match the filter, relative to the root directory.
	 */
	public static List<Path> listFiles(Path root, int maxDepth, BiPredicate<Path, BasicFileAttributes> filter) {
		try {
			return Files.find(root, maxDepth, filter).toList();
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/**
	 * @param path
	 * 		The path to the file that must exist.
	 *
	 * @return The same path if it exists.
	 */
	public static Path requireExisting(Path path) {
		Assertions.assertTrue(Files.exists(path), "Missing test sample: " + path);
		return path;
	}
}
