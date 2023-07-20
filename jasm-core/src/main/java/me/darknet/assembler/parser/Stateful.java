package me.darknet.assembler.parser;

import java.util.LinkedList;

public class Stateful<S> {

	private final LinkedList<S> stack = new LinkedList<>();

	public void enterState(S state) {
		this.stack.push(state);
	}

	public void leaveState() {
		this.stack.poll();
	}

	/**
	 * Purely cosmetic method to make the code more readable
	 *
	 * @param value state to indicate which one you are leaving
	 * @param <T>   used to allow any value to be passed in
	 */
	public <T> void leaveState(T value) {
		this.stack.poll();
	}

	public S getState() {
		return stack.peek();
	}

	public boolean isInState(S state) {
		return stack.contains(state);
	}

	public boolean isCurrentState(S state) {
		return stack.peek() == state;
	}

}
