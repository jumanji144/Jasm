package me.darknet.assembler.printer;

import me.darknet.dex.tree.definitions.MethodMember;
import me.darknet.dex.tree.definitions.code.Code;
import me.darknet.dex.tree.definitions.debug.DebugInformation;
import me.darknet.dex.tree.definitions.instructions.Label;
import me.darknet.dex.tree.simulation.StraightForwardSimulation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.IdentityHashMap;
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
        var obj = memberPrinter.printDeclaration(ctx).literal(definition.getName()).print(" ")
                .literal(definition.getType().descriptor()).print(" ").object();

        var code = definition.code();


        boolean hasPrior = code != null && code.getIn() != 0;
        if (hasPrior) {
            var params = code.getDebugInfo().parameterNames();

            var arr = obj.value("parameters").array();
            for (int i = 0; i < code.getIn(); i++) {
                if (params != null && i < params.size()) {
                    arr.print(params.get(i));
                } else {
                    arr.print("p" + i);
                }
            }
            arr.end();
        }

        if (code != null) {
            if (hasPrior) obj.next();

            var codeObj = obj.value("code").code();

            Map<Integer, String> registers = getRegisterNames(code);

            Map<Label, String> labelNames = new IdentityHashMap<>();
            int labelIndex = 0;
            for (var ins : code.getInstructions()) {
                if (ins instanceof Label label) {
                    labelNames.put(label, getLabelName(labelIndex++));
                }
            }

            DalvikCodePrinter printer = new DalvikCodePrinter(codeObj, registers, labelNames);
            StraightForwardSimulation simulation = new StraightForwardSimulation();

            simulation.execute(printer, code);

            codeObj.end();
        }

        obj.end();
    }

    private static @NotNull Map<Integer, String> getRegisterNames(Code code) {
        Map<Integer, String> registers = new HashMap<>();
        var locals = code.getDebugInfo().locals();
        var params = code.getDebugInfo().parameterNames();

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
        }
        return registers;
    }
}
