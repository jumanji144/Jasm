package me.darknet.assembler.printer;

import dev.xdark.blw.annotation.Annotation;
import dev.xdark.blw.annotation.Element;
import dev.xdark.blw.classfile.AccessFlag;
import dev.xdark.blw.classfile.Method;
import dev.xdark.blw.classfile.attribute.Parameter;
import dev.xdark.blw.code.Code;
import dev.xdark.blw.code.CodeElement;
import dev.xdark.blw.code.Label;
import dev.xdark.blw.code.attribute.Local;
import dev.xdark.blw.code.generic.GenericLabel;
import dev.xdark.blw.code.instruction.VarInstruction;
import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.Type;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.compile.analysis.jvm.IndexedStraightforwardSimulation;
import me.darknet.assembler.helper.Variables;
import me.darknet.assembler.util.BlwOpcodes;
import me.darknet.assembler.util.EscapeUtil;
import me.darknet.assembler.util.LabelUtil;
import me.darknet.assembler.util.VarNaming;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

public class JvmMethodPrinter implements MethodPrinter {

    protected final Method method;
    protected final JvmMemberPrinter memberPrinter;

    public JvmMethodPrinter(Method method) {
        this.method = method;
        this.memberPrinter = new JvmMemberPrinter(method, JvmMemberPrinter.Type.METHOD);
    }

    public @NotNull Variables buildVariables(PrintContext<?> ctx) {
        boolean isStatic = (method.accessFlags() & AccessFlag.ACC_STATIC) != 0;
        Code code = method.code();
        List<LocalInfo> localInfos = code == null || ctx.ignoreExistingVariableNames
                ? List.of()
                : collectLocalInfos(code, isStatic);
        NavigableMap<Integer, Variables.Parameter> parameterNames = new TreeMap<>();
        Set<String> usedNames = new HashSet<>();
        if (!isStatic) {
            parameterNames.put(0, new Variables.Parameter(0, "this", "Ljava/lang/Object;"));
            usedNames.add("this");
        }

        Map<String, String> parameterLocalNames = new HashMap<>();
        List<Parameter> methodParameters = method.parameters();
        int parameterOffset = isStatic ? 0 : 1;
        List<ClassType> parameterTypes = method.type().parameterTypes();
        int methodParameterIndexOffset = Math.max(0, methodParameters.size() - parameterTypes.size());
        for (int i = 0; i < parameterTypes.size(); i++) {
            int varSlot = i + parameterOffset;
            ClassType type = parameterTypes.get(i);
            String baseName = getParameterName(localInfos, methodParameters, i + methodParameterIndexOffset, varSlot, type, isStatic);
            String name = allocateUniqueName(baseName, varSlot, type, false, usedNames);
            parameterNames.put(varSlot, new Variables.Parameter(varSlot, name, type.descriptor()));
            parameterLocalNames.put(parameterLocalNameKey(varSlot, type.descriptor()), name);

            // Skip creating parameters for reserved slots
            if (Types.category(type) > 1)
                parameterOffset++;
        }

        List<Variables.Local> locals = new ArrayList<>();
        for (LocalInfo local : localInfos) {
            String name;
            if (!isStatic && local.index() == 0) {
                name = "this";
            } else {
                String parameterName = parameterLocalNames.get(parameterLocalNameKey(local.index(), local.descriptor()));
	            name = Objects.requireNonNullElseGet(parameterName, () -> allocateUniqueName(local, usedNames));
            }
            locals.add(new Variables.Local(local.index(), local.start(), local.end(), name, local.descriptor()));
        }

        return new Variables(parameterNames, locals);
    }

