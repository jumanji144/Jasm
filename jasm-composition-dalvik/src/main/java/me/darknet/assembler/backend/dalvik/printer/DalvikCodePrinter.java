package me.darknet.assembler.backend.dalvik.printer;

import me.darknet.assembler.backend.dalvik.compile.analysis.DalvikConstantTypeResolver.ResolvedType;
import me.darknet.assembler.printer.*;

import me.darknet.dex.file.instructions.Opcodes;
import me.darknet.dex.tree.definitions.OpcodeNames;
import me.darknet.dex.tree.definitions.instructions.*;
import me.darknet.dex.tree.simulation.ExecutionEngine;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * Utility class for printing instructions in the Dalvik assembly format.
 */
public class DalvikCodePrinter implements ExecutionEngine {
    private final PrintContext.CodePrint ctx;
    private final Map<Integer, String> registers;
    private final Map<Label, String> labels;
    private final Map<Instruction, ResolvedType> constantTypes;

	/**
	 * @param ctx
	 * 		Print context for the method body.
	 * @param registers
	 * 		Map of Dalvik register numbers to source-spelled names.
	 * @param labels
	 * 		Map of Dalvik label objects to source-spelled names.
	 * @param constantTypes
	 * 		Map of Dalvik constant instructions to their resolved types.
	 */
	public DalvikCodePrinter(@NotNull PrintContext.CodePrint ctx,
                             @NotNull Map<Integer, String> registers,
                             @NotNull Map<Label, String> labels,
                             @NotNull Map<Instruction, ResolvedType> constantTypes) {
        this.ctx = ctx;
        this.registers = registers;
        this.labels = labels;
        this.constantTypes = constantTypes;
    }

    @Override
    public void label(@NotNull Label label) {
        ctx.label(labels.get(label)).next();
        if (label.lineNumber() != -1)
            ctx.instruction("line")
                    .print(String.valueOf(label.lineNumber()))
                    .next();
    }

    @Override
    public void execute(@NotNull ArrayInstruction instruction) {
        ctx.instruction(opcode(instruction))
                .print(register(instruction.value())).arg()
                .print(register(instruction.array())).arg()
                .print(register(instruction.index()));
    }

    @Override
    public void execute(@NotNull ArrayLengthInstruction instruction) {
        ctx.instruction(opcode(instruction))
                .print(register(instruction.dest())).arg()
                .print(register(instruction.array()));
    }

    @Override
    public void execute(@NotNull Binary2AddrInstruction instruction) {
        ctx.instruction(opcode(instruction))
                .print(register(instruction.a())).arg()
                .print(register(instruction.b()));
    }

    @Override
    public void execute(@NotNull BinaryInstruction instruction) {
        ctx.instruction(opcode(instruction))
                .print(register(instruction.dest())).arg()
                .print(register(instruction.a())).arg()
                .print(register(instruction.b()));
    }

    @Override
    public void execute(@NotNull BinaryLiteralInstruction instruction) {
        ctx.instruction(opcode(instruction))
                .print(register(instruction.dest())).arg()
                .print(register(instruction.src())).arg()
                .print(String.valueOf(instruction.constant()));
    }

    @Override
    public void execute(@NotNull BranchInstruction instruction) {
        ctx.instruction(opcode(instruction))
                .print(register(instruction.a())).arg()
                .print(register(instruction.b())).arg()
                .print(labels.get(instruction.label()));
    }

    @Override
    public void execute(@NotNull BranchZeroInstruction instruction) {
        ctx.instruction(opcode(instruction))
                .print(register(instruction.a())).arg()
                .print(labels.get(instruction.label()));
    }

    @Override
    public void execute(@NotNull CheckCastInstruction instruction) {
        ctx.instruction(opcode(instruction))
                .print(register(instruction.register())).arg()
                .literal(instruction.type().descriptor());
    }

    @Override
    public void execute(@NotNull CompareInstruction instruction) {
        ctx.instruction(opcode(instruction))
                .print(register(instruction.dest())).arg()
                .print(register(instruction.a())).arg()
                .print(register(instruction.b()));
    }

