package me.darknet.assembler.backend.dalvik.compile.analysis;

import me.darknet.assembler.ast.primitive.ASTInstruction;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Access counts and source references for one source-spelled Dalvik register operand.
 *
 * @param registerName
 *        Source spelling of a register or explicit register range.
 * @param readCount
 *        Number of register operand reads.
 * @param writeCount
 *        Number of register operand writes.
 * @param references
 *        Unique source instructions that access this register or range, in source order.
 */
public record DalvikRegisterUsage(@NotNull String registerName,
                                  int readCount,
                                  int writeCount,
                                  @NotNull List<ASTInstruction> references) {}
