package me.darknet.assembler.compile.analysis;

import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.util.JvmTypeUtils;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Populates local variable table entries from analysis frames.
 */
@Deprecated
public final class AnalysisLocalTableSynthesizer {
    private AnalysisLocalTableSynthesizer() {}

    public static void synthesize(@NotNull MethodNode method, @NotNull AnalysisResults results,
                                  @NotNull ErrorCollector errorCollector, @NotNull InheritanceChecker checker,
                                  @NotNull List<Local> parameters, @NotNull LabelNode begin, @NotNull LabelNode end) {
        int paramOffset = parameters.size();
        if (method.localVariables == null) {
            method.localVariables = new java.util.ArrayList<>();
        }

        Map<String, Local> localsMap = new HashMap<>();
        results.frames().forEach((index, frame) -> {
            for (Local local : frame.locals().toList()) {
                if (local.index() < paramOffset)
                    continue;

                localsMap.merge(local.name(), local,
                        (a, b) -> mergeLocals(results, errorCollector, checker, method, index, a, b));
            }
        });

        localsMap.forEach((name, local) -> method.localVariables.add(new LocalVariableNode(
                name,
                local.safeType().getDescriptor(),
                null,
                begin,
                end,
                local.index()
        )));
    }

    private static @NotNull Local mergeLocals(@NotNull AnalysisResults results, @NotNull ErrorCollector errorCollector,
                                              @NotNull InheritanceChecker checker, @NotNull MethodNode method, int index,
                                              @NotNull Local a, @NotNull Local b) {
        Type at = a.type();
        Type bt = b.type();

        if (at == null)
            return b;
        if (bt == null)
            return a;
        if (at.getSort() != bt.getSort())
            return a.adaptType(JvmTypeUtils.OBJECT);

        try {
            return a.adaptType(Objects.requireNonNullElse(AnalysisUtils.commonType(checker, at, bt), JvmTypeUtils.OBJECT));
        } catch (RuntimeException ex) {
            AbstractInsnNode instruction = method.instructions.get(index);
            ASTInstruction ast = results.getInstructionToAstMap().get(instruction);
            if (ast != null) {
                errorCollector.addError(ex.getMessage(), ast.location());
                return a.adaptType(JvmTypeUtils.OBJECT);
            }
            throw new IllegalStateException("Failed merging synthesized locals without AST mapping", ex);
        }
    }
}
