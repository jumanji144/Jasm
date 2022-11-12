package me.darknet.assembler.compiler;

import lombok.Data;

@Data
public class FieldDescriptor {
	private final String name;
	private final String owner;
	private final String desc;

	public FieldDescriptor(String name, String desc) {
		if (name.contains(".")) {
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
			int lastSlash = name.lastIndexOf("/");
			if (lastSlash != -1) {
				this.name = desc.substring(lastSlash + 1);
				owner = desc.substring(0, lastSlash);
			} else {
				this.name = desc;
				owner = null;
			}
		}
		this.desc = desc;
	}
}
