package me.darknet.assembler.printer.impl.asm;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ASMAnnotationHolder {

	List<AnnotationNode> annotations = new ArrayList<>();

	static ASMAnnotationHolder fromClass(ClassNode node) {
		ASMAnnotationHolder holder = new ASMAnnotationHolder();
		holder.annotations.addAll(unNull(node.visibleAnnotations));
		holder.annotations.addAll(unNull(node.invisibleAnnotations));
		holder.annotations.addAll(unNull(node.visibleTypeAnnotations));
		holder.annotations.addAll(unNull(node.invisibleTypeAnnotations));
		return holder;
	}

	static ASMAnnotationHolder fromMethod(MethodNode node) {
		ASMAnnotationHolder holder = new ASMAnnotationHolder();
		holder.annotations.addAll(unNull(node.visibleAnnotations));
		holder.annotations.addAll(unNull(node.invisibleAnnotations));
		if(node.visibleParameterAnnotations != null) {
			for (List<AnnotationNode> visibleParameterAnnotation : node.visibleParameterAnnotations) {
				holder.annotations.addAll(visibleParameterAnnotation);
			}
		}
		if(node.invisibleParameterAnnotations != null) {
			for (List<AnnotationNode> invisibleParameterAnnotation : node.invisibleParameterAnnotations) {
				holder.annotations.addAll(invisibleParameterAnnotation);
			}
		}
		holder.annotations.addAll(unNull(node.visibleTypeAnnotations));
		holder.annotations.addAll(unNull(node.invisibleTypeAnnotations));
		holder.annotations.addAll(unNull(node.visibleLocalVariableAnnotations));
		holder.annotations.addAll(unNull(node.invisibleLocalVariableAnnotations));
		return holder;
	}

	static ASMAnnotationHolder fromField(org.objectweb.asm.tree.FieldNode node) {
		ASMAnnotationHolder holder = new ASMAnnotationHolder();
		holder.annotations.addAll(unNull(node.visibleAnnotations));
		holder.annotations.addAll(unNull(node.invisibleAnnotations));
		holder.annotations.addAll(unNull(node.visibleTypeAnnotations));
		holder.annotations.addAll(unNull(node.invisibleTypeAnnotations));
		return holder;
	}

	public AnnotationNode getAnnotationNode(int index) {
		return annotations.get(index);
	}

	static <T extends AnnotationNode> List<T> unNull(@Nullable List<T> list) {
		return list == null ? Collections.emptyList() : list;
	}

}
