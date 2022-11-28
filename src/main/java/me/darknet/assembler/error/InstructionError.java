package me.darknet.assembler.error;

import me.darknet.assembler.instructions.Argument;

import java.util.HashMap;
import java.util.Map;

public class InstructionError extends AssemblerError{

    Map<Argument, Error> argumentErrors = new HashMap<>();

}
