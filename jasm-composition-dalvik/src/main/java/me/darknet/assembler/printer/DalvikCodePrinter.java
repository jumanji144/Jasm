package me.darknet.assembler.printer;

import me.darknet.dex.tree.definitions.OpcodeNames;
import me.darknet.dex.tree.definitions.instructions.*;
import me.darknet.dex.tree.simulation.ExecutionEngine;

public class DalvikCodePrinter implements ExecutionEngine {

    private final PrintContext.CodePrint ctx;

    public DalvikCodePrinter(PrintContext.CodePrint ctx) {
        this.ctx = ctx;
    }

    public static String getLabelName(int index) {
        StringBuilder label = new StringBuilder();

        while (index >= 0) {
            label.insert(0, (char) ('A' + index % 26));
            index = (index / 26) - 1;
        }

        return label.toString();
    }

    private static String opcode(Instruction instruction) {
        return OpcodeNames.name(instruction.opcode());
    }


    @Override
    public void label(Label label) {
        ctx.label(getLabelName(label.index())).next();
    }

    @Override
    public void execute(ArrayInstruction arrayInstruction) {
        //ctx.instruction(opcode(arrayInstruction));
    }

    @Override
    public void execute(ArrayLengthInstruction arrayLengthInstruction) {

    }

    @Override
    public void execute(Binary2AddrInstruction binary2AddrInstruction) {

    }

    @Override
    public void execute(BinaryInstruction binaryInstruction) {

    }

    @Override
    public void execute(BinaryLiteralInstruction binaryLiteralInstruction) {

    }

    @Override
    public void execute(BranchInstruction branchInstruction) {

    }

    @Override
    public void execute(BranchZeroInstruction branchZeroInstruction) {

    }

    @Override
    public void execute(CheckCastInstruction checkCastInstruction) {

    }

    @Override
    public void execute(CompareInstruction compareInstruction) {

    }

    @Override
    public void execute(ConstInstruction constInstruction) {

    }

    @Override
    public void execute(ConstTypeInstruction constTypeInstruction) {

    }

    @Override
    public void execute(ConstWideInstruction constWideInstruction) {

    }

    @Override
    public void execute(ConstStringInstruction constStringInstruction) {

    }

    @Override
    public void execute(FillArrayDataInstruction fillArrayDataInstruction) {

    }

    @Override
    public void execute(FilledNewArrayInstruction filledNewArrayInstruction) {

    }

    @Override
    public void execute(GotoInstruction gotoInstruction) {

    }

    @Override
    public void execute(InstanceFieldInstruction instanceFieldInstruction) {

    }

    @Override
    public void execute(InstanceOfInstruction instanceOfInstruction) {

    }

    @Override
    public void execute(InvokeCustomInstruction invokeCustomInstruction) {

    }

    @Override
    public void execute(InvokeInstruction invokeInstruction) {

    }

    @Override
    public void execute(MonitorInstruction monitorInstruction) {

    }

    @Override
    public void execute(MoveExceptionInstruction moveExceptionInstruction) {

    }

    @Override
    public void execute(MoveInstruction moveInstruction) {

    }

    @Override
    public void execute(MoveObjectInstruction moveObjectInstruction) {

    }

    @Override
    public void execute(MoveResultInstruction moveResultInstruction) {

    }

    @Override
    public void execute(MoveWideInstruction moveWideInstruction) {

    }

    @Override
    public void execute(NewArrayInstruction newArrayInstruction) {

    }

    @Override
    public void execute(NewInstanceInstruction newInstanceInstruction) {

    }

    @Override
    public void execute(NopInstruction nopInstruction) {

    }

    @Override
    public void execute(PackedSwitchInstruction packedSwitchInstruction) {

    }

    @Override
    public void execute(ReturnInstruction returnInstruction) {

    }

    @Override
    public void execute(SparseSwitchInstruction sparseSwitchInstruction) {

    }

    @Override
    public void execute(StaticFieldInstruction staticFieldInstruction) {

    }

    @Override
    public void execute(ThrowInstruction throwInstruction) {

    }

    @Override
    public void execute(UnaryInstruction unaryInstruction) {

    }

    @Override
    public void execute(Instruction instruction) {
        ctx.instruction(instruction.toString()).next();
    }
}
