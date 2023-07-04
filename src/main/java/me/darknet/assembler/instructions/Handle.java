package me.darknet.assembler.instructions;

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
	private Kind kind;
	private String name;
	private String descriptor;
	public Handle(Kind kind, String name, String descriptor) {
		this.kind = kind;
		this.name = name;
		this.descriptor = descriptor;
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
