package me.darknet.assembler.compile.visitor;

import dev.xdark.blw.classfile.RecordComponentBuilder;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import me.darknet.assembler.visitor.ASTRecordComponentVisitor;

public class BlwRecordComponentVisitor implements ASTRecordComponentVisitor , BlwElementAdapter {
	private final RecordComponentBuilder<?> builder;

	public BlwRecordComponentVisitor(RecordComponentBuilder<?> builder) {
		this.builder = builder;
	}

	@Override
	public ASTAnnotationVisitor visitVisibleAnnotation(ASTIdentifier classType) {
		return new BlwAnnotationVisitor(
				builder.addVisibleRuntimeAnnotation(Types.instanceTypeFromInternalName(classType.literal())).child()
		);
	}

	@Override
	public ASTAnnotationVisitor visitInvisibleAnnotation(ASTIdentifier classType) {
		return new BlwAnnotationVisitor(
				builder.addInvisibleRuntimeAnnotation(Types.instanceTypeFromInternalName(classType.literal())).child()
		);
	}
}
