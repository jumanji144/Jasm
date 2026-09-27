# CLI
The Jasm CLI is the main way to easily use JASM.

The CLI is a thin wrapper around the internal api.

## Usage

The root `--target` option selects the default backend for subcommands.

```text
jasm [-t <target>] [COMMAND]
  -t, --target <target>   Default target platform (JVM or DALVIK; default: JVM)

Commands:
  compile    Compile Java Assembler source code
  decompile  Decompile Java Assembler bytecode
```

For example, `jasm --target DALVIK compile --target JVM ...` compiles for the JVM because the child option wins.

## Compile

`compile` accepts a `.jasm` file or inline source supplied with `--source`. 

The target determines the output format and default bytecode version:

- JVM defaults version to 8 
- Dalvik defaults version to 35.

- Use `--bytecode-version` to select another version _(Dalvik accepts 35 through 41)_.

```text
jasm compile [-hV] [-ic] [-at=target] [-bv=version] [-lib=path] [-o=file] [-ov=file] [-s=code] [-t=target] [file...]
  -t, --target <target>            Target override for this command
  -o, --output <file>              Output file
  -s, --source <code>              Inline source (cannot accompany source files)
  -bv, --bytecode-version <version> Bytecode version
  -ov, --overlay <file>            Overlay class file for method, field or annotation source
  -at, --annotation-target <path>  JVM annotation placement path
  -lib, --library-folder <path>    JVM inheritance-checker classpath folder
  -ic, --inheritance-checker       Enable the JVM inheritance checker
```

`--annotation-target` specifies where an annotation is placed when the source is not a full class:

- `path/to/class.<index>` for a class annotation
- `path/to/class.method.<name>.<descriptor>.<index>` for a method annotation
- `path/to/class.field.<name>.<descriptor>.<index>` for a field annotation

`--overlay` supplies the class to which a single method, field or annotation declaration is applied. 
Method and field declarations require an overlay. Annotations require both an overlay and an annotation target. 

Dalvik overlays must be standalone DEX files containing exactly one class.

`--library-folder` and `--annotation-target` are JVM-only options.

## Decompile

`decompile` renders a class file, JAR, DEX file or APK as JASM. 
A single class is printed to standard output unless `--output` is supplied. 
For an archive with multiple classes, use `--class` to select one class or provide an output directory to emit every class as a `.jasm` file. 
Class names may use dotted (`example.Foo`) or internal (`example/Foo`) form.

```text
jasm decompile [-hV] [-c=name] [--float-format=mode] [-i=<indent>] [-o=<output>] [-t=target] file
  -t, --target <target>                 Target override for this command
  -o, --output <file-or-directory>      Output file or directory
  -c, --class <name>                    Select a class from an archive or DEX/APK
  -i, --indent <indent>                 Indentation (default: four spaces)
      --float-representation <mode>     standard, hex or binary (default: standard)
      --float-format <mode>             Alias for --float-representation
```

Floating-point constants use `standard` decimal output by default. Use `hex` or `binary` to emit their raw bit patterns.
