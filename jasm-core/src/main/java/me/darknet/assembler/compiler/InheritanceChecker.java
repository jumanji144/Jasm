package me.darknet.assembler.compiler;

public interface InheritanceChecker {

    boolean isSubclassOf(String child, String parent);

    String getCommonSuperclass(String type1, String type2);

}
