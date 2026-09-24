package me.darknet.assembler;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.instructions.DefaultOperands;
import me.darknet.assembler.instructions.HandleOperands;
import me.darknet.assembler.instructions.MemberPath;
import me.darknet.assembler.instructions.OperandValues;
import me.darknet.assembler.instructions.PayloadSchema;
import me.darknet.assembler.instructions.SwitchKey;
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.TokenType;
import me.darknet.assembler.parser.processor.DeclarationRegistry;
import me.darknet.assembler.parser.processor.ProcessorContext;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Range;
import me.darknet.assembler.test.AssemblyParseFixture;
import me.darknet.assembler.test.DiagnosticAssertions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.function.Consumer;

public class InstructionsTest {

    public static void assertCode(String[] instructions, BytecodeFormat format, Consumer<ASTCode> consumer) {
        assertOne(
                ".method stub ()V {\n" + "code: {" + String.join("\n", instructions) + "\n}}\n", format,
                ASTMethod.class, (method) -> {
                    assertEquals("stub", method.getName().content());
                    assertEquals("()V", method.getDescriptor().content());
                    assertNotNull(method.getCode());
                    consumer.accept(method.getCode());
                }
        );
    }

    public static <T extends ASTElement> void assertOne(String input, BytecodeFormat format, Class<T> clazz,
            Consumer<T> consumer) {
        List<ASTElement> results = DiagnosticAssertions.requireOk(
                AssemblyParseFixture.processAst("<stdin>", input, format),
                "Failed to process instruction input"
        );
        assertEquals(1, results.size());
        ASTElement element = results.getFirst();
        assertNotNull(element);
        assertInstanceOf(clazz, element);
        consumer.accept((T) element);
    }

    @Test
    public void testJvmBytecode() {
        assertCode(
                new String[] { "ldc \"Hello World\"", "getstatic java/lang/System.out Ljava/io/PrintStream;", "swap",
                        "invokevirtual java/io/PrintStream.println (Ljava/lang/String;)V", "return" },
                BytecodeFormat.JVM, (code) -> {
                    List<ASTInstruction> instructions = code.getInstructions();
                    assertEquals(5, instructions.size());
                    assertEquals("ldc", instructions.get(0).identifier().content());
                    assertEquals("Hello World", instructions.get(0).arguments().getFirst().content());
                    assertEquals("getstatic", instructions.get(1).identifier().content());
                    assertEquals("java/lang/System.out", instructions.get(1).arguments().get(0).content());
                    assertEquals("Ljava/io/PrintStream;", instructions.get(1).arguments().get(1).content());
                    assertEquals("swap", instructions.get(2).identifier().content());
                    assertEquals("invokevirtual", instructions.get(3).identifier().content());
                    assertEquals("java/io/PrintStream.println", instructions.get(3).arguments().get(0).content());
                    assertEquals("(Ljava/lang/String;)V", instructions.get(3).arguments().get(1).content());
                    assertEquals("return", instructions.get(4).identifier().content());
                }
        );
    }

    @Test
    public void testInvokeDynamic() {
        assertCode(
                new String[] {
                        "invokedynamic foo (Ljava/lang/String;)V {invokestatic, me/darknet/assembler/InstructionsTest.bar, (Ljava/lang/String;)V} {}", },
                BytecodeFormat.JVM, (code) -> {
                }
        );
    }

    @Test
    public void testLabel() {
        assertCode(
                new String[] { "L1:", "getstatic java/lang/System.out Ljava/io/PrintStream;", "L2:",
                        "ldc \"Hello World\"", "L3:", "invokevirtual java/io/PrintStream.println (Ljava/lang/String;)V",
                        "return", "L4:" },
                BytecodeFormat.JVM, (code) -> {
                    List<ASTInstruction> instructions = code.getInstructions();
                    assertEquals(8, instructions.size());
                    assertEquals("L1", instructions.get(0).identifier().content());
                    assertEquals("getstatic", instructions.get(1).identifier().content());
                    assertEquals("L2", instructions.get(2).identifier().content());
                    assertEquals("ldc", instructions.get(3).identifier().content());
                    assertEquals("L3", instructions.get(4).identifier().content());
                    assertEquals("invokevirtual", instructions.get(5).identifier().content());
                    assertEquals("return", instructions.get(6).identifier().content());
                    assertEquals("L4", instructions.get(7).identifier().content());
                }
        );
    }

