package me.darknet.assembler.util;

import me.darknet.assembler.compiler.MethodDescriptor;
import me.darknet.assembler.compiler.impl.CachedClass;
import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.groups.HandleGroup;
import me.darknet.assembler.parser.groups.NumberGroup;
import me.darknet.assembler.parser.groups.TypeGroup;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Map;

public class GroupUtil {

    /**
     * Converts a group object to an ASM acceptable object.
     * @param container The current class object.
     * @param group The group object to convert.
     * @return The converted object.
     * @throws AssemblerException If the group object is not valid.
     */
    public static Object convert(CachedClass container, Group group) throws AssemblerException {
        if (group.getType() == Group.GroupType.NUMBER) {
            return ((NumberGroup) group).getNumber();
        } else if(group.type == Group.GroupType.TYPE) {
            TypeGroup typeGroup = (TypeGroup) group;
            try {
                return Type.getObjectType(typeGroup.descriptor.content());
            } catch (IllegalArgumentException e) {
                throw new AssemblerException("Invalid type: " + typeGroup.descriptor.content(), typeGroup.location());
            }
        } else if(group.type == Group.GroupType.HANDLE) {
            HandleGroup handle = (HandleGroup) group;
            String typeString = handle.getHandleType().content();
            if(!Handles.isValid(typeString)) {
                throw new AssemblerException("Unknown handle type " + typeString, handle.location());
            }
            int type = Handles.getType(typeString);
            MethodDescriptor md = new MethodDescriptor(handle.getName().content(), handle.getDescriptor().content());
            return new Handle(
                    type,
                    md.owner == null ? (container == null ? "" : container.fullyQualifiedName) : md.owner,
                    md.name,
                    md.getDescriptor(),
                    type == Opcodes.H_INVOKEINTERFACE);
        }else {
            return group.content();
        }
    }

    /**
     * Converts array of a group objects to an array of ASM acceptable objects.
     * @param container The current class object.
     * @param groups The group objects to convert.
     * @return The converted objects.
     * @throws AssemblerException If the group objects are not valid.
     */
    public static Object[] convert(CachedClass container, Group[] groups) throws AssemblerException {
        Object[] objs = new Object[groups.length];
        for (int i = 0; i < groups.length; i++) {
            Group group = groups[i];
            objs[i] = convert(container, group);
        }
        return objs;
    }

}
