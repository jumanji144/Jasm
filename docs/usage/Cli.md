# CLI
The Jasm CLI is the main way to easily use JASM.

The CLI is a thin wrapper around the internal api.

## Usage
Here is the help page of the `jasm` command:
```bash
Usage: jasm [-hV] [-t=<target>] [COMMAND]
Java Assembler CLI
  -h, --help              Show this help message and exit.
  -t, --target=<target>   Target platform
                          Possible values: JVM, DALVIK (default: JVM)
  -V, --version           Print version information and exit.
Commands:
  compile    Compile Java Assembler source code
  decompile  Decompile Java Assembler bytecode
```
Here the `target` platform specifies which instruction set the assembler will use.

### Compile
The `compile` command compiles a `.jasm` file into a `.class` file.

Here is the help page of the `compile` command:
```bash
Usage: jasm compile [-hV] [-at=target] [-bv=version] -o=file [-ov=file]
                    [-s=code] [file]
Compile Java Assembler source code
      [file]                Source file
      -at, --annotation-target=target
                            Annotation target
      -bv, --bytecode-version=version
                            Bytecode version (default: 8)
  -h, --help                Show this help message and exit.
  -o, --output=file         Output file
      -ov, --overlay=file   Overlay class file
                            Required for non-class code
  -s, --source=code         Source code
  -V, --version             Print version information and exit.
```
Most notable options are:   
`-at`/`--annotation-target`: Specifies a target path to know where to place an annotation when the target
source is not a full class file, the path is in the form of:
- `path/to/class.<index>` for a class file
- `path/to/class.method.<name>.<descriptor>.<index>` for a method
- `path/to/class.field.<name>.<descriptor>.<index>` for a field
Where `<index>` is the index of the annotation on the target.
`-ov`/`--overlay`: Specifies a class file to use as an overlay for the compiled class, which basically loads the class
file and then applies the singular method/field/annotation to that class file. (This is required for non-class code)
`-s`/`--source`: Specifies the source code to compile, this is useful for piping the source code into the compiler.

### Decompile
The `decompile` command decompiles a `.class` file into a `.jasm` file.

Here is the help page of the `decompile` command:
```bash
Usage: jasm decompile [-hV] [-i=<indent>] [-o=<output>] file
Decompile Java Assembler bytecode
      file                Source file
  -h, --help              Show this help message and exit.
  -i, --indent=<indent>   Indentation
  -o, --output=<output>   Output file
  -V, --version           Print version information and exit.
```
The options are pretty self explanatory.