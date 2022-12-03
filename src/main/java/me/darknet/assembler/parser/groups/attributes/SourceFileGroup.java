package me.darknet.assembler.parser.groups.attributes;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.IdentifierGroup;

import java.util.Collections;

@Getter
public class SourceFileGroup extends Group implements ClassAttributeGroup {

    private final IdentifierGroup sourceFileIdentifier;

    public SourceFileGroup(Token token, IdentifierGroup sourceFileIdentifier) {
        super(GroupType.SOURCE_FILE_DIRECTIVE, token, Collections.singletonList(sourceFileIdentifier));
        this.sourceFileIdentifier = sourceFileIdentifier;
    }

    public String getSourceFile() {
        return sourceFileIdentifier.content();
    }
}
