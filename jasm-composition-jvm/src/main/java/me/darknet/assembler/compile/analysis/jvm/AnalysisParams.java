package me.darknet.assembler.compile.analysis.jvm;

import dev.xdark.blw.code.CodeElement;
import dev.xdark.blw.code.TryCatchBlock;
import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compiler.InheritanceChecker;

import java.util.List;

public record AnalysisParams(InheritanceChecker checker,
							 List<Local> params,
							 List<CodeElement> method,
							 List<TryCatchBlock> exceptionHandlers)
{
}
