package me.darknet.assembler.compile;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.compile.analysis.AnalysisException;
import me.darknet.assembler.compile.analysis.AnalysisResults;
import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compile.analysis.MethodAnalysisResult;
import me.darknet.assembler.compile.analysis.VarCache;
import me.darknet.assembler.compile.analysis.VarCacheUpdater;
import me.darknet.assembler.compile.analysis.jvm.IndexedStraightforwardSimulation;
import me.darknet.assembler.compile.analysis.jvm.JvmAnalysisEngine;
import me.darknet.assembler.compile.analysis.jvm.JvmAnalysisRunner;
import me.darknet.assembler.compile.builder.JvmClassBuilder;
import me.darknet.assembler.compile.visitor.JvmRootVisitor;
import me.darknet.assembler.compiler.Compiler;
import me.darknet.assembler.compiler.CompilerOptions;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.transformer.Transformer;
import me.darknet.assembler.util.JvmTypeUtils;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.VarNaming;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static me.darknet.assembler.compile.builder.JvmClassBuilder.methodKey;

public class JvmCompiler implements Compiler {
	/**
	 * Compiles the given AST into a Java class file.
	 *
	 * @param ast
	 * 		The AST to compile
	 * @param options
	 * 		Compiler options.
	 *
	 * @return Result of the compilation, containing the compiled class file if successful, or a list of errors if not.
	 */
	@Override
	public @NotNull Result<JavaCompileResult> compile(@NotNull List<ASTElement> ast, @NotNull CompilerOptions<?> options) {
		JvmCompilerOptions jvmOptions = (JvmCompilerOptions) options;
		JvmClassBuilder builder = new JvmClassBuilder();
		ErrorCollector collector = new ErrorCollector();
		JvmRootVisitor visitor = new JvmRootVisitor(builder, jvmOptions);

		// If the user doesn't provide exactly one declaration, we cannot be sure which one is the intended target.
		if (ast.size() != 1) {
			collector.addError("Expected exactly one declaration", null);
			return new Result<>(new JavaCompileResult(null, builder), collector.getErrors(), collector.getWarns());
		}

		builder.setVersion(jvmOptions.version());

		// If the user provided an overlay, we want to apply our transformations on top of it, so we need to read it before visiting the AST.
		if (jvmOptions.overlay != null)
			applyOverlay(collector, builder, jvmOptions.overlay.classFile());

		// If the overlay somehow failed, we need to abort.
		if (collector.hasErr())
			return new Result<>(new JavaCompileResult(null, builder), collector.getErrors(), collector.getWarns());

		// Now we can visit the AST and build our class node.
		Transformer transformer = new Transformer(visitor);
		transformer.transform(ast)
				.ifErr(collector::addErrors)
				.ifWarn(collector::addWarnings);

		if (!collector.hasErr()) {
			try {
				// This just exists to reduce debugging needed to find out that the user didn't specify the class
				// definition properly in their JASM input.
				if (builder.type() == null)
					throw new IllegalStateException("Cannot build class, type name not specified");

				// Compile the class.
				byte[] bytes = writeClass(builder, jvmOptions);

				// Filling in the analysis results for the generated class.
				analyzeGeneratedClass(bytes, jvmOptions, builder, collector);

				// Wrap up.
				return new Result<>(new JavaCompileResult(new JavaClassRepresentation(bytes), builder),
						collector.getErrors(), collector.getWarns());
			} catch (Throwable t) {
				// We cannot continue, the result might be very corrupted.
				// Collect as much info that could have led to the error as possible.
				boolean recordedError = false;
				for (var methodEntry : builder.allResults().entrySet()) {
					AnalysisResults analysisResults = methodEntry.getValue();
					AnalysisException failure = analysisResults.getAnalysisFailure();
					if (failure != null) {
						AbstractInsnNode instruction = failure.getInstruction();
						if (instruction != null) {
							ASTInstruction targetInsn = analysisResults.getInstructionToAstMap().get(instruction);
							if (targetInsn != null) {
								Location location = targetInsn.location();
								collector.addError(failure.getMessage(), location);
							} else {
								collector.addError(failure.getMessage(), Location.UNKNOWN);
							}
							recordedError = true;
						}
					}
				}

				// Fallback if there were no reported errors from the analysis process
				if (!recordedError)
					collector.addError("Failed to write class: " + t.getMessage(), null);

				return new Result<>(new JavaCompileResult(null, builder),
						collector.getErrors(), collector.getWarns());
			}
		}

		return new Result<>(new JavaCompileResult(null, builder),
				collector.getErrors(), collector.getWarns());
	}

