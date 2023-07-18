package me.darknet.assembler;

import dev.xdark.blw.BytecodeLibrary;
import dev.xdark.blw.asm.AsmBytecodeLibrary;
import dev.xdark.blw.asm.ClassWriterProvider;
import org.objectweb.asm.ClassWriter;

public class JasmInterface {

	public static final BytecodeLibrary LIBRARY =
			new AsmBytecodeLibrary(ClassWriterProvider.flags(ClassWriter.COMPUTE_FRAMES));

}
