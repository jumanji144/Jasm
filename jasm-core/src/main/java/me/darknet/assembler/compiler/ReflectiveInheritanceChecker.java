package me.darknet.assembler.compiler;

public class ReflectiveInheritanceChecker implements InheritanceChecker {
    public static final ReflectiveInheritanceChecker INSTANCE =
            new ReflectiveInheritanceChecker(ReflectiveInheritanceChecker.class.getClassLoader());

    private final ClassLoader loader;

    public ReflectiveInheritanceChecker(ClassLoader loader) {
        this.loader = loader;
    }

    @Override
    public boolean isSubclassOf(String child, String parent) {
        try {
            Class<?> childClass = Class.forName(child.replace('/', '.'), false, this.loader);
            Class<?> parentClass = Class.forName(parent.replace('/', '.'), false, this.loader);
            return parentClass.isAssignableFrom(childClass);
        } catch (ClassNotFoundException e) {
            String missingType = e.getMessage();
            throw new TypeNotPresentException(missingType, e);
        }
    }

    @Override
    public String getCommonSuperclass(String type1, String type2) {
        try {
            Class<?> class1 = Class.forName(type1.replace('/', '.'), false, this.loader);
            Class<?> class2 = Class.forName(type2.replace('/', '.'), false, this.loader);
            if (class1.isAssignableFrom(class2))
                return type1;
            if (class2.isAssignableFrom(class1))
                return type2;
            if (class1.isInterface() || class2.isInterface())
                return "java/lang/Object";

            do {
                class1 = class1.getSuperclass();
            } while (!class1.isAssignableFrom(class2));
            return class1.getName().replace('.', '/');
        } catch (ClassNotFoundException e) {
            String missingType = e.getMessage();
            throw new TypeNotPresentException(missingType, e);
        }
    }
}
