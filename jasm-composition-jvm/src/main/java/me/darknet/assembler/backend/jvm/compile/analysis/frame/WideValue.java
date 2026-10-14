package me.darknet.assembler.backend.jvm.compile.analysis.frame;

import me.darknet.assembler.analysis.Value;

/**
 * Marker value for the JVM frame type representing a wide value <i>(long or double)</i>.
 */
public enum WideValue implements Value.BackendMarker {
    INSTANCE
}
