package me.darknet.assembler.compile.analysis.frame;

import me.darknet.assembler.compile.analysis.AnalysisUtils;
import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.util.JvmTypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Implementation of {@link TypedFrame}.
 */
public class TypedFrameImpl implements TypedFrame {
	private final Deque<Type> stack;
	private final Map<Integer, Local> locals;

	/**
	 * New frame with a given stack/variable table.
	 *
	 * @param stack
	 * 		Stack state.
	 * @param locals
	 * 		Variable table.
	 */
	public TypedFrameImpl(@NotNull Deque<Type> stack, @NotNull Map<Integer, Local> locals) {
		this.stack = stack;
		this.locals = locals;
	}

	/**
	 * New frame with an empty stack.
	 *
	 * @param locals
	 * 		Variable map to copy.
	 */
	public TypedFrameImpl(@NotNull Map<Integer, Local> locals) {
		this(new ArrayDeque<>(), new TreeMap<>(locals));
	}

	/**
	 * New frame with an empty stack and no variables.
	 */
	public TypedFrameImpl() {
		this(new ArrayDeque<>(), new TreeMap<>());
	}

	@Override
	public boolean merge(@NotNull InheritanceChecker checker, @NotNull Frame other) throws FrameMergeException {
		if (other instanceof TypedFrame typedOther)
			return merge(checker, typedOther);
		throw new FrameMergeException(this, other, "Cannot merge into differently typed frame");
	}

	/**
	 * Merges types of variables and stack items, taking place in this frame.
	 *
	 * @param checker
	 * 		Inheritance checker to use for determining common super-types.
	 * @param other
	 * 		Frame to merge into this one.
	 *
	 * @return {@code true} when changes were made during the merge process.
	 * {@code false} if no changes were made, indicating equal frames.
	 *
	 * @throws FrameMergeException
	 * 		When the stack sizes do not match.
	 */
	public boolean merge(@NotNull InheritanceChecker checker, @NotNull TypedFrame other) throws FrameMergeException {
		boolean changed = false;
		// Copy locals
		Map<Integer, Local> allLocals = new TreeMap<>(getLocals());
		allLocals.putAll(other.getLocals());

		// Merge locals
		for (Integer index : allLocals.keySet()) {
			Local ourLocal = getLocal(index);
			Local otherLocal = other.getLocal(index);
			if (ourLocal == null || otherLocal == null) {
				Local present = ourLocal == null ? otherLocal : ourLocal;
				if (!JvmTypeUtils.isTop(present.type()) || ourLocal == null) {
					setLocal(index, new Local(index, present.name(), JvmTypeUtils.TOP));
					changed = true;
				}
				continue;
			}

			Type first = normalize(ourLocal.type());
			Type second = normalize(otherLocal.type());
			Type merged = mergeLocalType(checker, first, second, this, other);
			if (!Objects.equals(merged, first)) {
				setLocal(index, new Local(index, ourLocal.name(), merged));
				changed = true;
			}
		}

		// Prepare stack for merging, sanity check stack sizes
		Deque<Type> otherStack = other.getStack();
		if (stack.size() != otherStack.size())
			throw new FrameMergeException(this, other,
					"Stack size mismatch, " + stack.size() + " != " + otherStack.size());

		// Merge stack
		Deque<Type> newStack = new ArrayDeque<>(stack.size());
		Iterator<Type> first = stack.iterator();
		Iterator<Type> second = otherStack.iterator();
		while (first.hasNext() && second.hasNext()) {
			Type type1 = normalize(first.next());
			Type type2 = normalize(second.next());
			Type merged = mergeStackType(checker, type1, type2, this, other);
			if (!Objects.equals(merged, type1))
				changed = true;
			newStack.add(merged);
		}
		stack.clear();
		stack.addAll(newStack);
		return changed;
	}

	@NotNull
	@Override
	public Deque<Type> getStack() {
		return stack;
	}

	@NotNull
	@Override
	public Map<Integer, Local> getLocals() {
		return locals;
	}

	@Override
	public void setLocal(int index, @NotNull Local local) {
		locals.put(index, local);
	}

	@Override
	public void pushType(@Nullable Type type) {
		Type pushed = type == null ? JvmTypeUtils.NULL : JvmTypeUtils.verificationType(type);
		stack.push(pushed);
		if (JvmTypeUtils.isWide(type))
			stack.push(JvmTypeUtils.VOID);
	}

	@Override
	public void pushNull() {
		pushType(null);
	}

	@Nullable
	@Override
	public Type peek() {
		if (stack.isEmpty())
			throw new IllegalStateException("Cannot peek from empty stack");
		Type type = stack.peek();
		return JvmTypeUtils.isNullMarker(type) ? null : type;
	}

	@Nullable
	@Override
	public Type pop() {
		try {
			Type type = stack.pop();
			return JvmTypeUtils.isNullMarker(type) ? null : type;
		} catch (NoSuchElementException e) {
			throw new IllegalStateException("Cannot pop from empty stack");
		}
	}

	@Override
	public void pop(int n) {
		for (int i = 0; i < n; i++)
			pop();
	}

	private TypedFrameImpl copyFrom(@NotNull TypedFrameImpl frame) {
		locals.putAll(frame.locals);
		stack.addAll(frame.stack);
		return this;
	}

