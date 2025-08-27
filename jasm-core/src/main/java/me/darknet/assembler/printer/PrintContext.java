package me.darknet.assembler.printer;

import me.darknet.assembler.util.EscapeUtil;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.function.BiConsumer;

@SuppressWarnings("unchecked")
public class PrintContext<T extends PrintContext<?>> {

    public static final String FOUR_SPACE_INDENT = "    ";
    public static final String TAB_INDENT = "\t";
    public static final String NO_INDENT = "";

    protected String indent = "";
    protected String indentStep;
    protected Writer writer;
    protected String labelPrefix;
    protected boolean debugTryCatchRanges;
    protected boolean ignoreExistingVariableNames;
    protected boolean forceWholeNumberRepresentation;

    public PrintContext(String indentStep, Writer writer) {
        this.indentStep = indentStep;
        this.writer = writer;
    }

    public PrintContext(String indentStep) {
        this(indentStep, new StringWriter());
    }

    public PrintContext(PrintContext<?> ctx) {
        this.debugTryCatchRanges = ctx.debugTryCatchRanges;
        this.ignoreExistingVariableNames = ctx.ignoreExistingVariableNames;
        this.forceWholeNumberRepresentation = ctx.forceWholeNumberRepresentation;
        this.indentStep = ctx.indentStep;
        this.writer = ctx.writer;
        this.indent = ctx.indent;
    }

    public void setIndentStep(String indent) {
        this.indentStep = indent;
    }

    public void setDebugTryCatchRanges(boolean debugTryCatchRanges) {
        this.debugTryCatchRanges = debugTryCatchRanges;
    }

    public void setLabelPrefix(String labelPrefix) {
        this.labelPrefix = labelPrefix;
    }

    public void setIgnoreExistingVariableNames(boolean ignoreExistingVariableNames) {
        this.ignoreExistingVariableNames = ignoreExistingVariableNames;
    }

    public void setForceWholeNumberRepresentation(boolean forceWholeNumberRepresentation) {
        this.forceWholeNumberRepresentation = forceWholeNumberRepresentation;
    }

    T append(String s) {
        try {
            writer.append(s);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return (T) this;
    }

    T append(char c) {
        try {
            writer.append(c);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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

    public T literal(int i) {
        append(String.valueOf(i));
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
        return writer.toString();
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

        public <E> ObjectPrint print(Iterable<E> iterable, BiConsumer<ObjectPrint, E> printer) {
            Iterator<E> iterator = iterable.iterator();
            if (iterator.hasNext()) {
                printer.accept(this, iterator.next());
                while (iterator.hasNext()) {
                    printer.accept(next(), iterator.next());
                }
            }
            return this;
        }

        @Override
        public ObjectPrint begin() {
            return this;
        }

        @Override
        public void end() {
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
            this.print("}");
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
            print(", ");
            return this;
        }

        public <E> ArrayPrint print(Iterable<E> iterable, BiConsumer<ArrayPrint, E> printer) {
            Iterator<E> iterator = iterable.iterator();
            if (iterator.hasNext()) {
                printer.accept(this, iterator.next());
                while (iterator.hasNext()) {
                    printer.accept(arg(), iterator.next());
                }
            }
            return this;
        }

        public <E> ArrayPrint printIndented(Iterable<E> iterable, BiConsumer<ArrayPrint, E> printer) {
            Iterator<E> iterator = iterable.iterator();
            if (iterator.hasNext()) {
                indent();
                newline();
                printer.accept(this, iterator.next());
                while (iterator.hasNext()) {
                    printer.accept(arg().newline(), iterator.next());
                }
                unindent();
            }
            newline();
            return this;
        }

        @Override
        public void end() {
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
            this.unindent().print(indent).print(key).print(": ").indent();
            return this;
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
            unindent();
            this.print(indent).print("}");
        }
    }
}
