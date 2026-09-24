package me.darknet.assembler;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.AnnotationVisibility;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.primitive.ASTString;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.TokenType;
import me.darknet.assembler.util.ElementMap;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Range;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import me.darknet.assembler.visitor.ASTMethodVisitor;
import me.darknet.assembler.visitor.Modifiers;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ASTMethodVisitorDispatchTest {
    @Test
    void dispatchesAnnotationVisibilityThroughUnifiedCallbacks() {
        ASTIdentifier first = identifier("first", 10);
        ASTIdentifier second = identifier("second", 20);
        Map<ASTIdentifier, List<ASTAnnotation>> parameterAnnotations = new IdentityHashMap<>();
        parameterAnnotations.put(first, List.of(
                annotation(AnnotationVisibility.VISIBLE, "parameter/Visible", 30),
                annotation(AnnotationVisibility.INVISIBLE, "parameter/Invisible", 40)
        ));
        parameterAnnotations.put(second, List.of(
                annotation(AnnotationVisibility.VISIBLE, "parameter/Second", 50)
        ));

        Modifiers modifiers = new Modifiers();
        modifiers.addModifier(identifier("static", 0));
        ASTMethod method = new ASTMethod(
                modifiers,
                identifier("test", 60),
                identifier("(II)V", 65),
                List.of(first, second),
                parameterAnnotations,
                List.of(),
                null,
                List.of(),
                null,
                List.of(),
                BytecodeFormat.JVM
        );
        method.addVisibleAnnotation(annotation(AnnotationVisibility.VISIBLE, "method/Visible", 80));
        method.addVisibleAnnotation(annotation(AnnotationVisibility.SYSTEM, "method/System", 90));
        method.addInvisibleAnnotation(annotation(AnnotationVisibility.INVISIBLE, "method/Invisible", 100));
        method.addVisibleTypeAnnotation(typeAnnotation(AnnotationVisibility.VISIBLE, "method/VisibleType", 110));
        method.addInvisibleTypeAnnotation(typeAnnotation(AnnotationVisibility.INVISIBLE, "method/InvisibleType", 130));

        RecordingVisitor visitor = new RecordingVisitor();
        method.accept(new ErrorCollector(), visitor);

        assertEquals(List.of(
                AnnotationVisibility.VISIBLE,
                AnnotationVisibility.SYSTEM,
                AnnotationVisibility.INVISIBLE
        ), visitor.annotationVisibilities);
        assertEquals(List.of(AnnotationVisibility.VISIBLE, AnnotationVisibility.INVISIBLE), visitor.typeVisibilities);
        assertEquals(Map.of(
                0, List.of(AnnotationVisibility.VISIBLE, AnnotationVisibility.INVISIBLE),
                1, List.of(AnnotationVisibility.VISIBLE)
        ), visitor.parameterVisibilities);
        assertEquals(1, visitor.endCount);
    }

    private static ASTAnnotation annotation(AnnotationVisibility visibility, String classType, int start) {
        return new ASTAnnotation(visibility, identifier(classType, start), new ElementMap<>());
    }

    private static ASTAnnotation typeAnnotation(AnnotationVisibility visibility, String classType, int start) {
        return new ASTAnnotation(
                visibility,
                identifier(classType, start),
                new ElementMap<>(),
                number("0", start + 20),
                identifier("ROOT", start + 22)
        );
    }

    private static ASTIdentifier identifier(String content, int start) {
        return new ASTIdentifier(token(TokenType.IDENTIFIER, content, start));
    }

    private static ASTNumber number(String content, int start) {
        return new ASTNumber(token(TokenType.NUMBER, content, start));
    }

    private static Token token(TokenType type, String content, int start) {
        return new Token(
                new Range(start, start + content.length() - 1),
                new Location(1, start + 1, content.length(), "ASTMethodVisitorDispatchTest"),
                type,
                content
        );
    }

    private static final class RecordingVisitor implements ASTMethodVisitor {
        private final List<AnnotationVisibility> annotationVisibilities = new ArrayList<>();
        private final List<AnnotationVisibility> typeVisibilities = new ArrayList<>();
        private final Map<Integer, List<AnnotationVisibility>> parameterVisibilities = new HashMap<>();
        private int endCount;

        @Override
        public ASTAnnotationVisitor visitAnnotation(AnnotationVisibility visibility, ASTIdentifier classType) {
            annotationVisibilities.add(visibility);
            return null;
        }

        @Override
        public ASTAnnotationVisitor visitTypeAnnotation(AnnotationVisibility visibility, ASTIdentifier classType,
                                                        ASTNumber typeRef, ASTIdentifier typePath) {
            typeVisibilities.add(visibility);
            return null;
        }

        @Override
        public ASTAnnotationVisitor visitParameterAnnotation(AnnotationVisibility visibility, int index,
                                                             ASTIdentifier classType) {
            parameterVisibilities.computeIfAbsent(index, ignored -> new ArrayList<>()).add(visibility);
            return null;
        }

        @Override
        public void visitParameter(int index, ASTIdentifier name) {}

        @Override
        public void visitAnnotationDefaultValue(ASTElement defaultValue) {}

        @Override
        public void visitSignature(ASTString signature) {}

        @Override
        public void visitEnd() {
            endCount++;
        }
    }
}