    @Test
    public void testLdc() {
        assertCode(new String[] { "ldc Ljava/lang/String;", }, BytecodeFormat.JVM, (code) -> {
            List<ASTInstruction> instructions = code.getInstructions();
            assertEquals(1, instructions.size());
            assertEquals("ldc", instructions.getFirst().identifier().content());
            assertEquals("Ljava/lang/String;", instructions.getFirst().arguments().getFirst().content());
        });
    }

    @Test
    public void testTableSwitch() {
        assertCode(
                new String[] { "tableswitch { min: 10," + "max: 20," + "default: L1," + "cases: {" + "L2," + "L4,"
                        + "L8" + "}" + "}", },
                BytecodeFormat.JVM, (code) -> {
                    List<ASTInstruction> instructions = code.getInstructions();
                    assertEquals(1, instructions.size());
                    assertEquals("tableswitch", instructions.getFirst().identifier().content());
                }
        );
    }

    @Test
    public void testLookupSwitch() {
        assertCode(
                new String[] { "lookupswitch {" + "0: L2," + "1: L4," + "2: L8," + "default: L10" + "}", },
                BytecodeFormat.JVM, (code) -> {
                    List<ASTInstruction> instructions = code.getInstructions();
                    assertEquals(1, instructions.size());
                    assertEquals("lookupswitch", instructions.getFirst().identifier().content());
                }
        );
    }

    @Test
    public void testWeirdStrings() {
        assertCode(
                new String[] { "ldc \":\"" }, BytecodeFormat.JVM, (code) -> {
                    List<ASTInstruction> instructions = code.getInstructions();
                    assertEquals(1, instructions.size());
                    assertEquals("ldc", instructions.getFirst().identifier().content());
                    assertEquals(":", instructions.getFirst().arguments().getFirst().content());
                }
        );
    }

    @Test
    public void memberPathsAndSwitchKeysResolveToTypedValues() {
        RecordingContext context = new RecordingContext();
        ASTIdentifier path = identifier("a/b$Outer.Inner.member");
        var memberOperand = DefaultOperands.MEMBER_PATH.getOperand();
        memberOperand.verify(context, path);
        assertEquals(new MemberPath("a/b$Outer.Inner", "member"), memberOperand.resolve(context, path));

        assertEquals(new SwitchKey(-1), OperandValues.switchKey(context, identifier("0xFFFFFFFF")));
        assertEquals(new SwitchKey(-2), OperandValues.switchKey(context, identifier("-0b10")));
        assertEquals(new SwitchKey(12), OperandValues.switchKey(context, identifier("+0xC")));
        assertTrue(context.messages.isEmpty());
    }

    @Test
    public void malformedTypedOperandsReportErrorsAndReturnNoValue() {
        RecordingContext context = new RecordingContext();
        var memberOperand = DefaultOperands.MEMBER_PATH.getOperand();
        ASTIdentifier malformedPath = identifier(".member");
        memberOperand.verify(context, malformedPath);
        assertNull(memberOperand.resolve(context, malformedPath));
        assertNull(OperandValues.switchKey(context, identifier("0x")));
        assertEquals(2, context.messages.size());
        assertTrue(context.messages.getFirst().contains("Expected member path in owner.name form"));
        assertTrue(context.messages.get(1).contains("Expected integer switch key"));
    }

