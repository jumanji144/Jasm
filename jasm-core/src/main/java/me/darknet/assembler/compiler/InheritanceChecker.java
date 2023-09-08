package me.darknet.assembler.compiler;

@FunctionalInterface
public interface InheritanceChecker {

    boolean isSubclassOf(String child, String parent);

}
