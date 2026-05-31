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
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JvmCompiler implements Compiler {
	@Override
	public @NotNull Result<JavaCompileResult> compile(List<ASTElement> ast, CompilerOptions<?> options) {
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
				byte[] bytes = writeClass(builder.node(), options);

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

	private void analyzeGeneratedClass(@NotNull byte[] bytes, @NotNull JvmCompilerOptions options,
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

	private static void applyOverlay(@NotNull ErrorCollector collector, @NotNull JvmClassBuilder builder, byte[] overlay) {
		if (overlay == null)
			return;

		try {
			builder.overlay(overlay);
		} catch (Throwable t) {
			collector.addError("Failed to read overlay: " + t.getMessage(), null);
		}
	}

	private static @NotNull List<Local> buildParameters(@NotNull ClassNode classNode,
	                                                    @NotNull MethodNode method,
	                                                    @NotNull VarCache varCache) {
		List<Local> parameters = new ArrayList<>();
		int localIndex = 0;

		// Add 'this' for virtual methods.
		if ((method.access & Opcodes.ACC_STATIC) == 0) {
			String name = findLocalName(method, 0, Type.getObjectType(classNode.name).getDescriptor(), "this");
			varCache.getOrCreate(name, 0, false);
			parameters.add(new Local(localIndex++, name, Type.getObjectType(classNode.name)));
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

	private static @NotNull String findLocalName(@NotNull MethodNode method, int index, @NotNull String descriptor,
	                                             @NotNull String fallback) {
		if (method.localVariables != null)
			for (LocalVariableNode localVariable : method.localVariables)
				if (localVariable.index == index && descriptor.equals(localVariable.desc))
					return localVariable.name;
		return fallback;
	}

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

	private static boolean isExecutable(@NotNull AbstractInsnNode instruction) {
		return instruction.getOpcode() >= 0;
	}

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

	private static @NotNull String methodKey(@NotNull String name, @NotNull String descriptor) {
		return name + descriptor;
	}

	private static int correctFlags(@NotNull ClassNode classNode) {
		int flags = ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS;
		if (classNode.version <= Opcodes.V1_5) {
			flags &= ~ClassWriter.COMPUTE_FRAMES;
		}
		return flags;
	}

	private static byte @NotNull [] writeClass(@NotNull ClassNode classNode, @NotNull CompilerOptions<?> options) {
		ClassWriter writer = new JvmClassWriter(correctFlags(classNode), options.inheritanceChecker());
		classNode.accept(writer);
		return writer.toByteArray();
	}
}