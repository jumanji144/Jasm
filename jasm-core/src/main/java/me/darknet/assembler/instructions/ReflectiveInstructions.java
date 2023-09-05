package me.darknet.assembler.instructions;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.exception.TransformationException;
import me.darknet.assembler.visitor.ASTInstructionVisitor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ReflectiveInstructions<V extends ASTInstructionVisitor> extends Instructions<V> {

    private final Class<V> visitorClass;
    private final Map<String, MethodHandle> lookupCache = new HashMap<>();

    public ReflectiveInstructions(Class<V> visitorClass, String defaultMethod) {
        super();
        this.defaultTranslator = (instruction, visitor) -> reflectiveAction(instruction, visitor, defaultMethod);
        this.visitorClass = visitorClass;
    }

    protected void register(String name, String translatorName) {
        register(
                name, new Operand[0], (instruction, visitor) -> reflectiveAction(instruction, visitor, translatorName)
        );
    }

    protected void register(String name, Operand[] operands, String translatorName) {
        register(name, operands, (instruction, visitor) -> reflectiveAction(instruction, visitor, translatorName));
    }

    void reflectiveAction(ASTInstruction instruction, V instance, String translatorName)
            throws TransformationException {
        try {
            MethodHandle method = lookupCache.get(translatorName);
            if (method == null) {
                List<ASTElement> arguments = instruction.arguments();
                Class<?>[] argumentTypes = new Class<?>[arguments.size()];
                for (int i = 0; i < arguments.size(); i++) {
                    argumentTypes[i] = arguments.get(i).getClass();
                }
                method = MethodHandles.lookup().unreflect(visitorClass.getMethod(translatorName, argumentTypes));
                lookupCache.put(translatorName, method);
            }
            List<Object> arguments = new ArrayList<>(instruction.arguments());
            arguments.add(0, instance);
            method.invokeWithArguments(arguments);
        } catch (Throwable e) {
            throw new TransformationException("Failed to invoke translator method", e);
        }
    }
}
