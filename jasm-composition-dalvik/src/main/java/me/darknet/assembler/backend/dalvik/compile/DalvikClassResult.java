package me.darknet.assembler.backend.dalvik.compile;

import me.darknet.assembler.backend.dalvik.DalvikClassRepresentation;
import me.darknet.assembler.compiler.ClassResult;
import org.jetbrains.annotations.Nullable;

/**
 * Dalvik class compilation result.
 *
 * @param representation
 * 		Target representation, or {@code null} when compilation failed.
 */
public record DalvikClassResult(
		@Nullable DalvikClassRepresentation representation) implements ClassResult<DalvikClassRepresentation> {}
