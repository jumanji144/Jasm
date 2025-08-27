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
                    .element(String.valueOf(label.lineNumber()))
                    .next();
    }

    @Override
    public void execute(ArrayInstruction arrayInstruction) {
        ctx.instruction(opcode(arrayInstruction))
                .element(register(arrayInstruction.value())).arg()
                .element(register(arrayInstruction.array())).arg()
                .element(register(arrayInstruction.index()));
    }

    @Override
    public void execute(ArrayLengthInstruction arrayLengthInstruction) {
        ctx.instruction(opcode(arrayLengthInstruction))
                .element(register(arrayLengthInstruction.dest())).arg()
                .element(register(arrayLengthInstruction.array()));
    }

    @Override
    public void execute(Binary2AddrInstruction binary2AddrInstruction) {
        ctx.instruction(opcode(binary2AddrInstruction))
                .element(register(binary2AddrInstruction.a())).arg()
                .element(register(binary2AddrInstruction.b()));
    }

    @Override
    public void execute(BinaryInstruction binaryInstruction) {
        ctx.instruction(opcode(binaryInstruction))
                .element(register(binaryInstruction.dest())).arg()
                .element(register(binaryInstruction.a())).arg()
                .element(register(binaryInstruction.b()));
    }

    @Override
    public void execute(BinaryLiteralInstruction binaryLiteralInstruction) {
        ctx.instruction(opcode(binaryLiteralInstruction))
                .element(register(binaryLiteralInstruction.dest())).arg()
                .element(register(binaryLiteralInstruction.src())).arg()
                .element(String.valueOf(binaryLiteralInstruction.constant()));
    }

    @Override
    public void execute(BranchInstruction branchInstruction) {
        ctx.instruction(opcode(branchInstruction))
                .element(register(branchInstruction.a())).arg()
                .element(register(branchInstruction.b())).arg()
                .element(labels.get(branchInstruction.label()));
    }

    @Override
    public void execute(BranchZeroInstruction branchZeroInstruction) {
        ctx.instruction(opcode(branchZeroInstruction))
                .element(register(branchZeroInstruction.a())).arg()
                .element(labels.get(branchZeroInstruction.label()));
    }

    @Override
    public void execute(CheckCastInstruction checkCastInstruction) {
        ctx.instruction(opcode(checkCastInstruction))
                .element(register(checkCastInstruction.register())).arg()
                .literal(checkCastInstruction.type().descriptor());
    }

    @Override
    public void execute(CompareInstruction compareInstruction) {
        ctx.instruction(opcode(compareInstruction))
                .element(register(compareInstruction.dest())).arg()
                .element(register(compareInstruction.a())).arg()
                .element(register(compareInstruction.b()));
    }

    @Override
    public void execute(ConstInstruction constInstruction) {
        ctx.instruction("const")
                .element(register(constInstruction.register())).arg()
                .element(String.valueOf(constInstruction.value()));
    }

    @Override
    public void execute(ConstTypeInstruction constTypeInstruction) {
        ctx.instruction("const-class")
                .element(register(constTypeInstruction.register())).arg()
                .literal(constTypeInstruction.type().descriptor());
    }

    @Override
    public void execute(ConstWideInstruction constWideInstruction) {
        ctx.instruction("const-wide")
                .element(register(constWideInstruction.register())).arg()
                .element(constWideInstruction.value() + "L");
    }

    @Override
    public void execute(ConstStringInstruction constStringInstruction) {
        ctx.instruction("const-string")
                .element(register(constStringInstruction.register())).arg()
                .string(constStringInstruction.string());
    }

    @Override
    public void execute(@NotNull ConstMethodHandleInstruction instruction) {
        ctx.instruction("const-method-handle")
                .element(register(instruction.destination())).arg();

        ConstantPrinter.printHandle(instruction.handle(), ctx);
    }

    @Override
    public void execute(@NotNull ConstMethodTypeInstruction instruction) {
        ctx.instruction("const-method-type")
                .element(register(instruction.destination())).arg()
                .literal(instruction.type().descriptor());
    }

    @Override
    public void execute(FillArrayDataInstruction fillArrayDataInstruction) {
        PrintContext.ArrayPrint arrayPrint = this.ctx.instruction(opcode(fillArrayDataInstruction))
                .element(register(fillArrayDataInstruction.array())).arg()
                .array();

        // build the correct number type for the element size
        List<Number> elements = new ArrayList<>();
        ByteBuffer buffer = ByteBuffer.wrap(fillArrayDataInstruction.data()).order(ByteOrder.LITTLE_ENDIAN);
        int elementSize = fillArrayDataInstruction.elementSize();
        while (buffer.hasRemaining()) {
            switch (elementSize) {
                case 1 -> elements.add(buffer.get());
                case 2 -> elements.add(buffer.getShort());
                case 4 -> elements.add(buffer.getInt());
                case 8 -> elements.add(buffer.getLong());
                default -> throw new IllegalStateException("Unexpected value: " + elementSize);
            }
        }
        arrayPrint.print(elements, (ap, num) -> {
            // hex
            if (num instanceof Byte || num instanceof Short || num instanceof Integer) {
                ap.element(String.format("0x%X", num));
            } else if (num instanceof Long) {
                ap.element(String.format("0x%XL", num));
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
                arrayPrint.arg().element(this.register(registers[i]));
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
        ctx.instruction(opcode(gotoInstruction))
                .element(labels.get(gotoInstruction.jump())).next();
    }

    @Override
    public void execute(InstanceFieldInstruction instanceFieldInstruction) {
        ctx.instruction(opcode(instanceFieldInstruction))
                .element(register(instanceFieldInstruction.value())).arg()
                .element(register(instanceFieldInstruction.instance())).arg()
                .literal(instanceFieldInstruction.owner().internalName())
                .print(".")
                .literal(instanceFieldInstruction.name()).arg()
                .literal(instanceFieldInstruction.type().descriptor());
    }

    @Override
    public void execute(InstanceOfInstruction instanceOfInstruction) {
        ctx.instruction(opcode(instanceOfInstruction))
                .element(register(instanceOfInstruction.destination())).arg()
                .element(register(instanceOfInstruction.register())).arg()
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
                .element(register(monitorInstruction.register()));
    }

    @Override
    public void execute(MoveExceptionInstruction moveExceptionInstruction) {
        ctx.instruction(opcode(moveExceptionInstruction))
                .element(register(moveExceptionInstruction.register()));
    }

    @Override
    public void execute(MoveInstruction moveInstruction) {
        ctx.instruction(opcode(moveInstruction))
                .element(register(moveInstruction.to())).arg()
                .element(register(moveInstruction.from()));
    }

    @Override
    public void execute(MoveObjectInstruction moveObjectInstruction) {
        ctx.instruction(opcode(moveObjectInstruction))
                .element(register(moveObjectInstruction.to())).arg()
                .element(register(moveObjectInstruction.from()));
    }

    @Override
    public void execute(MoveResultInstruction moveResultInstruction) {
        ctx.instruction(opcode(moveResultInstruction))
                .element(register(moveResultInstruction.to()));
    }

    @Override
    public void execute(MoveWideInstruction moveWideInstruction) {
        ctx.instruction(opcode(moveWideInstruction))
                .element(register(moveWideInstruction.to())).arg()
                .element(register(moveWideInstruction.from()));
    }

    @Override
    public void execute(NewArrayInstruction newArrayInstruction) {
        ctx.instruction(opcode(newArrayInstruction))
                .element(register(newArrayInstruction.dest())).arg()
                .element(register(newArrayInstruction.sizeRegister())).arg()
                .literal(newArrayInstruction.componentType().descriptor());
    }

    @Override
    public void execute(NewInstanceInstruction newInstanceInstruction) {
        ctx.instruction(opcode(newInstanceInstruction))
                .element(register(newInstanceInstruction.dest())).arg()
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
            ctx.element(register(returnInstruction.register()));
        }
    }

    @Override
    public void execute(SparseSwitchInstruction sparseSwitchInstruction) {

    }

    @Override
    public void execute(StaticFieldInstruction staticFieldInstruction) {
        ctx.instruction(opcode(staticFieldInstruction))
                .element(register(staticFieldInstruction.value())).arg()
                .literal(staticFieldInstruction.owner().internalName())
                .print(".")
                .literal(staticFieldInstruction.name()).arg()
                .literal(staticFieldInstruction.type().descriptor());
    }

    @Override
    public void execute(ThrowInstruction throwInstruction) {
        ctx.instruction(opcode(throwInstruction))
                .element(register(throwInstruction.value()));
    }

    @Override
    public void execute(UnaryInstruction unaryInstruction) {
        ctx.instruction(opcode(unaryInstruction))
                .element(register(unaryInstruction.dest())).arg()
                .element(register(unaryInstruction.source()));
    }

    @Override
    public void execute(Instruction instruction) {
        ctx.next();
    }
}
