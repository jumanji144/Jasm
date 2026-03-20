package me.darknet.assembler.printer;

import me.darknet.dex.file.instructions.Opcodes;
import me.darknet.dex.tree.definitions.OpcodeNames;
import me.darknet.dex.tree.definitions.constant.Constant;
import me.darknet.dex.tree.definitions.instructions.*;
import me.darknet.dex.tree.simulation.ExecutionEngine;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public class DalvikCodePrinter implements ExecutionEngine {

    private final PrintContext.CodePrint ctx;
    private final Map<Integer, String> registers;
    private final Map<Label, String> labels;
    public DalvikCodePrinter(PrintContext.CodePrint ctx,
                             Map<Integer, String> registers,
                             Map<Label, String> labels) {
        this.ctx = ctx;
        this.registers = registers;
        this.labels = labels;
    }

    private static String opcode(Instruction instruction) {
        return OpcodeNames.name(instruction.opcode());
    }

    private String register(int register) {
        return registers.get(register);
    }

    @Override
    public void label(Label label) {
        ctx.label(labels.get(label)).next();
        if (label.lineNumber() != -1)
            ctx.instruction("line")
                    .print(String.valueOf(label.lineNumber()))
                    .next();
    }

    @Override
    public void execute(ArrayInstruction arrayInstruction) {
        ctx.instruction(opcode(arrayInstruction))
                .print(register(arrayInstruction.value())).arg()
                .print(register(arrayInstruction.array())).arg()
                .print(register(arrayInstruction.index()));
    }

    @Override
    public void execute(ArrayLengthInstruction arrayLengthInstruction) {
        ctx.instruction(opcode(arrayLengthInstruction))
                .print(register(arrayLengthInstruction.dest())).arg()
                .print(register(arrayLengthInstruction.array()));
    }

    @Override
    public void execute(Binary2AddrInstruction binary2AddrInstruction) {
        ctx.instruction(opcode(binary2AddrInstruction))
                .print(register(binary2AddrInstruction.a())).arg()
                .print(register(binary2AddrInstruction.b()));
    }

    @Override
    public void execute(BinaryInstruction binaryInstruction) {
        ctx.instruction(opcode(binaryInstruction))
                .print(register(binaryInstruction.dest())).arg()
                .print(register(binaryInstruction.a())).arg()
                .print(register(binaryInstruction.b()));
    }

    @Override
    public void execute(BinaryLiteralInstruction binaryLiteralInstruction) {
        ctx.instruction(opcode(binaryLiteralInstruction))
                .print(register(binaryLiteralInstruction.dest())).arg()
                .print(register(binaryLiteralInstruction.src())).arg()
                .print(String.valueOf(binaryLiteralInstruction.constant()));
    }

    @Override
    public void execute(BranchInstruction branchInstruction) {
        ctx.instruction(opcode(branchInstruction))
                .print(register(branchInstruction.a())).arg()
                .print(register(branchInstruction.b())).arg()
                .print(labels.get(branchInstruction.label()));
    }

    @Override
    public void execute(BranchZeroInstruction branchZeroInstruction) {
        ctx.instruction(opcode(branchZeroInstruction))
                .print(register(branchZeroInstruction.a())).arg()
                .print(labels.get(branchZeroInstruction.label()));
    }

    @Override
    public void execute(CheckCastInstruction checkCastInstruction) {
        ctx.instruction(opcode(checkCastInstruction))
                .print(register(checkCastInstruction.register())).arg()
                .literal(checkCastInstruction.type().descriptor());
    }

    @Override
    public void execute(CompareInstruction compareInstruction) {
        ctx.instruction(opcode(compareInstruction))
                .print(register(compareInstruction.dest())).arg()
                .print(register(compareInstruction.a())).arg()
                .print(register(compareInstruction.b()));
    }

    @Override
    public void execute(ConstInstruction constInstruction) {
        ctx.instruction("const")
                .print(register(constInstruction.register())).arg()
                .print(String.valueOf(constInstruction.value()));
    }

    @Override
    public void execute(ConstTypeInstruction constTypeInstruction) {
        ctx.instruction("const-class")
                .print(register(constTypeInstruction.register())).arg()
                .literal(constTypeInstruction.type().descriptor());
    }

    @Override
    public void execute(ConstWideInstruction constWideInstruction) {
        ctx.instruction("const-wide")
                .print(register(constWideInstruction.register())).arg()
                .print(constWideInstruction.value() + "L");
    }

    @Override
    public void execute(ConstStringInstruction constStringInstruction) {
        ctx.instruction("const-string")
                .print(register(constStringInstruction.register())).arg()
                .string(constStringInstruction.string());
    }

    @Override
    public void execute(@NotNull ConstMethodHandleInstruction instruction) {
        ctx.instruction("const-method-handle")
                .print(register(instruction.destination())).arg();

        ConstantPrinter.printHandle(instruction.handle(), ctx);
    }

    @Override
    public void execute(@NotNull ConstMethodTypeInstruction instruction) {
        ctx.instruction("const-method-type")
                .print(register(instruction.destination())).arg()
                .literal(instruction.type().descriptor());
    }

    @Override
    public void execute(FillArrayDataInstruction fillArrayDataInstruction) {
        PrintContext.ArrayPrint arrayPrint = this.ctx.instruction(opcode(fillArrayDataInstruction))
                .print(register(fillArrayDataInstruction.array())).arg()
                .array();

        // build the correct number type for the print size
        List<Number> prints = new ArrayList<>();
        ByteBuffer buffer = ByteBuffer.wrap(fillArrayDataInstruction.data()).order(ByteOrder.LITTLE_ENDIAN);
        int printSize = fillArrayDataInstruction.elementSize();
        while (buffer.hasRemaining()) {
            switch (printSize) {
                case 1 -> prints.add(buffer.get());
                case 2 -> prints.add(buffer.getShort());
                case 4 -> prints.add(buffer.getInt());
                case 8 -> prints.add(buffer.getLong());
                default -> throw new IllegalStateException("Unexpected value: " + printSize);
            }
        }
        arrayPrint.print(prints, (ap, num) -> {
            // hex
            if (num instanceof Byte || num instanceof Short || num instanceof Integer) {
                ap.print(String.format("0x%X", num));
            } else if (num instanceof Long) {
                ap.print(String.format("0x%XL", num));
            } else {
                throw new IllegalStateException("Unexpected number type: " + num.getClass());
            }
        });

        arrayPrint.end();
    }

    private void printRegisterArray(PrintContext.ArrayPrint arrayPrint, int[] registers) {
        if (registers.length > 0) {
            arrayPrint.print(this.register(registers[0]));
            for (int i = 1; i < registers.length; i++) {
                arrayPrint.arg().print(this.register(registers[i]));
            }
        }
    }


    @Override
    public void execute(FilledNewArrayInstruction filledNewArrayInstruction) {
        var printer = ctx.instruction(opcode(filledNewArrayInstruction))
                .literal(filledNewArrayInstruction.componentType().descriptor()).arg()
                .array();

        if (filledNewArrayInstruction.isRange()) {
            printer.print(register(filledNewArrayInstruction.first())).arg()
                    .print(register(filledNewArrayInstruction.last()));
        } else {
            assert filledNewArrayInstruction.registers() != null;
            printRegisterArray(printer, filledNewArrayInstruction.registers());
        }

        printer.end();
    }

    @Override
    public void execute(GotoInstruction gotoInstruction) {
        ctx.instruction("goto")
                .print(labels.get(gotoInstruction.jump()));
    }

    @Override
    public void execute(InstanceFieldInstruction instanceFieldInstruction) {
        ctx.instruction(opcode(instanceFieldInstruction))
                .print(register(instanceFieldInstruction.value())).arg()
                .print(register(instanceFieldInstruction.instance())).arg()
                .literal(instanceFieldInstruction.owner().internalName())
                .print(".")
                .literal(instanceFieldInstruction.name()).arg()
                .literal(instanceFieldInstruction.type().descriptor());
    }

    @Override
    public void execute(InstanceOfInstruction instanceOfInstruction) {
        ctx.instruction(opcode(instanceOfInstruction))
                .print(register(instanceOfInstruction.destination())).arg()
                .print(register(instanceOfInstruction.register())).arg()
                .literal(instanceOfInstruction.type().descriptor());
    }

    @Override
    public void execute(InvokeCustomInstruction invokeCustomInstruction) {
        var arguments = ctx.instruction(opcode(invokeCustomInstruction)).array();

        if (invokeCustomInstruction.isRange()) {
            arguments.print(register(invokeCustomInstruction.first())).arg()
                    .print(register(invokeCustomInstruction.last()));
        } else {
            printRegisterArray(arguments, invokeCustomInstruction.argumentRegisters());
        }

        arguments.end();

        ConstantPrinter.printHandle(invokeCustomInstruction.handle(), ctx.arg());

        ctx.arg().literal(invokeCustomInstruction.name()).arg()
                .literal(invokeCustomInstruction.type().descriptor()).arg();

        var constantArguments = ctx.array();
        constantArguments.print(invokeCustomInstruction.arguments(), ConstantPrinter::printConstant);

        constantArguments.end();
    }

    @Override
    public void execute(InvokeInstruction invokeInstruction) {
        var arguments = ctx.instruction(opcode(invokeInstruction))
                .array();

        if (invokeInstruction.isRange()) {
            arguments.print(register(invokeInstruction.first())).arg()
                    .print(register(invokeInstruction.last()));
        } else {
            printRegisterArray(arguments, invokeInstruction.arguments());
        }

        arguments.end();

        ctx.arg().literal(invokeInstruction.owner().internalName())
                .print(".")
                .literal(invokeInstruction.name()).arg()
                .literal(invokeInstruction.type().descriptor());
    }

    @Override
    public void execute(MonitorInstruction monitorInstruction) {
        ctx.instruction(opcode(monitorInstruction))
                .print(register(monitorInstruction.register()));
    }

    @Override
    public void execute(MoveExceptionInstruction moveExceptionInstruction) {
        ctx.instruction(opcode(moveExceptionInstruction))
                .print(register(moveExceptionInstruction.register()));
    }

    @Override
    public void execute(MoveInstruction moveInstruction) {
        ctx.instruction(opcode(moveInstruction))
                .print(register(moveInstruction.to())).arg()
                .print(register(moveInstruction.from()));
    }

    @Override
    public void execute(MoveObjectInstruction moveObjectInstruction) {
        ctx.instruction(opcode(moveObjectInstruction))
                .print(register(moveObjectInstruction.to())).arg()
                .print(register(moveObjectInstruction.from()));
    }

    @Override
    public void execute(MoveResultInstruction moveResultInstruction) {
        ctx.instruction(opcode(moveResultInstruction))
                .print(register(moveResultInstruction.to()));
    }

    @Override
    public void execute(MoveWideInstruction moveWideInstruction) {
        ctx.instruction(opcode(moveWideInstruction))
                .print(register(moveWideInstruction.to())).arg()
                .print(register(moveWideInstruction.from()));
    }

    @Override
    public void execute(NewArrayInstruction newArrayInstruction) {
        ctx.instruction(opcode(newArrayInstruction))
                .print(register(newArrayInstruction.dest())).arg()
                .print(register(newArrayInstruction.sizeRegister())).arg()
                .literal(newArrayInstruction.componentType().descriptor());
    }

    @Override
    public void execute(NewInstanceInstruction newInstanceInstruction) {
        ctx.instruction(opcode(newInstanceInstruction))
                .print(register(newInstanceInstruction.dest())).arg()
                .literal(newInstanceInstruction.type().descriptor());
    }

    @Override
    public void execute(NopInstruction nopInstruction) {
        ctx.instruction(opcode(nopInstruction));
    }

    @Override
    public void execute(PackedSwitchInstruction packedSwitchInstruction) {

    }

    @Override
    public void execute(ReturnInstruction returnInstruction) {
        ctx.instruction(opcode(returnInstruction));
        if (returnInstruction.opcode() != Opcodes.RETURN_VOID) {
            ctx.print(register(returnInstruction.register()));
        }
    }

    @Override
    public void execute(SparseSwitchInstruction sparseSwitchInstruction) {

    }

    @Override
    public void execute(StaticFieldInstruction staticFieldInstruction) {
        ctx.instruction(opcode(staticFieldInstruction))
                .print(register(staticFieldInstruction.value())).arg()
                .literal(staticFieldInstruction.owner().internalName())
                .print(".")
                .literal(staticFieldInstruction.name()).arg()
                .literal(staticFieldInstruction.type().descriptor());
    }

    @Override
    public void execute(ThrowInstruction throwInstruction) {
        ctx.instruction(opcode(throwInstruction))
                .print(register(throwInstruction.value()));
    }

    @Override
    public void execute(UnaryInstruction unaryInstruction) {
        ctx.instruction(opcode(unaryInstruction))
                .print(register(unaryInstruction.dest())).arg()
                .print(register(unaryInstruction.source()));
    }

    @Override
    public void execute(Instruction instruction) {
        ctx.next();
    }
}