    @Override
    public void execute(@NotNull ConstInstruction instruction) {
        ctx.instruction("const")
                .print(register(instruction.register())).arg();
        switch (constantType(instruction)) {
            case FLOAT -> DalvikConstantPrinter.printFloat(ctx, Float.intBitsToFloat(instruction.value()));
            case INT -> ctx.print(Integer.toString(instruction.value()));
            case UNKNOWN -> DalvikConstantPrinter.printRawIntBits(ctx, instruction.value());
            case LONG, DOUBLE -> throw new IllegalStateException("Invalid resolved type for Dalvik const " + instruction);
        }
    }

    @Override
    public void execute(@NotNull ConstTypeInstruction instruction) {
        ctx.instruction("const-class")
                .print(register(instruction.register())).arg()
                .literal(instruction.type().descriptor());
    }

    @Override
    public void execute(@NotNull ConstWideInstruction instruction) {
        ctx.instruction("const-wide")
                .print(register(instruction.register())).arg();
        switch (constantType(instruction)) {
            case DOUBLE -> DalvikConstantPrinter.printDouble(ctx, Double.longBitsToDouble(instruction.value()));
            case LONG -> ctx.print(instruction.value() + "L");
            case UNKNOWN -> DalvikConstantPrinter.printRawLongBits(ctx, instruction.value());
            case INT, FLOAT -> throw new IllegalStateException("Invalid resolved type for Dalvik const-wide " + instruction);
        }
    }

    @Override
    public void execute(@NotNull ConstStringInstruction instruction) {
        ctx.instruction("const-string")
                .print(register(instruction.register())).arg()
                .string(instruction.string());
    }

    @Override
    public void execute(@NotNull ConstMethodHandleInstruction instruction) {
        ctx.instruction("const-method-handle")
                .print(register(instruction.destination())).arg();

        DalvikConstantPrinter.printHandle(instruction.handle(), ctx);
    }

    @Override
    public void execute(@NotNull ConstMethodTypeInstruction instruction) {
        ctx.instruction("const-method-type")
                .print(register(instruction.destination())).arg()
                .literal(instruction.type().descriptor());
    }

    @Override
    public void execute(@NotNull FillArrayDataInstruction instruction) {
        PrintContext.ObjectPrint object = this.ctx.instruction(opcode(instruction))
                .print(register(instruction.array())).arg()
                .object();
        int elementSize = instruction.elementSize();
        object.value("width").print(String.valueOf(elementSize)).next();
        PrintContext.ArrayPrint values = object.value("values").array();

        // Print raw little-endian bits so payload width and floating-point values survive a round trip.
        ByteBuffer buffer = ByteBuffer.wrap(instruction.data()).order(ByteOrder.LITTLE_ENDIAN);
        boolean first = true;
        while (buffer.hasRemaining()) {
            if (!first) {
                values.arg();
            }
            values.print(rawArrayValue(buffer, elementSize));
            first = false;
        }
        values.end();
        object.end();
    }

    @Override
    public void execute(@NotNull FilledNewArrayInstruction instruction) {
        var printer = ctx.instruction(opcode(instruction)).array();

        if (instruction.isRange()) {
            printer.print(register(instruction.first())).arg()
                    .print(register(instruction.last()));
        } else {
            int[] registers = instruction.registers();
            if (registers == null) {
                throw new IllegalStateException("Filled new-array instruction is not range-based but has no registers");
            }
            printRegisterArray(printer, registers);
        }

        printer.end();
        ctx.arg().literal(instruction.componentType().descriptor());
    }

    @Override
    public void execute(@NotNull GotoInstruction instruction) {
        ctx.instruction("goto")
                .print(labels.get(instruction.jump()));
    }

    @Override
    public void execute(@NotNull InstanceFieldInstruction instruction) {
        ctx.instruction(opcode(instruction))
                .print(register(instruction.value())).arg()
                .print(register(instruction.instance())).arg()
                .literal(instruction.owner().internalName())
                .print(".")
                .literal(instruction.name()).arg()
                .literal(instruction.type().descriptor());
    }

    @Override
    public void execute(@NotNull InstanceOfInstruction instruction) {
        ctx.instruction(opcode(instruction))
                .print(register(instruction.destination())).arg()
                .print(register(instruction.register())).arg()
                .literal(instruction.type().descriptor());
    }

