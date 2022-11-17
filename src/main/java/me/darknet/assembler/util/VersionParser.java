package me.darknet.assembler.util;

import lombok.experimental.UtilityClass;
import org.objectweb.asm.Opcodes;

@UtilityClass
public class VersionParser {

    public int getJavaVersion(String version) {
        String[] split = version.split("\\.");
        if (split.length != 2) {
            throw new IllegalArgumentException("Invalid version: " + version);
        }
        int major = Integer.parseInt(split[0]);
        int minor = Integer.parseInt(split[1]);
        // map to java class version
        if(major == 1 && minor == 1) return Opcodes.V1_1;
        if(major == 1) {
            return minor + 44;
        } else {
            return major + 44;
        }
    }

}
