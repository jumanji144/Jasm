package me.darknet.assembler;

import dev.xdark.blw.code.Instruction;
import dev.xdark.blw.code.JavaOpcodes;
import dev.xdark.blw.code.instruction.FieldInstruction;
import dev.xdark.blw.code.instruction.InstanceofInstruction;
import dev.xdark.blw.code.instruction.MethodInstruction;
import dev.xdark.blw.code.instruction.VariableIncrementInstruction;
import dev.xdark.blw.simulation.ExecutionEngines;
import dev.xdark.blw.type.MethodType;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.compile.analysis.BasicFieldValueLookup;
import me.darknet.assembler.compile.analysis.BasicMethodValueLookup;
import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compile.analysis.MethodAnalysisResult;
import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import me.darknet.assembler.compile.analysis.VarCache;
import me.darknet.assembler.compile.analysis.frame.Frame;
import me.darknet.assembler.compile.analysis.frame.TypedFrame;
import me.darknet.assembler.compile.analysis.frame.TypedFrameImpl;
import me.darknet.assembler.compile.analysis.frame.ValuedFrameImpl;
import me.darknet.assembler.compile.analysis.jvm.AnalysisSession;
import me.darknet.assembler.compile.analysis.jvm.JvmAnalysisEngine;
import me.darknet.assembler.compile.analysis.jvm.ValuedJvmAnalysisEngine;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.TokenType;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Range;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JvmAnalysisEngineExecutionTest {

    @Test
    void typedEngineMergesFramesThroughSessionPath() throws Exception {
        MethodAnalysisResult result = new MethodAnalysisResult();
        AnalysisSession<TypedFrame> session = new AnalysisSession<>(result);

        session.putFrame(10, new TypedFrameImpl(Map.of(
                0, new Local(0, "value", Types.instanceType(ArrayList.class))
        )));

        boolean changed = session.putAndMergeFrame(
                new TestJvmCompilerOptions().inheritanceChecker(),
                10,
                new TypedFrameImpl(Map.of(
                        0, new Local(0, "value", Types.instanceType(LinkedList.class))
                ))
        );

        assertTrue(changed);
        assertEquals(Types.instanceType(Object.class), session.getFrame(10).getLocalType(0));
    }

    @Test
    void valuedEngineFallsBackToUnknownReturnTypesWhenLookupCannotResolveValue() throws Exception {
        ValuedJvmAnalysisEngine engine = new ValuedJvmAnalysisEngine(new VarCache());
        engine.setMethodValueLookup(new BasicMethodValueLookup());

        ValuedFrameImpl frame = new ValuedFrameImpl();
        frame.push(Values.STRING_VALUE);
        ValuedFrameImpl output = executeWithFrame(engine, frame, new MethodInstruction(
                JavaOpcodes.INVOKESTATIC,
                Types.instanceType(Long.class),
                "parseLong",
                Types.methodType("(Ljava/lang/String;)J"),
                false
        ), Types.methodType("()V"), null);

        assertSame(Values.LONG_VALUE, output.pop(Types.LONG));
    }

    @Test
    void valuedEngineUsesFieldLookupForStaticReads() throws Exception {
        ValuedJvmAnalysisEngine engine = new ValuedJvmAnalysisEngine(new VarCache());
        engine.setFieldValueLookup(new BasicFieldValueLookup());

        ValuedFrameImpl output = executeWithFrame(engine, new ValuedFrameImpl(), new FieldInstruction(
                JavaOpcodes.GETSTATIC,
                Types.instanceType(Integer.class),
                "MAX_VALUE",
                Types.INT
        ), Types.methodType("()V"), null);

        Value.KnownIntValue value = assertInstanceOf(Value.KnownIntValue.class, output.pop());
        assertEquals(Integer.MAX_VALUE, value.value());
    }

    @Test
    void valuedEngineUsesInheritanceCheckerForKnownInstanceof() throws Exception {
        ValuedJvmAnalysisEngine engine = new ValuedJvmAnalysisEngine(new VarCache());
        engine.setChecker(new InheritanceChecker() {
            @Override
            public boolean isSubclassOf(String child, String parent) {
                return child.equals("java/util/ArrayList") && parent.equals("java/util/List");
            }

            @Override
            public String getCommonSuperclass(String type1, String type2) {
                return "java/lang/Object";
            }
        });

        ValuedFrameImpl frame = new ValuedFrameImpl();
        frame.push(Values.valueOfInstance(Types.instanceType(ArrayList.class)));
        ValuedFrameImpl output = executeWithFrame(engine, frame,
                new InstanceofInstruction(Types.instanceType(List.class)),
                Types.methodType("()V"), null);

        Value.KnownIntValue value = assertInstanceOf(Value.KnownIntValue.class, output.pop());
        assertEquals(1, value.value());
    }

    @Test
    void valuedEngineReportsInvalidIincTargets() throws Exception {
        VarCache varCache = new VarCache();
        ValuedJvmAnalysisEngine engine = new ValuedJvmAnalysisEngine(varCache);
        ErrorCollector collector = new ErrorCollector();
        engine.setErrorCollector(collector);

        VariableIncrementInstruction instruction = new VariableIncrementInstruction(7, 3);
        executeWithFrame(engine, new ValuedFrameImpl(), instruction, Types.methodType("()V"), astInstruction("iinc"));

        assertEquals(1, collector.getErrors().size());
        assertTrue(collector.getErrors().getFirst().getMessage().contains("Invalid iinc target"));
    }

    @SuppressWarnings("unchecked")
    private static <F extends Frame> F executeWithFrame(
            JvmAnalysisEngine<?> engine,
            F initialFrame,
            Instruction instruction,
            MethodType methodType,
            ASTInstruction astInstruction) {
        MethodAnalysisResult result = new MethodAnalysisResult();
        if (astInstruction != null) {
            result.recordInstructionMapping(astInstruction, instruction);
        }

	    AnalysisSession<Frame> session = new AnalysisSession<>(result);
        session.putFrame(0, initialFrame);
        engine.setResult(result);
        JvmAnalysisEngine<Frame> boundEngine =
                (JvmAnalysisEngine<Frame>) engine;
        boundEngine.bindSession(session);
        try {
            boundEngine.clearErrorsAt(instruction);
            session.setActiveFrame(0, initialFrame);
            ExecutionEngines.execute(boundEngine, instruction);
            return (F) session.frame();
        } finally {
            boundEngine.bindSession(null);
        }
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
