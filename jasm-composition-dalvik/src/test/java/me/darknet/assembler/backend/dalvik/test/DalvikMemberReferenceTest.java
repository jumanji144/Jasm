package me.darknet.assembler.backend.dalvik.test;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.query.AssemblyUtils;
import me.darknet.assembler.query.MemberReference;
import me.darknet.assembler.test.AssemblyParseFixture;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies member reference resolution for Dalvik instructions.
 */
class DalvikMemberReferenceTest {
	private static final String SOURCE = """
			.method public static sample ()V {
			    registers: 4,
			    code: {
			        iget v0 v1 sample/Owner.field I
			        sput-object v2 sample/Owner.staticField Ljava/lang/String;
			        invoke-virtual { v0 } sample/Owner.call (I)V
			        invoke-polymorphic { v0 } java/lang/invoke/MethodHandle.invokeExact (I)V (I)V
			        return-void
			    }
			}
			""";

	@Test
	void fieldAndMethodReferencesResolveOwnerNameAndDescriptor() {
		List<ASTInstruction> instructions = instructions(SOURCE);

		MemberReference iget = resolve(instructions.getFirst());
		assertNotNull(iget);
		assertEquals("sample/Owner", iget.owner());
		assertEquals("field", iget.name());
		assertEquals("I", iget.descriptor());
		assertFalse(iget.isMethod());

		MemberReference sput = resolve(instructions.get(1));
		assertNotNull(sput);
		assertEquals("sample/Owner", sput.owner());
		assertEquals("staticField", sput.name());
		assertEquals("Ljava/lang/String;", sput.descriptor());
		assertFalse(sput.isMethod());

		MemberReference invoke = resolve(instructions.get(2));
		assertNotNull(invoke);
		assertEquals("sample/Owner", invoke.owner());
		assertEquals("call", invoke.name());
		assertEquals("(I)V", invoke.descriptor());
		assertTrue(invoke.isMethod());
	}

	@Test
	void signaturePolymorphicInvocationResolvesItsMemberNotItsCallSitePrototype() {
		MemberReference invoke = resolve(instructions(SOURCE).get(3));
		assertNotNull(invoke);
		assertEquals("java/lang/invoke/MethodHandle", invoke.owner());
		assertEquals("invokeExact", invoke.name());
		assertEquals("(I)V", invoke.descriptor());
		assertTrue(invoke.isMethod());
	}

	@Test
	void instructionsWithoutMemberPathsResolveToNull() {
		List<ASTInstruction> instructions = instructions(SOURCE);
		assertNull(resolve(instructions.get(4)), "return-void has no member reference");
	}

	private static MemberReference resolve(ASTInstruction instruction) {
		return AssemblyUtils.resolveMemberReference(DalvikTargetContext.INSTANCE, instruction);
	}

	private static List<ASTInstruction> instructions(String source) {
		List<ASTElement> ast = AssemblyParseFixture.processAst(AssemblyParseFixture.STDIN, source,
				DalvikTargetContext.INSTANCE).requireValue();
		ASTMethod method = (ASTMethod) ast.getFirst();
		return method.getCode().getInstructions();
	}
}
