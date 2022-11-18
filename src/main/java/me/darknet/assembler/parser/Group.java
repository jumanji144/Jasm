package me.darknet.assembler.parser;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A group of tokens representing a logical expression.
 */
@Getter
public class Group {
	private final GroupType type;
	private final Token value;
	private final List<Group> children;
	private Group parent;
	@Setter
	private Location fallbackLocation;

	public Group(GroupType type, Token value) {
		this(type, value, Collections.emptyList());
	}

	public Group(GroupType type, List<? extends Group> children) {
		this(type, null, children);
	}

	public Group(GroupType type, Token value, Group... children) {
		this(type, value, Arrays.asList(children));
	}

	@SuppressWarnings("unchecked")
	public Group(GroupType type, Token value, List<? extends Group> children) {
		for (Group child : children) {
			if(child != null) {
				child.parent = this;
			}
		}
		this.type = type;
		this.value = value;
		this.children = (List<Group>) children;
	}

	/**
	 * @param type
	 * 		Type to check against.
	 *
	 * @return {@code true} when this {@link #getType()} matches the given type.
	 */
	public boolean isType(GroupType type) {
		return Objects.equals(this.type, type);
	}

	/**
	 * @return First token in group.
	 */
	public Token start() {
		if (children.isEmpty()) {
			return value;
		}
		return children.get(0).start();
	}

	/**
	 * @return Last token in group.
	 */
	public Token end() {
		if (children.isEmpty()) {
			return value;
		}
		return children.get(children.size() - 1).end();
	}

	public String content() {
		return value.getContent();
	}

	public Group get(int index) {
		return children.get(index);
	}

	public Location getStartLocation() {
		if (value == null) {
			if (children.isEmpty()) {
				if (fallbackLocation != null)
					return fallbackLocation;
				return new Location(-1, -1, "invalid", -1);
			}
			return children.get(0).getStartLocation();
		}
		return value.getLocation();
	}

	public Location getEndLocation() {
		Token end = end();
		if (end != null)
			return end.getLocation().add(end.getWidth());
		return getStartLocation();
	}

	@SuppressWarnings("unchecked")
	public <T> T getChild(Class<T> type) throws AssemblerException {
		for (Group child : children) {
			if (child.getClass() == type) {
				return (T) child;
			}
		}
		throw new AssemblerException("No child of type " + type + " found", getStartLocation());
	}

	public int size() {
		return children.size();
	}

	public Group getChild(GroupType type) {
		for (Group child : children) {
			if (child.type == type) {
				return child;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return "Group(" + type + "," + value + "," + children.size() + ")";
	}

	public enum GroupType {
		// Pass through of token types to allow for easy extension.
		NUMBER,
		STRING,
		IDENTIFIER,
		KEYWORD,
		ACCESS_MOD,
		ACCESS_MODS,
		LABEL,

		LOOKUP_SWITCH,
		CASE_LABEL,
		DEFAULT_LABEL,

		HANDLE,
		ARGS,
		TYPE,

		TABLE_SWITCH,
		CATCH,
		MACRO_DIRECTIVE, // TODO: Remove this.

		// higher order abstraction
		CLASS_DECLARATION,
		METHOD_DECLARATION,
		FIELD_DECLARATION,

		EXTENDS_DIRECTIVE,
		IMPLEMENTS_DIRECTIVE,
		SIGNATURE_DIRECTIVE,
		VERSION_DIRECTIVE,
		SOURCE_FILE_DIRECTIVE,
		INNER_CLASS_DIRECTIVE,
		NEST_HOST_DIRECTIVE,
		NEST_MEMBER_DIRECTIVE,
		PERMITTED_SUBCLASS_DIRECTIVE,
		BODY,
		END_BODY,

		ANNOTATION,
		ANNOTATION_PARAMETER,
		ENUM,
		INVISIBLE_ANNOTATION,
		MODULE,
		MODULE_REQUIRE,
		MODULE_EXPORT,
		MODULE_OPEN,
		MODULE_USE,
		MODULE_PROVIDE,
		MODULE_MAIN_CLASS,
		MODULE_PACKAGE,
		MODULE_TO,
		MODULE_WITH,
		INSTRUCTION,
		STACK_LIMIT,
		LOCAL_LIMIT,
		RETURN, THROWS, EXPR, METHOD_PARAMETER, TEXT, METHOD_PARAMETERS,

	}
}

