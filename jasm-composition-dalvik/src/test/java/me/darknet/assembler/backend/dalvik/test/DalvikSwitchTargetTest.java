package me.darknet.assembler.backend.dalvik.test;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.query.AssemblyUtils;
import me.darknet.assembler.query.SwitchTarget;
import me.darknet.assembler.test.AssemblyParseFixture;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies switch target resolution for Dalvik packed and sparse switch payloads.
 */
class DalvikSwitchTargetTest {
	private static final String SOURCE = """
			.method public static sample ()V {
			    registers: 2,
			    code: {
			        packed-switch v0 { first: 5, targets: { A, B } }
			        sparse-switch v1 { 7: C, -1: D }
			        return-void
			        A:
			        return-void
			        B:
			        return-void
			        C:
			        return-void
			        D:
			        return-void
			    }
			}
			""";

	@Test
	void packedSwitchTargetsCountFromFirstKeyWithoutDefault() {
		List<SwitchTarget> targets = resolve(0);
		assertEquals(2, targets.size());
		assertEquals("5", targets.get(0).context());
		assertEquals("A", targets.get(0).labelName());
		assertEquals("6", targets.get(1).context());
		assertEquals("B", targets.get(1).labelName());
	}

	@Test
	void sparseSwitchTargetsUseExplicitKeysInPayloadOrder() {
		List<SwitchTarget> targets = resolve(1);
		assertEquals(2, targets.size());
		assertEquals("7", targets.get(0).context());
		assertEquals("C", targets.get(0).labelName());
		assertEquals("-1", targets.get(1).context());
		assertEquals("D", targets.get(1).labelName());
	}

	@Test
	void nonSwitchInstructionsHaveNoTargets() {
		ASTMethod method = parse();
		ASTInstruction ret = method.getCode().getInstructions().getLast();
		assertTrue(AssemblyUtils.resolveSwitchTargets(DalvikTargetContext.INSTANCE, ret).isEmpty());
	}

	private static List<SwitchTarget> resolve(int index) {
		ASTInstruction instruction = parse().getCode().getInstructions().get(index);
		return AssemblyUtils.resolveSwitchTargets(DalvikTargetContext.INSTANCE, instruction);
	}

	private static ASTMethod parse() {
		List<ASTElement> ast = AssemblyParseFixture.processAst(AssemblyParseFixture.STDIN, SOURCE,
				DalvikTargetContext.INSTANCE).requireValue();
		return (ASTMethod) ast.getFirst();
	}
}
