package me.darknet.assembler.info;

public class FieldInfo extends MemberInfo {
    public FieldInfo(String owner, String name, String desc) {
        super(MemberType.FIELD, owner, name, desc);
    }
}
