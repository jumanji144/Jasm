package me.darknet.assembler.parser.groups.attributes;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.IdentifierGroup;
import me.darknet.assembler.util.VersionParser;

import java.util.Collections;

@Getter
public class VersionGroup extends Group implements ClassAttributeGroup {

    private final IdentifierGroup versionIdentifier;

    public VersionGroup(Token token, IdentifierGroup versionIdentifier) {
        super(GroupType.VERSION_DIRECTIVE, token, Collections.singletonList(versionIdentifier));
        this.versionIdentifier = versionIdentifier;
    }

    public int getVersion() {
        return VersionParser.getJavaVersion(versionIdentifier.content());
    }

    public String getVersionString() {
        return versionIdentifier.content();
    }

}
