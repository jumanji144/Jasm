package me.darknet.assembler.printer;

import java.util.HashMap;
import java.util.Map;

import static me.darknet.assembler.util.StringUtil.removeLast;

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

	T append(String s) {
		sb.append(s);
		return (T) this;
	}

	T append(char c) {
		sb.append(c);
		return (T) this;
	}

	public T begin() {
		return append(indent);
	}

	public void end() {
		append('\n');
	}

	public T print(String s) {
		return append(s);
	}

	public T element(String s) {
		print(s).print(" ");
		return (T) this;
	}

	public T literal(String s) {
		for (char c : s.toCharArray()) {
			String escape = LITERAL_ESCAPE_MAP.get(c);
			if (escape != null) {
				append(escape);
			} else {
				append(c);
			}
		}
		return (T) this;
	}

	public T string(String s) {
		append('\"');
		for (char c : s.toCharArray()) {
			String escape = BASE_ESCAPE_MAP.get(c);
			if (escape != null) {
				append(escape);
			} else {
				append(c);
			}
		}
		append('\"');
		return (T) this;
	}

	public ObjectPrint object() {
		append("{");
		line();
		return new ObjectPrint(this);
	}

	public DeclObjectPrint declObject() {
		append("{");
		newline();
		return new DeclObjectPrint(this);
	}

	public ArrayPrint array() {
		append("{ ");
		return new ArrayPrint(this);
	}

	public CodePrint code() {
		append("{");
		line();
		return new CodePrint(this);
	}

	public T newline() {
		append("\n").append(indent);
		return (T) this;
	}

	public T line() {
		append("\n");
		return (T) this;
	}

	public T next() {
		end();
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
			indent();
		}

		public ObjectPrint value(String key) {
			this.print(indent).print(key).print(": ");
			return this;
		}

		public ObjectPrint literalValue(String key) {
			this.print(indent).literal(key).print(": ");
			return this;
		}

		public ObjectPrint next() {
			this.print(",").line();
			return this;
		}

		@Override
		public ObjectPrint begin() {
			return this;
		}

		@Override
		public void end() {
			removeLast(sb, ",\n", 2 + indent.length());
			unindent();
			this.newline().print("}");
		}
	}

	public static class DeclObjectPrint extends PrintContext<DeclObjectPrint> {

		public DeclObjectPrint(PrintContext<?> ctx) {
			super(ctx);
		}

		public DeclObjectPrint next() {
			this.unindent().newline();
			return this;
		}

		public DeclObjectPrint doubleNext() {
			this.unindent().newline().newline();
			return this;
		}

		@Override
		public void end() {
			removeLast(sb, "\n", 1);
			this.newline().print("}");
		}

		public DeclObjectPrint begin() {
			this.indent().print(indent);
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
			removeLast(sb, ", ", 2);
			this.print(" }");
		}
	}

	public static class CodePrint extends PrintContext<CodePrint> {

		public CodePrint(PrintContext<?> ctx) {
			super(ctx);
			indent();
		}

		public CodePrint instruction(String key) {
			this.print(indent).print(key).print(" ");
			return this;
		}

		public CodePrint label(String key) {
			return this.unindent().print(indent).print(key).print(": ").indent();
		}

		public CodePrint arg() {
			this.print(" ");
			return this;
		}

		public CodePrint next() {
			this.line();
			return this;
		}

		@Override
		public void end() {
			removeLast(sb, "\n", 2);
			unindent();
			this.line().print(indent).print("}");
		}
	}

}
