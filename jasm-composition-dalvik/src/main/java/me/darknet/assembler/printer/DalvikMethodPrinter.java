package me.darknet.assembler.printer;

import me.darknet.dex.tree.definitions.MethodMember;
import me.darknet.dex.tree.simulation.StraightForwardSimulation;
import org.jetbrains.annotations.Nullable;

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

    @Override
    public void print(PrintContext<?> ctx) {
        memberPrinter.printAttributes(ctx);
        var obj = memberPrinter.printDeclaration(ctx).literal(definition.name()).print(" ")
                .literal(definition.type().descriptor()).print(" ").object();

        var code = definition.code();

        boolean hasPrior = code != null && code.in() != 0;
        if (hasPrior) {
            var arr = obj.value("parameters").array();
            for (int i = 0; i < code.in(); i++) {
                arr.print("p" + i);
                if (i < code.in() - 1) arr.arg();
            }
            arr.end();
        }

        if (code != null) {
            if (hasPrior) obj.next();

            var codeObj = obj.value("code").code();

            DalvikCodePrinter printer = new DalvikCodePrinter(codeObj);
            StraightForwardSimulation simulation = new StraightForwardSimulation();

            simulation.execute(printer, code);

            codeObj.end();
        }

        obj.end();
    }
}
