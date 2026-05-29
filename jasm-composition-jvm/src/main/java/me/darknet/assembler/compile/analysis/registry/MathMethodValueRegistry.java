package me.darknet.assembler.compile.analysis.registry;

import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import me.darknet.assembler.compile.analysis.func.D2DStaticFunc;
import me.darknet.assembler.compile.analysis.func.D2IStaticFunc;
import me.darknet.assembler.compile.analysis.func.D2JStaticFunc;
import me.darknet.assembler.compile.analysis.func.DD2DStaticFunc;
import me.darknet.assembler.compile.analysis.func.F2FStaticFunc;
import me.darknet.assembler.compile.analysis.func.F2IStaticFunc;
import me.darknet.assembler.compile.analysis.func.FF2FStaticFunc;
import me.darknet.assembler.compile.analysis.func.I2IStaticFunc;
import me.darknet.assembler.compile.analysis.func.II2IStaticFunc;
import me.darknet.assembler.compile.analysis.func.II2IThrowingStaticFunc;
import me.darknet.assembler.compile.analysis.func.J2IThrowingStaticFunc;
import me.darknet.assembler.compile.analysis.func.J2JStaticFunc;
import me.darknet.assembler.compile.analysis.func.JI2JThrowingStaticFunc;
import me.darknet.assembler.compile.analysis.func.JJ2JStaticFunc;
import me.darknet.assembler.compile.analysis.func.JJ2JThrowingStaticFunc;

/**
 * Registry for {@link Math} and {@link StrictMath} static methods.
 */
public final class MathMethodValueRegistry {
    private MathMethodValueRegistry() {
    }

