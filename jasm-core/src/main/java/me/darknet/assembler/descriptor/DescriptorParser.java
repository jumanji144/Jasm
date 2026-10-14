package me.darknet.assembler.descriptor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses JVM and Dalvik field, method, and internal-name descriptors.
 */
public final class DescriptorParser {
    private DescriptorParser() {}

    /**
     * @param source
     * 		Candidate descriptor text.
     * @param form
     * 		Shape the caller needs.
     *
     * @return Parsed descriptor, which is a {@link MethodDescriptor} for {@link DescriptorForm#METHOD}
     * 		and a {@link DescriptorType} for the other forms.
     *
     * @throws DescriptorSyntaxException
     * 		If the text does not satisfy {@code form}.
     */
    public static @NotNull Descriptor parse(@NotNull String source, @NotNull DescriptorForm form)
            throws DescriptorSyntaxException {
        return switch (form) {
            case FIELD -> parseFieldDescriptor(source);
            case METHOD -> parseMethodDescriptor(source);
            case ANY -> parseAny(source);
            case CLASS_OR_ARRAY -> requireReference(parseFieldDescriptor(source), source);
            case TYPE_REFERENCE -> parseTypeReference(source);
            case CLASS_TYPE -> requireClass(parseTypeReference(source), source);
            case INTERNAL_NAME -> parseInternalName(source);
        };
    }

    /**
     * @param source
     * 		Candidate text, either a value type or a method signature.
     *
     * @return Parsed descriptor, decided by a leading {@code '('}.
     *
     * @throws DescriptorSyntaxException
     * 		If the text is neither form.
     */
    public static @NotNull Descriptor parseAny(@NotNull String source) throws DescriptorSyntaxException {
        if (!source.isEmpty() && source.charAt(0) == '(') {
            return parseMethodDescriptor(source);
        }
        return parseFieldDescriptor(source);
    }

    /**
     * @param source
     * 		Candidate single-value type descriptor, void excluded.
     *
     * @return Parsed type.
     *
     * @throws DescriptorSyntaxException
     * 		If the text is not a well-formed type descriptor, or is {@code V}.
     */
    public static @NotNull DescriptorType parseFieldDescriptor(@NotNull String source) throws DescriptorSyntaxException {
        Cursor cursor = new Cursor(source);
        DescriptorType type = parseType(cursor, source, false);
        requireEnd(cursor, source);
        return type;
    }

    /**
     * @param source
     * 		Candidate method descriptor.
     *
     * @return Parsed signature.
     *
     * @throws DescriptorSyntaxException
     * 		If the text is not a well-formed method descriptor.
     */
    public static @NotNull MethodDescriptor parseMethodDescriptor(@NotNull String source) throws DescriptorSyntaxException {
        Cursor cursor = new Cursor(source);
        if (cursor.atEnd() || cursor.peek() != '(') {
            throw new DescriptorSyntaxException(source, cursor.offset, "method descriptor must start with '('");
        }
        cursor.advance();

        List<DescriptorType> parameters = new ArrayList<>();
        while (true) {
            if (cursor.atEnd()) {
                throw new DescriptorSyntaxException(source, cursor.offset, "missing ')'");
            }
            if (cursor.peek() == ')') {
                cursor.advance();
                break;
            }
            parameters.add(parseType(cursor, source, false));
        }

        DescriptorType returnType = parseType(cursor, source, true);
        requireEnd(cursor, source);
        return new MethodDescriptor(parameters, returnType);
    }

    /**
     * @param source
     * 		Candidate internal name.
     *
     * @return Parsed class type.
     *
     * @throws DescriptorSyntaxException
     * 		If the text is not a well-formed internal name.
     */
    public static @NotNull ClassDescriptor parseInternalName(@NotNull String source) throws DescriptorSyntaxException {
        if (source.isEmpty()) {
            throw new DescriptorSyntaxException(source, -1, "missing class name");
        }
        for (int i = 0; i < source.length(); i++) {
            if (isInvalidInternalNameChar(source.charAt(i))) {
                throw new DescriptorSyntaxException(
                        source, i, "character '" + source.charAt(i) + "' is not allowed in a class name");
            }
        }
        return new ClassDescriptor(source);
    }

