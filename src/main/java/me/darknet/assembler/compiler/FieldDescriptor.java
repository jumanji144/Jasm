package me.darknet.assembler.compiler;

import lombok.Getter;

@Getter
public class FieldDescriptor {
	private final String name;
	private final String owner;
	private final String desc;

	public FieldDescriptor(String name, String desc) {
		if (name.indexOf('.') >= 0) {
			String[] parts = name.split("\\.");
			if (parts.length > 1) {
				this.name = parts[1];
				owner = parts[0];
			} else if (parts.length > 0) {
				this.name = parts[0];
				owner = null;
			} else {
				this.name = null;
				owner = null;
			}
		} else {
			this.name = name;
			owner = null;
		}
		this.desc = desc;
	}

	public boolean hasDeclaredOwner() {
		return owner != null;
	}
}
