package me.darknet.assembler.compiler;

import lombok.Data;

@Data
public class MethodDescriptor {
	private final String name;
	private final String owner;
	private final String descriptor;
	private String returnType;

	public MethodDescriptor(String name, String desc) {
		if (name.contains(".")) {
			this.owner = name.substring(0, name.lastIndexOf('.'));
			this.name = name.substring(name.lastIndexOf('.') + 1);
		} else {
			this.owner = name.substring(0, name.lastIndexOf('/'));
			this.name = name.substring(name.lastIndexOf('/') + 1);
		}
		this.descriptor = desc;
	}

	public boolean hasDeclaredOwner() {
		return owner != null;
	}
}