    /**
     * Used to check if an instruction <i>(xload/xstore)</i> is covered by a variable scope over than the given one.
     *
     * @param variableInstructionIndex
     *         Index of variable instruction in {@link Code#elements()}.
     * @param locals
     *         List of variables and their occupied ranges/scopes in the method.
     * @param scope
     *         The current variable scope.
     *
     * @return {@code true} when the given instruction index is covered by a different {@link Local} scope other than the given one.
     * {@code false} when the given instruction index is not covered by a variable scope other than the given one.
     */
    private static boolean isVariableInstructionInOtherScope(int variableInstructionIndex, @NotNull List<LocalInfo> locals, @NotNull LocalInfo scope) {
        for (LocalInfo local : locals) {
            if (local == scope)
                continue;
            if (scope.index() == local.index()
                    && variableInstructionIndex >= local.start()
                    && variableInstructionIndex <= local.end())
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
        if (elements.isEmpty())
            return -1;
        start = Math.max(0, Math.min(start, elements.size() - 1));
        end = Math.max(0, Math.min(end, elements.size() - 1));
        if (start > end)
            return -1;
        for (int i = end; i >= start; i--)
            if (elements.get(i) instanceof VarInstruction(int opcode, int index)
                    && index == variableIndex
                    && BlwOpcodes.isVarStore(opcode))
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
            List<Variables.Parameter> parameterList = new ArrayList<>(variables.parameters().values());
            arr.print(parameterList, (arrayCtx, parameter) -> arr.print(parameter.name()));
            arr.end();

            Map<Integer, List<Annotation>> visParamAnnos = method.visibleRuntimeParameterAnnotations();
            Map<Integer, List<Annotation>> invisParamAnnos = method.invisibleRuntimeParameterAnnotations();
            if (!visParamAnnos.isEmpty() || !invisParamAnnos.isEmpty()) {
                Map<Integer, List<ParameterAnnotationEntry>> mergedParamAnnos = new TreeMap<>();
                visParamAnnos.forEach((k, v) -> mergedParamAnnos.merge(k, ParameterAnnotationEntry.visible(v), (a, b) -> {
                    a.addAll(b);
                    return a;
                }));
                invisParamAnnos.forEach((k, v) -> mergedParamAnnos.merge(k, ParameterAnnotationEntry.invisible(v), (a, b) -> {
                    a.addAll(b);
                    return a;
                }));

                obj.next();
                boolean isVirtual = !parameterList.isEmpty() && parameterList.getFirst().name().equals("this");
                var pannos = obj.value("parameter-annotations").object();
                var pannosIt = mergedParamAnnos.entrySet().iterator();
                while (pannosIt.hasNext()) {
                    var entry = pannosIt.next();
                    int sourceIndex = entry.getKey();
                    int printedIndex = sourceIndex + (isVirtual ? 1 : 0);
                    if (printedIndex < 0 || printedIndex >= parameterList.size())
                        continue;
                    List<ParameterAnnotationEntry> annos = entry.getValue();

                    var parameter = parameterList.get(printedIndex);
                    String parameterName = parameter.name();
                    var parr = pannos.value(parameterName).array();

                    Iterator<ParameterAnnotationEntry> annosIt = annos.iterator();
                    while (annosIt.hasNext()) {
                        ParameterAnnotationEntry anno = annosIt.next();
                        new JvmAnnotationPrinter(anno.annotation(), anno.visible()).print(parr);
                        if (annosIt.hasNext()) {
                            parr.append(",");
                            pannos.newline();
                        }
                    }

                    parr.end();
                    if (pannosIt.hasNext()) {
                        pannos.append(",");
                        ctx.line();
                    }
                }
                pannos.end();
            }
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
            JvmInstructionPrinter printer = new JvmInstructionPrinter(code, methodCode, variables, labelNames);
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

    private @NotNull List<LocalInfo> collectLocalInfos(@NotNull Code code, boolean isStatic) {
        List<LocalInfo> locals = new ArrayList<>();
        for (Local local : code.localVariables()) {
            LocalRange range = sanitizeLocalRange(code, local);
            if (range == null)
                continue;

            int index = local.index();
            boolean isThis = !isStatic && index == 0;
            String descriptor = local.type().descriptor();
            Type varType = Types.typeFromDescriptor(descriptor);
            ClassType varClassType = varType instanceof ClassType ct ? ct : Types.VOID;
            String originalName = isThis ? "this" : local.name();
            String baseName = isThis ? "this" : escapeVariableName(originalName, varClassType, index, isStatic);
            boolean escaped = !isThis && !originalName.equals(baseName);
            locals.add(new LocalInfo(index, range.start(), range.end(), baseName, descriptor, varClassType, escaped));
        }

        for (int i = 0; i < locals.size(); i++) {
            LocalInfo local = locals.get(i);
            if (local.end() <= local.start())
                continue;
            int priorLabelOffset = findPriorLabelOffset(code, local.start());
            int variableWriteInRange = getVariableWriteInRange(code, local.index(), priorLabelOffset, local.start());
            if (variableWriteInRange >= 0 && !isVariableInstructionInOtherScope(variableWriteInRange, locals, local))
                locals.set(i, local.withStart(variableWriteInRange));
        }

        return locals.stream()
                .filter(local -> !isLoadOnlyAlias(code, locals, local))
                .toList();
    }

    private static @Nullable LocalRange sanitizeLocalRange(@NotNull Code code, @NotNull Local local) {
        List<CodeElement> elements = code.elements();
        if (elements.isEmpty())
            return null;

		// Must be within the bounds of the code elements list.
        int maxIndex = elements.size() - 1;
        int start = local.start().getIndex();
        int end = local.end().getIndex();
        if (start > maxIndex && end > maxIndex)
            return null;
        if (start < 0 && end < 0)
            return null;

		// Clamp to valid range and ensure start is not after end.
        start = Math.max(0, Math.min(start, maxIndex));
        end = Math.max(0, Math.min(end, maxIndex));
        if (start > end)
            return null;

        return new LocalRange(start, end);
    }

    private static boolean isLoadOnlyAlias(@NotNull Code code, @NotNull List<LocalInfo> locals, @NotNull LocalInfo local) {
        int firstAccess = findFirstVariableAccessInRange(code, local.index(), local.start(), local.end());
        if (firstAccess < 0)
            return true;

        List<CodeElement> elements = code.elements();
        if (elements.get(firstAccess) instanceof VarInstruction varInsn && BlwOpcodes.isVarStore(varInsn.opcode()))
            return false;

        return locals.stream()
                .anyMatch(other -> other != local
                        && other.index() == local.index()
                        && other.start() < local.start());
    }

    private static int findFirstVariableAccessInRange(@NotNull Code code, int variableIndex, int start, int end) {
        List<CodeElement> elements = code.elements();
        if (elements.isEmpty())
            return -1;
        start = Math.max(0, Math.min(start, elements.size() - 1));
        end = Math.max(0, Math.min(end, elements.size() - 1));
        if (start > end)
            return -1;
        for (int i = start; i <= end; i++) {
            if (elements.get(i) instanceof VarInstruction varInsn && varInsn.variableIndex() == variableIndex)
                return i;
        }
        return -1;
    }

    private static boolean matchesSlotAndDesc(@NotNull LocalInfo local, int index, @NotNull String descriptor) {
        return local.index() == index && local.descriptor().equals(descriptor);
    }

    private static @NotNull String parameterLocalNameKey(int index, @NotNull String descriptor) {
        return index + ":" + descriptor;
    }

    private static @NotNull String getParameterName(@NotNull List<LocalInfo> locals, @NotNull List<Parameter> methodParameters,
            int methodParameterIndex, int slot, @NotNull ClassType type, boolean isStatic) {
        String name = findOriginalParameterName(locals, methodParameters, methodParameterIndex, slot, type, isStatic);
        if (name == null)
            return VarNaming.name(slot, type);
        return name;
    }

    private static @Nullable String findOriginalParameterName(@NotNull List<LocalInfo> locals, @NotNull List<Parameter> methodParameters,
            int methodParameterIndex, int slot, @NotNull ClassType type, boolean isStatic) {
		// Check if there's a method parameter with a valid name that matches the slot and type.
        if (methodParameterIndex < methodParameters.size()) {
            String methodParameterName = methodParameters.get(methodParameterIndex).name();
            if (methodParameterName != null)
                return escapeVariableName(methodParameterName, type, slot, isStatic);
        }

		// If not, try to find a local variable that matches the slot and type,
	    // preferring ones that start at the beginning of the method.
        return locals.stream()
                .filter(local -> matchesSlotAndDesc(local, slot, type.descriptor()))
                .min(Comparator.comparingInt((LocalInfo local) -> local.start() == 0 ? 0 : 1)
                        .thenComparingInt(LocalInfo::start))
                .map(LocalInfo::baseName)
                .orElse(null);
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

    private static @NotNull String allocateUniqueName(@NotNull LocalInfo local, @NotNull Set<String> usedNames) {
        return allocateUniqueName(local.baseName(), local.index(), local.classType(), local.escaped(), usedNames);
    }

    private static @NotNull String allocateUniqueName(@NotNull String baseName, int index, @NotNull ClassType type, boolean escaped, @NotNull Set<String> usedNames) {
        if (!usedNames.contains(baseName)) {
            usedNames.add(baseName);
            return baseName;
        }

        String prefix = escaped ? VarNaming.name(index, type) : baseName;
        String candidate = prefix;
        int suffix = 2;
        while (usedNames.contains(candidate))
            candidate = prefix + suffix++;
        usedNames.add(candidate);
        return candidate;
    }

    private static @NotNull String escapeVariableName(@NotNull String name, @NotNull ClassType type, int index, boolean isStatic) {
        // No fake 'this' name
        if (name.equals("this") && !(index == 0 && !isStatic))
            return VarNaming.name(index, type);

        // No bs empty names
        if (name.isBlank())
            return VarNaming.name(index, type);

        // No bs long names
        if (name.length() > 200)
            return VarNaming.name(index, type);

        // No bs descriptor chars in names
        if (name.indexOf('/') >= 0 || name.indexOf('.') >= 0 || name.indexOf(';') >= 0 || name.indexOf('[') >= 0
                || name.indexOf('<') >= 0 || name.indexOf('>') >= 0|| name.indexOf('-') >= 0)
            return VarNaming.name(index, type);

		// Standard escaping
        return EscapeUtil.escapeLiteral(name);
    }

    private record LocalInfo(int index, int start, int end, @NotNull String baseName, @NotNull String descriptor,
            @NotNull ClassType classType, boolean escaped) {
        private @NotNull LocalInfo withStart(int newStart) {
            return new LocalInfo(index, newStart, end, baseName, descriptor, classType, escaped);
        }
    }

    private record LocalRange(int start, int end) {}

    private record ParameterAnnotationEntry(@NotNull Annotation annotation, boolean visible) {
        private static @NotNull List<ParameterAnnotationEntry> visible(@NotNull List<Annotation> annotations) {
            return annotations.stream().map(annotation -> new ParameterAnnotationEntry(annotation, true)).toList();
        }

        private static @NotNull List<ParameterAnnotationEntry> invisible(@NotNull List<Annotation> annotations) {
            return annotations.stream().map(annotation -> new ParameterAnnotationEntry(annotation, false)).toList();
        }
    }
}
