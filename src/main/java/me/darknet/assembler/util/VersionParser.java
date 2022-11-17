package me.darknet.assembler.util;

import lombok.experimental.UtilityClass;
import org.objectweb.asm.Opcodes;

@UtilityClass
public class VersionParser {

    public int getJavaVersion(String version) {
        int major = Integer.parseInt(version);
        return 0 << 16 | (major + 44);
    }

}
