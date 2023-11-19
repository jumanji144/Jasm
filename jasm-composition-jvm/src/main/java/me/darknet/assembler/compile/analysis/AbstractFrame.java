package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.compiler.InheritanceChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class AbstractFrame<L extends Local, S extends StackEntry> {
	private final Deque<S> stack;
	private final Map<Integer, L> locals;

	/**
	 * New frame with a given stack/variable table.
	 *
	 * @param stack
	 * 		Stack state.
	 * @param locals
	 * 		Variable table.
	 */
	public AbstractFrame(@NotNull Deque<S> stack, @NotNull Map<Integer, L> locals) {
		this.stack = stack;
		this.locals = locals;
	}

	/**
	 * New frame with an empty stack.
	 *
	 * @param locals
	 * 		Variable map to copy.
	 */
	public AbstractFrame(@NotNull Map<Integer, L> locals) {
		this(new ArrayDeque<>(), new TreeMap<>(locals));
	}

	/**
	 * New frame with an empty stack and no variables.
	 */
	public AbstractFrame() {
		this(new ArrayDeque<>(), new TreeMap<>());
	}

	protected abstract @NotNull S newStackValue(@NotNull ClassType type);

	protected abstract @NotNull ClassType newNullStackValue();


	/**
	 * Merges types of variables and stack items, taking place in this frame.
	 *
	 * @param checker
	 * 		Inheritance checker to use for determining common super-types.
	 * @param other
	 * 		Frame to merge into this one.
	 * @param <F>
	 * 		Self frame type.
	 *
	 * @return {@code true} when changes were made during the merge process.
	 * {@code false} if no changes were made, indicating equal frames.
	 *
	 * @throws FrameMergeException
	 * 		When the stack sizes do not match.
	 */
	public abstract <F extends AbstractFrame<L, S>> boolean merge(@NotNull InheritanceChecker checker, @NotNull F other) throws FrameMergeException;

	/**
	 * @return Stack.
	 */
	@NotNull
	public Deque<S> getStack() {
		return stack;
	}

	/**
	 * @return Map of variables.
	 */
	@NotNull
	public Map<Integer, L> getLocals() {
		return locals;
	}

	/**
	 * @param index
	 * 		Index of variable to assign.
	 * @param local
	 * 		Variable info to assign.
	 */
	public void setLocal(int index, L local) {
		locals.put(index, local);
	}

	/**
	 * @param index
	 * 		Index of variable to get.
	 *
	 * @return Variable info.
	 */
	@Nullable
	public L getLocal(int index) {
		return locals.get(index);
	}

	/**
	 * @param index
	 * 		Index of variable to check.
	 *
	 * @return Type of the variable, or {@code null} if not a known variable.
	 */
	@Nullable
	public ClassType getLocalType(int index) {
		L local = getLocal(index);
		if (local == null)
			return null;
		return local.type();
	}

	/**
	 * @param index
	 * 		Index of variable to check.
	 *
	 * @return {@code true} when that index is a known variable.
	 */
	public boolean hasLocal(int index) {
		return getLocals().containsKey(index);
	}

	/**
	 * @param type
	 * 		Type to push onto the stack.
	 */
	public void push(@Nullable ClassType type) {
		if (type == null)
			throw new IllegalStateException("Cannot push null as typed value to stack");
		stack.push(newStackValue(type));
	}

	/**
	 * Push null value onto the stack.
	 */
	public abstract void pushNull();

	/**
	 * @param types
	 * 		Types to push onto the stack.
	 */
	public void push(ClassType... types) {
		for (ClassType type : types) {
			push(type);
		}
	}

	/**
	 * @param type
	 * 		Type to push onto the stack.
	 */
	public void pushType(@NotNull ClassType type) {
		push(type);
		if (type == Types.LONG || type == Types.DOUBLE) {
			push(Types.VOID);
		}
	}


	/**
	 * @return Top type on the stack.
	 */
	@NotNull
	public S peek() {
		if (stack.isEmpty())
			throw new IllegalStateException("Cannot peek from empty stack");
		return stack.peek();
	}

	/**
	 * Removes the top item from the stack, returning that value.
	 *
	 * @return Top type on the stack.
	 */
	@NotNull
	public S pop() {
		try {
			return stack.pop();
		} catch (NoSuchElementException e) {
			throw new IllegalStateException("Cannot pop from empty stack");
		}
	}

	/**
	 * @param n
	 * 		Number of items to pop off the stack.
	 */
	public void pop(int n) {
		for (int i = 0; i < n; i++) {
			pop();
		}
	}

	/**
	 * @return Top type <i>(offset by one)</i> on the stack.
	 */
	@NotNull
	public S pop2() {
		pop();
		return pop();
	}

	/**
	 * @param type
	 * 		Type to pop off the stack.
	 */
	@NotNull
	public S pop(ClassType type) {
		if (type == Types.LONG || type == Types.DOUBLE) {
			return pop2();
		} else {
			return pop();
		}
	}

	@NotNull
	public abstract AbstractFrame<L, S> copyFrom(@NotNull SimpleFrame frame);

	@NotNull
	public abstract AbstractFrame<L, S> copy();
}