	@NotNull
	@Override
	public TypedFrameImpl copy() {
		return new TypedFrameImpl().copyFrom(this);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		TypedFrameImpl frame = (TypedFrameImpl) o;
		return stack.equals(frame.stack) && locals.equals(frame.locals);
	}

	@Override
	public int hashCode() {
		return 31 * stack.hashCode() + locals.hashCode();
	}

	@Override
	public String toString() {
		return "Stack:" + stack.size() + ", Locals:" + locals.size();
	}

	/**
	 * @param type
	 * 		Type to normalize.
	 *
	 * @return Normalized type, or {@code null} if the type is {@code null}, TOP, or NULL.
	 */
	private static @Nullable Type normalize(@Nullable Type type) {
		if (type == null || JvmTypeUtils.isTop(type) || JvmTypeUtils.isNullMarker(type))
			return type;
		return JvmTypeUtils.verificationType(type);
	}

	/**
	 * Stack type merging.
	 *
	 * @param checker
	 * 		Inheritance checker to use for determining common super-types.
	 * @param first
	 * 		First type.
	 * @param second
	 * 		Second type.
	 * @param primary
	 * 		Frame the first type came from.
	 * @param secondary
	 * 		Frame the second type came from.
	 *
	 * @return Merged type.
	 *
	 * @throws FrameMergeException
	 * 		When the types are incompatible.
	 * @see #mergeType(InheritanceChecker, Type, Type, Frame, Frame)
	 */
	private static @NotNull Type mergeStackType(@NotNull InheritanceChecker checker,
	                                            @Nullable Type first, @Nullable Type second,
	                                            @NotNull Frame primary, @NotNull Frame secondary) throws FrameMergeException {
		if (JvmTypeUtils.VOID.equals(first) || JvmTypeUtils.VOID.equals(second)) {
			if (Objects.equals(first, second))
				return JvmTypeUtils.VOID;
			throw new FrameMergeException(primary, secondary, "Incompatible wide stack values");
		}
		return mergeType(checker, first, second, primary, secondary);
	}

	/**
	 * Local variable type merging.
	 *
	 * @param checker
	 * 		Inheritance checker to use for determining common super-types.
	 * @param first
	 * 		First type.
	 * @param second
	 * 		Second type.
	 * @param primary
	 * 		Frame the first type came from.
	 * @param secondary
	 * 		Frame the second type came from.
	 *
	 * @return Merged type.
	 *
	 * @throws FrameMergeException
	 * 		When the types are incompatible.
	 * @see #mergeType(InheritanceChecker, Type, Type, Frame, Frame)
	 */
	private static @Nullable Type mergeLocalType(@NotNull InheritanceChecker checker,
	                                             @Nullable Type first, @Nullable Type second,
	                                             @NotNull Frame primary, @NotNull Frame secondary) throws FrameMergeException {
		// Locals can be TOP, NULL, or a defined type. If either is TOP, the result is TOP.
		if (JvmTypeUtils.isTop(first) || JvmTypeUtils.isTop(second))
			return JvmTypeUtils.TOP;

		// Locals can be uninitialized, which is a special case.
		// If either is uninitialized, the result is TOP unless they are equal.
		if (JvmTypeUtils.isUninitialized(first) || JvmTypeUtils.isUninitialized(second))
			return Objects.equals(first, second) ? first : JvmTypeUtils.TOP;

		// A null local is a defined reference value. It must merge with the
		// reference type from the other path, rather than being confused with
		// an absent/TOP local.
		if (first == null)
			return second;
		if (second == null)
			return first;

		return mergeType(checker, first, second, primary, secondary);
	}

	/**
	 * Common logic for merging two types, used by both stack and local merges.
	 *
	 * @param checker
	 * 		Inheritance checker to use for determining common super-types.
	 * @param first
	 * 		First type.
	 * @param second
	 * 		Second type.
	 * @param primary
	 * 		Frame the first type came from.
	 * @param secondary
	 * 		Frame the second type came from.
	 *
	 * @return Merged type.
	 *
	 * @throws FrameMergeException
	 * 		When the types are incompatible.
	 */
	private static @NotNull Type mergeType(@NotNull InheritanceChecker checker,
	                                       @Nullable Type first, @Nullable Type second,
	                                       @NotNull Frame primary, @NotNull Frame secondary) throws FrameMergeException {
		// If either type is TOP, the result is TOP.
		if (JvmTypeUtils.isTop(first) || JvmTypeUtils.isTop(second))
			return JvmTypeUtils.TOP;

		// If either type is NULL, the result is the other type (or NULL if both are NULL).
		if (JvmTypeUtils.isNullMarker(first))
			return second == null || JvmTypeUtils.isNullMarker(second) ? JvmTypeUtils.NULL : second;
		if (JvmTypeUtils.isNullMarker(second))
			return first == null ? JvmTypeUtils.NULL : first;
		if (Objects.equals(first, second))
			return first == null ? JvmTypeUtils.NULL : first;

		// If either type is null at this point (not handled by caller cases for stack/local merging) then we have a problem.
		if (first == null || second == null)
			throw new FrameMergeException(primary, secondary, "Incompatible verifier types");

		// Merge and normalize the types using the inheritance checker.
		Type merged = AnalysisUtils.commonType(checker, first, second);
		if (merged == null)
			throw new FrameMergeException(primary, secondary, "Incompatible verifier types: " + first + " != " + second);
		return normalize(merged);
	}
}