    @Test
    public void payloadSchemasAcceptValidPayloadsAndRejectWrongShape() {
        PayloadSchema fixed = PayloadSchema.fixed("single-target",
                PayloadSchema.Field.value("target", ElementType.IDENTIFIER, "label"));
        RecordingContext fixedContext = new RecordingContext();
        assertNotNull(fixed.validate(fixedContext, object("{ target: L0 }")));
        assertTrue(fixedContext.messages.isEmpty());

        RecordingContext extraKeyContext = new RecordingContext();
        assertNull(fixed.validate(extraKeyContext, object("{ target: L0, extra: L1 }")));
        assertFalse(extraKeyContext.messages.isEmpty());

        PayloadSchema keyed = PayloadSchema.keyed("switch-payload",
                new PayloadSchema.CaseKeys(ElementType.IDENTIFIER, "label"),
                PayloadSchema.Field.value("default", ElementType.IDENTIFIER, "default label"));
        RecordingContext keyedContext = new RecordingContext();
        assertNotNull(keyed.validate(keyedContext, object("{ default: L0, 0: L1 }")));
        assertTrue(keyedContext.messages.isEmpty());

        RecordingContext malformedCaseContext = new RecordingContext();
        assertNull(keyed.validate(malformedCaseContext, object("{ default: L0, nope: L1 }")));
        assertFalse(malformedCaseContext.messages.isEmpty());

        RecordingContext malformedValueContext = new RecordingContext();
        assertNull(keyed.validate(malformedValueContext, object("{ default: L0, 0: 5 }")));
        assertFalse(malformedValueContext.messages.isEmpty());
    }

    @Test
    public void handleOperandChecksDescriptorFormAgainstHandleKind() {
        RecordingContext context = new RecordingContext();
        assertFalse(HandleOperands.verifyAndReport(context,
                new ASTArray(List.of(identifier("getfield"), identifier("Owner.value"), identifier("I")))));
        assertFalse(HandleOperands.verifyAndReport(context,
                new ASTArray(List.of(identifier("invokestatic"), identifier("Owner.call"), identifier("()V")))));
        assertFalse(HandleOperands.verifyAndReport(context, identifier("LambdaMetafactory.metafactory")));
        assertEquals("getfield", me.darknet.assembler.helper.Handle.KIND_NAMES
                .get(me.darknet.assembler.helper.Handle.Kind.GET_FIELD));

        assertTrue(HandleOperands.verifyAndReport(context,
                new ASTArray(List.of(identifier("getfield"), identifier("Owner.value"), identifier("()V")))));
        assertTrue(HandleOperands.verifyAndReport(context,
                new ASTArray(List.of(identifier("invokestatic"), identifier("Owner.call"), identifier("I")))));
        assertTrue(HandleOperands.verifyAndReport(context,
                new ASTArray(List.of(identifier("getfield"), identifier("Owner.value"), identifier("I"),
                        identifier("extra")))));
        assertEquals(3, context.messages.size());
    }

    @Test
    public void variableNamesRejectFractionalSlotIndices() {
        RecordingContext context = new RecordingContext();
        DefaultOperands.VARIABLE_NAME.getOperand().verify(context, number("1.5"));
        assertEquals(1, context.messages.size());
        assertTrue(context.messages.getFirst().contains("local variable name or index"));

        RecordingContext validContext = new RecordingContext();
        DefaultOperands.VARIABLE_NAME.getOperand().verify(validContext, identifier("local"));
        assertTrue(validContext.messages.isEmpty());
    }

    @Test
    public void numberOperandsAcceptSpecialValuesAndIntegerVariableSlots() {
        RecordingContext context = new RecordingContext();
        DefaultOperands.NUMBER.getOperand().verify(context, identifier("NaN"));
        DefaultOperands.NUMBER.getOperand().verify(context, identifier("-InfinityF"));
        DefaultOperands.VARIABLE_NAME.getOperand().verify(context, number("2"));
        assertTrue(context.messages.isEmpty());
    }

    private static ASTIdentifier identifier(String content) {
        return new ASTIdentifier(new Token(Range.EMPTY, Location.UNKNOWN, TokenType.IDENTIFIER, content));
    }

    private static ASTNumber number(String content) {
        return new ASTNumber(new Token(Range.EMPTY, Location.UNKNOWN, TokenType.NUMBER, content));
    }

    private static ASTObject object(String source) {
        List<ASTElement> elements = DiagnosticAssertions.requireOk(
                AssemblyParseFixture.parse(source), "Failed to parse payload object");
        assertEquals(1, elements.size());
        return assertInstanceOf(ASTObject.class, elements.getFirst());
    }

    private static final class RecordingContext extends ProcessorContext {
        private final List<String> messages = new java.util.ArrayList<>();

        private RecordingContext() {
            super(BytecodeFormat.JVM, DeclarationRegistry.createDefault());
        }

        @Override
        public void throwError(String message, Location location) {
            messages.add(message);
            super.throwError(message, location);
        }
    }

}
