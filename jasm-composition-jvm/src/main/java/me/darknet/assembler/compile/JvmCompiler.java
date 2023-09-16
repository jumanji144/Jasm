package me.darknet.assembler.compile;

import dev.xdark.blw.BytecodeLibrary;
import dev.xdark.blw.asm.AsmBytecodeLibrary;
import dev.xdark.blw.asm.ClassWriterProvider;
import dev.xdark.blw.classfile.ClassBuilder;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.compile.builder.BlwReplaceClassBuilder;
import me.darknet.assembler.compile.visitor.BlwRootVisitor;
import me.darknet.assembler.compiler.Compiler;
import me.darknet.assembler.compiler.CompilerOptions;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.transformer.Transformer;
import org.objectweb.asm.ClassWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class JvmCompiler implements Compiler {

    public static final BytecodeLibrary library = new AsmBytecodeLibrary(
            ClassWriterProvider.flags(ClassWriter.COMPUTE_FRAMES)
    );

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
    public Result<JavaClassRepresentation> compile(List<ASTElement> ast, CompilerOptions<?> options) {

        JvmCompilerOptions blwOptions = (JvmCompilerOptions) options;
        BlwReplaceClassBuilder builder = new BlwReplaceClassBuilder();

        ErrorCollector collector = new ErrorCollector();
        BlwRootVisitor visitor = new BlwRootVisitor(builder, blwOptions);

        if (ast.size() != 1) {
            collector.addError("Expected exactly one class declaration", null);
            return new Result<>(null, collector.getErrors());
        }

        builder.version(blwOptions.version);

        if(blwOptions.overlay != null)
            applyOverlay(collector, builder, blwOptions.overlay.data());
        if (collector.hasErr()) {
            return new Result<>(null, collector.getErrors());
        }

        Transformer transformer = new Transformer(visitor);
        transformer.transform(ast).ifErr(collector::addAll);

        if (!collector.hasErr()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                library.write(builder.build(), out);
            } catch (IOException e) {
                collector.addError("Failed to write class: " + e.getMessage(), null);
                // we cannot continue, the result might be very corrupted
                return new Result<>(null, collector.getErrors());
            }
            return new Result<>(new JavaClassRepresentation(out.toByteArray()), collector.getErrors());
        }

        return new Result<>(null, collector.getErrors());
    }

}
