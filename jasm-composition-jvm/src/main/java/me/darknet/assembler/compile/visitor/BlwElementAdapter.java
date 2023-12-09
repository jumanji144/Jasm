package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.specific.ASTValue;

import dev.xdark.blw.annotation.*;
import org.jetbrains.annotations.NotNull;

public interface BlwElementAdapter {
    @NotNull
    default Element elementFromValue(@NotNull ASTValue value) {
        ElementType valueType = value.type();
        return switch (valueType) {
            case STRING -> new ElementString(value.content());
            case NUMBER -> {
                ASTNumber number = (ASTNumber) value;
                if (number.isFloatingPoint()) {
                    if (number.isWide()) {
                        yield new ElementDouble(number.asDouble());
                    } else {
                        yield new ElementFloat(number.asFloat());
                    }
                } else {
                    if (number.isWide()) {
                        yield new ElementLong(number.asLong());
                    } else {
                        yield new ElementInt(number.asInt());
                    }
                }
            }
            case CHARACTER -> new ElementChar(value.content().charAt(0));
            case BOOL -> new ElementBoolean(Boolean.parseBoolean(value.content()));
            default -> throw new UnsupportedOperationException("Enum value of type not supported yet: " + valueType);
        };
    }
}
