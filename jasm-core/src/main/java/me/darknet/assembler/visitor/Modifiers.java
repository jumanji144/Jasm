package me.darknet.assembler.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Modifiers {

    private static final List<String> validModifiers = List.of(
            "public", "private", "protected", "static", "final", "abstract", "strictfp", "transient", "volatile",
            "synchronized", "native", "varargs", "bridge", "synthetic", "enum", "annotation", "module", "super",
            "interface", "record", "sealed", "open", "non-sealed", "constructor"
    );
    private final List<ASTIdentifier> modifiers = new ArrayList<>();

    public static boolean isValidModifier(String modifier) {
        return validModifiers.contains(modifier);
    }

    public void addModifier(ASTIdentifier modifier) {
        modifiers.add(modifier);
    }

    public boolean hasModifier(String modifier) {
        return modifiers.stream().anyMatch(i -> modifier.equals(i.content()));
    }

    public List<ASTIdentifier> modifiers() {
        return modifiers;
    }

    @Override
    public String toString() {
        return modifiers.stream().map(ASTIdentifier::literal).collect(Collectors.joining(", "));
    }
}
