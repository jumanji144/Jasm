package me.darknet.assembler.printer;

import dev.xdark.blw.classfile.AccessFlag;
import dev.xdark.blw.classfile.Method;
import dev.xdark.blw.code.CodeElement;
import dev.xdark.blw.code.Label;
import dev.xdark.blw.code.TryCatchBlock;
import dev.xdark.blw.code.attribute.Local;
import dev.xdark.blw.type.ClassType;
import me.darknet.assembler.helper.Names;
import me.darknet.assembler.util.EscapeUtil;
import me.darknet.assembler.util.IndexedStraightforwardSimulation;
import me.darknet.assembler.util.LabelUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JvmMethodPrinter implements MethodPrinter {

    protected Method method;
    protected MemberPrinter memberPrinter;

    public JvmMethodPrinter(Method method) {
        this.method = method;
        this.memberPrinter = new MemberPrinter(method, MemberPrinter.Type.METHOD);
    }

    static String escapeName(String name, int index, boolean isStatic) {
        if(name.equals("this") && !(index == 0 && !isStatic))
            return "p" + index;
        else
            return EscapeUtil.escapeLiteral(name);
    }

    public Names localNames() {
        List<Names.Local> locals = new ArrayList<>();
        boolean isStatic = (method.accessFlags() & AccessFlag.ACC_STATIC) != 0;
        if (method.code() != null) {
            for (Local localVariable : method.code().localVariables()) {
                // transform local name
                String name = escapeName(localVariable.name(), localVariable.index(), isStatic);
                locals.add(
                        new Names.Local(
                                localVariable.index(), localVariable.start().index(), localVariable.end().index(),
                                name, localVariable.type().descriptor()
                        )
                );
            }
        }
        Map<Integer, String> parameterNames = new HashMap<>();
        int offset = isStatic ? 0 : 1;
        if (!isStatic) {
            parameterNames.put(0, "this");
        }
        List<ClassType> types = method.type().parameterTypes();
        for (int i = 0; i < types.size(); i++) {
            String name = getName(locals, i, offset);
            parameterNames.put(i + offset, name);
        }
        return new Names(parameterNames, locals);
    }

    @NotNull
    private static String getName(List<Names.Local> locals, int i, int offset) {
        String name = null;
        // search for parameter name in local variables, first reference of the index which matches the type
        for (Names.Local local : locals) {
            if (local.index() == i + offset) {
                name = local.name();
                break;
            }
        }
        if (name == null)
            name = "p" + (i + offset);
        return name;
    }

    public static Map<Integer, String> getLabelNames(List<CodeElement> elements) {
        Map<Integer, String> labelNames = new HashMap<>();
        int labelIndex = 0;
        for (CodeElement element : elements) {
            if (element instanceof Label label) {
                labelNames.put(label.index(), LabelUtil.getLabelName(labelIndex++));
            }
        }
        return labelNames;
    }

    @Override
    public void print(PrintContext<?> ctx) {
        memberPrinter.printAttributes(ctx);
        var obj = memberPrinter.printDeclaration(ctx)
                .literal(method.name()).print(" ")
                .literal(method.type().descriptor()).print(" ")
                .object();
        Names names = localNames();
        if (!names.parameters().isEmpty()) {
            var arr = obj.value("parameters").array();

            for (String value : names.parameters().values()) {
                arr.print(value).arg();
            }

            arr.end();
            obj.next();
        }
        var methodCode = method.code();
        if (methodCode != null) {
            Map<Integer, String> labelNames = getLabelNames(methodCode.elements());
            if(!methodCode.tryCatchBlocks().isEmpty()) {
                var arr = obj.value("exceptions").array();

                for (TryCatchBlock tryCatchBlock : methodCode.tryCatchBlocks()) {
                    var exception = arr.array();

                    String start = labelNames.get(tryCatchBlock.start().index());
                    String end = labelNames.get(tryCatchBlock.end().index());
                    String handler = labelNames.get(tryCatchBlock.handler().index());

                    String type = tryCatchBlock.type() == null ? "*" : tryCatchBlock.type().descriptor();

                    exception.print(start).arg()
                            .print(end).arg()
                            .print(handler).arg()
                            .print(type);

                    exception.end();
                    arr.arg();
                }

                arr.end();
                obj.next();
            }
            var code = obj.value("code").code();
            InstructionPrinter printer = new InstructionPrinter(code, methodCode, names, labelNames);
            IndexedStraightforwardSimulation simulation = new IndexedStraightforwardSimulation();
            simulation.execute(printer, method);
            code.end();
        }

        obj.end();
    }

    @Override
    public AnnotationPrinter annotation(int index) {
        return memberPrinter.printAnnotation(index);
    }
}