	/**
	 * Analyzes the generated class to populate the analysis results for each method.
	 *
	 * @param bytes
	 * 		Compiled class bytes to analyze.
	 * @param options
	 * 		Compiler options. See: {@link JvmCompilerOptions#createEngine(VarCache)}.
	 * @param builder
	 * 		Builder to populate with analysis results.
	 * @param collector
	 * 		Collector to report any errors that occur during analysis.
	 */
	private void analyzeGeneratedClass(byte @NotNull [] bytes, @NotNull JvmCompilerOptions options,
	                                   @NotNull JvmClassBuilder builder, @NotNull ErrorCollector collector) {
		// Populate the node structure.
		ClassNode classNode = new ClassNode();
		new ClassReader(bytes).accept(classNode, 0);

		// Map our existing analysis results by method signature.
		Map<String, MethodAnalysisResult> existingResults = new HashMap<>();
		for (Map.Entry<MethodNode, AnalysisResults> entry : builder.getMethodAnalysisResults().entrySet())
			if (entry.getValue() instanceof MethodAnalysisResult result)
				existingResults.put(methodKey(entry.getKey().name, entry.getKey().desc), result);
		builder.getMethodAnalysisResults().clear();

		for (MethodNode method : classNode.methods) {
			// Only re-analyze methods that originated from the AST input.
			//
			// Overlay-only methods already exist in the base class and any information about them
			// is out-of-scope for our purposes here.
			if (!existingResults.containsKey(methodKey(method.name, method.desc)))
				continue;

			// Build parameters and known local variables.
			VarCache varCache = new VarCache();
			List<Local> parameters = buildParameters(classNode, method, varCache);
			seedKnownLocals(method, varCache);

			// We're going to reuse existing results if possible.
			// But the analysis logic below will repopulate the states, so we need to clear them first.
			MethodAnalysisResult result = existingResults.getOrDefault(methodKey(method.name, method.desc), new MethodAnalysisResult());
			result.resetAnalysisState();

			// We want to be able to correlate the instructions in the generated class with the AST instructions.
			mapAstInstructions(result, method);

			// First do a linear pass to populate the variable cache with as much info as possible.
			JvmAnalysisEngine<?> engine = options.createEngine(varCache);
			engine.setErrorCollector(collector);
			new IndexedStraightforwardSimulation().execute(new VarCacheUpdater(varCache), method);

			// Copying the InsnList model to a regular List...
			List<AbstractInsnNode> instructions = new ArrayList<>(method.instructions.size());
			for (int i = 0; i < method.instructions.size(); i++)
				instructions.add(method.instructions.get(i));

			// Summarize the method model information.
			JvmAnalysisRunner.Info info = new JvmAnalysisRunner.Info(
					options.inheritanceChecker(),
					Type.getMethodType(method.desc),
					parameters,
					instructions,
					method.tryCatchBlocks
			);

			// Now we can run the actual analysis.
			// This will populate the frames in the result, or set an analysis failure if something goes wrong.
			try {
				new JvmAnalysisRunner(engine.newFrameOps()).execute(engine, info, result);
			} catch (AnalysisException ex) {
				result.setAnalysisFailure(ex);
			}

			// Attempt to link any analysis failure to the original AST instruction,
			// so we can report it to the user with a proper location.
			recordAnalysisFailure(result, collector);

			// Done with this method, store the result for later retrieval.
			builder.setMethodAnalysis(method, result);
		}
	}

	/**
	 * Applies the given overlay to the builder, if it is not null.
	 *
	 * @param collector
	 * 		Collector to report any errors that occur during overlay application.
	 * @param builder
	 * 		Builder to apply the overlay to.
	 * @param overlay
	 * 		Overlay bytes to apply, or {@code null} if no overlay should be applied.
	 */
	private static void applyOverlay(@NotNull ErrorCollector collector, @NotNull JvmClassBuilder builder, byte @Nullable [] overlay) {
		if (overlay == null)
			return;

		try {
			builder.overlay(overlay);
		} catch (Throwable t) {
			collector.addError("Failed to read overlay: " + t.getMessage(), null);
		}
	}

