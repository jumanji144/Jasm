package me.darknet.assembler.printer;

import me.darknet.assembler.util.EscapeUtil;

import static me.darknet.assembler.util.StringUtil.removeLast;

@SuppressWarnings("unchecked")
public class PrintContext<T extends PrintContext<?>> {

    public static final String FOUR_SPACE_INDENT = "    ";
    public static final String TAB_INDENT = "\t";
    public static final String NO_INDENT = "";

    protected String indent = "";
    protected String indentStep;
    protected StringBuilder sb;

    public PrintContext(String indentStep) {
        this.indentStep = indentStep;
        this.sb = new StringBuilder();
    }

    public void clear() {
        this.sb = new StringBuilder();
    }

    public PrintContext(PrintContext<?> ctx) {
        this.indentStep = ctx.indentStep;
        this.sb = ctx.sb;
        this.indent = ctx.indent;
    }

    public void setIndentStep(String indent) {
        this.indentStep = indent;
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
        append(EscapeUtil.escapeLiteral(s));
        return (T) this;
    }

    public T string(String s) {
        append('\"');
        append(EscapeUtil.escapeString(s));
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
