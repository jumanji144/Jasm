package me.darknet.assembler.compiler;

public class EmptyInheritanceChecker implements InheritanceChecker {

    public static final EmptyInheritanceChecker INSTANCE = new EmptyInheritanceChecker();

    @Override
    public boolean isSubclassOf(String child, String parent) {
        return false;
    }

    @Override
    public String getCommonSuperclass(String type1, String type2) {
        return "java/lang/Object";
    }
}
