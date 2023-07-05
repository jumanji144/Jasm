package me.darknet.assembler.printer;

public class PrintContext<T extends PrintContext<?>> {

	protected String indent = "";
	protected String indentStep = "  ";
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
		sb.append(s).append(" ");
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

	public T newline() {
		sb.append("\n").append(indent);
		return (T) this;
	}

	public ArrayPrint array() {
		sb.append("{ ");
		return new ArrayPrint(this);
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
			if(sb.charAt(sb.length() - indent.length() - 3) == ',')
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
			if(sb.charAt(sb.length() - indent.length() - 2) == '\n')
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
			if(sb.charAt(sb.length() - 2) == ',')
				sb.delete(sb.length() - 2, sb.length() - 1);
			this.print("}");
		}
	}

}
