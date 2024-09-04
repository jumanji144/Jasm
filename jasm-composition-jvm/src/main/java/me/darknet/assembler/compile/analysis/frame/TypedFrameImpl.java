package me.darknet.assembler.compile.analysis.frame;

import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.compile.analysis.AnalysisUtils;
import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compiler.InheritanceChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TypedFrameImpl implements TypedFrame {
	/** Do not use the {@link Types#OBJECT} - We want a new instance for identity comparison */
	private static final ClassType NULL = Types.instanceTypeFromInternalName("java/lang/Object");
	private final Deque<ClassType> stack;
	private final Map<Integer, Local> locals;

	/**
	 * New frame with a given stack/variable table.
	 *
	 * @param stack
	 * 		Stack state.
	 * @param locals
	 * 		Variable table.
	 */
	public TypedFrameImpl(@NotNull Deque<ClassType> stack, @NotNull Map<Integer, Local> locals) {
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
		if (other instanceof TypedFrameImpl simpleOther)
			return merge(checker, simpleOther);
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
	@SuppressWarnings("AssignmentUsedAsCondition")
	public boolean merge(@NotNull InheritanceChecker checker, @NotNull TypedFrame other) throws FrameMergeException {
		boolean changed = false;
		for (Map.Entry<Integer, Local> entry : other.getLocals().entrySet()) {
			int index = entry.getKey();
			Local otherLocal = entry.getValue();
			ClassType otherType = otherLocal.type();
			ClassType ourType = getLocalType(index);

			// Skip top-type entries
			if (otherType == Types.VOID || ourType == Types.VOID)
				continue;

			if (ourType == null) {
				// Our frame doesn't have the local variable.
				// Copy it if we know the type.
				setLocal(index, otherLocal);

				// If we are learning the type of the variable where it was previously 'null'
				// then we will mark the frame as being changed. But if the other frame also has 'null'
				// as the type then we don't want to be marked as changed (will cause an infinite loop).
				changed = otherType != null;
			} else if (otherLocal.isNull()) {
				// Our frame can be updated to fill in 'null' with a known type.
				setLocal(index, otherLocal.adaptType(ourType));
			} else {
				ClassType merged = AnalysisUtils.commonType(checker, ourType, otherType);
				if (!Objects.equals(merged, ourType)) {
					changed = true;
					setLocal(index, otherLocal.adaptType(Objects.requireNonNullElse(merged, Types.OBJECT)));
				}
			}
		}

		Deque<ClassType> otherStack = other.getStack();
		int stackSize = stack.size();
		if (stackSize != otherStack.size())
			throw new FrameMergeException(this, other,
					"Stack size mismatch, " + stackSize + " != " + otherStack.size());

		Deque<ClassType> newStack = new ArrayDeque<>(stackSize);
		Iterator<ClassType> it1 = stack.iterator();
		Iterator<ClassType> it2 = otherStack.iterator();
		while (it1.hasNext() && it2.hasNext()) {
			ClassType type1 = it1.next();
			ClassType type2 = it2.next();
			if (Objects.equals(type1, type2)) {
				newStack.add(type1);
				continue;
			} else if (type1 == Types.VOID || type2 == Types.VOID) {
				newStack.add(Types.VOID);
				continue;
			}
			ClassType merged = AnalysisUtils.commonType(checker, type1, type2);
			if (!Objects.equals(merged, type1)) {
				changed = true;
				it1.remove();
				newStack.add(merged);
			} else {
				newStack.add(type1);
			}
		}
		stack.clear();
		stack.addAll(newStack);
		return changed;
	}

	@NotNull
	@Override
	public Deque<ClassType> getStack() {
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
	public void pushType(@Nullable ClassType type) {
		if (type == null)
			stack.push(NULL);
		else
			stack.push(type);
		if (type == Types.LONG || type == Types.DOUBLE)
			stack.push(Types.VOID);
	}

	@Override
	public void pushNull() {
		pushType(NULL);
	}

	@Nullable
	@Override
	public ClassType peek() {
		if (stack.isEmpty())
			throw new IllegalStateException("Cannot peek from empty stack");
		ClassType type = stack.peek();
		if (type == NULL)
			return null;
		return type;
	}

	@Nullable
	@Override
	public ClassType pop() {
		try {
			ClassType type = stack.pop();
			if (type == NULL)
				return null;
			return type;
		} catch (NoSuchElementException e) {
			throw new IllegalStateException("Cannot pop from empty stack");
		}
	}

	@Override
	public void pop(int n) {
		for (int i = 0; i < n; i++) {
			pop();
		}
	}

	/**
	 * @param frame
	 * 		Frame to copy stack/locals from.
	 *
	 * @return Self.
	 */
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

		if (!stack.equals(frame.stack))
			return false;
		return locals.equals(frame.locals);
	}

	@Override
	public int hashCode() {
		int result = stack.hashCode();
		result = 31 * result + locals.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "Stack:" + stack.size() + ", Locals:" + locals.size();
	}
}
