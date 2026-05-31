package me.darknet.assembler.printer;

import me.darknet.assembler.helper.Variables;
import me.darknet.assembler.util.EscapeUtil;
import me.darknet.assembler.util.LabelUtil;
import me.darknet.assembler.util.VarNaming;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;

public class JvmMethodPrinter implements MethodPrinter {
    protected final MethodNode method;
    protected final MethodNode printableMethod;
    protected final JvmMemberPrinter memberPrinter;

    public JvmMethodPrinter(MethodNode method) {
        this.method = method;
        this.printableMethod = normalizeMethodBoundaries(method);
        this.memberPrinter = new JvmMemberPrinter(method, JvmMemberPrinter.Type.METHOD);
    }

    public @NotNull Variables buildVariables(PrintContext<?> ctx) {
		// Collect local variable information and parameter names, and allocate unique names for all variables.
        boolean isStatic = (printableMethod.access & Opcodes.ACC_STATIC) != 0;
        List<LocalInfo> localInfos = printableMethod.localVariables == null || ctx.ignoreExistingVariableNames
                ? List.of()
                : collectLocalInfos(printableMethod, isStatic);

		// Tracking for used names.
        NavigableMap<Integer, Variables.Parameter> parameterNames = new TreeMap<>();
        Set<String> usedNames = new HashSet<>();

		// Start with implicit 'this' parameter for instance methods.
        if (!isStatic) {
            parameterNames.put(0, new Variables.Parameter(0, "this", "Ljava/lang/Object;"));
            usedNames.add("this");
        }

		// Allocate names for method parameters, using local variable information and method parameter metadata
	    // when available to preserve original names where possible.
        Map<String, String> parameterLocalNames = new HashMap<>();
        List<ParameterNode> methodParameters = printableMethod.parameters == null ? List.of() : printableMethod.parameters;
        Type[] parameterTypes = Type.getArgumentTypes(printableMethod.desc);
        int methodParameterIndexOffset = Math.max(0, methodParameters.size() - parameterTypes.length);
        int slot = isStatic ? 0 : 1;
        for (int i = 0; i < parameterTypes.length; i++) {
            Type type = parameterTypes[i];
            String baseName = getParameterName(localInfos, methodParameters, i + methodParameterIndexOffset, slot, type, isStatic);
            String name = allocateUniqueName(baseName, slot, type, false, usedNames);
            parameterNames.put(slot, new Variables.Parameter(slot, name, type.getDescriptor()));
            parameterLocalNames.put(parameterLocalNameKey(slot, type.getDescriptor()), name);
            slot += type.getSize();
        }

		// Convert our local info models to the final variable models.
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

    @Override
    public void print(PrintContext<?> ctx) {
        memberPrinter.printAttributes(ctx);
        var obj = memberPrinter.printDeclaration(ctx).literal(method.name).print(" ")
                .literal(method.desc).print(" ").object();
        Variables variables = buildVariables(ctx);
        boolean hasPrior = !variables.parameters().isEmpty();
        if (hasPrior) {
            var arr = obj.value("parameters").array();
            List<Variables.Parameter> parameterList = new ArrayList<>(variables.parameters().values());
            arr.print(parameterList, (arrayCtx, parameter) -> arr.print(parameter.name()));
            arr.end();

            Map<Integer, List<ParameterAnnotationEntry>> mergedParamAnnos = mergeParameterAnnotations();
            if (!mergedParamAnnos.isEmpty()) {
                obj.next();
                boolean isVirtual = !parameterList.isEmpty() && parameterList.getFirst().name().equals("this");
                var pannos = obj.value("parameter-annotations").object();
                Iterator<Map.Entry<Integer, List<ParameterAnnotationEntry>>> pannosIt = mergedParamAnnos.entrySet().iterator();
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
                        JvmAnnotationPrinter.forTopLevelAnno(anno.annotation(), anno.visible()).print(parr);
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

        if (method.annotationDefault != null) {
            if (hasPrior)
                obj.next();

            obj.value("default-value");
            JvmAnnotationPrinter.forElements().printElement(obj, method.annotationDefault);
            hasPrior = true;
        }

        if (method.exceptions != null && !method.exceptions.isEmpty()) {
            if (hasPrior)
                obj.next();

            var arr = obj.value("throws").array();
            arr.print(method.exceptions, (arrayCtx, exceptionType) -> arr.literal(exceptionType));
            arr.end();
            hasPrior = true;
        }

        if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0) {
            if (hasPrior)
                obj.next();

            // Print exception ranges
            Map<LabelNode, String> labelNames = getLabelNames(ctx, printableMethod);
            if (!printableMethod.tryCatchBlocks.isEmpty()) {
                var arr = obj.value("exceptions").array();
                arr.printIndented(printableMethod.tryCatchBlocks, (print, tcb) -> {
                    var exception = print.array();
                    String start = labelNames.get(tcb.start);
                    String end = labelNames.get(tcb.end);
                    String handler = labelNames.get(tcb.handler);
                    String type = tcb.type == null ? "*" : Type.getObjectType(tcb.type).getDescriptor();
                    exception.print(start).arg().print(end).arg().print(handler).arg().literal(type).end();
                });
                arr.end();
                obj.next();
            }

            // Print instructions
			if (printableMethod.instructions != null) {
				var code = obj.value("code").code();
				new JvmInstructionPrinter(code, printableMethod.tryCatchBlocks, variables, labelNames).print(printableMethod.instructions);
				code.end();
			}
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

    private @NotNull Map<Integer, List<ParameterAnnotationEntry>> mergeParameterAnnotations() {
        Map<Integer, List<ParameterAnnotationEntry>> merged = new TreeMap<>();
        mergeParameterAnnotations(merged, method.visibleParameterAnnotations, true);
        mergeParameterAnnotations(merged, method.invisibleParameterAnnotations, false);
        return merged;
    }

    private static void mergeParameterAnnotations(@NotNull Map<Integer, List<ParameterAnnotationEntry>> merged,
                                                  @Nullable List<AnnotationNode>[] annotations, boolean visible) {
        if (annotations == null) {
            return;
        }
        for (int i = 0; i < annotations.length; i++) {
            List<AnnotationNode> entries = annotations[i];
            if (entries == null || entries.isEmpty()) {
                continue;
            }
            List<ParameterAnnotationEntry> target = merged.computeIfAbsent(i, ignored -> new ArrayList<>());
            for (AnnotationNode entry : entries) {
                target.add(new ParameterAnnotationEntry(entry, visible));
            }
        }
    }

	/**
	 * Collects local variable information/scopes in the method,
	 * and attempts to adjust variable scopes to more accurately reflect actual variable lifetimes.
	 *
	 * @param isStatic
	 *        {@code true} when the method is static, {@code false} otherwise.
	 *
	 * @return List of local variable information with adjusted scopes.
	 */
    private @NotNull List<LocalInfo> collectLocalInfos(@NotNull MethodNode methodView, boolean isStatic) {
        List<LocalInfo> locals = new ArrayList<>();
        for (LocalVariableNode local : methodView.localVariables) {
            LocalRange range = sanitizeLocalRange(methodView.instructions, local);
            if (range == null)
                continue;

            int index = local.index;
            boolean isThis = !isStatic && index == 0;
            String descriptor = local.desc;
            Type type = Type.getType(descriptor);
            String originalName = isThis ? "this" : local.name;
            String baseName = isThis ? "this" : escapeVariableName(originalName, type, index, isStatic);
            boolean escaped = !isThis && !originalName.equals(baseName);
            locals.add(new LocalInfo(index, range.start(), range.end(), baseName, descriptor, type, escaped));
        }

        for (int i = 0; i < locals.size(); i++) {
            LocalInfo local = locals.get(i);
            if (local.end() <= local.start())
                continue;

            int priorLabelOffset = findPriorLabelOffset(methodView.instructions, local.start());
            int variableWriteInRange = getVariableWriteInRange(methodView.instructions, local.index(), priorLabelOffset, local.start());
            if (variableWriteInRange >= 0 && !isVariableInstructionInOtherScope(variableWriteInRange, locals, local))
                locals.set(i, local.withStart(variableWriteInRange));
        }

        return locals.stream().filter(local -> !isLoadOnlyAlias(methodView.instructions, locals, local)).toList();
    }

	/**
	 * Used to check if an instruction <i>(xload/xstore)</i> is covered by a variable scope over than the given one.
	 *
	 * @param variableInstructionIndex
	 * 		Index of variable instruction in {@link InsnList}.
	 * @param locals
	 * 		List of variable scopes to check against.
	 * @param scope
	 * 		The current variable scope.
	 *
	 * @return {@code true} when the given instruction index is covered by
	 * a different {@link LocalInfo} scope other than the given one.
	 * {@code false} when the given instruction index is not covered
	 * by a variable scope other than the given one.
	 */
    private static boolean isVariableInstructionInOtherScope(int variableInstructionIndex,
                                                             @NotNull List<LocalInfo> locals,
                                                             @NotNull LocalInfo scope) {
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
	 * 		Code to check for other variable instructions in.
	 * @param variableIndex
	 * 		Local variable index to filter variable instructions by.
	 * @param start
	 * 		Start range in {@link InsnList}
	 * @param end
	 * 		End range in {@link InsnList}.
	 *
	 * @return Latest instruction index in {@link InsnList} of a variable instruction
	 * writing to the given variable index within the given range. Otherwise, {@code -1}.
	 */
    private static int getVariableWriteInRange(@NotNull InsnList code, int variableIndex, int start, int end) {
        if (code.size() == 0)
            return -1;

        start = Math.max(0, Math.min(start, code.size() - 1));
        end = Math.max(0, Math.min(end, code.size() - 1));
        if (start > end)
            return -1;

        for (int i = end; i >= start; i--) {
            AbstractInsnNode instruction = code.get(i);
            if (instruction instanceof VarInsnNode varInsn
                    && varInsn.var == variableIndex
                    && isVarStore(varInsn.getOpcode())) {
                return i;
            }
        }

        return -1;
    }

    private static boolean isVarStore(int opcode) {
        return opcode == Opcodes.ISTORE || opcode == Opcodes.LSTORE || opcode == Opcodes.FSTORE
                || opcode == Opcodes.DSTORE || opcode == Opcodes.ASTORE;
    }

    private static int findPriorLabelOffset(@NotNull InsnList code, int start) {
        for (int i = start - 1; i >= 0; i--) {
            if (code.get(i) instanceof LabelNode) {
                return i;
            }
        }
        return 0;
    }

    private static @Nullable LocalRange sanitizeLocalRange(@NotNull InsnList code, @NotNull LocalVariableNode local) {
        if (code.size() == 0)
            return null;

        int maxIndex = code.size() - 1;
        int start = code.indexOf(local.start);
        int end = code.indexOf(local.end);
        if (start < 0 && end < 0)
            return null;

        if (start > maxIndex && end > maxIndex)
            return null;

        start = Math.max(0, Math.min(start, maxIndex));
        end = Math.max(0, Math.min(end, maxIndex));
        if (start > end)
            return null;

        return new LocalRange(start, end);
    }

    private static boolean isLoadOnlyAlias(@NotNull InsnList code, @NotNull List<LocalInfo> locals, @NotNull LocalInfo local) {
        int firstAccess = findFirstVariableAccessInRange(code, local.index(), local.start(), local.end());
        if (firstAccess < 0)
            return true;

        AbstractInsnNode instruction = code.get(firstAccess);
        if (instruction instanceof VarInsnNode varInsn && isVarStore(varInsn.getOpcode()))
            return false;

        return locals.stream()
		        .anyMatch(other -> other != local
				        && other.index() == local.index()
				        && other.start() < local.start());
    }

    private static int findFirstVariableAccessInRange(@NotNull InsnList code, int variableIndex, int start, int end) {
        if (code.size() == 0)
            return -1;

        start = Math.max(0, Math.min(start, code.size() - 1));
        end = Math.max(0, Math.min(end, code.size() - 1));
        if (start > end)
            return -1;

        for (int i = start; i <= end; i++) {
            AbstractInsnNode instruction = code.get(i);
            if (instruction instanceof VarInsnNode varInsn && varInsn.var == variableIndex)
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

    private static @NotNull String getParameterName(@NotNull List<LocalInfo> locals,
                                                    @NotNull List<ParameterNode> methodParameters,
                                                    int methodParameterIndex,
                                                    int slot,
                                                    @NotNull Type type,
                                                    boolean isStatic) {
        String name = findOriginalParameterName(locals, methodParameters, methodParameterIndex, slot, type, isStatic);
        if (name == null)
            return VarNaming.name(slot, type);
        return name;
    }

    private static @Nullable String findOriginalParameterName(@NotNull List<LocalInfo> locals,
                                                              @NotNull List<ParameterNode> methodParameters,
                                                              int methodParameterIndex,
                                                              int slot,
                                                              @NotNull Type type,
                                                              boolean isStatic) {
        if (methodParameterIndex < methodParameters.size()) {
            String methodParameterName = methodParameters.get(methodParameterIndex).name;
            if (methodParameterName != null) {
                return escapeVariableName(methodParameterName, type, slot, isStatic);
            }
        }

        return locals.stream()
                .filter(local -> matchesSlotAndDesc(local, slot, type.getDescriptor()))
                .min(Comparator.comparingInt((LocalInfo local) -> local.start() == 0 ? 0 : 1)
		                .thenComparingInt(LocalInfo::start))
                .map(LocalInfo::baseName)
                .orElse(null);
    }

    private static Map<LabelNode, String> getLabelNames(PrintContext<?> ctx, @NotNull MethodNode method) {
        InsnList instructions = method.instructions;
        Map<LabelNode, String> labelNames = new IdentityHashMap<>();
        int labelIndex = 0;
        for (int i = 0; i < instructions.size(); i++) {
            AbstractInsnNode instruction = instructions.get(i);
            if (instruction instanceof LabelNode label) {
                String labelName = LabelUtil.getLabelName(labelIndex++);
                if (ctx.labelPrefix != null)
                    labelName = ctx.labelPrefix + labelName;
                labelNames.put(label, labelName);
            }
        }
        return labelNames;
    }

	/**
	 * Ensures that the method's instruction list starts and ends with a label,
	 * which is required for correct variable scope analysis.
	 * <p>
	 * <b>Example 1</b>: Some variable gets used before a label starts,
	 * so you cannot give it a correct scope that actually includes the usage
	 * since the earliest 'start' label is after the usage.
	 * <pre>{@code
	 *  ldc "foo"
	 *  astore x // Written to before any label
	 * A:
	 *  aload x
	 *  areturn
	 * B:
	 * }</pre>
	 * <b>Example 2</b>: Some variable gets used but there's no label after that usage,
	 * so you cannot give it a correct scope that actually includes the usage
	 * since the furthest 'end' label is before the usage.
	 * <pre>{@code
	 * A:
	 *  aload x
	 *  return
	 *  // No end label
	 * }</pre>
	 *
	 * @param method
	 * 		Method to normalize.
	 *
	 * @return Normalized method.
	 */
    private static @NotNull MethodNode normalizeMethodBoundaries(@NotNull MethodNode method) {
        if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0)
            return method;

        boolean missingLeadingLabel = !(method.instructions.getFirst() instanceof LabelNode);
        boolean missingTrailingLabel = !(method.instructions.getLast() instanceof LabelNode);
        if (!missingLeadingLabel && !missingTrailingLabel)
            return method;

        MethodNode normalized = copyMethod(method);
        if (missingLeadingLabel) {
            if (normalized.instructions.getFirst() == null)
                normalized.instructions.add(new LabelNode());
            else
                normalized.instructions.insert(new LabelNode());
        }
        if (missingTrailingLabel)
            normalized.instructions.add(new LabelNode());

        return normalized;
    }

    private static @NotNull MethodNode copyMethod(@NotNull MethodNode method) {
        MethodNode copy = new MethodNode(
                method.access,
                method.name,
                method.desc,
                method.signature,
                method.exceptions == null ? null : method.exceptions.toArray(String[]::new)
        );
        method.accept(copy);
        return copy;
    }

    private static @NotNull String allocateUniqueName(@NotNull LocalInfo local, @NotNull Set<String> usedNames) {
        return allocateUniqueName(local.baseName(), local.index(), local.type(), local.escaped(), usedNames);
    }

    private static @NotNull String allocateUniqueName(@NotNull String baseName, int index, @NotNull Type type,
                                                      boolean escaped, @NotNull Set<String> usedNames) {
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

    private static @NotNull String escapeVariableName(@NotNull String name, @NotNull Type type, int index, boolean isStatic) {
        if (name.equals("this") && !(index == 0 && !isStatic))
            return VarNaming.name(index, type);

        if (name.isBlank())
            return VarNaming.name(index, type);

        if (name.length() > 200)
            return VarNaming.name(index, type);

        if (name.indexOf('/') >= 0
		        || name.indexOf('.') >= 0
		        || name.indexOf(';') >= 0
		        || name.indexOf('[') >= 0
                || name.indexOf('<') >= 0
		        || name.indexOf('>') >= 0
		        || name.indexOf('-') >= 0)
            return VarNaming.name(index, type);

        return EscapeUtil.escapeLiteral(name);
    }

    private record LocalInfo(int index, int start, int end, @NotNull String baseName, @NotNull String descriptor,
                             @NotNull Type type, boolean escaped) {
        private @NotNull LocalInfo withStart(int newStart) {
            return new LocalInfo(index, newStart, end, baseName, descriptor, type, escaped);
        }
    }

    private record LocalRange(int start, int end) {}

    private record ParameterAnnotationEntry(@NotNull AnnotationNode annotation, boolean visible) {}
}
