package me.darknet.assembler;

import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTDeclaration;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ASTProcessorAttributeTest {

    @Test
    void parsesArrayDefaultAnnotationValues() {
        ASTProcessorTest.assertOne(
                ".method public abstract array ()[I { parameters: { this }, default-value: { 0, 1, 2 } }",
                ASTMethod.class,
                method -> {
                    ASTArray value = assertInstanceOf(ASTArray.class, method.getAnnotationDefaultValue());
                    assertEquals(3, value.values().size());
                    assertEquals("0", value.values().get(0).content());
                    assertEquals("1", value.values().get(1).content());
                    assertEquals("2", value.values().get(2).content());
                }
        );
    }

    @Test
    void parsesNestedAnnotationDefaultValues() {
        ASTProcessorTest.assertOne(
                ".method public abstract subanno ()Ljava/lang/annotation/Retention; {" +
                        " parameters: { this }," +
                        " default-value: .annotation java/lang/annotation/Retention {" +
                        "  value: .enum java/lang/annotation/RetentionPolicy CLASS" +
                        " }" +
                        "}",
                ASTMethod.class,
                method -> {
                    ASTDeclaration value = assertInstanceOf(ASTDeclaration.class, method.getAnnotationDefaultValue());
                    assertEquals(".annotation", value.keyword().content());
                    assertEquals("java/lang/annotation/Retention", value.element(0).content());
                    assertNotNull(value.element(1));
                }
        );
    }
}
