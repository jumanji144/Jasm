package me.darknet.assembler;

import me.darknet.assembler.instructions.Instruction;
import me.darknet.assembler.instructions.InstructionTrait;
import me.darknet.assembler.instructions.Instructions;
import me.darknet.assembler.instructions.Operand;
import me.darknet.assembler.instructions.OperandRole;
import me.darknet.assembler.visitor.ASTInstructionVisitor;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that instruction registries reject duplicate mnemonics and freeze after construction.
 */
class InstructionRegistryTest {
    @Test
    void rejectsDuplicateMnemonicWithoutReplacingDefinition() {
        ManualInstructions instructions = new ManualInstructions();
        instructions.addDefinition("nop");
        Instruction<ASTInstructionVisitor> original = instructions.get("nop");
        assertNotNull(original);

        assertThrows(IllegalArgumentException.class, () -> instructions.addDefinition("nop"));
        assertSame(original, instructions.get("nop"));
        instructions.seal();
    }

    @Test
    void defaultConstructionFreezesRegistry() {
        AutomaticInstructions instructions = new AutomaticInstructions();
        assertNotNull(instructions.get("nop"));

        assertThrows(UnsupportedOperationException.class, () -> instructions.addDefinition("extra"));
    }

    @Test
    void explicitFreezeClosesManualConstruction() {
        ManualInstructions instructions = new ManualInstructions();
        instructions.addDefinition("nop");
        instructions.seal();

        assertThrows(UnsupportedOperationException.class, () -> instructions.addDefinition("extra"));
    }

    private abstract static class TestInstructions extends Instructions<ASTInstructionVisitor> {
        protected TestInstructions() {
            super();
        }

        protected TestInstructions(boolean registerInstructions) {
            super(registerInstructions);
        }

        protected final void addDefinition(String name) {
            register(name, new Operand[0], (instruction, visitor) -> {},
                    sourceMetadata(EnumSet.noneOf(InstructionTrait.class), List.of(), null, name));
        }

        protected final void seal() {
            freeze();
        }
    }

    private static final class ManualInstructions extends TestInstructions {
        private ManualInstructions() {
            super(false);
        }

        @Override
        protected void registerInstructions() {
            // no-op
        }
    }

    private static final class AutomaticInstructions extends TestInstructions {
        private AutomaticInstructions() {
            super();
        }

        @Override
        protected void registerInstructions() {
            addDefinition("nop");
        }
    }
}
