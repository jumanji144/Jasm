package me.darknet.assembler.compile.visitor;

import dev.xdark.blw.code.*;
import dev.xdark.blw.code.attribute.generic.GenericLocal;
import dev.xdark.blw.code.generic.GenericLabel;
import dev.xdark.blw.code.instruction.*;
import dev.xdark.blw.constant.OfDouble;
import dev.xdark.blw.constant.OfFloat;
import dev.xdark.blw.constant.OfInt;
import dev.xdark.blw.constant.OfLong;
import dev.xdark.blw.type.*;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.*;
import me.darknet.assembler.util.BlwOpcodes;
import me.darknet.assembler.util.ConstantMapper;
import me.darknet.assembler.visitor.ASTJvmInstructionVisitor;

import java.util.*;

public class BlwCodeVisitor implements ASTJvmInstructionVisitor, JavaOpcodes {

    private final Label begin;
    private final Label end;
    private final CodeBuilder.Nested<?> meta;
    private final CodeListBuilder.Nested<?> list;
    private final Map<String, GenericLabel> labels = new HashMap<>();
    private final MethodType type;
    private final List<String> parameters;
    private final List<String> locals = new ArrayList<>();
    private ASTInstruction last;
    private int opcode = 0;

    public BlwCodeVisitor(MethodType type, CodeBuilder.Nested<?> builder, List<String> parameters) {
        this.meta = builder;
        this.list = builder.codeList();
        this.type = type;
        this.parameters = parameters;
        this.begin = new GenericLabel();
        this.end = new GenericLabel();
        list.element(begin);
    }

    public int getOrCreateLocal(String name) {
        int index = parameters.indexOf(name);
        if(index > -1) return index;
        index = locals.indexOf(name);
        if(index > -1) return index + parameters.size();
        locals.add(name);
        return locals.size() + parameters.size() - 1;
    }

    public Label getOrCreateLabel(String name) {
        if(labels.containsKey(name)) {
            return labels.get(name);
        } else {
            GenericLabel label = new GenericLabel();
            labels.put(name, label);
            return label;
        }
    }

    @Override
    public void visitInstruction(ASTInstruction instruction) {
        last = instruction;
        opcode = BlwOpcodes.opcode(instruction.identifier().content());
    }

    @Override
    public void visitInsn() {
        String content = last.identifier().content();
        add(switch (content) {
            case "iconst_m1" -> new ConstantInstruction.Int(new OfInt(-1));
            case "iconst_0", "iconst_1", "iconst_2", "iconst_3", "iconst_4", "iconst_5",
                    "lconst_0", "lconst_1", "fconst_0", "fconst_1", "fconst_2", "dconst_0", "dconst_1" -> {
                int value = content.charAt(content.length() - 1) - '0';
                yield switch (content.charAt(0)) {
                    case 'i' -> new ConstantInstruction.Int(new OfInt(value));
                    case 'l' -> new ConstantInstruction.Long(new OfLong(value));
                    case 'f' -> new ConstantInstruction.Float(new OfFloat(value));
                    case 'd' -> new ConstantInstruction.Double(new OfDouble(value));
                    default -> throw new IllegalStateException("Unexpected value: " + content.charAt(0));
                };
            }
            case "i2l", "i2f", "i2d", "l2i", "l2f", "l2d", "f2i", "f2l", "f2d", "d2i", "d2l", "d2f",
                    "i2b", "i2c", "i2s" -> {
                PrimitiveType from = switch (content.charAt(0)) {
                    case 'i' -> Types.INT;
                    case 'l' -> Types.LONG;
                    case 'f' -> Types.FLOAT;
                    case 'd' -> Types.DOUBLE;
                    default -> throw new IllegalStateException();
                };
                PrimitiveType to = switch (content.charAt(2)) {
                    case 'i' -> Types.INT;
                    case 'l' -> Types.LONG;
                    case 'f' -> Types.FLOAT;
                    case 'd' -> Types.DOUBLE;
                    default -> throw new IllegalStateException();
                };
                yield new PrimitiveConversionInstruction(from, to);
            }
            default -> new SimpleInstruction(opcode);
        });
    }

    @Override
    public void visitIntInsn(ASTNumber operand) {
        add(new ConstantInstruction.Int(new OfInt(operand.asInt())));
    }

