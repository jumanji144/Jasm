package me.darknet.assembler.compile.visitor;

import dev.xdark.blw.annotation.generic.GenericArrayBuilder;
import dev.xdark.blw.type.InstanceType;
import dev.xdark.blw.type.ObjectType;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.specific.ASTValue;

import dev.xdark.blw.annotation.*;
import me.darknet.assembler.error.ErrorCollectionException;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.visitor.ASTAnnotationArrayVisitor;
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
            case BOOL -> ElementBoolean.of(Boolean.parseBoolean(value.content()));
            default -> throw new UnsupportedOperationException("Enum value of type not supported yet: " + valueType);
        };
    }

    @NotNull
    default Element elementFromTypeIdentifier(@NotNull ASTIdentifier className) {
        ObjectType type = Types.objectTypeFromInternalName(className.literal());
        return new dev.xdark.blw.annotation.ElementType(type);
    }

    @NotNull
    default Element elementFromEnum(@NotNull ASTIdentifier className, @NotNull  ASTIdentifier enumName) {
        InstanceType type = Types.instanceTypeFromInternalName(className.literal());
        return new ElementEnum(type, enumName.literal());
    }

    @NotNull
    default Element elementFromArray(@NotNull ASTArray array) {
        GenericArrayBuilder builder = new GenericArrayBuilder();
        ErrorCollector collector = new ErrorCollector();
        ASTAnnotationArrayVisitor.accept(new BlwAnnotationArrayVisitor(builder), array, collector);
        if (collector.hasErr())
            throw new ErrorCollectionException("Failed building array element from ast", collector);
        return builder.build();
    }
}
