package me.darknet.assembler.compile.visitor;

import dev.xdark.blw.annotation.Element;
import dev.xdark.blw.annotation.ElementDouble;
import dev.xdark.blw.annotation.ElementString;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.specific.ASTValue;
import org.jetbrains.annotations.NotNull;

public interface BlwElementAdapter {
	@NotNull
	default Element elementFromValue(@NotNull ASTValue value) {
		ElementType valueType = value.type();
		return switch (valueType) {
			case STRING -> new ElementString(value.content());
			case NUMBER -> new ElementDouble(Double.parseDouble(value.content()));
			default -> throw new UnsupportedOperationException("Enum value of type not supported yet: " + valueType);
		};
	}
}
