package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTValue;
import me.darknet.assembler.visitor.ASTAnnotationArrayVisitor;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.ArrayList;
import java.util.List;

public class JvmAnnotationVisitor implements ASTAnnotationVisitor, JvmAnnotationElementAdapter {
	private final AnnotationNode annotation;

	public JvmAnnotationVisitor(AnnotationNode annotation) {
		this.annotation = annotation;
	}

	@Override
	public void visitValue(ASTIdentifier name, ASTValue value) {
		addElement(annotation, name.literal(), elementFromValue(value));
	}

	@Override
	public void visitTypeValue(ASTIdentifier name, ASTIdentifier className) {
		addElement(annotation, name.literal(), elementFromTypeIdentifier(className));
	}

	@Override
	public void visitEnumValue(ASTIdentifier name, ASTIdentifier className, ASTIdentifier enumName) {
		addElement(annotation, name.literal(), elementFromEnum(className, enumName));
	}

	@Override
	public ASTAnnotationVisitor visitAnnotationValue(ASTIdentifier name, ASTIdentifier className) {
		AnnotationNode nested = new AnnotationNode(Type.getObjectType(className.literal()).getDescriptor());
		addElement(annotation, name.literal(), nested);
		return new JvmAnnotationVisitor(nested);
	}

	@Override
	public ASTAnnotationArrayVisitor visitArrayValue(ASTIdentifier name) {
		List<Object> values = new ArrayList<>();
		addElement(annotation, name.literal(), values);
		return new JvmAnnotationArrayVisitor(values);
	}

	@Override
	public void visitEnd() {
	}
}

