package me.darknet.assembler.compile;

import dev.xdark.blw.code.CodeElement;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.compile.analysis.AnalysisException;
import me.darknet.assembler.compile.analysis.AnalysisResults;
import me.darknet.assembler.compile.builder.BlwReplaceClassBuilder;
import me.darknet.assembler.compile.visitor.BlwRootVisitor;
import me.darknet.assembler.compile.visitor.JavaCompileResult;
import me.darknet.assembler.compiler.Compiler;
import me.darknet.assembler.compiler.CompilerOptions;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.transformer.Transformer;

import dev.xdark.blw.BytecodeLibrary;
import dev.xdark.blw.asm.AsmBytecodeLibrary;
import dev.xdark.blw.asm.ClassWriterProvider;
import dev.xdark.blw.classfile.ClassBuilder;
import dev.xdark.blw.classfile.ClassFileView;
import dev.xdark.blw.version.JavaVersion;
import me.darknet.assembler.util.Location;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class JvmCompiler implements Compiler {

    private BytecodeLibrary library;

    private void applyOverlay(ErrorCollector collector, ClassBuilder builder, byte[] overlay) {
        if (overlay != null) {
            try {
                library.read(new ByteArrayInputStream(overlay), builder);
            } catch (IOException e) {
                collector.addError("Failed to read overlay: " + e.getMessage(), null);
            }
        }
    }

    @Override
    public @NotNull Result<JavaCompileResult> compile(List<ASTElement> ast, CompilerOptions<?> options) {
        this.library = new AsmBytecodeLibrary(new ClassWriterProvider() {
            @Override
            public ClassWriter newClassWriterFor(ClassReader classReader, ClassFileView classFileView) {
                return new JvmClassWriter(classReader, correctFlags(classFileView), options.inheritanceChecker());
            }

            @Override
            public ClassWriter newClassWriterFor(ClassFileView classFileView) {
                return new JvmClassWriter(correctFlags(classFileView), options.inheritanceChecker());
            }

            int correctFlags(ClassFileView classFileView) {
                JavaVersion version = classFileView.version();
                int flags = ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS;
                if (version.majorVersion() <= Opcodes.V1_5) {
                    flags &= ~ClassWriter.COMPUTE_FRAMES;
                }
                return flags;
            }
        });

        JvmCompilerOptions blwOptions = (JvmCompilerOptions) options;
        BlwReplaceClassBuilder builder = new BlwReplaceClassBuilder();

        ErrorCollector collector = new ErrorCollector();
        BlwRootVisitor visitor = new BlwRootVisitor(builder, blwOptions);

        if (ast.size() != 1) {
            collector.addError("Expected exactly one declaration", null);
            return new Result<>(new JavaCompileResult(null, builder), collector.getErrors());
        }

        builder.setVersion(blwOptions.version);

        if (blwOptions.overlay != null)
            applyOverlay(collector, builder, blwOptions.overlay.classFile());
        if (collector.hasErr()) {
            return new Result<>(new JavaCompileResult(null, builder), collector.getErrors());
        }

        Transformer transformer = new Transformer(visitor);
        transformer.transform(ast).ifErr(collector::addAll);

        if (!collector.hasErr()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                library.write(builder.build(), out);
            } catch (Throwable t) {
                // We cannot continue, the result might be very corrupted.
                // Collect as much info that could have led to the error as possible.
                boolean recordedError = false;
                for (var methodEntry : builder.allResults().entrySet()) {
                    AnalysisResults analysisResults = methodEntry.getValue();
                    AnalysisException failure = analysisResults.getAnalysisFailure();
                    if (failure != null) {
                        CodeElement element = failure.getElement();
                        if (element != null) {
                            ASTInstruction targetInsn = analysisResults.getCodeToAstMap().get(element);
                            if (targetInsn != null) {
                                Location location = targetInsn.location();
                                collector.addError(failure.getMessage(), location);
                                recordedError = true;
                            } else {
                                collector.addError(failure.getMessage(), Location.UNKNOWN);
                                recordedError = true;
                            }
                        }
                    }
                }

                // Fallback if there were no reported errors from the analysis process
                if (!recordedError)
                    collector.addError("Failed to write class: " + t.getMessage(), null);

                return new Result<>(new JavaCompileResult(null, builder), collector.getErrors());
            }
            return new Result<>(new JavaCompileResult(new JavaClassRepresentation(out.toByteArray()), builder), collector.getErrors());
        }

        return new Result<>(new JavaCompileResult(null, builder), collector.getErrors());
    }

    public BytecodeLibrary library() {
        return library;
    }

}