	/**
	 * Builds the parameter list for the given method, including the {@code this} parameter for virtual methods.
	 *
	 * @param node
	 * 		Node of the class containing the method, used to determine the type of the {@code this} parameter.
	 * @param method
	 * 		Node of the method to build parameters for.
	 * @param varCache
	 * 		Variable cache to populate with the parameters.
	 *
	 * @return List of parameters for the method, in order.
	 */
	private static @NotNull List<Local> buildParameters(@NotNull ClassNode node,
	                                                    @NotNull MethodNode method,
	                                                    @NotNull VarCache varCache) {
		List<Local> parameters = new ArrayList<>();
		int localIndex = 0;

		// Add 'this' for virtual methods.
		if ((method.access & Opcodes.ACC_STATIC) == 0) {
			String name = findLocalName(method, 0, Type.getObjectType(node.name).getDescriptor(), "this");
			varCache.getOrCreate(name, 0, false);
			parameters.add(new Local(localIndex++, name, Type.getObjectType(node.name)));
		}

		// Add method parameters.
		for (Type parameterType : Type.getArgumentTypes(method.desc)) {
			String fallbackName = VarNaming.name(localIndex, parameterType);
			String name = findLocalName(method, localIndex, parameterType.getDescriptor(), fallbackName);
			boolean wide = JvmTypeUtils.isWide(parameterType);
			varCache.getOrCreate(name, localIndex, wide);
			parameters.add(new Local(localIndex++, name, parameterType));
			if (wide) {
				parameters.add(null);
				localIndex++;
			}
		}

		return parameters;
	}

	/**
	 * Finds the name of a local variable with the given index and descriptor in the given method.
	 *
	 * @param method
	 * 		Method to search for the local variable.
	 * @param index
	 * 		Variable index.
	 * @param descriptor
	 * 		Variable descriptor.
	 * @param fallback
	 * 		Fallback variable name.
	 *
	 * @return Name of the local variable with the given index and descriptor, or the fallback name if no match is found.
	 */
	private static @NotNull String findLocalName(@NotNull MethodNode method, int index,
	                                             @NotNull String descriptor, @NotNull String fallback) {
		if (method.localVariables != null)
			for (LocalVariableNode localVariable : method.localVariables)
				if (localVariable.index == index && descriptor.equals(localVariable.desc))
					return localVariable.name;
		return fallback;
	}

	/**
	 * @param method
	 * 		Method to seed known local variables for.
	 * @param varCache
	 * 		Variable cache to populate with known local variables.
	 */
	private static void seedKnownLocals(@NotNull MethodNode method, @NotNull VarCache varCache) {
		if (method.localVariables == null)
			return;

		for (LocalVariableNode localVariable : method.localVariables) {
			Type type = Type.getType(localVariable.desc);
			boolean wide = JvmTypeUtils.isWide(type);
			VarCache.Variable variable = varCache.getOrCreate(localVariable.name, localVariable.index, wide);
			variable.updateTypeHint(type);
		}
	}

	/**
	 * Maps the instructions in the given method to the AST instructions in the given analysis result,
	 * so we can correlate analysis failures to the original AST instructions.
	 *
	 * @param result
	 * 		Analysis result containing the AST instructions to map to.
	 * @param method
	 * 		Method to map the instructions of.
	 */
	private static void mapAstInstructions(@NotNull MethodAnalysisResult result, @NotNull MethodNode method) {
		List<ASTInstruction> astInstructions = result.getOrderedAstInstructions();
		if (astInstructions.isEmpty())
			return;

		int astIndex = 0;
		for (AbstractInsnNode instruction = method.instructions.getFirst();
		     instruction != null && astIndex < astInstructions.size();
		     instruction = instruction.getNext()) {
			if (!isExecutable(instruction))
				continue;
			ASTInstruction astInstruction = astInstructions.get(astIndex++);
			result.recordInstructionMapping(astInstruction, instruction);
		}
	}

	/**
	 * @param instruction
	 * 		Instruction to check.
	 *
	 * @return {@code true} if the instruction is executable, {@code false} otherwise.
	 */
	private static boolean isExecutable(@NotNull AbstractInsnNode instruction) {
		return instruction.getOpcode() >= 0;
	}

	/**
	 * @param result
	 * 		Analysis result containing the analysis failure to record.
	 * @param collector
	 * 		Collector to report the analysis failure to.
	 */
	private static void recordAnalysisFailure(@NotNull MethodAnalysisResult result, @NotNull ErrorCollector collector) {
		AnalysisException failure = result.getAnalysisFailure();
		if (failure == null)
			return;

		AbstractInsnNode instruction = failure.getInstruction();
		if (instruction != null) {
			ASTInstruction targetInsn = result.getInstructionToAstMap().get(instruction);
			if (targetInsn != null) {
				collector.addError(failure.getMessage(), targetInsn.location());
				return;
			}
		}

		collector.addError(failure.getMessage(), Location.UNKNOWN);
	}

