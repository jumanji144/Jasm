package me.darknet.assembler.parser.groups;

import lombok.Getter;
import me.darknet.assembler.parser.AnnotationTarget;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.util.ArrayUtil;

public class AnnotationGroup extends Group {

    @Getter
    IdentifierGroup classGroup;
    @Getter
    AnnotationParamGroup[] params;
    @Getter
    AnnotationTarget target;
    @Getter
    boolean invisible;



    public AnnotationGroup(Token value, AnnotationTarget target, boolean invisible, IdentifierGroup classGroup, AnnotationParamGroup[] params) {
        super(GroupType.ANNOTATION, value, ArrayUtil.add(params, classGroup));
        this.params = params;
        this.classGroup = classGroup;
        this.target = target;
        this.invisible = invisible;
    }

}
