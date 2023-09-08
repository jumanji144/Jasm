package me.darknet.assembler.compile.analysis;

import dev.xdark.blw.code.CodeElement;
import dev.xdark.blw.code.Instruction;
import dev.xdark.blw.code.Label;
import dev.xdark.blw.code.TryCatchBlock;
import dev.xdark.blw.simulation.ExecutionEngines;
import dev.xdark.blw.simulation.Simulation;
import me.darknet.assembler.compiler.InheritanceChecker;

import java.util.Collection;
import java.util.List;

public class AnalysisSimulation implements Simulation<AnalysisEngine, AnalysisSimulation.Info> {

    public record Info(List<CodeElement> method, List<TryCatchBlock> exceptionHandlers) { }

    private ForkingCodeWalker walker;
    private final InheritanceChecker inheritanceChecker;

    public AnalysisSimulation(InheritanceChecker inheritanceChecker) {
        this.inheritanceChecker = inheritanceChecker;
    }

    @Override
    public void execute(AnalysisEngine engine, Info info) {
        this.walker = new ForkingCodeWalker(info.method(), info.exceptionHandlers(), engine, inheritanceChecker);
        while (true) {
            walker.advance();
            CodeElement element = walker.element();
            if (element == null) {
                return;
            }
            if (element instanceof Label) {
                engine.label((Label) element);
            } else {
                ExecutionEngines.execute(engine, (Instruction) element);
            }
        }
    }

    public Collection<Frame> frames() {
        return walker.frames.values();
    }

}
