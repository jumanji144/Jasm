package me.darknet.assembler.backend.jvm.compile.visitor;

import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.specific.ASTValue;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.ArrayList;

public interface JvmAnnotationElementAdapter {
	@NotNull
	default Object elementFromValue(@NotNull ASTValue value) {
		ElementType valueType = value.type();
		return switch (valueType) {
			case STRING -> value.content();
			case NUMBER -> {
				ASTNumber number = (ASTNumber) value;
				if (number.isFloatingPoint()) {
					if (number.isWide()) {
						yield number.asDouble();
					}
					yield number.asFloat();
				}
				if (number.isWide()) {
					yield number.asLong();
				}
				yield number.asInt();
			}
			case CHARACTER -> value.content().charAt(0);
			case BOOL -> Boolean.parseBoolean(value.content());
			default -> throw new UnsupportedOperationException("Enum value of type not supported yet: " + valueType);
		};
	}

	@NotNull
	default Type elementFromTypeIdentifier(@NotNull ASTIdentifier className) {
		return Type.getObjectType(className.literal());
	}

	@NotNull
	default String[] elementFromEnum(@NotNull ASTIdentifier className, @NotNull ASTIdentifier enumName) {
		return new String[]{Type.getObjectType(className.literal()).getDescriptor(), enumName.literal()};
	}


	default void addElement(@NotNull AnnotationNode annotation, @NotNull String name, @NotNull Object value) {
		if (annotation.values == null) {
			annotation.values = new ArrayList<>();
		}
		annotation.values.add(name);
		annotation.values.add(value);
	}
}

