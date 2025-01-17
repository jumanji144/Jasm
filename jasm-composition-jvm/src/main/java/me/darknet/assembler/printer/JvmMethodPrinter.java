package me.darknet.assembler.printer;

import dev.xdark.blw.annotation.Element;
import dev.xdark.blw.classfile.AccessFlag;
import dev.xdark.blw.classfile.Method;
import dev.xdark.blw.code.Code;
import dev.xdark.blw.code.CodeElement;
import dev.xdark.blw.code.Label;
import dev.xdark.blw.code.attribute.Local;
import dev.xdark.blw.code.generic.GenericLabel;
import dev.xdark.blw.code.instruction.VarInstruction;
import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.PrimitiveType;
import dev.xdark.blw.type.Type;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.compile.analysis.jvm.IndexedStraightforwardSimulation;
import me.darknet.assembler.helper.Variables;
import me.darknet.assembler.util.BlwOpcodes;
import me.darknet.assembler.util.EscapeUtil;
import me.darknet.assembler.util.LabelUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class JvmMethodPrinter implements MethodPrinter {

    protected final Method method;
    protected final JvmMemberPrinter memberPrinter;

    public JvmMethodPrinter(Method method) {
        this.method = method;
        this.memberPrinter = new JvmMemberPrinter(method, JvmMemberPrinter.Type.METHOD);
    }

    public @NotNull Variables buildVariables(PrintContext<?> ctx) {
        Map<String, Type> nameToType = new HashMap<>();
        List<Variables.Local> locals = new ArrayList<>();
        boolean isStatic = (method.accessFlags() & AccessFlag.ACC_STATIC) != 0;
        Code code = method.code();
        if (code != null && !ctx.ignoreExistingVariableNames) {
            for (Local local : code.localVariables()) {
                // Transform local name to be legal
                int index = local.index();
                boolean isThis = !isStatic && index == 0;
                String baseName = isThis ? "this" : local.name();
                String name = isThis ? "this" : escapeVariableName(baseName, index, isStatic);
                String descriptor = local.type().descriptor();
                Type varType = Types.typeFromDescriptor(descriptor);
                boolean escaped = !baseName.equals(name);

                // De-conflict variable names if two names of incompatible types occupy the same name.
                //    int foo = 0       ---> foo
                //    String foo = ""   ---> foo2
                //    byte[] foo = ...  ---> foo3
                Type existingVarType = nameToType.get(name);
                if (requiresDeconfliction(varType, existingVarType)) {
                    // If we have an escaped name like "\\u0000" we cannot just append a number to it and call it a day.
                    // In these cases we will revert the name back to an auto-generated value based on its index.
                    if (escaped)
                        name = "v" + local.index();

                    int i = 2;
                    String prefix = name;
                    while (nameToType.get(name) != null)
                        name = prefix + (i++);
                }

                int start = local.start().getIndex();
                int end = local.end().getIndex();

                // Expand the range of the variable if necessary.
                // In many cases the range of the variable follows this example:
                //
                //  Variable[start=x, end=...]
                //   - astore foo
                //   - label x
                //   - aload foo
                //
                // Thus we will want to move the 'start' backwards to reach to the first write.
                int priorLabelOffset = findPriorLabelOffset(code, start);
                int variableWriteInRange = getVariableWriteInRange(code, index, priorLabelOffset, start);
                if (variableWriteInRange >= 0 && !isVariableInstructionInOtherScope(variableWriteInRange, code, local))
                    start = variableWriteInRange;

                locals.add(new Variables.Local(index, start, end, name, descriptor));
                nameToType.put(name, varType);
            }
        }
        NavigableMap<Integer, Variables.Parameter> parameterNames = new TreeMap<>();
        int offset = isStatic ? 0 : 1;
        if (!isStatic) {
            // TODO: May want to pass the declaring class's type so we don't just use object here
            parameterNames.put(0, new Variables.Parameter(0, "this", "Ljava/lang/Object;"));
        }
        List<ClassType> types = method.type().parameterTypes();
        for (int i = 0; i < types.size(); i++) {
            int varSlot = i + offset;
            String name = getName(locals, varSlot);
            ClassType type = types.get(i);
            parameterNames.put(varSlot, new Variables.Parameter(0, name, type.descriptor()));

            // Skip creating parameters for reserved slots
            if (Types.category(type) > 1)
                offset++;
        }
        return new Variables(parameterNames, locals);
    }

    /**
     * Used to check if an instruction <i>(xload/xstore)</i> is covered by a variable scope over than the given one.
     *
     * @param variableInstructionIndex
     *         Index of variable instruction in {@link Code#elements()}.
     * @param code
     *         Code to check for other variable scopes.
     * @param scope
     *         The current variable scope.
     *
     * @return {@code true} when the given instruction index is covered by a different {@link Local} scope other than the given one.
     * {@code false} when the given instruction index is not covered by a variable scope other than the given one.
     */
    private boolean isVariableInstructionInOtherScope(int variableInstructionIndex, @NotNull Code code, @NotNull Local scope) {
        for (Local local : code.localVariables()) {
            if (local == scope)
                continue;
            if (scope.index() == local.index()
                    && variableInstructionIndex >= local.start().getIndex()
                    && variableInstructionIndex <= local.end().getIndex())
                return true;
        }
        return false;
    }

    /**
     * @param code
     *         Code to check for other variable instructions in.
     * @param variableIndex
     *         Local variable index to filter variable instructions by.
     * @param start
     *         Start range in {@link Code#elements()}.
     * @param end
     *         End range in {@link Code#elements()}.
     *
     * @return Latest instruction index in {@link Code#elements()} of a variable instruction
     * writing to the given variable index within the given range. Otherwise, {@code -1}.
     */
    private static int getVariableWriteInRange(@NotNull Code code, int variableIndex, int start, int end) {
        List<CodeElement> elements = code.elements();
        for (int i = end; i >= start; i--)
            if (elements.get(i) instanceof VarInstruction varInsn
                    && varInsn.variableIndex() == variableIndex
                    && BlwOpcodes.isVarStore(varInsn.opcode()))
                return i;
        return -1;
    }

    /**
     * @param code
     *         Code to check for labels.
     * @param start
     *         Start index of search.
     *
     * @return Latest index of a label before the given start
     */
    private static int findPriorLabelOffset(@NotNull Code code, int start) {
        List<CodeElement> elements = code.elements();
        for (int i = start - 1; i >= 0; i--) {
            if (elements.get(i) instanceof Label)
                return i;
        }
        return 0;
    }

    @Override
    public void print(PrintContext<?> ctx) {
        memberPrinter.printAttributes(ctx);
        var obj = memberPrinter.printDeclaration(ctx).literal(method.name()).print(" ")
                .literal(method.type().descriptor()).print(" ").object();
        Variables variables = buildVariables(ctx);
        boolean hasPrior = !variables.parameters().isEmpty();
        if (hasPrior) {
            var arr = obj.value("parameters").array();
            arr.print(variables.parameters().values(), (arrayCtx, parameter) -> arr.print(parameter.name()));
            arr.end();
        }
        Element annotationDefault = method.annotationDefault();
        if (annotationDefault != null) {
            if (hasPrior) obj.next();
            obj.value("default-value");
            JvmAnnotationPrinter.forEmbeddedAnno(null).printElement(obj, annotationDefault);
            hasPrior = true;
        }
        var methodCode = method.code();
        if (methodCode != null) {
            // Ensure there are labels at the absolute start/end of the method so variable ranges won't be wonky.
            List<CodeElement> elements = methodCode.elements();
            if (!elements.isEmpty() && !(elements.getFirst() instanceof Label))
                elements.addFirst(new GenericLabel());
            if (!elements.isEmpty() && !(elements.getLast() instanceof Label))
                elements.add(new GenericLabel());

            // Separator between code and parameters element
            if (hasPrior) obj.next();

            // Populate label names
            Map<Integer, String> labelNames = getLabelNames(ctx, elements);

            // Print exception ranges
            if (!methodCode.tryCatchBlocks().isEmpty()) {
                var arr = obj.value("exceptions").array();
                arr.printIndented(methodCode.tryCatchBlocks(), (print, tcb) -> {
                    var exception = print.array();
                    String start = labelNames.get(tcb.start().getIndex());
                    String end = labelNames.get(tcb.end().getIndex());
                    String handler = labelNames.get(tcb.handler().getIndex());

                    String type = tcb.type() == null ? "*" : tcb.type().descriptor();

                    exception.print(start).arg()
                            .print(end).arg()
                            .print(handler).arg()
                            .literal(type);

                    exception.end();
                });

                arr.end();
                obj.next();
            }

            // Print instructions
            var code = obj.value("code").code();
            InstructionPrinter printer = new InstructionPrinter(code, methodCode, variables, labelNames);
            IndexedStraightforwardSimulation simulation = new IndexedStraightforwardSimulation();
            simulation.execute(printer, method.code());
            code.end();
        }

        obj.end();
    }

    @Override
    public @Nullable AnnotationPrinter annotation(int index) {
        return memberPrinter.printAnnotation(index);
    }

    @Override
    public @Nullable AnnotationPrinter visibleAnnotation(int index) {
        return memberPrinter.printVisibleAnnotation(index);
    }

    @Override
    public @Nullable AnnotationPrinter invisibleAnnotation(int index) {
        return memberPrinter.printInvisibleAnnotation(index);
    }

    private static @NotNull String getName(List<Variables.Local> locals, int i) {
        String name = null;

        // search for parameter name in local variables, first reference of the index which matches the type
        for (Variables.Local local : locals) {
            if (local.index() == i) {
                name = local.name();
                break;
            }
        }

        if (name == null)
            name = "p" + i;
        return name;
    }

    private static Map<Integer, String> getLabelNames(PrintContext<?> ctx, List<CodeElement> elements) {
        Map<Integer, String> labelNames = new HashMap<>();
        int labelIndex = 0;
        for (CodeElement element : elements) {
            if (element instanceof Label label) {
                String labelName = LabelUtil.getLabelName(labelIndex++);
                if (ctx.labelPrefix != null) labelName = ctx.labelPrefix + labelName;
                labelNames.put(label.getIndex(), labelName);
            }
        }
        return labelNames;
    }

    private static boolean requiresDeconfliction(@NotNull Type varType, @Nullable Type existingVarType) {
        // If there is no existing type, no conflicts are possible.
        if (existingVarType == null)
            return false;

        // If the var types are different (primitive vs class type) then we MUST deconflict.
        if (varType.getClass() != existingVarType.getClass())
            return true;

        // If the class types do not match we MUST deconflict.
        if (varType instanceof ClassType varClass
                && existingVarType instanceof ClassType existingClass
                && !varClass.descriptor().equals(existingClass.descriptor()))
            return true;

        // If the primitive types do not match we MUST deconflict.
        return varType instanceof PrimitiveType varPrim
                && existingVarType instanceof PrimitiveType existingPrim
                && varPrim.kind() != existingPrim.kind();
    }

    private static @NotNull String escapeVariableName(@NotNull String name, int index, boolean isStatic) {
        // No fake 'this' name
		if (name.equals("this") && !(index == 0 && !isStatic))
            return "v" + index;

		// No bs long names
		if (name.length() > 200)
            return "v" + index;

		// No bs descriptor chars in names
	    if (name.indexOf('/') >= 0 || name.indexOf('.') >= 0 || name.indexOf(';') >= 0 || name.indexOf('[') >= 0)
			return "v" + index;

		// Standard escaping
        return EscapeUtil.escapeLiteral(name);
    }
}