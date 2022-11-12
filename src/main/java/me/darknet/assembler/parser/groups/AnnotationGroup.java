package me.darknet.assembler.parser.groups;

import lombok.Getter;
import me.darknet.assembler.parser.AnnotationTarget;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.util.ArrayUtil;

import java.util.List;

@Getter
public class AnnotationGroup extends Group {
	private final IdentifierGroup classGroup;
	private final List<AnnotationParamGroup> params;
	private final AnnotationTarget target;
	private final boolean invisible;

	public AnnotationGroup(Token value, AnnotationTarget target, boolean invisible,
						   IdentifierGroup classGroup, List<AnnotationParamGroup> params) {
		super(GroupType.ANNOTATION, value, ArrayUtil.add(params, classGroup));
		this.params = params;
		this.classGroup = classGroup;
		this.target = target;
		this.invisible = invisible;
	}

}
