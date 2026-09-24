package me.darknet.assembler.ast.specific;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.util.CollectionUtil;
import me.darknet.assembler.util.ElementMapView;
import me.darknet.assembler.util.ImmutableElementMap;
import me.darknet.assembler.visitor.Modifiers;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * AST model of a method declaration.
 */
public class ASTMethod extends ASTMember {

    private final List<ASTIdentifier> parameters;
    private final Map<ASTIdentifier, List<ASTAnnotation>> parameterAnnotations;
    private final List<ASTIdentifier> declaredExceptions;
    private final List<ASTException> exceptions;
    private final ASTElement defaultValue;
    private final ASTCode code;
    private final ElementMapView<ASTIdentifier, @Nullable ASTElement> methodAttributes;

    public ASTMethod(Modifiers modifiers, ASTIdentifier name, ASTIdentifier descriptor, List<ASTIdentifier> parameters,
                     Map<ASTIdentifier, List<ASTAnnotation>> parameterAnnotations,
                     List<ASTIdentifier> declaredExceptions, ASTElement defaultValue, List<ASTException> exceptions,
                     ASTCode code, ElementMapView<ASTIdentifier, @Nullable ASTElement> methodAttributes) {
        super(ElementType.METHOD, modifiers, name, descriptor);
        this.parameters = CollectionUtil.immutableCopy(parameters);
        this.parameterAnnotations = freezeParameterAnnotations(parameterAnnotations);
        this.declaredExceptions = CollectionUtil.immutableCopy(declaredExceptions);
        this.exceptions = CollectionUtil.immutableCopy(exceptions);
        this.defaultValue = defaultValue;
        this.code = code;
        this.methodAttributes = ImmutableElementMap.copyOf(methodAttributes);
        addChildren(this.parameters);
        addChildren(this.declaredExceptions);
        addChildren(this.exceptions);
        for (List<ASTAnnotation> annotations : this.parameterAnnotations.values()) {
            addChildren(annotations);
        }
        if (defaultValue != null) addChild(defaultValue);
        if (code != null) addChild(code);
        addChildren(this.methodAttributes.elements());
    }

    /**
     * @return Default annotation value for this method, or {@code null} when not present.
     */
    public @Nullable ASTElement getAnnotationDefaultValue() {
        return defaultValue;
    }

    /**
     * @return List of parameter names for this method.
     */
    public List<ASTIdentifier> getParameters() {
        return parameters;
    }

    /**
     * @return Map of parameter names to their annotations.
     */
    public Map<ASTIdentifier, List<ASTAnnotation>> getParameterAnnotations() {
        return parameterAnnotations;
    }

    /**
     * @return List of thrown exceptions declared by this method.
     */
    public List<ASTIdentifier> getDeclaredExceptions() {
        return declaredExceptions;
    }

    /**
     * @return List of try-catch handlers for this method.
     */
    public List<ASTException> getExceptionHandlers() {
        return exceptions;
    }

    /**
     * @return AST code model for this method, or {@code null} when the method is abstract or native.
     */
    public ASTCode getCode() {
        return code;
    }

    /**
     * @return Ordered raw method-body attributes retained as source syntax.
     */
    public ElementMapView<ASTIdentifier, @Nullable ASTElement> getMethodAttributes() {
        return methodAttributes;
    }

    private static Map<ASTIdentifier, List<ASTAnnotation>> freezeParameterAnnotations(
            Map<ASTIdentifier, List<ASTAnnotation>> parameterAnnotations) {
        if (parameterAnnotations == null || parameterAnnotations.isEmpty())
            return Collections.emptyMap();
        Map<ASTIdentifier, List<ASTAnnotation>> copy = new IdentityHashMap<>();
        parameterAnnotations.forEach((parameter, annotations) ->
                copy.put(parameter, CollectionUtil.immutableCopy(annotations)));
        return Collections.unmodifiableMap(copy);
    }
}
