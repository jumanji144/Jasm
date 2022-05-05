package me.darknet.assembler.compiler;

import me.darknet.assembler.instructions.ParseInfo;
import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Parser;
import me.darknet.assembler.parser.ParserContext;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.*;

import static me.darknet.assembler.parser.Group.GroupType;
import static org.objectweb.asm.Opcodes.*;

public class Compiler {

    public ClassWriter cw;
    public int version;

    public String className;

    public List<Method> methods = new ArrayList<>();

    public Compiler(int version) {
        cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        // compute max stack and local variables needed because they are not known at compile time
        this.version = version;
    }

    public void compile(ParserContext ctx) throws AssemblerException {

        for (Group g : ctx.groups) {
            switch (g.type) {
                case CLASS_DECLARATION: {
                    int access = getAccess(g.getChild(GroupType.ACCESS_MODS));
                    String fullyQualifiedClassName = g.getChild(GroupType.IDENTIFIER).content();
                    String superClassName = "java/lang/Object";
                    List<String> interfaces = new ArrayList<>();
                    // check if super class is specified
                    if (g.size() > 2) {
                        Group extendsGroup = g.getChild(GroupType.EXTENDS_DIRECTIVE);
                        if (extendsGroup != null) {
                            superClassName = extendsGroup.getChild(GroupType.IDENTIFIER).content();
                        }
                    }
                    cw.visit(version, access, fullyQualifiedClassName, null, superClassName, interfaces.toArray(new String[0]));
                    className = fullyQualifiedClassName;
                    break;
                }
                case FIELD_DECLARATION: {
                    int access = getAccess(g.getChild(GroupType.ACCESS_MODS));
                    String name = g.get(1).content();
                    String desc = g.get(2).content();

                    cw.visitField(access, name, desc, null, null);
                    break;
                }
                case METHOD_DECLARATION: {
                    int access = getAccess(g.getChild(GroupType.ACCESS_MODS));
                    String methodDesc = g.get(1).content();

                    MethodDescriptor md = new MethodDescriptor(methodDesc);
                    MethodVisitor vs = cw.visitMethod(access, md.name, md.desc, null, null);

                    boolean isStatic = (access & ACC_STATIC) != 0;

                    Method method = new Method(vs, md.name, isStatic);

                    Group body = g.getChild(GroupType.BODY);
                    method.compile(body);

                    method.verify();

                    methods.add(method);

                    vs.visitMaxs(0, 0);
                    vs.visitEnd();
                }
            }
        }

    }

    public int getAccess(Group access) {
        int accessFlags = 0;
        for (Group g : access.getChildren()) {
            switch (g.content()) {
                case Parser.KEYWORD_PUBLIC:
                    accessFlags |= ACC_PUBLIC;
                    break;
                case Parser.KEYWORD_PRIVATE:
                    accessFlags |= ACC_PRIVATE;
                    break;
                case Parser.KEYWORD_STATIC:
                    accessFlags |= ACC_STATIC;
                    break;
            }
        }
        return accessFlags;
    }

    public byte[] finish() throws AssemblerException {
        return cw.toByteArray();
    }

}
