package me.darknet.assembler;

import me.darknet.assembler.compile.JvmCompilerOptions;
import me.darknet.assembler.compiler.InheritanceChecker;

public class TestJvmCompilerOptions extends JvmCompilerOptions {
	public TestJvmCompilerOptions() {
		inheritanceChecker = new InheritanceChecker() {
			@Override
			public boolean isSubclassOf(String child, String parent) {
				return false;
			}

			@Override
			public String getCommonSuperclass(String type1, String type2) {
				return "java/lang/Object";
			}
		};
	}
}
