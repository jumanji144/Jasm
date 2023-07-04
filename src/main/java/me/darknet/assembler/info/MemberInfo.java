package me.darknet.assembler.info;

public class MemberInfo {

	protected final MemberType memberType;
	protected final String owner;
	protected final String name;
	protected final String desc;
	public MemberInfo(MemberType memberType, String owner, String name, String desc) {
		this.memberType = memberType;
		this.owner = owner;
		this.name = name;
		this.desc = desc;
	}

	public MemberType getType() {
		return memberType;
	}

	public String getOwner() {
		return owner;
	}

	public String getName() {
		return name;
	}

	public String getDesc() {
		return desc;
	}

	public enum MemberType {
		METHOD, FIELD
	}

}
