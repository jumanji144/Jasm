package me.darknet.assembler.printer;

import dev.xdark.blw.code.Code;
import dev.xdark.blw.type.Type;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.compile.analysis.jvm.IndexedStraightforwardSimulation;
import me.darknet.assembler.helper.Names;
import me.darknet.assembler.util.EscapeUtil;
import me.darknet.assembler.util.LabelUtil;

import dev.xdark.blw.classfile.AccessFlag;
import dev.xdark.blw.classfile.Method;
import dev.xdark.blw.code.CodeElement;
import dev.xdark.blw.code.Label;
import dev.xdark.blw.code.attribute.Local;
import dev.xdark.blw.type.ClassType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JvmMethodPrinter implements MethodPrinter {

    protected final Method method;
    protected final MemberPrinter memberPrinter;
    protected String labelPrefix;

    public JvmMethodPrinter(Method method) {
        this.method = method;
        this.memberPrinter = new MemberPrinter(method, MemberPrinter.Type.METHOD);
    }

    static String escapeName(String name, int index, boolean isStatic) {
        if (name.equals("this") && !(index == 0 && !isStatic))
            return "p" + index;
        else
            return EscapeUtil.escapeLiteral(name);
    }

    public void setLabelPrefix(String labelPrefix) {
        this.labelPrefix = labelPrefix;
    }

    public Names localNames() {
        Map<String, Type> nameToType = new HashMap<>();
        List<Names.Local> locals = new ArrayList<>();
        boolean isStatic = (method.accessFlags() & AccessFlag.ACC_STATIC) != 0;
        Code code = method.code();
        if (code != null) {
            for (Local local : code.localVariables()) {
                // Transform local name to be legal
                int index = local.index();
                String name = escapeName(local.name(), index, isStatic);
                String descriptor = local.type().descriptor();
                Type varType = Types.typeFromDescriptor(descriptor);

                // De-conflict variable names if two names of incompatible types occupy the same name.
                //    int foo = 0       ---> foo
                //    String foo = ""   ---> foo2
                //    byte[] foo = ...  ---> foo3
                Type existingVarType = nameToType.get(name);
                if (existingVarType != null && varType.getClass() != existingVarType.getClass()) {
                    int i = 2;
                    String prefix = name;
                    while (nameToType.get(name) != null)
                        name = prefix + (i++);
                }

                locals.add(new Names.Local(index, local.start().getIndex(), local.end().getIndex(), name, descriptor));
                nameToType.put(name, varType);
            }
        }
        Map<Integer, String> parameterNames = new HashMap<>();
        int offset = isStatic ? 0 : 1;
        if (!isStatic) {
            parameterNames.put(0, "this");
        }
        List<ClassType> types = method.type().parameterTypes();
        for (int i = 0; i < types.size(); i++) {
            int varSlot = i + offset;
            String name = getName(locals, varSlot);
            parameterNames.put(varSlot, name);

            // Skip creating parameters for reserved slots
            if (Types.category(types.get(i)) > 1)
                i++;
        }
        return new Names(parameterNames, locals);
    }

    @NotNull
    private static String getName(List<Names.Local> locals, int i) {
        String name = null;
        // search for parameter name in local variables, first reference of the index which matches the type
        for (Names.Local local : locals) {
            if (local.index() == i) {
                name = local.name();
                break;
            }
        }
        if (name == null)
            name = "p" + i;
        return name;
    }

    public Map<Integer, String> getLabelNames(List<CodeElement> elements) {
        Map<Integer, String> labelNames = new HashMap<>();
        int labelIndex = 0;
        for (CodeElement element : elements) {
            if (element instanceof Label label) {
                String labelName = LabelUtil.getLabelName(labelIndex++);
                if (labelPrefix != null) labelName = labelPrefix + labelName;
                labelNames.put(label.getIndex(), labelName);
            }
        }
        return labelNames;
    }

    @Override
    public void print(PrintContext<?> ctx) {
        memberPrinter.printAttributes(ctx);
        var obj = memberPrinter.printDeclaration(ctx).literal(method.name()).print(" ")
                .literal(method.type().descriptor()).print(" ").object();
        Names names = localNames();
        boolean hasParameters = !names.parameters().isEmpty();
        if (hasParameters) {
            var arr = obj.value("parameters").array();
            arr.print(names.parameters().values(), PrintContext::print);
            arr.end();
        }
        var methodCode = method.code();
        if (methodCode != null) {
            if (hasParameters) obj.next();
            Map<Integer, String> labelNames = getLabelNames(methodCode.elements());
            if (!methodCode.tryCatchBlocks().isEmpty()) {
                var arr = obj.value("exceptions").array();
                arr.print(methodCode.tryCatchBlocks(), (print, tcb) -> {
                    var exception = print.array();
                    String start = labelNames.get(tcb.start().getIndex());
                    String end = labelNames.get(tcb.end().getIndex());
                    String handler = labelNames.get(tcb.handler().getIndex());

                    String type = tcb.type() == null ? "*" : tcb.type().descriptor();

                    exception.print(start).arg().print(end).arg().print(handler).arg().print(type);

                    exception.end();
                });

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