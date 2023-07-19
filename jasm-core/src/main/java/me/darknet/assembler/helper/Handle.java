package me.darknet.assembler.helper;

import me.darknet.assembler.ast.primitive.ASTArray;

import java.util.Map;

public record Handle(Handle.Kind kind, String name, String descriptor) {

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

	public static Handle from(ASTArray array) {
		Handle.Kind kind = Handle.Kind.valueOf(array.getValues().get(0).getContent());
		String name = array.getValues().get(1).getContent();
		String descriptor = array.getValues().get(2).getContent();

		return new Handle(kind, name, descriptor);
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
