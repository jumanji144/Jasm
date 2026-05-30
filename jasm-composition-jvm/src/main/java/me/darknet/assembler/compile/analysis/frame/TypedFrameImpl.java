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
	private static final Type NULL = Type.getObjectType("java/lang/Object");
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
	public boolean merge(@NotNull InheritanceChecker checker, @NotNull TypedFrame other) throws FrameMergeException {
		boolean changed = false;
		for (Map.Entry<Integer, Local> entry : other.getLocals().entrySet()) {
			int index = entry.getKey();
			Local otherLocal = entry.getValue();
			Type otherType = otherLocal.type();
			Type ourType = getLocalType(index);

			if (JvmTypeUtils.VOID.equals(otherType) || JvmTypeUtils.VOID.equals(ourType))
				continue;

			if (!hasLocal(index)) {
				// If we don't have the local, copy it from the other frame.
				// We do not set 'changed' since expanding local variable scope is not going to change
				// behavior of frames that previously passed analysis.
				setLocal(index, otherLocal);
			} else {
				Type merged = AnalysisUtils.commonType(checker, ourType, otherType);
				if (!Objects.equals(merged, ourType)) {
					if (merged == null) {
						// Value is explicitly 'null'
						setLocal(index, new Local(index, otherLocal.name(), null));
					} else {
						// Value is some known type (we don't care if it *can* be null or not,
						// just not provably null)
						setLocal(index, otherLocal.adaptType(merged));
					}
					changed = true;
				}
			}
		}

		Deque<Type> otherStack = other.getStack();
		int stackSize = stack.size();
		if (stackSize != otherStack.size())
			throw new FrameMergeException(this, other,
					"Stack size mismatch, " + stackSize + " != " + otherStack.size());

		Deque<Type> newStack = new ArrayDeque<>(stackSize);
		Iterator<Type> it1 = stack.iterator();
		Iterator<Type> it2 = otherStack.iterator();
		while (it1.hasNext() && it2.hasNext()) {
			Type type1 = it1.next();
			Type type2 = it2.next();
			if (Objects.equals(type1, type2)) {
				newStack.add(type1);
				continue;
			} else if (JvmTypeUtils.VOID.equals(type1) || JvmTypeUtils.VOID.equals(type2)) {
				newStack.add(JvmTypeUtils.VOID);
				continue;
			}
			Type merged = AnalysisUtils.commonType(checker, type1, type2);
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
		if (type == null)
			stack.push(NULL);
		else
			stack.push(type);
		if (JvmTypeUtils.isWide(type))
			stack.push(JvmTypeUtils.VOID);
	}

	@Override
	public void pushNull() {
		pushType(NULL);
	}

	@Nullable
	@Override
	public Type peek() {
		if (stack.isEmpty())
			throw new IllegalStateException("Cannot peek from empty stack");
		Type type = stack.peek();
		if (type == NULL)
			return null;
		return type;
	}

	@Nullable
	@Override
	public Type pop() {
		try {
			Type type = stack.pop();
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
