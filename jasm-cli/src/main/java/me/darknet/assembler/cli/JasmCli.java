package me.darknet.assembler.cli;

import me.darknet.assembler.cli.commands.MainCommand;

import picocli.CommandLine;

public class JasmCli {

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new MainCommand());
        commandLine.setCaseInsensitiveEnumValuesAllowed(true);
        commandLine.execute(args);
    }

}
