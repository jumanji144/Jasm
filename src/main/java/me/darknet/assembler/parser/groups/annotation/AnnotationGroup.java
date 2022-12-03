package me.darknet.assembler.parser.groups.annotation;

import lombok.Getter;
import me.darknet.assembler.parser.AnnotationTarget;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.attributes.ClassAttributeGroup;
import me.darknet.assembler.parser.groups.attributes.FieldAttributeGroup;
import me.darknet.assembler.parser.groups.IdentifierGroup;
import me.darknet.assembler.parser.groups.attributes.MethodAttributeGroup;
import me.darknet.assembler.util.GroupLists;

import java.util.List;

@Getter
public class AnnotationGroup extends Group implements ClassAttributeGroup, FieldAttributeGroup, MethodAttributeGroup {
	private final IdentifierGroup classGroup;
	private final List<AnnotationParamGroup> params;
	private final AnnotationTarget target;
	private final boolean invisible;

	public AnnotationGroup(Token value, AnnotationTarget target, boolean invisible,
						   IdentifierGroup classGroup, List<AnnotationParamGroup> params) {
		super(GroupType.ANNOTATION, value, GroupLists.add(params, classGroup));
		this.params = params;
		this.classGroup = classGroup;
		this.target = target;
		this.invisible = invisible;
	}

}