    /**
     * @param source
     * 		Candidate type text written as a descriptor or as an internal name.
     *
     * @return Parsed type.
     *
     * @throws DescriptorSyntaxException
     * 		If the text is a method signature, or is not a well-formed type in either spelling.
     */
    public static @NotNull DescriptorType parseTypeReference(@NotNull String source) throws DescriptorSyntaxException {
        if (!source.isEmpty() && source.charAt(0) == '(') {
            throw new DescriptorSyntaxException(source, -1, "method descriptor is not a value type");
        }

        // Ambiguity is resolved the way the JVM resolves it: a leading '[' starts an array descriptor,
        // and a text that starts with 'L' and ends with ';' is a class descriptor. Everything else is
        // an internal name, so 'Lbad' names the class 'Lbad' rather than the class 'bad'.
        boolean descriptorShaped = !source.isEmpty()
                && (source.charAt(0) == '[' || (source.charAt(0) == 'L' && source.charAt(source.length() - 1) == ';'));
        if (descriptorShaped) {
            return parseFieldDescriptor(source);
        }

        // Scanned here rather than delegated to the model's invariant check, so that an invalid name is
        // reported at its own offset instead of surfacing as a bare IllegalArgumentException.
        for (int i = 0; i < source.length(); i++) {
            if (isInvalidInternalNameChar(source.charAt(i))) {
                throw new DescriptorSyntaxException(
                        source, i, "character '" + source.charAt(i) + "' is not allowed in a class name");
            }
        }
        return new ClassDescriptor(source);
    }

    /**
     * @param source
     * 		Candidate descriptor text.
     *
     * @return Parsed descriptor, or {@code null} when the text does not satisfy {@code form}.
     */
    public static @Nullable Descriptor tryParse(@NotNull String source, @NotNull DescriptorForm form) {
        try {
            return parse(source, form);
        } catch (DescriptorSyntaxException exception) {
            return null;
        }
    }

    /**
     * @param source
     * 		Candidate descriptor text.
     *
     * @return {@code true} when the text is a well-formed single-value type descriptor.
     */
    public static boolean isValidFieldDescriptor(@NotNull String source) {
        return tryParse(source, DescriptorForm.FIELD) != null;
    }

    /**
     * @param source
     * 		Candidate descriptor text.
     *
     * @return {@code true} when the text is a well-formed method descriptor.
     */
    public static boolean isValidMethodDescriptor(@NotNull String source) {
        return tryParse(source, DescriptorForm.METHOD) != null;
    }

    /**
     * Checks that a name is usable as the body of a class descriptor or as an internal name.
     *
     * <p>
     * Internal names cannot contain {@code ;}, {@code [}, {@code (}, {@code )}, {@code .}, or whitespace.
     * Those characters therefore identify malformed descriptor-shaped input.
     *
     * @param internalName
     * 		Candidate internal name, without a surrounding {@code L} and {@code ;}.
     *
     * @throws IllegalArgumentException
     * 		If the name is empty or carries a character that cannot appear in an internal name.
     */
    public static void requireValidInternalName(@NotNull String internalName) {
        if (internalName.isEmpty()) {
            throw new IllegalArgumentException("Internal name must not be empty");
        }
        for (int i = 0; i < internalName.length(); i++) {
            char character = internalName.charAt(i);
            if (isInvalidInternalNameChar(character)) {
                throw new IllegalArgumentException(
                        "Character '" + character + "' is not allowed in internal name '" + internalName + "'"
                );
            }
        }
    }

    /**
     * @param character
     * 		Character to test.
     *
     * @return {@code true} when the character cannot appear in an internal name.
     */
    public static boolean isInvalidInternalNameChar(char character) {
        return character == ';' || character == '[' || character == '(' || character == ')'
                || character == '.' || Character.isWhitespace(character);
    }

