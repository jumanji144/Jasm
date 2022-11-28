package me.darknet.assembler;

import me.darknet.assembler.compiler.Compiler;
import me.darknet.assembler.exceptions.AssemblerException;
import me.darknet.assembler.parser.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class Assembler {

    public static ParserContext parse(String path) throws IOException, AssemblerException {
        return parse("stdin", Files.readAllBytes(Paths.get(path)));
    }

    public static ParserContext parse(String source, byte[] bytes) throws AssemblerException {
        Parser parser = new Parser();
        List<Token> tokens = parser.tokenize(source, new String(bytes));

        ParserContext ctx = new ParserContext(new LinkedList<>(tokens), parser);
        ctx.setOneLine(true);

        ctx.parse();

        return ctx;
    }

    public static byte[] assemble(int classVersion, String path) throws IOException, AssemblerException {
        return assemble(path, classVersion, Files.readAllBytes(Paths.get(path)));
    }

    public static byte[] assemble(String source, int classVersion, byte[] bytes) throws AssemblerException {
        ParserContext ctx = parse(source, bytes);
        Compiler compiler = new Compiler(classVersion);
        compiler.compile(ctx);
        return compiler.finish();
    }

    public static void main(String[] args) throws IOException, AssemblerException {
        if(args.length != 3) {
            System.out.println("Usage: java -jar assembler.jar <class version target> <path to file> <output>");
        }

        String path = args[1];
        String output = args[2];

        int classVersion = Integer.parseInt(args[0]);

        try {
            long start = System.nanoTime();
            byte[] bytes = assemble(classVersion, path);
            long end = System.nanoTime();
            System.out.println("Assembled in " + (end - start) / 1000000 + "ms");
            try {
                Files.write(Paths.get(output), bytes);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        } catch (AssemblerException e) {
            System.err.println(e.describe());
            e.printStackTrace();
        } catch (NoSuchFileException e) {
            System.err.println("File not found: " + e.getFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
