package me.darknet.assembler.compiler;

import lombok.Getter;
import org.objectweb.asm.Type;

@Getter
public class MethodDescriptor {
	private final String name;
	private final String owner;
	private final String descriptor;
	private final String returnType;

	public MethodDescriptor(String name, String desc) {
		if (name.contains(".")) {
			this.owner = name.substring(0, name.lastIndexOf('.'));
			this.name = name.substring(name.lastIndexOf('.') + 1);
		} else {
			this.owner = null;
			this.name = name;
		}
		this.descriptor = desc;
		returnType = Type.getReturnType(descriptor).getDescriptor();
	}

	public boolean hasDeclaredOwner() {
		return owner != null;
	}
}