    /**
     * @param type
     * 		Parsed type.
     * @param source
     * 		Full descriptor text, used in the failure message.
     *
     * @return The same type, having confirmed it names a class or array.
     *
     * @throws DescriptorSyntaxException
     * 		If the type is a primitive.
     */
    private static @NotNull DescriptorType requireReference(@NotNull DescriptorType type, @NotNull String source)
            throws DescriptorSyntaxException {
        if (type instanceof PrimitiveType) {
            throw new DescriptorSyntaxException(source, -1, "expected a class or array type, not a primitive");
        }
        return type;
    }

    /**
     * @param type
     * 		Parsed type.
     * @param source
     * 		Full descriptor text, used in the failure message.
     *
     * @return The same type, having confirmed it names a class.
     *
     * @throws DescriptorSyntaxException
     * 		If the type is an array or a primitive.
     */
    private static @NotNull DescriptorType requireClass(@NotNull DescriptorType type, @NotNull String source)
            throws DescriptorSyntaxException {
        if (type instanceof ArrayDescriptor) {
            throw new DescriptorSyntaxException(source, -1, "expected a class type, not an array");
        }
        if (type instanceof PrimitiveType) {
            throw new DescriptorSyntaxException(source, -1, "expected a class type, not a primitive");
        }
        return type;
    }

    /**
     * @param cursor
     * 		Offset positioned at a type.
     * @param source
     * 		Full descriptor text, used in the failure message.
     * @param allowVoid
     * 		Whether {@code V} may appear at this position.
     *
     * @return Parsed type.
     *
     * @throws DescriptorSyntaxException
     * 		If no valid type starts at the cursor.
     */
    private static @NotNull DescriptorType parseType(@NotNull Cursor cursor, @NotNull String source, boolean allowVoid)
            throws DescriptorSyntaxException {
        if (cursor.atEnd()) {
            throw new DescriptorSyntaxException(source, cursor.offset, "missing type descriptor");
        }

        char character = cursor.peek();
        if (character == '[') {
            cursor.advance();
            // An array of void is not a type, so the component is parsed without the void allowance.
            return new ArrayDescriptor(parseType(cursor, source, false));
        }

        PrimitiveType primitive = PrimitiveType.fromDescriptor(character);
        if (primitive != null) {
            cursor.advance();
            if (primitive.isVoid() && !allowVoid) {
                throw new DescriptorSyntaxException(
                        source, cursor.offset - 1, "void is only valid as a method return type");
            }
            return primitive;
        }

        if (character == 'L') {
            int nameStart = cursor.offset + 1;
            int terminator = source.indexOf(';', nameStart);
            if (terminator < 0) {
                throw new DescriptorSyntaxException(source, source.length(), "missing terminating ';'");
            }
            String internalName = source.substring(nameStart, terminator);
            if (internalName.isEmpty()) {
                throw new DescriptorSyntaxException(source, nameStart, "class descriptor has no class name");
            }
            for (int i = 0; i < internalName.length(); i++) {
                if (isInvalidInternalNameChar(internalName.charAt(i))) {
                    throw new DescriptorSyntaxException(
                            source, nameStart + i,
                            "character '" + internalName.charAt(i) + "' is not allowed in a class name");
                }
            }
            cursor.offset = terminator + 1;
            return new ClassDescriptor(internalName);
        }

        throw new DescriptorSyntaxException(
                source, cursor.offset, "unknown type descriptor character '" + character + "'");
    }

    /**
     * @param cursor
     * 		Cursor that must be positioned at the end of the descriptor.
     * @param source
     * 		Full descriptor text, used in the failure message.
     *
     * @throws DescriptorSyntaxException
     * 		If unconsumed text remains.
     */
    private static void requireEnd(@NotNull Cursor cursor, @NotNull String source) throws DescriptorSyntaxException {
        if (!cursor.atEnd()) {
            throw new DescriptorSyntaxException(
                    source, cursor.offset, "unexpected trailing '" + cursor.peek() + "'");
        }
    }

    /**
     * Mutable offset into the descriptor being parsed.
     */
    private static final class Cursor {
        private final String source;
        private int offset;

        private Cursor(String source) {
            this.source = source;
        }

        private boolean atEnd() {
            return offset >= source.length();
        }

        private char peek() {
            return source.charAt(offset);
        }

        private void advance() {
            offset++;
        }
    }
}
