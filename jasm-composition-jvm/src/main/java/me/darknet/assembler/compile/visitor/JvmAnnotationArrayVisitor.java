package me.darknet.assembler.compile.visitor;

import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTValue;
import me.darknet.assembler.visitor.ASTAnnotationArrayVisitor;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.ArrayList;
import java.util.List;

public class JvmAnnotationArrayVisitor implements ASTAnnotationArrayVisitor, JvmAnnotationElementAdapter {
	private final List<Object> values;

	public JvmAnnotationArrayVisitor(List<Object> values) {
		this.values = values;
	}

	@Override
	public void visitValue(ASTValue value) {
		values.add(elementFromValue(value));
	}

	@Override
	public void visitTypeValue(ASTIdentifier className) {
		values.add(Type.getObjectType(className.literal()));
	}

	@Override
	public void visitEnumValue(ASTIdentifier className, ASTIdentifier enumName) {
		values.add(elementFromEnum(className, enumName));
	}

	@Override
	public ASTAnnotationVisitor visitAnnotationValue(ASTIdentifier className) {
		AnnotationNode annotation = new AnnotationNode(Type.getObjectType(className.literal()).getDescriptor());
		values.add(annotation);
		return new JvmAnnotationVisitor(annotation);
	}

	@Override
	public ASTAnnotationArrayVisitor visitArrayValue() {
		List<Object> nested = new ArrayList<>();
		values.add(nested);
		return new JvmAnnotationArrayVisitor(nested);
	}

	@Override
	public void visitEnd() {
	}
}