    public static MethodValueRegistry create() {
        MethodValueRegistry.Builder builder = MethodValueRegistry.builder();
        String owner = "java/lang/Math";
        builder.registerStatic(owner, "max", "(II)I", (II2IStaticFunc) Math::max);
        builder.registerStatic(owner, "max", "(JJ)J", (JJ2JStaticFunc) Math::max);
        builder.registerStatic(owner, "max", "(FF)F", (FF2FStaticFunc) Math::max);
        builder.registerStatic(owner, "max", "(DD)D", (DD2DStaticFunc) Math::max);
        builder.registerStatic(owner, "min", "(II)I", (II2IStaticFunc) Math::min);
        builder.registerStatic(owner, "min", "(JJ)J", (JJ2JStaticFunc) Math::min);
        builder.registerStatic(owner, "min", "(FF)F", (FF2FStaticFunc) Math::min);
        builder.registerStatic(owner, "min", "(DD)D", (DD2DStaticFunc) Math::min);
        builder.registerStatic(owner, "floorDiv", "(II)I", (II2IThrowingStaticFunc) Math::floorDiv);
        builder.registerStatic(owner, "floorDiv", "(JI)J", (JI2JThrowingStaticFunc) Math::floorDiv);
        builder.registerStatic(owner, "floorDiv", "(JJ)J", (JJ2JThrowingStaticFunc) Math::floorDiv);
        builder.registerStatic(owner, "floorMod", "(II)I", (II2IThrowingStaticFunc) Math::floorMod);
        builder.registerStatic(owner, "floorMod", "(JI)I", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownLongValue a &&
                    params.get(1) instanceof Value.KnownIntValue b) {
                try {
                    return Values.valueOf(Math.floorMod(a.value(), b.value()));
                } catch (Throwable ignored) {
                }
            }
            return Values.INT_VALUE;
        });
        builder.registerStatic(owner, "floorMod", "(JJ)J", (JJ2JThrowingStaticFunc) Math::floorMod);
        builder.registerStatic(owner, "addExact", "(II)I", (II2IThrowingStaticFunc) Math::addExact);
        builder.registerStatic(owner, "addExact", "(JJ)J", (JJ2JThrowingStaticFunc) Math::addExact);
        builder.registerStatic(owner, "multiplyHigh", "(JJ)J", (JJ2JThrowingStaticFunc) Math::multiplyHigh);
        builder.registerStatic(owner, "multiplyFull", "(II)J", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownIntValue a &&
                    params.get(1) instanceof Value.KnownIntValue b) {
                try {
                    return Values.valueOf(Math.multiplyFull(a.value(), b.value()));
                } catch (Throwable ignored) {
                }
            }
            return Values.LONG_VALUE;
        });
        builder.registerStatic(owner, "multiplyExact", "(II)I", (II2IThrowingStaticFunc) Math::multiplyExact);
        builder.registerStatic(owner, "multiplyExact", "(JI)J", (JI2JThrowingStaticFunc) Math::multiplyExact);
        builder.registerStatic(owner, "multiplyExact", "(JJ)J", (JJ2JThrowingStaticFunc) Math::multiplyExact);
        builder.registerStatic(owner, "pow", "(DD)D", (DD2DStaticFunc) Math::pow);
        builder.registerStatic(owner, "nextAfter", "(DD)D", (DD2DStaticFunc) Math::nextAfter);
        builder.registerStatic(owner, "nextAfter", "(FD)F", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownFloatValue a &&
                    params.get(1) instanceof Value.KnownDoubleValue b)
                return Values.valueOf(Math.nextAfter(a.value(), b.value()));
            return Values.FLOAT_VALUE;
        });
        builder.registerStatic(owner, "floor", "(D)D", (D2DStaticFunc) Math::floor);
        builder.registerStatic(owner, "nextUp", "(F)F", (F2FStaticFunc) Math::nextUp);
        builder.registerStatic(owner, "nextUp", "(D)D", (D2DStaticFunc) Math::nextUp);
        builder.registerStatic(owner, "nextDown", "(F)F", (F2FStaticFunc) Math::nextDown);
        builder.registerStatic(owner, "nextDown", "(D)D", (D2DStaticFunc) Math::nextDown);
        builder.registerStatic(owner, "absExact", "(I)I", (I2IStaticFunc) Math::absExact);
        builder.registerStatic(owner, "absExact", "(J)J", (J2JStaticFunc) Math::absExact);
        builder.registerStatic(owner, "toIntExact", "(J)I", (J2IThrowingStaticFunc) Math::toIntExact);
        builder.registerStatic(owner, "incrementExact", "(I)I", (I2IStaticFunc) Math::incrementExact);
        builder.registerStatic(owner, "incrementExact", "(J)J", (J2JStaticFunc) Math::incrementExact);
        builder.registerStatic(owner, "decrementExact", "(I)I", (I2IStaticFunc) Math::decrementExact);
        builder.registerStatic(owner, "decrementExact", "(J)J", (J2JStaticFunc) Math::decrementExact);
        builder.registerStatic(owner, "subtractExact", "(II)I", (II2IThrowingStaticFunc) Math::subtractExact);
        builder.registerStatic(owner, "subtractExact", "(JJ)J", (JJ2JThrowingStaticFunc) Math::subtractExact);
        builder.registerStatic(owner, "abs", "(I)I", (I2IStaticFunc) Math::abs);
        builder.registerStatic(owner, "abs", "(J)J", (J2JStaticFunc) Math::abs);
        builder.registerStatic(owner, "abs", "(F)F", (F2FStaticFunc) Math::abs);
        builder.registerStatic(owner, "abs", "(D)D", (D2DStaticFunc) Math::abs);
        builder.registerStatic(owner, "atan2", "(DD)D", (DD2DStaticFunc) Math::atan2);
        builder.registerStatic(owner, "copySign", "(DD)D", (DD2DStaticFunc) Math::copySign);
        builder.registerStatic(owner, "copySign", "(FF)F", (FF2FStaticFunc) Math::copySign);
        builder.registerStatic(owner, "acos", "(D)D", (D2DStaticFunc) Math::acos);
        builder.registerStatic(owner, "asin", "(D)D", (D2DStaticFunc) Math::asin);
        builder.registerStatic(owner, "atan", "(D)D", (D2DStaticFunc) Math::atan);
        builder.registerStatic(owner, "tan", "(D)D", (D2DStaticFunc) Math::tan);
        builder.registerStatic(owner, "tanh", "(D)D", (D2DStaticFunc) Math::tanh);
        builder.registerStatic(owner, "sin", "(D)D", (D2DStaticFunc) Math::sin);
        builder.registerStatic(owner, "sinh", "(D)D", (D2DStaticFunc) Math::sinh);
        builder.registerStatic(owner, "cos", "(D)D", (D2DStaticFunc) Math::cos);
        builder.registerStatic(owner, "cosh", "(D)D", (D2DStaticFunc) Math::cosh);
        builder.registerStatic(owner, "cbrt", "(D)D", (D2DStaticFunc) Math::cbrt);
        builder.registerStatic(owner, "sqrt", "(D)D", (D2DStaticFunc) Math::sqrt);
        builder.registerStatic(owner, "exp", "(D)D", (D2DStaticFunc) Math::exp);
        builder.registerStatic(owner, "expm1", "(D)D", (D2DStaticFunc) Math::expm1);
        builder.registerStatic(owner, "getExponent", "(D)I", (D2IStaticFunc) Math::getExponent);
        builder.registerStatic(owner, "getExponent", "(F)I", (F2IStaticFunc) Math::getExponent);
        builder.registerStatic(owner, "signum", "(D)D", (D2DStaticFunc) Math::signum);
        builder.registerStatic(owner, "signum", "(F)F", (F2FStaticFunc) Math::signum);
        builder.registerStatic(owner, "round", "(F)I", (F2IStaticFunc) Math::round);
        builder.registerStatic(owner, "round", "(D)J", (D2JStaticFunc) Math::round);
        builder.registerStatic(owner, "log", "(D)D", (D2DStaticFunc) Math::log);
        builder.registerStatic(owner, "log10", "(D)D", (D2DStaticFunc) Math::log10);
        builder.registerStatic(owner, "log1p", "(D)D", (D2DStaticFunc) Math::log1p);
        builder.registerStatic(owner, "ulp", "(D)D", (D2DStaticFunc) Math::ulp);
        builder.registerStatic(owner, "ulp", "(F)F", (F2FStaticFunc) Math::ulp);
        builder.registerStatic(owner, "toDegrees", "(D)D", (D2DStaticFunc) Math::toDegrees);
        builder.registerStatic(owner, "toRadians", "(D)D", (D2DStaticFunc) Math::toRadians);
        builder.registerStatic(owner, "IEEEremainder", "(DD)D", (DD2DStaticFunc) Math::IEEEremainder);
        builder.registerStatic(owner, "hypot", "(DD)D", (DD2DStaticFunc) Math::hypot);
        builder.registerStatic(owner, "scalb", "(FI)F", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownFloatValue a &&
                    params.get(1) instanceof Value.KnownIntValue b)
                return Values.valueOf(Math.scalb(a.value(), b.value()));
            return Values.FLOAT_VALUE;
        });
        builder.registerStatic(owner, "scalb", "(DI)D", params -> {
            if (params.size() == 2 &&
                    params.get(0) instanceof Value.KnownDoubleValue a &&
                    params.get(1) instanceof Value.KnownIntValue b)
                return Values.valueOf(Math.scalb(a.value(), b.value()));
            return Values.DOUBLE_VALUE;
        });
        builder.registerStatic(owner, "fma", "(FFF)F", params -> {
            if (params.size() == 3 &&
                    params.get(0) instanceof Value.KnownFloatValue a &&
                    params.get(1) instanceof Value.KnownFloatValue b &&
                    params.get(2) instanceof Value.KnownFloatValue c)
                return Values.valueOf(Math.fma(a.value(), b.value(), c.value()));
            return Values.FLOAT_VALUE;
        });
        builder.registerStatic(owner, "fma", "(DDD)D", params -> {
            if (params.size() == 3 &&
                    params.get(0) instanceof Value.KnownDoubleValue a &&
                    params.get(1) instanceof Value.KnownDoubleValue b &&
                    params.get(2) instanceof Value.KnownDoubleValue c)
                return Values.valueOf(Math.fma(a.value(), b.value(), c.value()));
            return Values.DOUBLE_VALUE;
        });
        builder.copyStaticOwner("java/lang/Math", "java/lang/StrictMath");
        return builder.build();
    }
}
