package me.darknet.assembler;

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
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

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
                0, new Local(0, "value", Type.getType(ArrayList.class))
        )));

        boolean changed = session.putAndMergeFrame(
                new TestJvmCompilerOptions().inheritanceChecker(),
                10,
                new TypedFrameImpl(Map.of(
                        0, new Local(0, "value", Type.getType(LinkedList.class))
                ))
        );

        assertTrue(changed);
        assertEquals(Type.getType(Object.class), session.getFrame(10).getLocalType(0));
    }

    @Test
    void valuedFrameTreatsTopStackValueAsStableMergeResult() throws Exception {
        ValuedFrameImpl frame = new ValuedFrameImpl();
        frame.pushRaw(Values.TOP_VALUE);

        ValuedFrameImpl other = new ValuedFrameImpl();
        other.pushRaw(Values.STRING_VALUE);

        InheritanceChecker checker = new TestJvmCompilerOptions().inheritanceChecker();
        assertFalse(frame.merge(checker, other));
        assertSame(Values.TOP_VALUE, frame.pop());
    }

    @Test
    void valuedEngineFallsBackToUnknownReturnTypesWhenLookupCannotResolveValue() throws Exception {
        ValuedJvmAnalysisEngine engine = new ValuedJvmAnalysisEngine(new VarCache());
        engine.setMethodValueLookup(new BasicMethodValueLookup());

        ValuedFrameImpl frame = new ValuedFrameImpl();
        frame.push(Values.STRING_VALUE);
        ValuedFrameImpl output = executeWithFrame(engine, frame, new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(Long.class),
                "parseLong",
                "(Ljava/lang/String;)J",
                false
        ), null);

        assertSame(Values.LONG_VALUE, output.pop(Type.LONG_TYPE));
    }

    @Test
    void valuedEngineUsesFieldLookupForStaticReads() throws Exception {
        ValuedJvmAnalysisEngine engine = new ValuedJvmAnalysisEngine(new VarCache());
        engine.setFieldValueLookup(new BasicFieldValueLookup());

        ValuedFrameImpl output = executeWithFrame(engine, new ValuedFrameImpl(), new FieldInsnNode(
                Opcodes.GETSTATIC,
                Type.getInternalName(Integer.class),
                "MAX_VALUE",
                Type.INT_TYPE.getDescriptor()
        ), null);

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
        frame.push(Values.valueOfInstance(Type.getType(ArrayList.class)));
        ValuedFrameImpl output = executeWithFrame(engine, frame,
                new TypeInsnNode(Opcodes.INSTANCEOF, Type.getInternalName(List.class)),
                null);

        Value.KnownIntValue value = assertInstanceOf(Value.KnownIntValue.class, output.pop());
        assertEquals(1, value.value());
    }

    @Test
    void valuedEngineReportsInvalidIincTargets() throws Exception {
        VarCache varCache = new VarCache();
        ValuedJvmAnalysisEngine engine = new ValuedJvmAnalysisEngine(varCache);
        ErrorCollector collector = new ErrorCollector();
        engine.setErrorCollector(collector);

        IincInsnNode instruction = new IincInsnNode(7, 3);
        executeWithFrame(engine, new ValuedFrameImpl(), instruction, astInstruction("iinc"));

        assertEquals(1, collector.getErrors().size());
        assertTrue(collector.getErrors().getFirst().getMessage().contains("Invalid iinc target"));
    }

    @SuppressWarnings("unchecked")
    private static <F extends Frame> F executeWithFrame(
            JvmAnalysisEngine<?> engine,
            F initialFrame,
            AbstractInsnNode instruction,
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
        boundEngine.setSession(session);
        try {
            boundEngine.clearErrorsAt(instruction);
            session.setActiveFrame(0, initialFrame);
            boundEngine.execute(instruction);
            return (F) session.frame();
        } finally {
            boundEngine.setSession(null);
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
