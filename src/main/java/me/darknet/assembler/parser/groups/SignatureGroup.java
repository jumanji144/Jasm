package me.darknet.assembler.parser.groups;

import lombok.Getter;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

import java.util.Collections;

@Getter
public class SignatureGroup extends Group implements ClassAttributeGroup, FieldAttributeGroup, MethodAttributeGroup {
	private final IdentifierGroup descriptor;

	public SignatureGroup(Token value, IdentifierGroup descriptor) {
		super(GroupType.SIGNATURE_DIRECTIVE, value, Collections.singletonList(descriptor));
		this.descriptor = descriptor;
	}
}
