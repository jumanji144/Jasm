package me.darknet.assembler.transform;

import me.darknet.assembler.exceptions.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.groups.attributes.AttributeGroup;
import me.darknet.assembler.parser.groups.attributes.ClassAttributeGroup;
import me.darknet.assembler.parser.groups.attributes.FieldAttributeGroup;
import me.darknet.assembler.parser.groups.attributes.MethodAttributeGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AttributeStore {
	private final List<ClassAttributeGroup> classAttributes = new ArrayList<>();
	private final List<FieldAttributeGroup> fieldAttributes = new ArrayList<>();
	private final List<MethodAttributeGroup> methodAttributes = new ArrayList<>();

	/**
	 * @param group
	 * 		Attribute group to record.
	 */
	public void recordGroup(AttributeGroup group) {
		if (group instanceof ClassAttributeGroup)
			classAttributes.add((ClassAttributeGroup) group);
		if (group instanceof FieldAttributeGroup)
			fieldAttributes.add((FieldAttributeGroup) group);
		if (group instanceof MethodAttributeGroup)
			methodAttributes.add((MethodAttributeGroup) group);
	}

	@SuppressWarnings("unchecked")
	public <T extends ClassAttributeGroup> T getFirstClassAttribute(Class<T> type) {
		return getFirst((List<? extends Group>) (Object) classAttributes, type);
	}

	@SuppressWarnings("unchecked")
	public <T extends FieldAttributeGroup> T getFirstFieldAttribute(Class<T> type) {
		return getFirst((List<? extends Group>) (Object) fieldAttributes, type);
	}

	@SuppressWarnings("unchecked")
	public <T extends MethodAttributeGroup> T getFirstMethodAttribute(Class<T> type) {
		return getFirst((List<? extends Group>) (Object) methodAttributes, type);
	}

	@SuppressWarnings("unchecked")
	private <T extends AttributeGroup> T getFirst(List<? extends Group> collection, Class<T> type) {
		return (T) collection.stream()
				.filter(attr -> type.isAssignableFrom(attr.getClass()))
				.findFirst()
				.orElse(null);
	}

	@SuppressWarnings("unchecked")
	public <T extends MethodAttributeGroup> List<T> getClassAttributesOfType(Class<T> type) {
		return getOfType((List<? extends Group>) (Object) classAttributes, type);
	}

	@SuppressWarnings("unchecked")
	public <T extends MethodAttributeGroup> List<T> getFieldAttributesOfType(Class<T> type) {
		return getOfType((List<? extends Group>) (Object) fieldAttributes, type);
	}

	@SuppressWarnings("unchecked")
	public <T extends MethodAttributeGroup> List<T> getMethodAttributesOfType(Class<T> type) {
		return getOfType((List<? extends Group>) (Object) methodAttributes, type);
	}

	@SuppressWarnings("unchecked")
	private <T extends AttributeGroup> List<T> getOfType(List<? extends Group> collection, Class<T> type) {
		return (List<T>) collection.stream()
				.filter(attr -> type.isAssignableFrom(attr.getClass()))
				.collect(Collectors.toList());
	}

	/**
	 * Clear stored attributes.
	 */
	public void clear() {
		classAttributes.clear();
		fieldAttributes.clear();
		methodAttributes.clear();
	}

	/**
	 * @param classVisitor
	 * 		Class to visit with stored class attributes.
	 *
	 * @throws AssemblerException
	 * 		When the visitor encounters an error handling attributes.
	 */
	public void accept(ClassGroupVisitor classVisitor) throws AssemblerException {
		for (ClassAttributeGroup attribute : classAttributes) {
			classVisitor.visitAttribute(attribute);
		}
	}

	/**
	 * @param fieldVisitor
	 * 		Field to visit with stored field attributes.
	 *
	 * @throws AssemblerException
	 * 		When the visitor encounters an error handling attributes.
	 */
	public void accept(FieldGroupVisitor fieldVisitor) throws AssemblerException {
		for (FieldAttributeGroup attribute : fieldAttributes) {
			fieldVisitor.visitAttribute(attribute);
		}
	}

	/**
	 * @param methodVisitor
	 * 		Method to visit with stored method attributes.
	 *
	 * @throws AssemblerException
	 * 		When the visitor encounters an error handling attributes.
	 */
	public void accept(MethodGroupVisitor methodVisitor) throws AssemblerException {
		for (MethodAttributeGroup attribute : methodAttributes) {
			methodVisitor.visitAttribute(attribute);
		}
	}
}
