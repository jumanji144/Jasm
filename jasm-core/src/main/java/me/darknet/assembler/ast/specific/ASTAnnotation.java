package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.primitive.ASTNumber;
import me.darknet.assembler.error.ErrorCollector;
import me.darknet.assembler.util.CollectionUtil;
import me.darknet.assembler.util.ElementMapView;
import me.darknet.assembler.util.ImmutableElementMap;
import me.darknet.assembler.visitor.ASTAnnotationVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * AST model of an annotation.
 */
public class ASTAnnotation extends ASTElement {

	private final ASTIdentifier classType;
	private final ImmutableElementMap<ASTIdentifier, ASTElement> values;
	private final ASTNumber typeRef;
	private final ASTIdentifier typePath;
	private final boolean visible;
	private final boolean system;

	/**
	 * Creates a new annotation.
	 *
	 * @param visible
	 *        {@code true} if this annotation is runtime-visible, {@code false} otherwise.
	 * @param classType
	 * 		Type identifier of the annotation.
	 * @param values
	 * 		Map of annotation element values.
	 */
	public ASTAnnotation(boolean visible, ASTIdentifier classType, ElementMapView<ASTIdentifier, ASTElement> values) {
		this(visible, false, classType, values, null, null);
	}

	/**
	 * Creates a new type annotation.
	 *
	 * @param visible
	 *        {@code true} if this annotation is runtime-visible, {@code false} otherwise.
	 * @param classType
	 * 		Type identifier of the annotation.
	 * @param values
	 * 		Map of annotation element values.
	 * @param typeRef
	 * 		Type reference for type-annotations, or {@code null} if this is not a type-annotation.
	 * @param typePath
	 * 		Type path for type-annotations, or {@code null} if this is not a type-annotation.
	 */
	public ASTAnnotation(boolean visible, ASTIdentifier classType, ElementMapView<ASTIdentifier, ASTElement> values, ASTNumber typeRef, ASTIdentifier typePath) {
		this(visible, false, classType, values, typeRef, typePath);
	}

	/**
	 * Creates a new annotation, of any type.
	 *
	 * @param visible
	 *        {@code true} if this annotation is runtime-visible, {@code false} otherwise.
	 * @param system
	 *        {@code true} if this annotation is Dalvik system-visible, {@code false} otherwise.
	 * @param classType
	 * 		Type identifier of the annotation.
	 * @param values
	 * 		Map of annotation element values.
	 * @param typeRef
	 * 		Type reference for type-annotations, or {@code null} if this is not a type-annotation.
	 * @param typePath
	 * 		Type path for type-annotations, or {@code null} if this is not a type-annotation.
	 */
	public ASTAnnotation(boolean visible, boolean system, ASTIdentifier classType,
	                     ElementMapView<ASTIdentifier, ASTElement> values, ASTNumber typeRef, ASTIdentifier typePath) {
		this(visible, system, classType, ImmutableElementMap.copyOf(values), typeRef, typePath);
	}

	private ASTAnnotation(boolean visible, boolean system, ASTIdentifier classType,
	                      ImmutableElementMap<ASTIdentifier, ASTElement> values, ASTNumber typeRef, ASTIdentifier typePath) {
		super(ElementType.ANNOTATION, CollectionUtil.mergeNonNull(values.elements(), classType, typeRef, typePath));
		this.visible = visible;
		this.system = system;
		this.classType = classType;
		this.values = values;
		this.typeRef = typeRef;
		this.typePath = typePath;
	}

	/**
	 * @return {@code true} if this annotation is a type-annotation, {@code false} otherwise.
	 */
	public boolean isTypeAnnotation() {
		return typePath != null;
	}

	/**
	 * @return {@code true} if this annotation is runtime-visible, {@code false} otherwise.
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * @return {@code true} if this annotation is Dalvik system-visible, {@code false} otherwise.
	 */
	public boolean isSystem() {
		return system;
	}

	/**
	 * @return Type reference for type-annotations, or {@code null} if this is not a type-annotation.
	 */
	public @Nullable ASTNumber getTypeRef() {
		return typeRef;
	}

	/**
	 * @return Type path for type-annotations, or {@code null} if this is not a type-annotation.
	 */
	public @Nullable ASTIdentifier getTypePath() {
		return typePath;
	}

	/**
	 * @return Annotation type identifier.
	 */
	public @NotNull ASTIdentifier getClassType() {
		return classType;
	}

	/**
	 * @return Map of annotation element values.
	 */
	public @NotNull ElementMapView<ASTIdentifier, ASTElement> getValueMap() {
		return values;
	}

	/**
	 * @param name
	 * 		Name of the annotation element.
	 * @param <T>
	 * 		Type of the annotation element value.
	 *
	 * @return Value of the annotation element, or {@code null} if not present.
	 */
	public <T extends ASTElement> T getValue(String name) {
		return values.get(name);
	}

	public void accept(ErrorCollector collector, @Nullable ASTAnnotationVisitor visitor) {
		if (visitor == null)
			return;
		ASTAnnotationVisitor.accept(visitor, values.pairs(), collector);
	}
}
