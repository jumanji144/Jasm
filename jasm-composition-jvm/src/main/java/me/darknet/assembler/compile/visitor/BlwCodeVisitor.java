package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.*;
import me.darknet.assembler.compile.JvmCompilerOptions;
import me.darknet.assembler.compile.analysis.AnalysisException;
import me.darknet.assembler.compile.analysis.AnalysisResults;
import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compile.analysis.frame.Frame;
import me.darknet.assembler.compile.analysis.jvm.AnalysisSimulation;
import me.darknet.assembler.compile.analysis.jvm.JvmAnalysisEngine;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.util.BlwOpcodes;
import me.darknet.assembler.util.ConstantMapper;
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
    private final Map<String, GenericLabel> nameToLabel = new HashMap<>();
    private final List<Local> parameters;
    /**
     * We only track local names since the stack analysis will provide us more
     * detail later. See {@link #visitEnd()}
     */
    private final List<String> localNames = new ArrayList<>();
    private final JvmAnalysisEngine<Frame> analysisEngine;
    private ASTInstruction last;
    private int opcode = 0;

    /**
     * @param options
     *                   Compiler option to pull values from.
     * @param builder
     *                   Builder to insert code into.
     * @param parameters
     *                   Parameter variables.
     */
    @SuppressWarnings("unchecked")
    public BlwCodeVisitor(JvmCompilerOptions options, CodeBuilder<?> builder, List<Local> parameters) {
        this.codeBuilder = builder;
        this.codeBuilderList = builder.codeList().child();
        this.checker = options.inheritanceChecker();
        this.analysisEngine = (JvmAnalysisEngine<Frame>) options.createEngine(this::getLocalName);
        this.parameters = parameters;

        // Populate variables from params.
        parameters.stream().filter(Objects::nonNull).forEach(param -> getOrCreateLocal(param.name(), param.size() > 1));
    }

    /**
     * @return Analysis of the method code.
     */
    @NotNull
    public AnalysisResults getAnalysisResults() {
        return analysisEngine;
    }

    /**
     * @param index
     *              Index of variable.
     *
     * @return Name of variable, or dummy value for unknown index.
     */
    @NotNull
    public String getLocalName(int index) {
        if (index < 0 || index >= localNames.size())
            return "<?>";
        return localNames.get(index);
    }

    /**
     * @param name
     *             Name of variable.
     * @param wide
     *             {@code true} for {@code long}/{@code double} types.
     *
     * @return Index of variable.
     */
    private int getOrCreateLocal(String name, boolean wide) {
        int index = localNames.indexOf(name);
        if (index > -1)
            return index;
        index = localNames.size();
        localNames.add(name);
        if (wide)
            localNames.add(null);
        return index;
    }

    /**
     * @param name
     *               Name of variable.
     * @param opcode
     *               Instruction responsible for accessing the variable.
     *
     * @return Index of variable.
     */
    private int getOrCreateLocalVar(String name, int opcode) {
        if (opcode == LSTORE || opcode == DSTORE || opcode == LLOAD || opcode == DLOAD) {
            return getOrCreateLocal(name, true);
        }
        return getOrCreateLocal(name, false);
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
    public void visitInstruction(ASTInstruction instruction) {
        last = instruction;
        if (instruction instanceof ASTLabel)
            return;
        opcode = BlwOpcodes.opcode(instruction.identifier().content());
    }

    @Override
    public void visitException(ASTIdentifier start, ASTIdentifier end, ASTIdentifier handler, ASTIdentifier type) {
        GenericLabel startLabel = nameToLabel.get(start.content()); // assume that labels don't have escapable characters
        GenericLabel endLabel = nameToLabel.get(end.content());
        GenericLabel handlerLabel = nameToLabel.get(handler.content());

        String typeName = type.literal();
        InstanceType exceptionType = typeName.equals("*") ? null : Types.instanceTypeFromDescriptor(typeName);
        codeBuilder.tryCatchBlock(new TryCatchBlock(startLabel, endLabel, handlerLabel, exceptionType));
    }

    @Override
    public void visitInsn() {
        String content = last.identifier().content();
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
        add(new VarInstruction(opcode, getOrCreateLocalVar(var.literal(), opcode)));
    }

    @Override
    public void visitIincInsn(ASTIdentifier var, ASTNumber increment) {
        add(new VariableIncrementInstruction(getOrCreateLocal(var.literal(), false), increment.asInt()));
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
        if (opcode == NEW) {
            add(new AllocateInstruction(Types.instanceTypeFromInternalName(type.literal())));
        } else if (opcode == ANEWARRAY) {
            InstanceType elementType = Types.instanceTypeFromDescriptor(type.literal());
            ArrayType arrayType = Types.arrayType(elementType);
            add(new AllocateInstruction(arrayType));
        } else {
            TypeReader reader = new TypeReader(type.literal());
            ObjectType objectType = Objects.requireNonNullElse((ObjectType) reader.read(), Types.OBJECT);
            Instruction instruction = switch (opcode) {
                case CHECKCAST -> new CheckCastInstruction(objectType);
                case INSTANCEOF -> new InstanceofInstruction(objectType);
                default -> throw new IllegalStateException("Unexpected value: " + opcode);
            };
            add(instruction);
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
        add(
                new LookupSwitchInstruction(
                        keys.stream().mapToInt(Integer::intValue).toArray(), getOrCreateLabel(defaultLabel.content()),
                        labels
                )
        );
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
        boolean itf = last.identifier().content().endsWith("interface");
        String literal = path.literal();
        int index = literal.lastIndexOf('.');
        String owner = literal.substring(0, index);
        String name = literal.substring(index + 1);
        InstanceType objectType = Types.instanceTypeFromInternalName(owner);
        MethodType type = Types.methodType(descriptor.literal());
        add(new MethodInstruction(opcode, objectType, name, type, itf));
    }

    @Override
    public void visitInvokeDynamicInsn(ASTIdentifier name, ASTIdentifier descriptor, ASTArray bsm, ASTArray bsmArgs) {
        add(
                new InvokeDynamicInstruction(
                        name.literal(),
                        Objects.requireNonNullElse(new TypeReader(descriptor.literal()).read(), Types.OBJECT),
                        ConstantMapper.methodHandleFromArray(bsm),
                        bsmArgs.values().stream().filter(Objects::nonNull).map(ConstantMapper::fromConstant).toList()
                )
        );
    }

    @Override
    public void visitMultiANewArrayInsn(ASTIdentifier descriptor, ASTNumber numDimensions) {
        add(new AllocateInstruction(Types.arrayTypeFromDescriptor(descriptor.literal())));
    }

    @Override
    public void visitLabel(ASTIdentifier label) {
        codeBuilderList.addLabel(getOrCreateLabel(label.content()));
    }

    @Override
    public void visitLineNumber(ASTIdentifier label, ASTNumber line) {
        getOrCreateLabel(label.content()).setLineNumber(line.asInt());
    }

    @Override
    public void visitEnd() {
        Label begin, end;
        if (codeBuilderList.getFirstElement()instanceof Label startLabel) {
            begin = startLabel;
        } else {
            // TODO: Warn user that they're missing a start label and this will fuck analysis up
            //  - analysisEngine.addWarning(Warning.MISSING_START_LABEL);
            //    - Should be language agnostic (IE, don't just use strings)
            //    - Not sure what other kinds of warnings will be added later, if this is it, then a simple enum works ig
            codeBuilderList.addLabel(0, begin = new GenericLabel());
        }

        if (codeBuilderList.getLastElement()instanceof Label lastLabel) {
            end = lastLabel;
        } else {
            codeBuilderList.addLabel(end = new GenericLabel());
        }

        for (Local parameter : parameters) {
            if (parameter == null)
                continue; // wide parameter
            codeBuilder.localVariable(
                    new GenericLocal(begin, end, parameter.index(), parameter.name(), parameter.type(), null)
            );
        }

        // Analyze stack for local variable information.
        AnalysisSimulation simulation = new AnalysisSimulation(analysisEngine.newFrameOps());
        Code code = codeBuilder.build();
        try {
            simulation.execute(
                    analysisEngine,
                    new AnalysisSimulation.Info(checker, parameters, code.elements(), code.tryCatchBlocks())
            );
        } catch (AnalysisException ex) {
            analysisEngine.setAnalysisFailure(ex);
        }

        // Populate variables
        int paramOffset = parameters.size();
        analysisEngine.frames().values().stream().flatMap(Frame::locals).filter(local -> local.index() >= paramOffset)
                .distinct().forEach(local -> {
                    int index = local.index();
                    ClassType type = local.type();
                    String name = getLocalName(index);
                    codeBuilder.localVariable(new GenericLocal(begin, end, index, name, type, null));
                });
    }

    private void add(Instruction instruction) {
        codeBuilderList.addInstruction(instruction);
    }
}
