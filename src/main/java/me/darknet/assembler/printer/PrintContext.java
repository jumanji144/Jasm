package me.darknet.assembler.printer;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class PrintContext<T extends PrintContext<?>> {

	private static final Map<Character, String> BASE_ESCAPE_MAP = Map.ofEntries(
			Map.entry('\b', "\\b"),
			Map.entry('\t', "\\t"),
			Map.entry('\n', "\\n"),
			Map.entry('\f', "\\f"),
			Map.entry('\r', "\\r"),
			Map.entry('\"', "\\\""),
			Map.entry('\\', "\\\\"),
			Map.entry('\'', "\\'")
	);
	private static final Map<Character, String> LITERAL_ESCAPE_MAP = new HashMap<>();

	static {
		LITERAL_ESCAPE_MAP.putAll(BASE_ESCAPE_MAP);
		LITERAL_ESCAPE_MAP.put(' ', "\\u0020");
		LITERAL_ESCAPE_MAP.put(',', "\\u002C");
		LITERAL_ESCAPE_MAP.put(':', "\\u003A");
		LITERAL_ESCAPE_MAP.put('{', "\\u007B");
		LITERAL_ESCAPE_MAP.put('}', "\\u007D");
	}

	protected String indent = "";
	protected String indentStep;
	protected StringBuilder sb;

	public PrintContext(String indentStep) {
		this.indentStep = indentStep;
		this.sb = new StringBuilder();
	}

	public PrintContext(PrintContext<?> ctx) {
		this.indentStep = ctx.indentStep;
		this.sb = ctx.sb;
		this.indent = ctx.indent;
	}

	public T begin() {
		sb.append(getIndent());
		return (T) this;
	}

	public void end() {
		sb.append("\n");
	}

	public T print(String s) {
		sb.append(s);
		return (T) this;
	}

	public T element(String s) {
		print(s).print(" ");
		return (T) this;
	}

	public T literal(String s) {
		for (char c : s.toCharArray()) {
			String escape = LITERAL_ESCAPE_MAP.get(c);
			if (escape != null) {
				sb.append(escape);
			} else {
				sb.append(c);
			}
		}
		return (T) this;
	}

	public T string(String s) {
		sb.append("\"");
		for (char c : s.toCharArray()) {
			String escape = BASE_ESCAPE_MAP.get(c);
			if (escape != null) {
				sb.append(escape);
			} else {
				sb.append(c);
			}
		}
		sb.append("\"");
		return (T) this;
	}

	public ObjectPrint object() {
		sb.append("{");
		newline();
		return new ObjectPrint(this);
	}

	public DeclObjectPrint declObject() {
		sb.append("{");
		newline();
		return new DeclObjectPrint(this);
	}

	public ArrayPrint array() {
		sb.append("{ ");
		return new ArrayPrint(this);
	}

	public CodePrint code() {
		sb.append("{");
		newline();
		return new CodePrint(this);
	}

	public T newline() {
		sb.append("\n").append(indent);
		return (T) this;
	}

	public T next() {
		return (T) this;
	}

	public T indent() {
		indent += indentStep;
		return (T) this;
	}

	public String toString() {
		return sb.toString();
	}

	public T unindent() {
		indent = indent.substring(0, indent.length() - indentStep.length());
		return (T) this;
	}

	public String getIndent() {
		return indent;
	}

	public static class ObjectPrint extends PrintContext<ObjectPrint> {

		public ObjectPrint(PrintContext<?> ctx) {
			super(ctx);
		}

		public ObjectPrint value(String key) {
			this.print(indent).print(key).print(": ");
			return this;
		}

		public ObjectPrint next() {
			this.print(", ").newline();
			return this;
		}

		@Override
		public void end() {
			// remove last comma
			if (sb.charAt(sb.length() - indent.length() - 3) == ',')
				sb.delete(sb.length() - indent.length() - 3, sb.length() - indent.length());
			this.newline().print("}");
		}
	}

	public static class DeclObjectPrint extends PrintContext<DeclObjectPrint> {

		public DeclObjectPrint(PrintContext<?> ctx) {
			super(ctx);
		}

		public DeclObjectPrint next() {
			this.unindent().newline().newline();
			return this;
		}

		@Override
		public void end() {
			// remove last newline
			if (sb.charAt(sb.length() - indent.length() - 2) == '\n')
				sb.delete(sb.length() - indent.length() - 2, sb.length() - indent.length() - 1);
			this.newline().print("}");
		}

		public DeclObjectPrint value() {
			this.indent();
			return this;
		}
	}

	public static class ArrayPrint extends PrintContext<ArrayPrint> {

		public ArrayPrint(PrintContext<?> ctx) {
			super(ctx);
		}

		public ArrayPrint arg() {
			this.print(", ");
			return this;
		}

		@Override
		public void end() {
			// remove last comma
			if (sb.charAt(sb.length() - 2) == ',')
				sb.delete(sb.length() - 2, sb.length() - 1);
			this.print("}");
		}
	}

	public static class CodePrint extends PrintContext<CodePrint> {

		public CodePrint(PrintContext<?> ctx) {
			super(ctx);
		}

		public CodePrint instruction(String key) {
			this.indent().print(indent).print(key);
			return this;
		}

		public CodePrint arg() {
			this.print(" ");
			return this;
		}

		public CodePrint next() {
			this.unindent().newline();
			return this;
		}

		@Override
		public void end() {
			// remove last newline
			if (sb.charAt(sb.length() - indent.length() - 1) == '\n')
				sb.delete(sb.length() - indent.length() - 1, sb.length() - indent.length());
			this.newline().print(indent).print("}");
		}
	}

}
