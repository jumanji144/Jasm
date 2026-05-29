package me.darknet.assembler.test;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Utility for decompiling JVM class bytecode to Java source code using CFR.
 */
public class JvmDecompilationFixture {
	/**
	 * @param code
	 * 		Class bytecode.
	 *
	 * @return Decompiled code.
	 */
	public static String decompile(byte[] code) {
		CompletableFuture<String> future = new CompletableFuture<>();
		String name = new ClassReader(code).getClassName();
		new CfrDriver.Builder()
				.withClassFileSource(new SingleSource(name, code))
				.withOutputSink(new OutputSinkFactoryImpl(future))
				.build()
				.analyse(Collections.singletonList(name));
		try {
			return future.get(3500, TimeUnit.MILLISECONDS);
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * @param classes
	 * 		Map of internal class names (Example: {@code com/example/FooBar}) to class bytecode.
	 * @param target
	 * 		The internal name of the class to decompile (Example: {@code com/example/FooBar}).
	 *
	 * @return Decompiled code.
	 */
	public static String decompile(Map<String, byte[]> classes, String target) {
		CompletableFuture<String> future = new CompletableFuture<>();
		new CfrDriver.Builder()
				.withClassFileSource(new MultiSource(classes))
				.withOutputSink(new OutputSinkFactoryImpl(future))
				.build()
				.analyse(Collections.singletonList(target));
		try {
			return future.get(3500, TimeUnit.MILLISECONDS);
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * @param code
	 * 		Input decompiled code.
	 *
	 * @return Filtered decompiled code.
	 */
	private static String filter(String code) {
		// Cut off any comments at the beginning, such as the CFR header and any 'cannot find type X' warnings.
		int importIndex = code.indexOf("import ");
		return (importIndex > 0) ? code.substring(importIndex) : code;
	}

	private record MultiSource(Map<String, byte[]> classes) implements ClassFileSource {
		@Override
		public void informAnalysisRelativePathDetail(String usePath, String classFilePath) {
			// no-op
		}

		@Override
		public Collection<String> addJar(String jarPath) {
			return Collections.emptyList();
		}

		@Override
		public String getPossiblyRenamedPath(String path) {
			return path;
		}

		@Override
		public Pair<byte[], String> getClassFileContent(String path) throws IOException {
			byte[] bytes = classes.get(path);
			return (bytes != null) ? new Pair<>(bytes, path) : null;
		}
	}

	private record SingleSource(String name, byte[] code) implements ClassFileSource {
		@Override
		public void informAnalysisRelativePathDetail(String usePath, String classFilePath) {
			// no-op
		}

		@Override
		public Collection<String> addJar(String jarPath) {
			return Collections.emptyList();
		}

		@Override
		public String getPossiblyRenamedPath(String path) {
			return path;
		}

		@Override
		public Pair<byte[], String> getClassFileContent(String path) throws IOException {
			return path.equals(name + ".class") ? new Pair<>(code, name) : null;
		}
	}

	private record OutputSinkFactoryImpl(CompletableFuture<String> future) implements OutputSinkFactory {
		@Override
		public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> available) {
			return Arrays.asList(SinkClass.values());
		}

		@Override
		public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
			switch (sinkType) {
				case JAVA:
					return sinkable -> future.complete(filter(sinkable.toString()));
				case EXCEPTION:
					return t -> System.err.println("CFR-Error: " + t);
				case SUMMARY:
				case PROGRESS:
				default:
					return t -> {
					};
			}
		}
	}
}
