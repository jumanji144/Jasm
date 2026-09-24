package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.AnnotationVisibility;
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

import java.util.Objects;

/**
 * AST model of an annotation.
 */
public class ASTAnnotation extends ASTElement {

	private final AnnotationVisibility visibility;
	private final ASTIdentifier classType;
	private final ImmutableElementMap<ASTIdentifier, ASTElement> values;
	private final ASTNumber typeRef;
	private final ASTIdentifier typePath;

	/**
	 * Creates a new annotation.
	 *
	 * @param visibility
	 *        Visibility represented by the annotation's source syntax.
	 * @param classType
	 * 		Type identifier of the annotation.
	 * @param values
	 * 		Map of annotation element values.
	 */
	public ASTAnnotation(AnnotationVisibility visibility, ASTIdentifier classType, ElementMapView<ASTIdentifier, ASTElement> values) {
		this(visibility, classType, ImmutableElementMap.copyOf(values), null, null);
	}

	/**
	 * Creates a new annotation, optionally carrying type-annotation metadata.
	 *
	 * @param visibility
	 *        Visibility represented by the annotation's source syntax.
	 * @param classType
	 * 		Type identifier of the annotation.
	 * @param values
	 * 		Map of annotation element values.
	 * @param typeRef
	 * 		Type reference for type-annotations, or {@code null} if this is not a type-annotation.
	 * @param typePath
	 * 		Type path for type-annotations, or {@code null} if this is not a type-annotation.
	 */
	public ASTAnnotation(AnnotationVisibility visibility, ASTIdentifier classType,
	                     ElementMapView<ASTIdentifier, ASTElement> values, ASTNumber typeRef, ASTIdentifier typePath) {
		this(visibility, classType, ImmutableElementMap.copyOf(values), typeRef, typePath);
	}

	private ASTAnnotation(AnnotationVisibility visibility, ASTIdentifier classType,
	                      ImmutableElementMap<ASTIdentifier, ASTElement> values, ASTNumber typeRef, ASTIdentifier typePath) {
		super(ElementType.ANNOTATION, CollectionUtil.mergeNonNull(values.elements(), classType, typeRef, typePath));
		this.visibility = Objects.requireNonNull(visibility, "visibility");
		this.classType = classType;
		this.values = values;
		this.typeRef = typeRef;
		this.typePath = typePath;
	}

	/**
	 * @return Visibility represented by the annotation's source syntax.
	 */
	public @NotNull AnnotationVisibility getVisibility() {
		return visibility;
	}

	/**
	 * @return {@code true} if this annotation is a type-annotation, {@code false} otherwise.
	 */
	public boolean isTypeAnnotation() {
		return typePath != null;
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
