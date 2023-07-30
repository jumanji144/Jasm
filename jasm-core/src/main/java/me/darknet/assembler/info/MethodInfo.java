package me.darknet.assembler.info;

public class MethodInfo extends MemberInfo {
    public MethodInfo(String owner, String name, String desc) {
        super(MemberType.METHOD, owner, name, desc);
    }

    @Override
    public String toString() {
        return "MethodInfo{" + "memberType=" + memberType + ", owner='" + owner + '\'' + ", name='" + name + '\''
                + ", desc='" + desc + '\'' + '}';
    }
}