	/**
	 * Writes the class represented by the given builder to a byte array.
	 *
	 * @param builder
	 * 		Builder containing the class to write.
	 * @param options
	 * 		Compiler options, used to determine how to write the class <i>(Ex: whether to merge with an overlay)</i>.
	 *
	 * @return Compiled class bytes.
	 */
	private static byte @NotNull [] writeClass(@NotNull JvmClassBuilder builder, @NotNull JvmCompilerOptions options) {
		ClassNode node = builder.node();

		// Determine flags for writing the class.
		int flags = options.asmArgs;
		if (node.version <= Opcodes.V1_5)
			flags &= ~ClassWriter.COMPUTE_FRAMES;

		// If there is no overlay specified we can just write the class as is,
		// without needing to worry about merging methods or anything.
		if (options.overlay == null) {
			ClassWriter writer = new JvmClassWriter(flags, options.inheritanceChecker());
			node.accept(writer);
			return writer.toByteArray();
		}

		// However, if there is an overlay, we need to be careful.
		// We don't want to recompute frames for methods that haven't been modified,
		// so we need to merge the methods from the overlay with the ones from our class node.
		ClassReader overlayReader = new ClassReader(options.overlay.classFile());
		JvmClassWriter writer = new JvmClassWriter(overlayReader, flags, options.inheritanceChecker());
		writeClassWithoutMethods(node, writer);
		writeOverlayMethods(node, overlayReader, writer, builder.modifiedMethodKeys(), flags);
		return writer.toByteArray();
	}

	/**
	 * Write top-level class information from the given class node to the given writer, but skip all methods.
	 * We'll write methods in a following pass in {@link #writeOverlayMethods(ClassNode, ClassReader, JvmClassWriter, Set, int)}.
	 *
	 * @param node
	 * 		Node of the class we're writing, used to get all the non-method information.
	 * @param writer
	 * 		Writer to write the class to.
	 */
	private static void writeClassWithoutMethods(@NotNull ClassNode node, @NotNull ClassVisitor writer) {
		node.accept(new ClassVisitor(Opcodes.ASM9, writer) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				return null;
			}
		});
	}

	/**
	 * Writes the methods from the overlay class, merging them with the methods from our class node.
	 *
	 * @param node
	 * 		Node of the class we're writing.
	 * @param overlayReader
	 * 		Reader of the overlay class.
	 * @param writer
	 * 		Writer to write the class to.
	 * @param modifiedMethodKeys
	 * 		Set of method keys that have been modified.
	 * @param methodFlags
	 * 		Flags to use when writing methods.
	 */
	private static void writeOverlayMethods(@NotNull ClassNode node,
	                                        @NotNull ClassReader overlayReader,
	                                        @NotNull JvmClassWriter writer,
	                                        @NotNull Set<String> modifiedMethodKeys,
	                                        int methodFlags) {
		// Create a map of method keys to method nodes for the methods in our class node,
		// so we can easily look them up when visiting the overlay.
		Map<String, MethodNode> finalMethods = new LinkedHashMap<>(); // Track declaration order.
		for (MethodNode method : node.methods)
			finalMethods.put(methodKey(method.name, method.desc), method);

		Set<String> emittedMethods = new HashSet<>();
		overlayReader.accept(new ClassVisitor(Opcodes.ASM9) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
			                                 String[] exceptions) {
				// Skip this method if it has been modified, we'll emit it ourselves later.
				String key = methodKey(name, descriptor);
				MethodNode finalMethod = finalMethods.get(key);
				if (finalMethod == null)
					return null;

				// This method exists in both the overlay and our class node, so we need to merge them.
				emittedMethods.add(key);
				if (!modifiedMethodKeys.contains(key)) {
					// This method hasn't been modified, we can just copy it from the overlay without recomputing frames.
					writer.setFlags(0);
					return writer.visitMethod(access, name, descriptor, signature, exceptions);
				} else {
					// This method has been modified, we need to emit it from our class node, which will recompute frames if necessary.
					emitMethod(writer, finalMethod, methodFlags);
					return null;
				}
			}
		}, 0);

		// Finally, we need to emit any methods that are in our class node but not in the overlay,
		// since they won't be emitted by the ClassVisitor above.
		for (MethodNode method : node.methods) {
			String key = methodKey(method.name, method.desc);
			if (emittedMethods.add(key))
				emitMethod(writer, method, methodFlags);
		}
	}

	/**
	 * Write a method to the given writer.
	 *
	 * @param writer
	 * 		Writer to write the method to.
	 * @param method
	 * 		Method to emit.
	 * @param flags
	 * 		Flags to use when writing the method.
	 */
	private static void emitMethod(@NotNull JvmClassWriter writer, @NotNull MethodNode method, int flags) {
		writer.setFlags(flags);
		MethodVisitor visitor = writer.visitMethod(
				method.access,
				method.name,
				method.desc,
				method.signature,
				method.exceptions == null ? null : method.exceptions.toArray(String[]::new)
		);
		method.accept(visitor);
	}
}