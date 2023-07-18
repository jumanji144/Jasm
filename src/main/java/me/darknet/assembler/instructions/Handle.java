package me.darknet.assembler.instructions;

import me.darknet.assembler.ast.primitive.ASTIdentifier;

import java.util.Map;

public class Handle {

	public static Map<String, Kind> KINDS = Map.of(
			"getfield", Kind.GET_FIELD,
			"getstatic", Kind.GET_STATIC,
			"putfield", Kind.PUT_FIELD,
			"putstatic", Kind.PUT_STATIC,
			"invokevirtual", Kind.INVOKE_VIRTUAL,
			"invokestatic", Kind.INVOKE_STATIC,
			"invokespecial", Kind.INVOKE_SPECIAL,
			"newinvokespecial", Kind.NEW_INVOKE_SPECIAL,
			"invokeinterface", Kind.INVOKE_INTERFACE
	);
	private final Kind kind;
	private final ASTIdentifier name;
	private final ASTIdentifier descriptor;

	public Handle(Kind kind, ASTIdentifier name, ASTIdentifier descriptor) {
		this.kind = kind;
		this.name = name;
		this.descriptor = descriptor;
	}

	public Kind getKind() {
		return kind;
	}

	public ASTIdentifier getName() {
		return name;
	}

	public ASTIdentifier getDescriptor() {
		return descriptor;
	}

	public enum Kind {
		GET_FIELD,
		GET_STATIC,
		PUT_FIELD,
		PUT_STATIC,
		INVOKE_VIRTUAL,
		INVOKE_STATIC,
		INVOKE_SPECIAL,
		NEW_INVOKE_SPECIAL,
		INVOKE_INTERFACE
	}

}
