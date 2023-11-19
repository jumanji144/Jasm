package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.compiler.InheritanceChecker;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SimpleFrame extends AbstractFrame<SimpleLocal, SimpleStackEntry> {
	private final Deque<SimpleStackEntry> stack;
	private final Map<Integer, SimpleLocal> locals;

	/**
	 * New frame with a given stack/variable table.
	 *
	 * @param stack
	 * 		Stack state.
	 * @param locals
	 * 		Variable table.
	 */
	public SimpleFrame(@NotNull Deque<SimpleStackEntry> stack, @NotNull Map<Integer, SimpleLocal> locals) {
		this.stack = stack;
		this.locals = locals;
	}

	/**
	 * New frame with an empty stack.
	 *
	 * @param locals
	 * 		Variable map to copy.
	 */
	public SimpleFrame(@NotNull Map<Integer, SimpleLocal> locals) {
		this(new ArrayDeque<>(), new TreeMap<>(locals));
	}

	/**
	 * New frame with an empty stack and no variables.
	 */
	public SimpleFrame() {
		this(new ArrayDeque<>(), new TreeMap<>());
	}

	@Override
	public <F extends AbstractFrame<SimpleLocal, SimpleStackEntry>> boolean merge(@NotNull InheritanceChecker checker, @NotNull F other) throws FrameMergeException {
		boolean changed = false;
		for (Map.Entry<Integer, SimpleLocal> entry : other.getLocals().entrySet()) {
			int index = entry.getKey();
			SimpleLocal otherLocal = entry.getValue();
			ClassType otherType = otherLocal.type();
			ClassType ourType = getLocalType(index);
			if (otherType == Types.VOID || ourType == Types.VOID) {
				continue;
			}
			if (ourType == null) {
				changed = true;
				setLocal(index, otherLocal);
			} else {
				ClassType merged = Commons.commonType(checker, ourType, otherType);
				if (!Objects.equals(merged, ourType)) {
					changed = true;
					locals.put(index, otherLocal.adaptType(merged));
				}
			}
		}

		if (stack.size() != other.getStack().size())
			throw new FrameMergeException(this, other, "Stack size mismatch, " + stack.size() + " != " + other.getStack().size());

		Deque<SimpleStackEntry> newStack = new ArrayDeque<>();
		Iterator<SimpleStackEntry> it1 = stack.iterator();
		Iterator<SimpleStackEntry> it2 = other.getStack().iterator();
		while (it1.hasNext() && it2.hasNext()) {
			SimpleStackEntry entry1 = it1.next();
			SimpleStackEntry entry2 = it2.next();
			ClassType type1 = entry1.type();
			ClassType type2 = entry2.type();
			if (type1 == Types.VOID || type2 == Types.VOID) {
				newStack.add(SimpleStackEntry.VOID);
				continue;
			}
			ClassType merged = Commons.commonType(checker, type1, type2);
			if (!Objects.equals(merged, type1)) {
				changed = true;
				it1.remove();
				newStack.add(SimpleStackEntry.get(merged));
			} else {
				newStack.add(entry1);
			}
		}
		stack.clear();
		stack.addAll(newStack);
		return changed;
	}

	/**
	 * @return Stack.
	 */
	@NotNull
	@Override
	public Deque<SimpleStackEntry> getStack() {
		return stack;
	}

	/**
	 * @return Map of variables.
	 */
	@NotNull
	@Override
	public Map<Integer, SimpleLocal> getLocals() {
		return locals;
	}

	@Override
	public void pushNull() {
		push(newNullStackValue());
	}

	@Override
	protected @NotNull SimpleStackEntry newStackValue(@NotNull ClassType type) {
		return SimpleStackEntry.get(type);
	}

	@Override
	protected @NotNull ClassType newNullStackValue() {
		return Commons.OBJECT;
	}

	/**
	 * @param frame
	 * 		Frame to copy stack/locals from.
	 *
	 * @return Self.
	 */
	@NotNull
	@Override
	public SimpleFrame copyFrom(@NotNull SimpleFrame frame) {
		locals.putAll(frame.locals);
		stack.addAll(frame.stack);
		return this;
	}

	/**
	 * @return Copy of the current frame.
	 */
	@NotNull
	@Override
	public SimpleFrame copy() {
		return new SimpleFrame().copyFrom(this);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		SimpleFrame frame = (SimpleFrame) o;

		if (!stack.equals(frame.stack)) return false;
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
