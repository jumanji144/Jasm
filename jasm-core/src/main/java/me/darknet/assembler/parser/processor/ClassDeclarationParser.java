package me.darknet.assembler.parser.processor;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTDeclaration;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.visitor.Modifiers;

import java.util.List;

/**
 * Parses class declarations.
 */
final class ClassDeclarationParser {
    private ClassDeclarationParser() {}
    /**
     * @param registry
     * 		Registry to register the parser in.
     */
    static void register(DeclarationRegistry registry) {
        registry.register("class", ClassDeclarationParser::parseClass);
    }

    /**
     * @param context Context to parse the declaration in.
     * @param declaration Declaration to parse.
     * @return Parsed class, or {@code null} if the declaration is invalid.
     */
    private static ASTClass parseClass(ProcessorContext context, ASTDeclaration declaration) {
        List<ASTElement> elements = declaration.elements().stream().toList();
        if (elements.size() < 2) {
            context.throwError("Expected class name and body", declaration.location());
            return null;
        }

        int bodyIndex = elements.size() - 1;
        ASTDeclaration body = context.validateEmptyableElement(
                context.declarationElement(declaration, bodyIndex), ElementType.DECLARATION, "class body", declaration
        );
        if (body == null)
            return null;

        int nameIndex = bodyIndex - 1;
        ASTIdentifier name = context.validateIdentifier(
                context.declarationElement(declaration, nameIndex), "class name", declaration
        );
        if (name == null)
            return null;

        Modifiers modifiers = ModifierParser.parseModifiers(context, nameIndex, declaration);
        List<ASTElement> classBody = context.parseDeclarations(
                body.elements().stream().toList(),
                "class member or member attribute",
                body.location(),
                "field",
                "method",
                "annotation",
                "visible-annotation",
                "invisible-annotation",
                "type-visible-annotation",
                "type-invisible-annotation",
                "signature",
                "deprecated"
        );

        return new ASTClass(modifiers, name, classBody)
                .accept(context.state().collectAttributes());
    }
}
