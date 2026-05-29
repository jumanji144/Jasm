package me.darknet.assembler;

import dev.xdark.blw.code.JavaOpcodes;
import dev.xdark.blw.code.instruction.FieldInstruction;
import dev.xdark.blw.code.instruction.InstanceofInstruction;
import dev.xdark.blw.code.instruction.MethodInstruction;
import dev.xdark.blw.code.instruction.VariableIncrementInstruction;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.compile.analysis.BasicFieldValueLookup;
import me.darknet.assembler.compile.analysis.BasicMethodValueLookup;
import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import me.darknet.assembler.compile.analysis.VarCache;
import me.darknet.assembler.compile.analysis.frame.TypedFrameImpl;
import me.darknet.assembler.compile.analysis.frame.ValuedFrameImpl;
import me.darknet.assembler.compile.analysis.jvm.TypedJvmAnalysisEngine;
import me.darknet.assembler.compile.analysis.jvm.ValuedJvmAnalysisEngine;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.TokenType;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Range;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


class JvmAnalysisEngineExecutionTest {

    @Test
    void typedEngineMergesFramesThroughEnginePath() throws Exception {
        TypedJvmAnalysisEngine engine = new TypedJvmAnalysisEngine(new VarCache());

        // Put an arbitrary frame with an 'ArrayList' local.
        engine.putFrame(10, new TypedFrameImpl(Map.of(
                0, new Local(0, "value", Types.instanceType(java.util.ArrayList.class))
        )));

        // Merge a frame with an 'LinkedList' local at the same slot,
        // which should cause the types to merge to their common superclass, 'Object'.
        boolean changed = engine.putAndMergeFrame(
                new TestJvmCompilerOptions().inheritanceChecker(),
                10,
                new TypedFrameImpl(Map.of(
                        0, new Local(0, "value", Types.instanceType(java.util.LinkedList.class))
                ))
        );

        assertTrue(changed);
        assertEquals(Types.instanceType(Object.class), engine.getFrame(10).getLocalType(0));
    }

    @Test
    void valuedEngineFallsBackToUnknownReturnTypesWhenLookupCannotResolveValue() {
        ValuedJvmAnalysisEngine engine = new ValuedJvmAnalysisEngine(new VarCache());
        engine.setMethodValueLookup(new BasicMethodValueLookup());

        // Create unknown string value
        ValuedFrameImpl frame = new ValuedFrameImpl();
        frame.push(Values.STRING_VALUE);
        engine.setActiveFrame(0, frame);

        // Parse the unknown value as a long.
        // The lookup cannot operate on an unknown value, so the result will also be unknown.
        engine.execute(new MethodInstruction(
                JavaOpcodes.INVOKESTATIC,
                Types.instanceType(Long.class),
                "parseLong",
                Types.methodType("(Ljava/lang/String;)J"),
                false
        ));
        assertSame(Values.LONG_VALUE, frame.pop(Types.LONG));
    }

    @Test
    void valuedEngineUsesFieldLookupForStaticReads() {
        ValuedJvmAnalysisEngine engine = new ValuedJvmAnalysisEngine(new VarCache());
        engine.setFieldValueLookup(new BasicFieldValueLookup());

        ValuedFrameImpl frame = new ValuedFrameImpl();
        engine.setActiveFrame(0, frame);

        // Proper field lookup should give us a value with a known value
        engine.execute(new FieldInstruction(
                JavaOpcodes.GETSTATIC,
                Types.instanceType(Integer.class),
                "MAX_VALUE",
                Types.INT
        ));
        Value.KnownIntValue value = assertInstanceOf(Value.KnownIntValue.class, frame.pop());
        assertEquals(Integer.MAX_VALUE, value.value());
    }

    @Test
    void valuedEngineUsesInheritanceCheckerForKnownInstanceof() {
        ValuedJvmAnalysisEngine engine = new ValuedJvmAnalysisEngine(new VarCache());
        engine.setChecker(new InheritanceChecker() {
            @Override
            public boolean isSubclassOf(String child, String parent) {
                // Hacky impl but works for this test
                return child.equals("java/util/ArrayList") && parent.equals("java/util/List");
            }

            @Override
            public String getCommonSuperclass(String type1, String type2) {
                return "java/lang/Object";
            }
        });

        // Push an 'ArrayList' value
        ValuedFrameImpl frame = new ValuedFrameImpl();
        frame.push(Values.valueOfInstance(Types.instanceType(java.util.ArrayList.class)));
        engine.setActiveFrame(0, frame);

        // Check if it's an instance of 'List'.
        engine.execute(new InstanceofInstruction(Types.instanceType(java.util.List.class)));

        // Should be.
        Value.KnownIntValue value = assertInstanceOf(Value.KnownIntValue.class, frame.pop());
        assertEquals(1, value.value());
    }

    @Test
    void valuedEngineReportsInvalidIincTargets() {
        VarCache varCache = new VarCache();
        ValuedJvmAnalysisEngine engine = new ValuedJvmAnalysisEngine(varCache);
        ErrorCollector collector = new ErrorCollector();
        engine.setErrorCollector(collector);
        engine.setActiveFrame(0, new ValuedFrameImpl());

        // 'iinc' instructions should only be valid for local variables of type int,
        // and the target variable index must exist in the current frame.
        VariableIncrementInstruction instruction = new VariableIncrementInstruction(7, 3);
        engine.recordInstructionMapping(astInstruction("iinc"), instruction);
        engine.execute(instruction);

        assertEquals(1, collector.getErrors().size());
        assertTrue(collector.getErrors().getFirst().getMessage().contains("Invalid iinc target"));
    }

    private static ASTInstruction astInstruction(String opcode, String... args) {
        ASTIdentifier identifier = new ASTIdentifier(new Token(
                Range.EMPTY,
                new Location(1, 1, opcode.length(), "<test>"),
                TokenType.IDENTIFIER,
                opcode
        ));
        List<ASTIdentifier> arguments = java.util.Arrays.stream(args)
                .map(arg -> new ASTIdentifier(new Token(
                        Range.EMPTY,
                        new Location(1, 1, arg.length(), "<test>"),
                        TokenType.IDENTIFIER,
                        arg
                )))
                .toList();
        return new ASTInstruction(identifier, List.copyOf(arguments));
    }
}
