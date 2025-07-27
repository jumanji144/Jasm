package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.code.instruction.FieldInstruction;
import me.darknet.assembler.compile.analysis.jvm.FieldValueLookup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Basic implementation of {@link FieldValueLookup} with some common fields implemented.
 */
public class BasicFieldValueLookup implements FieldValueLookup {
    protected static final Map<String, Value> CONST_FIELDS = new HashMap<>();

    @Override
    public @Nullable Value accept(@NotNull FieldInstruction instruction, Value.@Nullable ObjectValue context) {
        if (context == null) {
            String field = instruction.owner().internalName() + "." + instruction.name();
            return CONST_FIELDS.get(field);
        }
        return null;
    }

    static {
        CONST_FIELDS.put("java/lang/Byte.BYTES", Values.valueOf(Byte.BYTES));
        CONST_FIELDS.put("java/lang/Byte.SIZE", Values.valueOf(Byte.SIZE));
        CONST_FIELDS.put("java/lang/Byte.MIN_VALUE", Values.valueOf(Byte.MIN_VALUE));
        CONST_FIELDS.put("java/lang/Byte.MAX_VALUE", Values.valueOf(Byte.MAX_VALUE));
        //
        CONST_FIELDS.put("java/lang/Short.BYTES", Values.valueOf(Short.BYTES));
        CONST_FIELDS.put("java/lang/Short.SIZE", Values.valueOf(Short.SIZE));
        CONST_FIELDS.put("java/lang/Short.MIN_VALUE", Values.valueOf(Short.MIN_VALUE));
        CONST_FIELDS.put("java/lang/Short.MAX_VALUE", Values.valueOf(Short.MAX_VALUE));
        //
        CONST_FIELDS.put("java/lang/Integer.BYTES", Values.valueOf(Integer.BYTES));
        CONST_FIELDS.put("java/lang/Integer.SIZE", Values.valueOf(Integer.SIZE));
        CONST_FIELDS.put("java/lang/Integer.MIN_VALUE", Values.valueOf(Integer.MIN_VALUE));
        CONST_FIELDS.put("java/lang/Integer.MAX_VALUE", Values.valueOf(Integer.MAX_VALUE));
        //
        CONST_FIELDS.put("java/lang/Long.BYTES", Values.valueOf(Long.BYTES));
        CONST_FIELDS.put("java/lang/Long.SIZE", Values.valueOf(Long.SIZE));
        CONST_FIELDS.put("java/lang/Long.MIN_VALUE", Values.valueOf(Long.MIN_VALUE));
        CONST_FIELDS.put("java/lang/Long.MAX_VALUE", Values.valueOf(Long.MAX_VALUE));
        //
        CONST_FIELDS.put("java/lang/Float.BYTES", Values.valueOf(Float.BYTES));
        CONST_FIELDS.put("java/lang/Float.SIZE", Values.valueOf(Float.SIZE));
        CONST_FIELDS.put("java/lang/Float.MIN_VALUE", Values.valueOf(Float.MIN_VALUE));
        CONST_FIELDS.put("java/lang/Float.MAX_VALUE", Values.valueOf(Float.MAX_VALUE));
        CONST_FIELDS.put("java/lang/Float.MIN_EXPONENT", Values.valueOf(Float.MIN_EXPONENT));
        CONST_FIELDS.put("java/lang/Float.MAX_EXPONENT", Values.valueOf(Float.MAX_EXPONENT));
        CONST_FIELDS.put("java/lang/Float.MIN_NORMAL", Values.valueOf(Float.MIN_NORMAL));
        CONST_FIELDS.put("java/lang/Float.NaN", Values.valueOf(Float.NaN));
        CONST_FIELDS.put("java/lang/Float.NEGATIVE_INFINITY", Values.valueOf(Float.NEGATIVE_INFINITY));
        CONST_FIELDS.put("java/lang/Float.POSITIVE_INFINITY", Values.valueOf(Float.POSITIVE_INFINITY));
        //
        CONST_FIELDS.put("java/lang/Double.BYTES", Values.valueOf(Double.BYTES));
        CONST_FIELDS.put("java/lang/Double.SIZE", Values.valueOf(Double.SIZE));
        CONST_FIELDS.put("java/lang/Double.MIN_VALUE", Values.valueOf(Double.MIN_VALUE));
        CONST_FIELDS.put("java/lang/Double.MAX_VALUE", Values.valueOf(Double.MAX_VALUE));
        CONST_FIELDS.put("java/lang/Double.MIN_EXPONENT", Values.valueOf(Double.MIN_EXPONENT));
        CONST_FIELDS.put("java/lang/Double.MAX_EXPONENT", Values.valueOf(Double.MAX_EXPONENT));
        CONST_FIELDS.put("java/lang/Double.MIN_NORMAL", Values.valueOf(Double.MIN_NORMAL));
        CONST_FIELDS.put("java/lang/Double.NaN", Values.valueOf(Double.NaN));
        CONST_FIELDS.put("java/lang/Double.NEGATIVE_INFINITY", Values.valueOf(Double.NEGATIVE_INFINITY));
        CONST_FIELDS.put("java/lang/Double.POSITIVE_INFINITY", Values.valueOf(Double.POSITIVE_INFINITY));
        //
        CONST_FIELDS.put("java/lang/Character.BYTES", Values.valueOf(Character.BYTES));
        CONST_FIELDS.put("java/lang/Character.SIZE", Values.valueOf(Character.SIZE));
        CONST_FIELDS.put("java/lang/Character.MIN_RADIX", Values.valueOf(Character.MIN_RADIX));
        CONST_FIELDS.put("java/lang/Character.MAX_RADIX", Values.valueOf(Character.MAX_RADIX));
        CONST_FIELDS.put("java/lang/Character.MIN_VALUE", Values.valueOf(Character.MIN_VALUE));
        CONST_FIELDS.put("java/lang/Character.MAX_VALUE", Values.valueOf(Character.MAX_VALUE));
        CONST_FIELDS.put("java/lang/Character.UNASSIGNED", Values.valueOf(Character.UNASSIGNED));
        //
        CONST_FIELDS.put("java/lang/Character.UPPERCASE_LETTER", Values.valueOf(Character.UPPERCASE_LETTER));
        CONST_FIELDS.put("java/lang/Character.LOWERCASE_LETTER", Values.valueOf(Character.LOWERCASE_LETTER));
        CONST_FIELDS.put("java/lang/Character.TITLECASE_LETTER", Values.valueOf(Character.TITLECASE_LETTER));
        CONST_FIELDS.put("java/lang/Character.MODIFIER_LETTER", Values.valueOf(Character.MODIFIER_LETTER));
        CONST_FIELDS.put("java/lang/Character.OTHER_LETTER", Values.valueOf(Character.OTHER_LETTER));
        CONST_FIELDS.put("java/lang/Character.NON_SPACING_MARK", Values.valueOf(Character.NON_SPACING_MARK));
        CONST_FIELDS.put("java/lang/Character.ENCLOSING_MARK", Values.valueOf(Character.ENCLOSING_MARK));
        CONST_FIELDS.put("java/lang/Character.COMBINING_SPACING_MARK", Values.valueOf(Character.COMBINING_SPACING_MARK));
        CONST_FIELDS.put("java/lang/Character.DECIMAL_DIGIT_NUMBER", Values.valueOf(Character.DECIMAL_DIGIT_NUMBER));
        CONST_FIELDS.put("java/lang/Character.LETTER_NUMBER", Values.valueOf(Character.LETTER_NUMBER));
        CONST_FIELDS.put("java/lang/Character.OTHER_NUMBER", Values.valueOf(Character.OTHER_NUMBER));
        CONST_FIELDS.put("java/lang/Character.SPACE_SEPARATOR", Values.valueOf(Character.SPACE_SEPARATOR));
        CONST_FIELDS.put("java/lang/Character.LINE_SEPARATOR", Values.valueOf(Character.LINE_SEPARATOR));
        CONST_FIELDS.put("java/lang/Character.PARAGRAPH_SEPARATOR", Values.valueOf(Character.PARAGRAPH_SEPARATOR));
        CONST_FIELDS.put("java/lang/Character.CONTROL", Values.valueOf(Character.CONTROL));
        CONST_FIELDS.put("java/lang/Character.FORMAT", Values.valueOf(Character.FORMAT));
        CONST_FIELDS.put("java/lang/Character.PRIVATE_USE", Values.valueOf(Character.PRIVATE_USE));
        CONST_FIELDS.put("java/lang/Character.SURROGATE", Values.valueOf(Character.SURROGATE));
        CONST_FIELDS.put("java/lang/Character.DASH_PUNCTUATION", Values.valueOf(Character.DASH_PUNCTUATION));
        CONST_FIELDS.put("java/lang/Character.START_PUNCTUATION", Values.valueOf(Character.START_PUNCTUATION));
        CONST_FIELDS.put("java/lang/Character.END_PUNCTUATION", Values.valueOf(Character.END_PUNCTUATION));
        CONST_FIELDS.put("java/lang/Character.CONNECTOR_PUNCTUATION", Values.valueOf(Character.CONNECTOR_PUNCTUATION));
        CONST_FIELDS.put("java/lang/Character.OTHER_PUNCTUATION", Values.valueOf(Character.OTHER_PUNCTUATION));
        CONST_FIELDS.put("java/lang/Character.MATH_SYMBOL", Values.valueOf(Character.MATH_SYMBOL));
        CONST_FIELDS.put("java/lang/Character.CURRENCY_SYMBOL", Values.valueOf(Character.CURRENCY_SYMBOL));
        CONST_FIELDS.put("java/lang/Character.MODIFIER_SYMBOL", Values.valueOf(Character.MODIFIER_SYMBOL));
        CONST_FIELDS.put("java/lang/Character.OTHER_SYMBOL", Values.valueOf(Character.OTHER_SYMBOL));
        CONST_FIELDS.put("java/lang/Character.INITIAL_QUOTE_PUNCTUATION", Values.valueOf(Character.INITIAL_QUOTE_PUNCTUATION));
        CONST_FIELDS.put("java/lang/Character.FINAL_QUOTE_PUNCTUATION", Values.valueOf(Character.FINAL_QUOTE_PUNCTUATION));
        CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_UNDEFINED", Values.valueOf(Character.DIRECTIONALITY_UNDEFINED));
        CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_LEFT_TO_RIGHT", Values.valueOf(Character.DIRECTIONALITY_LEFT_TO_RIGHT));
        CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_RIGHT_TO_LEFT", Values.valueOf(Character.DIRECTIONALITY_RIGHT_TO_LEFT));
        CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC", Values.valueOf(Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC));
        CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_EUROPEAN_NUMBER", Values.valueOf(Character.DIRECTIONALITY_EUROPEAN_NUMBER));
        CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR", Values.valueOf(Character.DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR));
        CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR", Values.valueOf(Character.DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR));
        CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_ARABIC_NUMBER", Values.valueOf(Character.DIRECTIONALITY_ARABIC_NUMBER));
        CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_COMMON_NUMBER_SEPARATOR", Values.valueOf(Character.DIRECTIONALITY_COMMON_NUMBER_SEPARATOR));
        CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_NONSPACING_MARK", Values.valueOf(Character.DIRECTIONALITY_NONSPACING_MARK));
        CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_BOUNDARY_NEUTRAL", Values.valueOf(Character.DIRECTIONALITY_BOUNDARY_NEUTRAL));
        CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_PARAGRAPH_SEPARATOR", Values.valueOf(Character.DIRECTIONALITY_PARAGRAPH_SEPARATOR));
        CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_SEGMENT_SEPARATOR", Values.valueOf(Character.DIRECTIONALITY_SEGMENT_SEPARATOR));
        CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_WHITESPACE", Values.valueOf(Character.DIRECTIONALITY_WHITESPACE));
        CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_OTHER_NEUTRALS", Values.valueOf(Character.DIRECTIONALITY_OTHER_NEUTRALS));
        CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING", Values.valueOf(Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING));
        CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE", Values.valueOf(Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE));
        CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING", Values.valueOf(Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING));
        CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE", Values.valueOf(Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE));
        CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_POP_DIRECTIONAL_FORMAT", Values.valueOf(Character.DIRECTIONALITY_POP_DIRECTIONAL_FORMAT));
        CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_LEFT_TO_RIGHT_ISOLATE", Values.valueOf(Character.DIRECTIONALITY_LEFT_TO_RIGHT_ISOLATE));
        CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_RIGHT_TO_LEFT_ISOLATE", Values.valueOf(Character.DIRECTIONALITY_RIGHT_TO_LEFT_ISOLATE));
        CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_FIRST_STRONG_ISOLATE", Values.valueOf(Character.DIRECTIONALITY_FIRST_STRONG_ISOLATE));
        CONST_FIELDS.put("java/lang/Character.DIRECTIONALITY_POP_DIRECTIONAL_ISOLATE", Values.valueOf(Character.DIRECTIONALITY_POP_DIRECTIONAL_ISOLATE));
        CONST_FIELDS.put("java/lang/Character.MIN_HIGH_SURROGATE", Values.valueOf(Character.MIN_HIGH_SURROGATE));
        CONST_FIELDS.put("java/lang/Character.MAX_HIGH_SURROGATE", Values.valueOf(Character.MAX_HIGH_SURROGATE));
        CONST_FIELDS.put("java/lang/Character.MIN_LOW_SURROGATE", Values.valueOf(Character.MIN_LOW_SURROGATE));
        CONST_FIELDS.put("java/lang/Character.MAX_LOW_SURROGATE", Values.valueOf(Character.MAX_LOW_SURROGATE));
        CONST_FIELDS.put("java/lang/Character.MIN_SURROGATE", Values.valueOf(Character.MIN_SURROGATE));
        CONST_FIELDS.put("java/lang/Character.MAX_SURROGATE", Values.valueOf(Character.MAX_SURROGATE));
        CONST_FIELDS.put("java/lang/Character.MIN_SUPPLEMENTARY_CODE_POINT", Values.valueOf(Character.MIN_SUPPLEMENTARY_CODE_POINT));
        CONST_FIELDS.put("java/lang/Character.MIN_CODE_POINT", Values.valueOf(Character.MIN_CODE_POINT));
        CONST_FIELDS.put("java/lang/Character.MAX_CODE_POINT", Values.valueOf(Character.MAX_CODE_POINT));
        //
        CONST_FIELDS.put("java/lang/Math.E", Values.valueOf(Math.E));
        CONST_FIELDS.put("java/lang/Math.PI", Values.valueOf(Math.PI));
        CONST_FIELDS.put("java/lang/StrictMath.E", Values.valueOf(StrictMath.E));
        CONST_FIELDS.put("java/lang/StrictMath.PI", Values.valueOf(StrictMath.PI));
        //
        CONST_FIELDS.put("java/io/File.separator", Values.valueOfString(File.separator));
        CONST_FIELDS.put("java/io/File.separatorChar", Values.valueOf(File.separatorChar));
        CONST_FIELDS.put("java/io/File.pathSeparator", Values.valueOfString(File.pathSeparator));
        CONST_FIELDS.put("java/io/File.pathSeparatorChar", Values.valueOf(File.pathSeparatorChar));
    }
}
