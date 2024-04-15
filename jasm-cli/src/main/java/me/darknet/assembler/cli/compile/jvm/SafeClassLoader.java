package me.darknet.assembler.cli.compile.jvm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;

public final class SafeClassLoader extends URLClassLoader {

    private final URL[] urls;

    public SafeClassLoader(URL[] urls) {
        super(urls, ClassLoader.getSystemClassLoader().getParent());
        this.urls = urls;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        InputStream in = getResourceAsStream(name.replace('.', '/') + ".class");
        if (in == null) {
            throw new ClassNotFoundException(name);
        }
        ClassReader cr;
        try (in) {
            cr = new ClassReader(in);
        } catch (IOException ex) {
            throw new ClassNotFoundException(name, ex);
        }
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, cr.getAccess(), cr.getClassName(), null, cr.getSuperName(), cr.getInterfaces());
        byte[] bc = cw.toByteArray();
        return defineClass(null, bc, 0, bc.length);
    }

    @Override
    public URL findResource(String name) {
        for (URL url : urls) {
            if (url.getFile().endsWith(name)) {
                return url;
            }
        }
        return super.findResource(name);
    }

    static {
        ClassLoader.registerAsParallelCapable();
    }
}