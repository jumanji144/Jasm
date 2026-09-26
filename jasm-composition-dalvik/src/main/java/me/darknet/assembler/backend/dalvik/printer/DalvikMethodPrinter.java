package me.darknet.assembler.backend.dalvik.printer;

import me.darknet.assembler.printer.*;

import me.darknet.dex.tree.definitions.MethodMember;
import me.darknet.dex.tree.definitions.code.Code;
import me.darknet.dex.tree.definitions.debug.DebugInformation;
import me.darknet.dex.tree.definitions.instructions.Label;
import me.darknet.dex.tree.simulation.StraightForwardSimulation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class DalvikMethodPrinter implements MethodPrinter {

    private final MethodMember definition;
    private final DalvikMemberPrinter memberPrinter;

    public DalvikMethodPrinter(MethodMember definition) {
        this.definition = definition;
        this.memberPrinter = new DalvikMemberPrinter(definition, definition, DalvikMemberPrinter.Type.METHOD);
    }

    @Override
    public @Nullable AnnotationPrinter annotation(int index) {
        return memberPrinter.printAnnotation(index);
    }

    @Override
    public @Nullable AnnotationPrinter visibleAnnotation(int index) {
        return memberPrinter.printAnnotation(index);
    }

    @Override
    public @Nullable AnnotationPrinter invisibleAnnotation(int index) {
        return memberPrinter.printAnnotation(index);
    }

    private static String getLabelName(int index) {
        StringBuilder label = new StringBuilder();

        while (index >= 0) {
            label.insert(0, (char) ('A' + index % 26));
            index = (index / 26) - 1;
        }

        return label.toString();
    }

    @Override
    public void print(PrintContext<?> ctx) {
        memberPrinter.printAttributes(ctx);
        if (definition.getSignature() != null) {
            ctx.begin().element(".signature").string(definition.getSignature()).next();
        }
        var obj = memberPrinter.printDeclaration(ctx).literal(definition.getName()).print(" ")
                .literal(definition.getType().descriptor()).print(" ").object();

        var code = definition.getCode();
        Map<Label, String> labelNames = code == null ? Map.of() : getLabelNames(code);

        boolean hasPrior = false;
        if (code != null) {
            obj.value("registers").print(String.valueOf(code.getRegisters()));
            hasPrior = true;

            int parameterCount = definition.getType().parameterTypes().size();
            if (parameterCount > 0) {
                obj.next();
                PrintContext.ArrayPrint parameters = obj.value("parameters").array();
                List<String> names = definition.getParameterNames() == null
                        ? List.of()
                        : definition.getParameterNames();
                DebugInformation debug = code.getDebugInfo();
                List<String> debugNames = debug == null ? List.of() : debug.parameterNames();
                for (int i = 0; i < parameterCount; i++) {
                    String name = i < names.size() ? names.get(i) : null;
                    if (name == null && i < debugNames.size()) {
                        name = debugNames.get(i);
                    }
                    parameters.print(name == null ? "p" + i : name);
                    if (i < parameterCount - 1) {
                        parameters.arg();
                    }
                }
                parameters.end();
            }

            if (!definition.getThrownTypes().isEmpty()) {
                obj.next();
                PrintContext.ArrayPrint thrownTypes = obj.value("throws").array();
                for (int i = 0; i < definition.getThrownTypes().size(); i++) {
                    thrownTypes.literal(definition.getThrownTypes().get(i));
                    if (i < definition.getThrownTypes().size() - 1) {
                        thrownTypes.arg();
                    }
                }
                thrownTypes.end();
            }
        }

        if (code != null && !code.tryCatch().isEmpty()) {
            if (hasPrior) obj.next();

            PrintContext.ArrayPrint exceptions = obj.value("exceptions").array();
            boolean firstHandler = true;
            for (var tryCatch : code.tryCatch()) {
                if (tryCatch.handlers().isEmpty()) {
                    throw new IllegalStateException("Cannot print a try-catch entry without handlers");
                }
                for (var handler : tryCatch.handlers()) {
                    if (!firstHandler) {
                        exceptions.arg();
                    }
                    exceptions.array()
                            .print(labelNames.get(tryCatch.begin())).arg()
                            .print(labelNames.get(tryCatch.end())).arg()
                            .print(labelNames.get(handler.handler())).arg()
                            .literal(handler.exceptionType() == null ? "*" : handler.exceptionType().internalName())
                            .end();
                    firstHandler = false;
                }
            }
            exceptions.end();
            hasPrior = true;
        }

        if (code != null) {
            if (hasPrior) obj.next();

            var codeObj = obj.value("code").code();

            Map<Integer, String> registers = getRegisterNames(code);

            DalvikCodePrinter printer = new DalvikCodePrinter(codeObj, registers, labelNames);
            StraightForwardSimulation simulation = new StraightForwardSimulation();

            simulation.execute(printer, code);

            codeObj.end();
        }

        obj.end();
    }

    private static @NotNull Map<Label, String> getLabelNames(@NotNull Code code) {
        Map<Label, String> labelNames = new IdentityHashMap<>();
        int labelIndex = 0;
        for (var instruction : code.getInstructions()) {
            if (instruction instanceof Label label) {
                labelNames.put(label, getLabelName(labelIndex++));
            }
        }
        return labelNames;
    }

    private static @NotNull Map<Integer, String> getRegisterNames(@NotNull Code code) {
        // TODO: Implement proper register name resolution using debug information, similar to how we do for JVM impl.
        /*
        DebugInformation debugInfo = code.getDebugInfo();
        List<DebugInformation.LocalVariable> locals = debugInfo == null ? Collections.emptyList() : debugInfo.locals();
        List<String> params = debugInfo == null ? Collections.emptyList() : debugInfo.parameterNames();

        for (DebugInformation.LocalVariable local : locals) {
            // intermittent name changes are not supported, so we just use the first name we see
            registers.putIfAbsent(local.register(), local.name());
        }

        int paramBase = code.getRegisters() - code.getIn();

        for (int i = 0; i < code.getIn(); i++) {
            if (params != null && i < params.size()) {
                registers.putIfAbsent(paramBase + i, params.get(i));
                continue;
            }
            registers.putIfAbsent(paramBase + i, "p" + i);
        }
        for (int i = 0; i < code.getRegisters() - code.getIn(); i++) {
            registers.putIfAbsent(i, "v" + (i));
         */

        Map<Integer, String> registers = new HashMap<>();
        for (int i = 0; i < code.getRegisters(); i++) {
            registers.put(i, "v" + i);
        }
        return registers;
    }
}