    @Override
    public void execute(@NotNull InvokeCustomInstruction instruction) {
        var arguments = ctx.instruction(opcode(instruction)).array();

        if (instruction.isRange()) {
            arguments.print(register(instruction.first())).arg()
                    .print(register(instruction.last()));
        } else {
            printRegisterArray(arguments, instruction.argumentRegisters());
        }

        arguments.end();

        ctx.arg().literal(instruction.name()).arg()
                .literal(instruction.type().descriptor()).arg();

        DalvikConstantPrinter.printHandle(instruction.handle(), ctx.arg());

        ctx.arg();

        var constantArguments = ctx.array();
        constantArguments.print(instruction.arguments(), DalvikConstantPrinter::printConstant);

        constantArguments.end();
    }

    @Override
    public void execute(@NotNull InvokeInstruction instruction) {
        var arguments = ctx.instruction(opcode(instruction))
                .array();

        if (instruction.isRange()) {
            arguments.print(register(instruction.first())).arg()
                    .print(register(instruction.last()));
        } else {
            printRegisterArray(arguments, instruction.arguments());
        }

        arguments.end();

        ctx.arg().literal(instruction.owner().internalName())
                .print(".")
                .literal(instruction.name()).arg()
                .literal(instruction.methodType().descriptor());

        if (instruction.opcode() == Opcodes.INVOKE_POLYMORPHIC)
            ctx.arg().literal(instruction.type().descriptor());
    }

    @Override
    public void execute(@NotNull MonitorInstruction instruction) {
        ctx.instruction(opcode(instruction))
                .print(register(instruction.register()));
    }

    @Override
    public void execute(@NotNull MoveExceptionInstruction instruction) {
        ctx.instruction(opcode(instruction))
                .print(register(instruction.register()));
    }

    @Override
    public void execute(@NotNull MoveInstruction instruction) {
        ctx.instruction(opcode(instruction))
                .print(register(instruction.to())).arg()
                .print(register(instruction.from()));
    }

    @Override
    public void execute(@NotNull MoveObjectInstruction instruction) {
        ctx.instruction(opcode(instruction))
                .print(register(instruction.to())).arg()
                .print(register(instruction.from()));
    }

    @Override
    public void execute(@NotNull MoveResultInstruction instruction) {
        ctx.instruction(opcode(instruction))
                .print(register(instruction.to()));
    }

    @Override
    public void execute(@NotNull MoveWideInstruction instruction) {
        ctx.instruction(opcode(instruction))
                .print(register(instruction.to())).arg()
                .print(register(instruction.from()));
    }

    @Override
    public void execute(@NotNull NewArrayInstruction instruction) {
        ctx.instruction(opcode(instruction))
                .print(register(instruction.dest())).arg()
                .print(register(instruction.sizeRegister())).arg()
                .literal(instruction.componentType().descriptor());
    }

    @Override
    public void execute(@NotNull NewInstanceInstruction instruction) {
        ctx.instruction(opcode(instruction))
                .print(register(instruction.dest())).arg()
                .literal(instruction.type().descriptor());
    }

    @Override
    public void execute(@NotNull NopInstruction instruction) {
        ctx.instruction(opcode(instruction));
    }

    @Override
    public void execute(@NotNull PackedSwitchInstruction instruction) {
        var object = ctx.instruction("packed-switch")
                .print(register(instruction.register())).arg()
                .object();
        object.value("first").print(String.valueOf(instruction.firstKey())).next();
        var targets = object.value("targets").array();
        for (int i = 0; i < instruction.targets().size(); i++) {
            if (i > 0) {
                targets.arg();
            }
            targets.print(labels.get(instruction.targets().get(i)));
        }
        targets.end();
        object.end();
    }

    @Override
    public void execute(@NotNull SparseSwitchInstruction instruction) {
        var object = ctx.instruction("sparse-switch")
                .print(register(instruction.register())).arg()
                .object();
        var sortedTargets = new TreeMap<>(instruction.targets());
        boolean first = true;
        for (var entry : sortedTargets.entrySet()) {
            if (!first) {
                object.next();
            }
            object.value(String.valueOf(entry.getKey())).print(labels.get(entry.getValue()));
            first = false;
        }
        object.end();
    }

    @Override
    public void execute(@NotNull ReturnInstruction instruction) {
        ctx.instruction(opcode(instruction));
        if (instruction.opcode() != Opcodes.RETURN_VOID) {
            ctx.print(register(instruction.register()));
        }
    }

