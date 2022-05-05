package me.darknet.assembler;

import me.darknet.assembler.compiler.Compiler;
import me.darknet.assembler.parser.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class Assembler {

    public static ParserContext parse(String path) throws IOException, AssemblerException {
        return parse(Files.readAllBytes(Path.of(path)));
    }

    public static ParserContext parse(byte[] bytes) throws AssemblerException {
        Parser parser = new Parser();
        List<Token> tokens = parser.tokenize(null, new String(bytes));

        ParserContext ctx = new ParserContext(new LinkedList<>(tokens), parser);

        ctx.parse();

        return ctx;
    }

    public static byte[] assemble(int classVersion, String path) throws IOException, AssemblerException {
        return assemble(classVersion, Files.readAllBytes(Path.of(path)));
    }

    public static byte[] assemble(int classVersion, byte[] bytes) throws AssemblerException {
        ParserContext ctx = parse(bytes);
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
            byte[] bytes = assemble(classVersion, path);
            try {
                Files.write(Path.of(output), bytes);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        } catch (AssemblerException e) {
            System.err.println(e.describe());
        } catch (NoSuchFileException e) {
            System.err.println("File not found: " + e.getFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