    @Override
    public void visitNewArrayInsn(ASTIdentifier type) {
        add(new AllocateInstruction(Types.arrayType(switch (type.content().charAt(0)) {
            case 'v' -> Types.VOID;
            case 'l' -> Types.LONG;
            case 'd' -> Types.DOUBLE;
            case 'i' -> Types.INT;
            case 'f' -> Types.FLOAT;
            case 'c' -> Types.CHAR;
            case 's' -> Types.SHORT;
            case 'b' -> type.content().charAt(1) == 'o' ? Types.BOOLEAN : Types.BYTE;
            default -> throw new IllegalStateException("Unexpected value: " + type.content());
        })));
    }

    @Override
    public void visitLdcInsn(ASTElement constant) {
        add(ConstantInstruction.wrap(ConstantMapper.fromConstant(constant)));
    }

    @Override
    public void visitVarInsn(ASTIdentifier var) {
        add(new VarInstruction(opcode, getOrCreateLocal(var.literal())));

    }

    @Override
    public void visitIincInsn(ASTIdentifier var, ASTNumber increment) {
        add(new VariableIncrementInstruction(getOrCreateLocal(var.literal()), increment.asInt()));
    }

    @Override
    public void visitJumpInsn(ASTIdentifier label) {
        // we can assume that labels will not contain characters needing escaping
        Label l = getOrCreateLabel(label.content());
        add(switch (opcode) {
            case GOTO, GOTO_W, JSR, JSR_W -> new ImmediateJumpInstruction(opcode, l);
            default -> new ConditionalJumpInstruction(opcode, l);
        });
    }

    @Override
    public void visitTypeInsn(ASTIdentifier type) {
        ObjectType objectType = Types.instanceTypeFromInternalName(type.literal());
        add(switch (opcode) {
            case CHECKCAST -> new CheckCastInstruction(objectType);
            case INSTANCEOF -> new InstanceofInstruction(objectType);
            case NEW -> new AllocateInstruction(objectType);
            case ANEWARRAY -> new AllocateInstruction(Types.arrayType(objectType));
            default -> throw new IllegalStateException("Unexpected value: " + opcode);
        });
    }

    @Override
    public void visitLookupSwitchInsn(ASTObject lookupSwitchObject) {
        ASTIdentifier defaultLabel = lookupSwitchObject.value("default");
        assert defaultLabel != null;
        List<Integer> keys = new ArrayList<>();
        List<Label> labels = new ArrayList<>();
        for (var pair : lookupSwitchObject.values().pairs()) {
            if(pair.first().content().equals("default")) continue;
            keys.add(Integer.parseInt(pair.first().content()));
            labels.add(getOrCreateLabel(pair.second().content()));
        }
        add(new LookupSwitchInstruction(
                keys.stream().mapToInt(Integer::intValue).toArray(),
                getOrCreateLabel(defaultLabel.content()),
                labels));
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
        add(new TableSwitchInstruction(
                min.asInt(),
                getOrCreateLabel(defaultLabel.content()),
                labels));
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
        add(new InvokeDynamicInstruction(
                name.literal(),
                new TypeReader(descriptor.literal()).read(),
                ConstantMapper.fromArray(bsm),
                bsmArgs.values().stream().filter(Objects::nonNull).map(ConstantMapper::fromConstant).toList()
        ));
    }

    @Override
    public void visitMultiANewArrayInsn(ASTIdentifier descriptor, ASTNumber numDimensions) {
        add(new AllocateInstruction(Types.arrayTypeFromDescriptor(descriptor.literal())));
    }

    @Override
    public void visitLabel(ASTIdentifier label) {
        getOrCreateLabel(label.content());
    }

    @Override
    public void visitLineNumber(ASTIdentifier label, ASTNumber line) {
        getOrCreateLabel(label.content()).lineNumber(line.asInt());
    }

    @Override
    public void visitEnd() {
        list.element(end);
        List<ClassType> paramTypes = type.parameterTypes();
        for (int i = 0; i < parameters.size(); i++) {
            String name = parameters.get(i);
            ClassType type = paramTypes.get(i);
            meta.localVariable(new GenericLocal(begin, end, i, name, type, null));
        }
        for (int i = 0; i < locals.size(); i++) {
            int index = i + parameters.size();
            String name = locals.get(i);
            // TODO: analyze local types
            meta.localVariable(new GenericLocal(begin, end, index, name, null, null));
        }
    }

    void add(Instruction instruction) {
        list.element(instruction);
    }
}