    @Override
    public void execute(@NotNull StaticFieldInstruction instruction) {
        ctx.instruction(opcode(instruction))
                .print(register(instruction.value())).arg()
                .literal(instruction.owner().internalName())
                .print(".")
                .literal(instruction.name()).arg()
                .literal(instruction.type().descriptor());
    }

    @Override
    public void execute(@NotNull ThrowInstruction instruction) {
        ctx.instruction(opcode(instruction))
                .print(register(instruction.value()));
    }

    @Override
    public void execute(@NotNull UnaryInstruction instruction) {
        ctx.instruction(opcode(instruction))
                .print(register(instruction.dest())).arg()
                .print(register(instruction.source()));
    }

    @Override
    public void execute(@NotNull Instruction instruction) {
        ctx.next();
    }

    private void printRegisterArray(@NotNull PrintContext.ArrayPrint arrayPrint, int @NotNull [] registers) {
        if (registers.length > 0) {
            arrayPrint.print(register(registers[0]));
            for (int i = 1; i < registers.length; i++) {
                arrayPrint.arg().print(register(registers[i]));
            }
        }
    }

    private @NotNull String register(int register) {
        String name = registers.get(register);
        if (name == null)
            throw new IllegalStateException("No name assigned to Dalvik register v" + register);
        return name;
    }

    private @NotNull ResolvedType constantType(Instruction instruction) {
        ResolvedType type = constantTypes.get(instruction);
        if (type == null)
            throw new IllegalStateException("No resolved type for Dalvik constant " + instruction);
        return type;
    }

    private static @NotNull String opcode(@NotNull Instruction instruction) {
        return switch (instruction) {
            case InvokeCustomInstruction invoke -> invoke.isRange() ? "invoke-custom/range" : "invoke-custom";
            case InvokeInstruction invoke -> {
                String name = OpcodeNames.name(invoke.opcode());
                yield invoke.isRange() ? name + "/range" : name;
            }
            case MoveInstruction ignored -> "move";
            case MoveWideInstruction ignored -> "move-wide";
            case MoveObjectInstruction ignored -> "move-object";
            case ConstInstruction ignored -> "const";
            case ConstWideInstruction ignored -> "const-wide";
            case ConstStringInstruction ignored -> "const-string";
            case GotoInstruction ignored -> "goto";
            default -> {
                String name = OpcodeNames.name(instruction.opcode());
                if (name == null)
                    throw new IllegalStateException("Unsupported Dalvik opcode: 0x" + Integer.toHexString(instruction.opcode()));
                yield normalizeEncodedOpcode(name);
            }
        };
    }

    private static @NotNull String normalizeEncodedOpcode(@NotNull String name) {
        return switch (name) {
            case "move-from16", "move-16" -> "move";
            case "move-wide-from16", "move-wide-16" -> "move-wide";
            case "move-object-from16", "move-object-16" -> "move-object";
            case "const-4", "const-16", "const-high16" -> "const";
            case "const-wide-16", "const-wide-32", "const-wide-high16" -> "const-wide";
            case "goto-16", "goto-32" -> "goto";
            default -> {
                if (name.endsWith("-2addr"))
                    yield name.substring(0, name.length() - "-2addr".length()) + "/2addr";
                if (name.endsWith("-lit8"))
                    yield name.substring(0, name.length() - "-lit8".length()) + "/lit8";
                if (name.endsWith("-lit16"))
                    yield name.substring(0, name.length() - "-lit16".length()) + "/lit16";
                yield name.endsWith("-range")
                        ? name.substring(0, name.length() - "-range".length()) + "/range"
                        : name;
            }
        };
    }

    private static @NotNull String rawArrayValue(@NotNull ByteBuffer buffer, int elementSize) {
        return switch (elementSize) {
            case 1 -> String.format(Locale.ROOT, "0x%02X", buffer.get() & 0xff);
            case 2 -> String.format(Locale.ROOT, "0x%04X", Short.toUnsignedInt(buffer.getShort()));
            case 4 -> String.format(Locale.ROOT, "0x%08X", buffer.getInt());
            case 8 -> String.format(Locale.ROOT, "0x%016X", buffer.getLong());
            default -> throw new IllegalStateException("Unexpected value: " + elementSize);
        };
    }
}
