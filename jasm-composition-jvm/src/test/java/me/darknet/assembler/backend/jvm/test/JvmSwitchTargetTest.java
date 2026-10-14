package me.darknet.assembler.backend.jvm.test;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.backend.jvm.JvmTargetContext;
import me.darknet.assembler.query.AssemblyUtils;
import me.darknet.assembler.query.SwitchTarget;
import me.darknet.assembler.test.AssemblyParseFixture;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies switch target resolution for JVM switch payloads.
 */
class JvmSwitchTargetTest {
	private static final String SOURCE = """
			.method public static sample ()V {
			    code: {
			        lookupswitch { default: L0, 0: L1, 7: L2 }
			        tableswitch { min: 1, default: L0, cases: { L1, L2 } }
			        return
			        L0:
			        return
			        L1:
			        return
			        L2:
			        return
			    }
			}
			""";

	@Test
	void lookupSwitchTargetsAreReturnedInPayloadOrder() {
		List<SwitchTarget> targets = resolve(0);
		assertEquals(3, targets.size());
		assertEquals("default", targets.get(0).context());
		assertEquals("L0", targets.get(0).labelName());
		assertEquals("0", targets.get(1).context());
		assertEquals("L1", targets.get(1).labelName());
		assertEquals("7", targets.get(2).context());
		assertEquals("L2", targets.get(2).labelName());
	}

	@Test
	void tableSwitchTargetsCountFromMinimumAndIncludeDefault() {
		List<SwitchTarget> targets = resolve(1);
		assertEquals(3, targets.size());
		assertEquals("default", targets.get(0).context());
		assertEquals("1", targets.get(1).context());
		assertEquals("L1", targets.get(1).labelName());
		assertEquals("2", targets.get(2).context());
		assertEquals("L2", targets.get(2).labelName());
	}

	@Test
	void nonSwitchInstructionsHaveNoTargets() {
		ASTMethod method = parse();
		ASTInstruction ret = method.getCode().getInstructions().getLast();
		assertTrue(AssemblyUtils.resolveSwitchTargets(JvmTargetContext.INSTANCE, ret).isEmpty());
	}

	private static List<SwitchTarget> resolve(int index) {
		ASTInstruction instruction = parse().getCode().getInstructions().get(index);
		return AssemblyUtils.resolveSwitchTargets(JvmTargetContext.INSTANCE, instruction);
	}

	private static ASTMethod parse() {
		List<ASTElement> ast = AssemblyParseFixture.processAst(AssemblyParseFixture.STDIN, SOURCE,
				JvmTargetContext.INSTANCE).requireValue();
		return (ASTMethod) ast.getFirst();
	}
}
