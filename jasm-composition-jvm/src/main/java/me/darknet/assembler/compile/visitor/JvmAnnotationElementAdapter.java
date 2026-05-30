package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.ast.specific.ASTValue;
import me.darknet.assembler.error.ErrorCollectionException;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.visitor.ASTAnnotationArrayVisitor;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.ArrayList;
import java.util.List;

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

	@NotNull
	default List<Object> elementFromArray(@NotNull ASTArray array) {
		List<Object> values = new ArrayList<>();
		ErrorCollector collector = new ErrorCollector();
		ASTAnnotationArrayVisitor.accept(new JvmAnnotationArrayVisitor(values), array, collector);
		if (collector.hasErr()) {
			throw new ErrorCollectionException("Failed building array element from ast", collector);
		}
		return values;
	}

	default void addElement(@NotNull AnnotationNode annotation, @NotNull String name, @NotNull Object value) {
		if (annotation.values == null) {
			annotation.values = new ArrayList<>();
		}
		annotation.values.add(name);
		annotation.values.add(value);
	}
}

