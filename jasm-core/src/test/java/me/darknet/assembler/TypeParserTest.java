package me.darknet.assembler;

import me.darknet.assembler.info.MemberInfo;
import me.darknet.assembler.util.TypeParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TypeParserTest {

    @ParameterizedTest
    @MethodSource("me.darknet.assembler.TypeParserTest#provideMemberInfo")
    public void testParseMemberInfo(String input) {
        String[] args = input.split(" ");
        String name = args[0];
        String descriptor = args[1];
        MemberInfo memberInfo = TypeParser.parseMemberInfo(name, descriptor);
        Assertions.assertNotNull(memberInfo);
    }

    public String[] provideMemberInfo() {
        return new String[] { "java/lang/String.toString ()Ljava/lang/String;", "java/lang/String.value [C",
                "name Ljava/lang/String;" };
    }

}
