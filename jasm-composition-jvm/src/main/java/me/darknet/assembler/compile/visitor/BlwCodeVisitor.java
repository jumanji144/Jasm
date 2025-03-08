package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.*;
import me.darknet.assembler.compile.JvmCompilerOptions;
import me.darknet.assembler.compile.analysis.*;
import me.darknet.assembler.compile.analysis.frame.Frame;
import me.darknet.assembler.compile.analysis.jvm.AnalysisSimulation;
import me.darknet.assembler.compile.analysis.jvm.IndexedStraightforwardSimulation;
import me.darknet.assembler.compile.analysis.jvm.JvmAnalysisEngine;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.helper.Handle;
import me.darknet.assembler.util.BlwOpcodes;
import me.darknet.assembler.util.ConstantMapper;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.visitor.ASTJvmInstructionVisitor;

import dev.xdark.blw.code.*;
import dev.xdark.blw.code.attribute.generic.GenericLocal;
import dev.xdark.blw.code.generic.GenericLabel;
import dev.xdark.blw.code.instruction.*;
import dev.xdark.blw.constant.OfDouble;
import dev.xdark.blw.constant.OfFloat;
import dev.xdark.blw.constant.OfInt;
import dev.xdark.blw.constant.OfLong;
import dev.xdark.blw.type.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BlwCodeVisitor implements ASTJvmInstructionVisitor, JavaOpcodes {
    private final CodeBuilder<?> codeBuilder;
    private final CodeListBuilder codeBuilderList;
    private final InheritanceChecker checker;
    private final ErrorCollector errorCollector;
    private final Map<String, GenericLabel> nameToLabel = new HashMap<>();
    private final List<Local> parameters;
    private final VarCache varCache = new VarCache();
    private final List<ASTInstruction> visitedInstructions = new ArrayList<>();
    private final JvmAnalysisEngine<Frame> analysisEngine;
    private final boolean writeVariables;
    private ASTInstruction currentInstructionAst;
    private int opcode = 0;

    /**
     * @param options
     *                   Compiler option to pull values from.
     * @param errorCollector
     *                   Collector for error reporting.
     * @param builder
     *                   Builder to insert code into.
     * @param parameters
     *                   Parameter variables.
     */
    @SuppressWarnings("unchecked")
    public BlwCodeVisitor(JvmCompilerOptions options, ErrorCollector errorCollector, CodeBuilder<?> builder, List<Local> parameters) {
        this.codeBuilder = builder;
        this.codeBuilderList = builder.codeList().child();
        this.checker = options.inheritanceChecker();
        this.errorCollector = errorCollector;
        this.analysisEngine = (JvmAnalysisEngine<Frame>) options.createEngine(varCache);
        this.parameters = parameters;
        this.writeVariables = options.doWriteVariables();

        analysisEngine.setErrorCollector(errorCollector);

        // Populate variables from params.
        parameters.stream().filter(Objects::nonNull).forEach(param -> varCache.getOrCreate(param.name(), param.size() > 1));
    }

    /**
     * @return Analysis of the method code.
     */
    @NotNull
    public AnalysisResults getAnalysisResults() {
        return analysisEngine;
    }

    /**
     * @param name
     *             Name of label.
     *
     * @return Label reference.
     */
    private Label getOrCreateLabel(String name) {
        return nameToLabel.computeIfAbsent(name, n -> new GenericLabel());
    }

    @Override
    public void visitInstruction(@NotNull ASTInstruction instruction) {
        currentInstructionAst = instruction;
        if (instruction instanceof ASTLabel)
            return;
        opcode = BlwOpcodes.opcode(instruction.identifier().content());
        visitedInstructions.add(instruction);
    }

    @Override
    public void visitException(@NotNull ASTIdentifier start, @NotNull ASTIdentifier end, @NotNull ASTIdentifier handler, @NotNull ASTIdentifier type) {
        GenericLabel startLabel = nameToLabel.get(start.content()); // assume that labels don't have escapable characters
        GenericLabel endLabel = nameToLabel.get(end.content());
        GenericLabel handlerLabel = nameToLabel.get(handler.content());

        String typeName = type.literal();
        InstanceType exceptionType = typeName.equals("*") ? null : Types.instanceTypeFromDescriptor(typeName);
        codeBuilder.tryCatchBlock(new TryCatchBlock(startLabel, endLabel, handlerLabel, exceptionType));
    }

    @Override
    public void visitInsn() {
        String content = currentInstructionAst.identifier().content();
        Instruction instruction = switch (content) {
            case "iconst_m1" -> new ConstantInstruction.Int(new OfInt(-1));
            case "iconst_0", "iconst_1", "iconst_2", "iconst_3", "iconst_4", "iconst_5", "lconst_0", "lconst_1", "fconst_0", "fconst_1", "fconst_2", "dconst_0", "dconst_1" -> {
                int value = content.charAt(content.length() - 1) - '0';
                yield switch (content.charAt(0)) {
                    case 'i' -> new ConstantInstruction.Int(new OfInt(value));
                    case 'l' -> new ConstantInstruction.Long(new OfLong(value));
                    case 'f' -> new ConstantInstruction.Float(new OfFloat(value));
                    case 'd' -> new ConstantInstruction.Double(new OfDouble(value));
                    default -> throw new IllegalStateException("Unexpected value: " + content.charAt(0));
                };
            }
            case "i2l", "i2f", "i2d", "l2i", "l2f", "l2d", "f2i", "f2l", "f2d", "d2i", "d2l", "d2f", "i2b", "i2c", "i2s" -> {
                char fromChar = content.charAt(0);
                PrimitiveType from = switch (fromChar) {
                    case 'i' -> Types.INT;
                    case 'l' -> Types.LONG;
                    case 'f' -> Types.FLOAT;
                    case 'd' -> Types.DOUBLE;
                    default -> throw new IllegalStateException(
                            "Unknown from type for primitive conversion: " + fromChar
                    );
                };
                char toChar = content.charAt(2);
                PrimitiveType to = switch (toChar) {
                    case 'i' -> Types.INT;
                    case 'l' -> Types.LONG;
                    case 'f' -> Types.FLOAT;
                    case 'd' -> Types.DOUBLE;
                    case 'c' -> Types.CHAR;
                    case 's' -> Types.SHORT;
                    case 'b' -> Types.BYTE;
                    default -> throw new IllegalStateException("Unknown to type for primitive conversion: " + toChar);
                };
                yield new PrimitiveConversionInstruction(from, to);
            }
            default -> new SimpleInstruction(opcode);
        };
        add(instruction);
    }

    @Override
    public void visitIntInsn(ASTNumber operand) {
        add(new ConstantInstruction.Int(new OfInt(operand.asInt())));
    }

    @Override
    public void visitNewArrayInsn(ASTIdentifier type) {
        ClassType component = switch (type.content().charAt(0)) {
            case 'v' -> Types.VOID;
            case 'l' -> Types.LONG;
            case 'd' -> Types.DOUBLE;
            case 'i' -> Types.INT;
            case 'f' -> Types.FLOAT;
            case 'c' -> Types.CHAR;
            case 's' -> Types.SHORT;
            case 'b' -> type.content().charAt(1) == 'o' ? Types.BOOLEAN : Types.BYTE;
            default -> throw new IllegalStateException("Unexpected value: " + type.content());
        };
        add(new AllocateInstruction(Types.arrayType(component)));
    }

    @Override
    public void visitLdcInsn(ASTElement constant) {
        add(ConstantInstruction.wrap(ConstantMapper.fromConstant(constant)));
    }

    @Override
    public void visitVarInsn(ASTIdentifier var) {
        String name = var.literal();
        boolean wide = opcode == LSTORE || opcode == DSTORE || opcode == LLOAD || opcode == DLOAD;
        int index = varCache.getOrCreate(name, wide);
        add(new VarInstruction(opcode, index));
    }

    @Override
    public void visitIincInsn(ASTIdentifier var, ASTNumber increment) {
        add(new VariableIncrementInstruction(varCache.getOrCreate(var.literal(), false), increment.asInt()));
    }

    @Override
    public void visitJumpInsn(ASTIdentifier label) {
        // we can assume that labels will not contain characters needing escaping
        Label l = getOrCreateLabel(label.content());
        Instruction instruction = switch (opcode) {
            case GOTO, GOTO_W, JSR, JSR_W -> new ImmediateJumpInstruction(opcode, l);
            default -> new ConditionalJumpInstruction(opcode, l);
        };
        add(instruction);
    }

    @Override
    public void visitTypeInsn(ASTIdentifier type) {
        String literal = type.literal();
        if (opcode == NEW) {
            literal = adaptDescToInternalName("new", literal);
            ObjectType objectType = Types.instanceTypeFromInternalName(literal);
            add(new AllocateInstruction(objectType));
        } else if (opcode == CHECKCAST || opcode == INSTANCEOF) {
            literal = adaptDescToInternalNameOrArray(literal);
            ObjectType objectType = Types.objectTypeFromInternalName(literal);
            Instruction instruction = switch (opcode) {
                case CHECKCAST -> new CheckCastInstruction(objectType);
                case INSTANCEOF -> new InstanceofInstruction(objectType);
                default -> throw new IllegalStateException("Unexpected value: " + opcode);
            };
            add(instruction);
        } else if (opcode == ANEWARRAY) {
            literal = adaptDescToInternalNameOrArray(literal);
            ObjectType componentType = Types.objectTypeFromInternalName(literal);
            add(new AllocateInstruction(Types.arrayType(componentType)));
        } else {
            throw new IllegalStateException("Unexpected value: " + opcode);
        }
    }

    @Override
    public void visitLookupSwitchInsn(ASTObject lookupSwitchObject) {
        ASTIdentifier defaultLabel = lookupSwitchObject.value("default");
        assert defaultLabel != null;
        List<Integer> keys = new ArrayList<>();
        List<Label> labels = new ArrayList<>();
        for (var pair : lookupSwitchObject.values().pairs()) {
            if (pair.first().content().equals("default"))
                continue;
            keys.add(Integer.parseInt(pair.first().content()));
            labels.add(getOrCreateLabel(pair.second().content()));
        }
        add(new LookupSwitchInstruction(
                keys.stream().mapToInt(Integer::intValue).toArray(),
                getOrCreateLabel(defaultLabel.content()),
                labels
        ));
    }

    @Override
    public void visitTableSwitchInsn(ASTObject tableSwitchObject) {
        ASTNumber min = tableSwitchObject.value("min");
        assert min != null;
        // max is not important as it is just min + length - 1
        ASTIdentifier defaultLabel = tableSwitchObject.value("default");
        assert defaultLabel != null;
        List<Label> labels = new ArrayList<>();
        ASTArray cases = tableSwitchObject.value("cases");
        assert cases != null;
        for (ASTElement value : cases.values()) {
            assert value instanceof ASTIdentifier;
            labels.add(getOrCreateLabel(value.content()));
        }
        add(new TableSwitchInstruction(min.asInt(), getOrCreateLabel(defaultLabel.content()), labels));
    }

    @Override
    public void visitFieldInsn(ASTIdentifier path, ASTIdentifier descriptor) {
        String literal = path.literal();
        int index = literal.lastIndexOf('.');
        String owner = literal.substring(0, index);
        String name = literal.substring(index + 1);
        InstanceType objectType = Types.instanceTypeFromInternalName(owner);
        ClassType type = new TypeReader(descriptor.literal()).requireClassType();
        add(new FieldInstruction(opcode, objectType, name, type));
    }

    @Override
    public void visitMethodInsn(ASTIdentifier path, ASTIdentifier descriptor) {
        boolean itf = currentInstructionAst.identifier().content().endsWith("interface");
        String literal = path.literal();
        int index = literal.lastIndexOf('.');
        String owner = literal.substring(0, index);
        String name = literal.substring(index + 1);
        InstanceType objectType = Types.instanceTypeFromInternalName(owner);
        MethodType type = Types.methodType(descriptor.literal());
        add(new MethodInstruction(opcode, objectType, name, type, itf));
    }

    @Override
    public void visitInvokeDynamicInsn(ASTIdentifier name, ASTIdentifier descriptor, ASTElement bsm, ASTArray bsmArgs) {
        Handle handle;

        if (bsm instanceof ASTIdentifier identifier) {
            handle = Handle.HANDLE_SHORTCUTS.get(identifier.content());
        } else if (bsm instanceof ASTArray array) {
            handle = Handle.from(array);
        } else {
            throw new IllegalStateException("Unexpected value: " + bsm);
        }

        add(new InvokeDynamicInstruction(
                name.literal(),
                Objects.requireNonNullElse(new TypeReader(descriptor.literal()).read(), Types.OBJECT),
                ConstantMapper.methodHandleFromHandle(handle),
                bsmArgs.values().stream().filter(Objects::nonNull).map(ConstantMapper::fromConstant).toList()
        ));
    }

    @Override
    public void visitMultiANewArrayInsn(ASTIdentifier descriptor, ASTNumber numDimensions) {
        int dimSize = numDimensions.asInt();
        String literal = descriptor.literal();
        Type literalType = Types.typeFromDescriptor(literal);
        if (literalType instanceof ArrayType literalArrayType) {
            add(new AllocateMultiDimArrayInstruction(literalArrayType, dimSize));
        } else {
            ArrayType arrayType = Types.arrayType((ClassType) literalType);
            var insn = new AllocateMultiDimArrayInstruction(arrayType, dimSize);
            add(insn);
            analysisEngine.warn(insn, "Expected array type, got class name");
        }
    }

    @Override
    public void visitLabel(@NotNull ASTIdentifier label) {
        visitedInstructions.add((ASTInstruction) label.parent());
        Label labelElement = getOrCreateLabel(label.content());
        analysisEngine.recordInstructionMapping(currentInstructionAst, labelElement);
        codeBuilderList.addLabel(labelElement);
    }

    @Override
    public void visitLineNumber(ASTNumber line) {
        Label label = new GenericLabel();
        label.setLineNumber(line.asInt());
        codeBuilderList.addLabel(label);
    }

    @Override
    public void visitEnd() {
        Label begin, end;
        if (codeBuilderList.getFirstElement() instanceof Label startLabel) {
            begin = startLabel;
        } else {
            // TODO: Warn user that they're missing a start label and this will fuck analysis up
            //  - analysisEngine.addWarning(Warning.MISSING_START_LABEL);
            //    - Should be language agnostic (IE, don't just use strings)
            //    - Not sure what other kinds of warnings will be added later, if this is it, then a simple enum works ig
            codeBuilderList.addLabel(0, begin = new GenericLabel());
        }

        if (codeBuilderList.getLastElement() instanceof Label lastLabel) {
            end = lastLabel;
        } else {
            codeBuilderList.addLabel(end = new GenericLabel());
        }

        for (Local parameter : parameters) {
            if (parameter == null)
                continue; // wide parameter

            int index = parameter.index();
            String name = parameter.name();
            ClassType type = parameter.safeType();
            if (writeVariables)
                codeBuilder.localVariable(new GenericLocal(begin, end, index, name, type, null));

            // Mark parameter variable as being assigned before the code
            var parameterVar = varCache.getFirstByIndex(index);
            if (parameterVar != null)
                parameterVar.updateFirstAssignedOffset(-1);
        }

        // Analyze stack for local variable information.
        Code code = codeBuilder.build();
        AnalysisSimulation.Info method = new AnalysisSimulation.Info(checker, parameters, code.elements(), code.tryCatchBlocks());
        try {
            // Variable analysis
            new IndexedStraightforwardSimulation()
                    .execute(new VarCacheUpdater(varCache), code);

            // Code analysis
            AnalysisSimulation simulation = new AnalysisSimulation(analysisEngine.newFrameOps());
            simulation.execute(analysisEngine, method);
        } catch (AnalysisException ex) {
            analysisEngine.setAnalysisFailure(ex);

            ASTInstruction problemAst = analysisEngine.getCodeToAstMap().get(ex.getElement());
            if (problemAst != null)
                errorCollector.addError(ex.getMessage(), problemAst.location());
            else
                errorCollector.addError(ex.getMessage(), Location.UNKNOWN);
        }

        if (writeVariables) {
            // Populate variables
            //  - Our variables are not scoped, so we can merge by name.
            //  - Known 'null' locals can merge with duplicate locals which may have type info associated with them
            Map<String, Local> localsMap = new HashMap<>();
            int paramOffset = parameters.size();
            analysisEngine.frames().forEach((index, frame) -> {
                for (Local local : frame.locals().toList()) {
                    // Skip parameters
                    if (local.index() < paramOffset)
                        continue;

                    localsMap.merge(local.name(), local, (a, b) -> {
                        ClassType at = a.type(); // These may be 'null' for known 'null' values
                        ClassType bt = b.type();

                        // Edge cases: If we have null values use the other local (with type info hopefully)
                        if (at == null)
                            return b;
                        if (bt == null)
                            return a;

                        // Edge case: If the types aren't of the same sort they cannot possibly be merged.
                        // There is no "correct" solution at this step, so just yield object and call it a day.
                        //
                        // Reproduction code:
                        //   ldc "foo"
                        //   astore foo
                        //   bipush 10
                        //   istore foo <--- Foo cannot be an object and an int in our system
                        if (at.getClass() != bt.getClass())
                            return a.adaptType(Types.OBJECT);

                        // Merge locals by type
                        try {
                            return a.adaptType(common(at, bt));
                        } catch (ValueMergeException e) {
                            // If the values could not be merged, report where the failure originated from.
                            CodeElement element = code.elements().get(index);
                            ASTInstruction ast = analysisEngine.getCodeToAstMap().get(element);
                            if (ast != null) {
                                errorCollector.addError(e.getMessage(), ast.location());
                                return a.adaptType(Types.OBJECT);
                            } else {
                                throw new IllegalStateException();
                            }
                        }
                    });
                }
            });
            localsMap.forEach((name, local) -> {
                int index = local.index();
                ClassType type = local.safeType();
                codeBuilder.localVariable(new GenericLocal(begin, end, index, name, type, null));
            });
        }
    }

    private void add(@NotNull Instruction instruction) {
        codeBuilderList.addInstruction(instruction);

        // The ASTMethod will record the current ASTInstruction being mapped. Thus, when we see a call to something like
        // visitTableSwitch and we add a TableSwitchInstruction to the CodeBuilder, we know that added instruction maps
        // to the current AST being visited.
        analysisEngine.recordInstructionMapping(currentInstructionAst, instruction);
    }

    private @NotNull ClassType common(@NotNull ClassType a, @NotNull ClassType b) throws ValueMergeException {
        if (a instanceof ObjectType ao && b instanceof ObjectType bo) {
            if (ao instanceof ArrayType aa) {
                if (bo instanceof ArrayType ba) {
                    // a == array, b == array
                    Type ct = common(aa.rootComponentType(), ba.rootComponentType());
                    String arrayDims = "[".repeat(aa.dimensions());
                    return Types.arrayTypeFromDescriptor(arrayDims + ct.descriptor());
                }

                // a == array, b == not-array
                return Types.OBJECT;
            } else if (bo instanceof ArrayType) {
                // a == not-array, b == array
                return Types.OBJECT;
            }

            // a == object, b == object
            String ct = checker.getCommonSuperclass(ao.internalName(), bo.internalName());
            return Types.instanceTypeFromInternalName(ct);
        } else if (a instanceof PrimitiveType ap && b instanceof PrimitiveType bp) {
            return ap.widen(bp);
        }
        throw new ValueMergeException("Cannot find common type between " + a.descriptor() + " and " + b.descriptor());
    }

    private static @NotNull String adaptDescToInternalName(@NotNull String op, @NotNull String desc) {
        char first = desc.charAt(0);
        if (first == 'L' && desc.charAt(desc.length()-1) == ';')
            desc = desc.substring(1, desc.length()-1); // Adapt if user put in desc format accidentally
        else if (first == '[')
            throw new IllegalStateException("Cannot use '" + op + "' to allocate an array type");
        return desc;
    }

    private static @NotNull String adaptDescToInternalNameOrArray(@NotNull String desc) {
        char first = desc.charAt(0);
        if (first == 'L' && desc.charAt(desc.length()-1) == ';')
            desc = desc.substring(1, desc.length()-1); // Adapt if user put in desc format accidentally
        else if (first == '[')
            return desc;
        return desc;
    }
}
