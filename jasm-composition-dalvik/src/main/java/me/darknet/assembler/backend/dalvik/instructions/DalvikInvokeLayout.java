package me.darknet.assembler.backend.dalvik.instructions;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.descriptor.Descriptors;
import me.darknet.assembler.descriptor.DescriptorForm;
import me.darknet.assembler.descriptor.DescriptorParser;
import me.darknet.assembler.descriptor.DescriptorType;
import me.darknet.assembler.descriptor.MethodDescriptor;
import me.darknet.assembler.error.DiagnosticCode;
import me.darknet.assembler.instructions.OperandRefiner;
import me.darknet.assembler.instructions.OperandRole;
import me.darknet.assembler.instructions.ValidatedOperand;
import me.darknet.assembler.parser.processor.ProcessorContext;
import me.darknet.dex.tree.type.ClassType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Logical arguments of an invoke, grouped against the descriptor that types them.
 * <p>
 * An ordinary list names one register for each logical argument. A {@code long} or {@code double} argument is named by
 * its first register, and the register after it is its tail. The grouping reads only the prototype, whether a receiver
 * leads, and the words or entries. It never consults register values or what the analysis could infer about them, so a
 * call whose arguments are unknown groups exactly as a known one does.
 */
public final class DalvikInvokeLayout {
    private DalvikInvokeLayout() {}

    /**
     * Refiner for the register list of an ordinary, custom, or polymorphic invoke. The list is operand 0 of every such
     * form, so the refiner checks that list against the descriptor at {@code descriptorOperand}: one entry for the
     * receiver when there is one, then one entry for each argument. Each entry for a {@code long} or {@code double}
     * argument is marked wide, which is what reserves its tail and encodes both of its words.
     * <p>
     * Raw register words are left as written, because their words are not grouped into arguments.
     *
     * @param descriptorOperand
     * 		Operand position holding the method descriptor that types the arguments.
     * @param receiver
     * 		{@code true} when the first entry is the receiver of the call.
     *
     * @return Refiner that completes the register list of the invoke.
     */
    public static @NotNull OperandRefiner logicalArguments(int descriptorOperand, boolean receiver) {
        return (context, source, operands) ->
                refineLogicalArguments(context, source, operands, descriptorOperand, receiver);
    }

    private static @NotNull List<ValidatedOperand> refineLogicalArguments(@NotNull ProcessorContext context,
                                                                          @NotNull ASTInstruction source,
                                                                          @NotNull List<ValidatedOperand> operands,
                                                                          int descriptorOperand, boolean receiver) {
        ValidatedOperand registers = operands.getFirst();
        if (!(registers.value() instanceof RegisterList list))
            return operands;

        ASTIdentifier descriptor = source.argument(descriptorOperand, ASTIdentifier.class);
        if (!(DescriptorParser.tryParse(descriptor.literal(), DescriptorForm.METHOD) instanceof MethodDescriptor method)) {
            context.throwUnexpectedElementError("method descriptor", descriptor);
            return operands;
        }

        List<DescriptorType> parameters = method.parameters();
        List<RegisterRef> entries = list.registers();
        int expected = parameters.size() + (receiver ? 1 : 0);
        if (entries.size() != expected) {
            context.throwError(DiagnosticCode.OPERAND_SHAPE,
                    countMessage(source.identifier().literal(), descriptor.literal(), receiver, parameters.size(),
                            entries.size()),
                    registers.source().location());
            return operands;
        }

        List<RegisterRef> refined = new ArrayList<>(entries.size());
        for (int entry = 0; entry < entries.size(); entry++) {
            RegisterRef register = entries.get(entry);
            int parameter = receiver ? entry - 1 : entry;
            boolean wide = parameter >= 0 && Descriptors.isWideType(parameters.get(parameter));
            refined.add(wide ? new RegisterRef(register.name(), register.index(), OperandRole.WidthPolicy.WIDE) : register);
        }

        List<ValidatedOperand> result = new ArrayList<>(operands);
        result.set(0, new ValidatedOperand(0, new RegisterList(List.copyOf(refined)), registers.source()));
        return result;
    }

    private static @NotNull String countMessage(@NotNull String mnemonic, @NotNull String descriptor, boolean receiver,
                                                int parameters, int entries) {
        String shape = receiver
                ? "a receiver and " + parameters + " argument" + (parameters == 1 ? "" : "s")
                : parameters + " argument" + (parameters == 1 ? "" : "s");
        return mnemonic + " takes " + shape + " for " + descriptor + ", with one list entry for each, but the list has "
                + entries + " entr" + (entries == 1 ? "y" : "ies") + ". A long or double argument is named by its first "
                + "register alone";
    }

    /**
     * @param parameters
     * 		Parameter types of the prototype the words are grouped against.
     * @param receiver
     * 		{@code true} when the first word is the receiver of an instance invoke.
     * @param words
     * 		Register words in the order they are encoded.
     *
     * @return {@code true} when the words are exactly the receiver, if there is one, followed by one group per parameter,
     * where each long or double parameter occupies two consecutive registers in order. A word count that differs from
     * the prototype, or a wide pair that is not consecutive, is not a faithful grouping.
     */
    public static boolean groupsFaithfully(@NotNull List<ClassType> parameters, boolean receiver, int @NotNull [] words) {
        int next = 0;
        if (receiver) {
            if (words.length == 0)
                return false;
            next = 1;
        }
        for (ClassType parameter : parameters) {
            boolean wide = Descriptors.isWideType(parameter.descriptor());
            int width = wide ? 2 : 1;
            if (words.length - next < width)
                return false;
            if (wide && words[next + 1] != words[next] + 1)
                return false;
            next += width;
        }
        return next == words.length;
    }

    /**
     * @param parameters
     * 		Parameter types of the prototype the words are grouped against.
     * @param receiver
     * 		{@code true} when the first word is the receiver of an instance invoke.
     * @param words
     * 		Register words that {@link #groupsFaithfully} accepts, in the order they are encoded.
     *
     * @return The register each logical argument names, in order. A long or double argument is named by its first word,
     * so the tail it occupies after that word is not listed.
     */
    public static int @NotNull [] logicalRegisters(@NotNull List<ClassType> parameters, boolean receiver,
                                                   int @NotNull [] words) {
        int[] registers = new int[parameters.size() + (receiver ? 1 : 0)];
        int entry = 0;
        int word = 0;
        if (receiver)
            registers[entry++] = words[word++];
        for (ClassType parameter : parameters) {
            registers[entry++] = words[word];
            word += Descriptors.isWideType(parameter.descriptor()) ? 2 : 1;
        }
        return registers;
    }
}
