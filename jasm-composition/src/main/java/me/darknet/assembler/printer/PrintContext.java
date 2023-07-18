package me.darknet.assembler.printer;

import java.io.IOException;
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
	protected Appendable sb;

	public PrintContext(String indentStep, Appendable stream) {
		this.indentStep = indentStep;
		this.sb = stream;
	}

	public PrintContext(String indentStep) {
		this(indentStep, new StringBuilder());
	}

	T append(String s) {
		try {
			sb.append(s);
		} catch (IOException e) {
			// ignore
		}
		return (T) this;
	}

	T append(char c) {
		try {
			sb.append(c);
		} catch (IOException e) {
			// ignore
		}
		return (T) this;
	}

	public PrintContext(PrintContext<?> ctx) {
		this.indentStep = ctx.indentStep;
		this.sb = ctx.sb;
		this.indent = ctx.indent;
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

	public ObjectPrint object(int length) {
		append("{");
		newline();
		return new ObjectPrint(this, length);
	}

	public DeclObjectPrint declObject(int length) {
		append("{");
		newline();
		return new DeclObjectPrint(this, length);
	}

	public ArrayPrint array(int length) {
		append("{ ");
		return new ArrayPrint(this, length);
	}

	public CodePrint code(int length) {
		append("{");
		newline();
		return new CodePrint(this, length);
	}

	public T newline() {
		append("\n").append(indent);
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

		private int length = 0;
		private int index = 0;

		public ObjectPrint(PrintContext<?> ctx, int length) {
			super(ctx);
			this.length = length;
		}

		public ObjectPrint value(String key) {
			this.print(indent).print(key).print(": ");
			return this;
		}

		public ObjectPrint next() {
			if (index++ < length - 1)
				this.print(",").newline();
			return this;
		}

		@Override
		public void end() {
			this.newline().print("}");
		}
	}

	public static class DeclObjectPrint extends PrintContext<DeclObjectPrint> {

		private int length = 0;
		private int index = 0;

		public DeclObjectPrint(PrintContext<?> ctx, int length) {
			super(ctx);
			this.length = length;
		}

		public DeclObjectPrint next() {
			if (index++ < length - 1)
				this.unindent().newline().newline();
			else
				this.unindent();
			return this;
		}

		@Override
		public void end() {
			this.newline().print("}");
		}

		public DeclObjectPrint value() {
			this.indent();
			return this;
		}
	}

	public static class ArrayPrint extends PrintContext<ArrayPrint> {

		private int length = 0;
		private int index = 0;

		public ArrayPrint(PrintContext<?> ctx, int length) {
			super(ctx);
			this.length = length;
		}

		public ArrayPrint arg() {
			if (index++ < length - 1) this.print(", ");
			else this.print(" ");
			return this;
		}

		@Override
		public void end() {
			this.print("}");
		}
	}

	public static class CodePrint extends PrintContext<CodePrint> {

		private int length = 0;
		private int index = 0;

		public CodePrint(PrintContext<?> ctx, int length) {
			super(ctx);
			this.length = length;
		}

		public CodePrint instruction(String key) {
			this.indent().print(indent).print(key).print(" ");
			return this;
		}

		public CodePrint arg() {
			this.print(" ");
			return this;
		}

		public CodePrint next() {
			if (index++ < length - 1) this.unindent().newline();
			else this.unindent();
			return this;
		}

		@Override
		public void end() {
			this.newline().print(indent).print("}");
		}
	}

}
