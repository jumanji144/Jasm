package me.darknet.assembler.compiler;

import java.lang.annotation.ElementType;

@interface Exported {
    String value();

    @interface E {
        String value();
    }
}

@interface BuildConfig {

    @Exported("BUILD_TYPE")
    ElementType[] value();

    Exported value2();

    String value3();
}

@BuildConfig(value = {ElementType.FIELD}, value2 = @Exported("BUILD_TYPE"), value3 = "debug")
public class Debug {


}
