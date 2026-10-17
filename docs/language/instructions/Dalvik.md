# Dalvik Instructions

Dalvik is a register based language, meaning that all operations are performed on registers.
The Dalvik instruction set has less abstractions than the JVM instruction set, but still has some conveniences
like predicting the type of otherwise ambiguous instruction data like `const` and `const-wide` payloads.

## Objects

Objects used by the dalvik instruction set:

### Register

A register is a slot in the method's register file holding a single value.
It can be represented by the following expressions:

- `vN`: A numeric register, where `N` is the register index (`v0`, `v12`)
- Any identifier: A named register, which is given a slot automatically

Important: Named registers are allocated the lowest free slot below the method's parameters that no numeric `vN` operand of the same method uses, so the `registers`
declaration has
to leave room for every local a method uses.

A wide value (`long` or `double`) occupies two registers _(the one it names and the one directly after it)_.

A method with parameters must declare its register count, which also decides where the parameters start, as the last
[parameters](../Basics.md#method) of the register file:

```
.method public static sum (II)I {
    registers: 4,
    parameters: { a, b },
    code: {
        add-int v0 a b
        return v0
    }
}
```

A parameter is also reachable by its position as `pN` (`p0`, `p1`, ...), and the receiver of an instance method is
reachable as `this`.

Instructions that take a variable number of registers (`invoke-*`, `filled-new-array`) spell them as an array, which
has two shapes:

- A list: `{ v0, v1, v2 }`, naming at most five registers
- A range: `{ v0, v5 }`, exactly two registers giving the first and the last register of an inclusive range

The `/range` forms require the range shape and can name up to 255 registers, so an argument list of
more than five registers has to use them. A canonical `invoke-*` can also name its registers as [raw words](#raw-invoke-words).

### Raw invoke words

A canonical `invoke-*` instruction, including `invoke-custom` and `invoke-polymorphic`, accepts its registers as raw
words, written as an object with one key:

```
invoke-static { words: { v0, v2 } } Owner.call (J)V
```

The `words` object supplies exactly the listed register words, in the order written. Use it for a call whose words
cannot be described as logical arguments, and the disassembler uses it for such calls:

- Each entry is one register word. The descriptor does not widen an entry, so `{ words: { v0, v2 } }` for `(J)V` names
  `v0` and `v2`, not the pair `v0` and `v1`.
- The words are not checked against the descriptor. A missing or extra word is kept as written, and so is a receiver
  in a position the descriptor does not expect.
- Words that are at most five in number, and each fit `v0` through `v15`, use the non-range encoding. That encoding
  takes any order and may repeat a word.
- Any other words must be consecutive registers in the order written, and the range encoding names them by the first
  register and the count. Words that are neither, such as `{ words: { v0, v16 } }`, are a compile error. The words are
  never sorted, deduplicated, packed, or moved into other registers.
- Each word is a register, which must be within the declared register count and must fit the encoding.
- The object is accepted only by canonical mnemonics. It is an error on `invoke-*/range` and on `filled-new-array`, and
  an object with any key other than `words` is an error.

The object keeps the register words, not the original encoding. The same words may be written with a different encoding
from the one that was read, such as the non-range form for a short low list. The object does not switch off other
checks, so the compiler's verification of the emitted code still applies.

Ordinary lists keep their current meaning in this version: each entry of `{ v0, v1 }` is one register word, and the
`/range` forms keep their two-endpoint spelling. This is the current contract for invokes and it changes in the invoke
cutover.

When the disassembler prints a call, it keeps the ordinary spelling for a call whose words group faithfully against the
descriptor. Those words are a receiver first, unless the instruction is `invoke-static` or `invoke-custom`, which take no
receiver. Then come one register for each argument, with two consecutive registers for each `long` or `double` argument,
and no words beyond those. Any other call is printed as the raw object with the base mnemonic, such as `invoke-static`
rather than `invoke-static/range`, and every word of a range is listed.

### Register field limits

Each form stores its register operands in fields of a fixed width, so every register it names must fit those fields.
A register outside the limit of its form is a compile error, which names the register and the form. It is never
wrapped onto another register, and a rejected compile writes no DEX output. The limit applies to the slot a named
register is allocated to as well as to a `vN` operand. The method's declared register count is a separate limit,
so a register beyond it is reported as exceeding that count, in addition to any field limit it also breaks.

| Form                                                                                                                    | Each register                   | Other limits                                                           |
|-------------------------------------------------------------------------------------------------------------------------|---------------------------------|------------------------------------------------------------------------|
| Three-register arithmetic, such as `add-int dest a b`                                                                   | at most `v255`                  | none                                                                   |
| Two-address arithmetic, such as `add-int dest dest b`, which is used only when the destination is also the first source | at most `v15`                   | none                                                                   |
| Arithmetic with an eight-bit literal, such as `add-int dest src 3`                                                      | at most `v255`                  | literal from `-128` through `127`                                      |
| Arithmetic with a sixteen-bit literal, such as `add-int dest src 300` or `rsub-int dest src 300`                        | at most `v15`                   | literal from `-32768` through `32767`; shifts have no sixteen-bit form |
| Non-range `invoke-*` or `filled-new-array` list                                                                         | at most `v15`                   | at most five registers                                                 |
| `/range` `invoke-*` or `filled-new-array/range`                                                                         | first and last at most `v65535` | at most 255 registers in the range                                     |
| Raw words of an `invoke-*`, `{ words: { ... } }`, in the non-range form                                                 | at most `v15`                   | at most five words, in any order                                       |
| Raw words of an `invoke-*`, `{ words: { ... } }`, in the range form                                                     | first and last at most `v65535` | consecutive words in the order written, at most 255                    |
| Destination of `const`, `const-wide`, `const-string`, `const-class`, `const-method-handle` or `const-method-type`       | at most `v255`                  | none                                                                   |

`const` picks its encoding from the destination and the value, so it is not limited to the short form. The
four-bit form is used only for `v0` through `v15` with a value from `-8` through `7`. Other values use the 16-bit form
when they fit 16 bits, the high-16-bit form when their low 16 bits are zero, and the 32-bit form otherwise. Every value
keeps its exact bits, including the payload of a `NaN`.

### Literal

A literal is an integer written inside the instruction rather than held in a register. Where an arithmetic instruction
accepts one, it is the last operand. It is a whole number within the range of an `int`, from `-2147483648` through
`2147483647`. A number is never a register name, and a register is always an identifier.

The encoding is chosen once the registers of the instruction are known, from two forms:

- the eight-bit form takes a literal from `-128` through `127`, with registers from `v0` through `v255`
- the sixteen-bit form takes a literal from `-32768` through `32767`, with registers from `v0` through `v15`

The eight-bit form is used whenever the literal and the registers both fit it, and shifts have only the eight-bit
form. A literal that no single form can hold with the registers of its instruction is a compile error. The compiler
never materializes the literal, moves a register, or allocates a scratch register to make it fit.

### Constant

A constant is a value present in the dex constant pool and can be represented by the following expressions:

- `number`: An `int`, `long`, `float` or `double`, depending on the [number suffix](../Syntax.md#number)
- `string`: A string constant
- `identifier`:
    - A class type (`Lsome/package/Class;`, `some/package/Class`, `[I`)
    - A method type (`(Lsome/package/Argument;IIJJZZ)Lsome/package/Return;`)
    - `null`, `true` or `false`
- `handle`: A [handle](#handle), spelled as an array or by its shortcut name
- `.enum owner name descriptor` or `.member owner name descriptor`: An enum or member constant

`const`, `const-wide`, `const-string`, `const-class`, `const-method-type` and `const-method-handle` each take one kind
of constant. The bootstrap arguments of [`invoke-custom`](#instructions) are a list of constants, which additionally
allows the `.enum` and `.member` forms.

### Handle

Format:

```
{ kind, owner.member, descriptor }
```

A handle is a way to describe how the runtime obtains a `java/lang/invoke/MethodHandle` from the instructions given.
The vocabulary is the same as the JVM's, and each kind describes the same operation as one of the instruction forms
below, so it is encoded as the dex kind that matches:

- `invokevirtual` (equivalent to: `invoke-virtual { registers } owner.name methodDescriptor`)
- `invokestatic` (equivalent to: `invoke-static { registers } owner.name methodDescriptor`)
- `invokespecial` (equivalent to: `invoke-direct { registers } owner.name methodDescriptor`, which is the dex `invoke-direct` kind)
- `getfield` (equivalent to: `iget dest instance owner.name fieldDescriptor`)
- `putfield` (equivalent to: `iput src instance owner.name fieldDescriptor`)
- `getstatic` (equivalent to: `sget dest owner.name fieldDescriptor`)
- `putstatic` (equivalent to: `sput src owner.name fieldDescriptor`)
- `invokeinterface` (equivalent to: `invoke-interface { registers } owner.name methodDescriptor`)
- `newinvokespecial` (equivalent to: `new-instance dest LSample;` followed by `invoke-direct { dest } LSample;.<init> methodDescriptor`, which is the dex constructor kind and how a handle to `<init>` is written)

A field kind names the field itself and not the width of its value, so a wide field uses the same kind and is read or
written through the `-wide` form of the instruction.

A handle naming one of the shortcut methods (`ConstantBootstraps.nullConstant`, `LambdaMetafactory.metafactory`, etc.)
can be written by that name instead of as an array for brevity. The full list is defined in
`me.darknet.assembler.helper.Handle#HANDLE_SHORTCUTS`

### Array data

Format:

```
{
    width: width,
    values: { value... }
}
```

An array data payload fills the elements of an array from literals, `width` being the size of an element in bytes and
only `1`, `2`, `4` and `8` being encodable.
The payload can also be written as a bare array of literals:

```
{ 1, 2, 3, 4 }
```

where the element width is inferred as the narrowest width that holds every literal losslessly.

### Packed switch

Format:

```
{
    first: first,
    targets: { label... }
}
```

A packed switch jumps to the target whose index in `targets` matches `value - first`, so `first` is the smallest key
and every later target is keyed by an implicit increment.
Values outside that range fall through to the instruction after the switch.

### Sparse switch

Format:

```
{
    key: label,
    ...
}
```

A sparse switch jumps to the target whose key equals `value`, for any integer key, in any order.
A value matching no key falls through to the instruction after the switch.

## Instructions

| Opcode                                                                                                | Registers [before] -> [after] | Description                                                                                                                                                 |
|-------------------------------------------------------------------------------------------------------|-------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `nop`                                                                                                 | ->                            | does nothing                                                                                                                                                |
| `move dest src`                                                                                       | `src` -> `dest`               | moves the value in `src` into `dest`                                                                                                                        |
| `move-wide dest src`                                                                                  | `src` -> `dest`               | moves the wide value in `src` into `dest`                                                                                                                   |
| `move-object dest src`                                                                                | `src` -> `dest`               | moves the object in `src` into `dest`                                                                                                                       |
| `move-result dest`                                                                                    | -> `dest`                     | stores the result of the previous invoke in `dest`                                                                                                          |
| `move-result-wide dest`                                                                               | -> `dest`                     | stores the wide result of the previous invoke in `dest`                                                                                                     |
| `move-result-object dest`                                                                             | -> `dest`                     | stores the object result of the previous invoke in `dest`                                                                                                   |
| `move-exception dest`                                                                                 | -> `dest`                     | stores the caught exception in `dest`, as the first instruction of a handler                                                                                |
| `return-void`                                                                                         | ->                            | returns from the method                                                                                                                                     |
| `return src`                                                                                          | `src` ->                      | returns the `int` in `src` from the method                                                                                                                  |
| `return-wide src`                                                                                     | `src` ->                      | returns the wide value in `src` from the method                                                                                                             |
| `return-object src`                                                                                   | `src` ->                      | returns the object in `src` from the method                                                                                                                 |
| `const dest value`                                                                                    | -> `dest`                     | stores the constant `value` in `dest`, as an `int` or a `float`                                                                                             |
| `const-wide dest value`                                                                               | -> `dest`                     | stores the wide constant `value` in `dest`, as a `long` or a `double`                                                                                       |
| `const-string dest "string"`                                                                          | -> `dest`                     | stores the `string` constant in `dest`                                                                                                                      |
| `const-class dest type`                                                                               | -> `dest`                     | stores the class `type` in `dest`                                                                                                                           |
| <code>const-method-handle dest [h: [handle](#handle)]</code>                                          | -> `dest`                     | stores the method [handle](#handle) in `dest`                                                                                                               |
| `const-method-type dest descriptor`                                                                   | -> `dest`                     | stores the method type `descriptor` in `dest`                                                                                                               |
| `monitor-enter src`                                                                                   | `src` ->                      | acquires the monitor of the object in `src`                                                                                                                 |
| `monitor-exit src`                                                                                    | `src` ->                      | releases the monitor of the object in `src`                                                                                                                 |
| `check-cast src type`                                                                                 | `src` -> `src`                | throws a `ClassCastException` if `src` cannot be cast to the class `type`                                                                                   |
| `instance-of dest src type`                                                                           | `src` -> `dest`               | stores `1` in `dest` when `src` is an instance of the class `type`, otherwise `0`                                                                           |
| `array-length dest src`                                                                               | `src` -> `dest`               | stores the length of the array in `src` in `dest`                                                                                                           |
| `new-instance dest type`                                                                              | -> `dest`                     | creates a new instance of the class `type` in `dest`                                                                                                        |
| `new-array dest size componentType`                                                                   | `size` -> `dest`              | creates a new array of `componentType` with length `size` in `dest`                                                                                         |
| `filled-new-array { registers } componentType`                                                        | `registers` ->                | creates an array of `componentType` filled with `registers`, whose result is read with `move-result`                                                        |
| `filled-new-array/range { first, last } componentType`                                                | `registers` ->                | creates an array of `componentType` filled with the registers from `first` through `last`                                                                   |
| <code>fill-array-data array [[data](#array-data): payload]</code>                                     | `array` ->                    | fills the array in `array` with the [payload](#array-data) data                                                                                             |
| `throw src`                                                                                           | `src` ->                      | throws the exception in `src`                                                                                                                               |
| `goto label`                                                                                          | ->                            | jumps to `label`                                                                                                                                            |
| <code>packed-switch value [[payload](#packed-switch): payload]</code>                                 | `value` ->                    | jumps to the target whose index matches `value`, falling through when `value` is outside the range                                                          |
| <code>sparse-switch value [[payload](#sparse-switch): payload]</code>                                 | `value` ->                    | jumps to the target whose key matches `value`, falling through when no key matches                                                                          |
| `cmpl-float dest a b`                                                                                 | `a`, `b` -> `dest`            | compares the `float`s `a` and `b`, storing `-1`, `0` or `1`, with a `nan` storing `-1`                                                                      |
| `cmpg-float dest a b`                                                                                 | `a`, `b` -> `dest`            | compares the `float`s `a` and `b`, storing `-1`, `0` or `1`, with a `nan` storing `1`                                                                       |
| `cmpl-double dest a b`                                                                                | `a`, `b` -> `dest`            | compares the `double`s `a` and `b`, storing `-1`, `0` or `1`, with a `nan` storing `-1`                                                                     |
| `cmpg-double dest a b`                                                                                | `a`, `b` -> `dest`            | compares the `double`s `a` and `b`, storing `-1`, `0` or `1`, with a `nan` storing `1`                                                                      |
| `cmp-long dest a b`                                                                                   | `a`, `b` -> `dest`            | compares the `long`s `a` and `b`, storing `-1`, `0` or `1`                                                                                                  |
| `if-eq a b label`                                                                                     | `a`, `b` ->                   | jumps to `label` if `a` is equal to `b`                                                                                                                     |
| `if-ne a b label`                                                                                     | `a`, `b` ->                   | jumps to `label` if `a` is not equal to `b`                                                                                                                 |
| `if-lt a b label`                                                                                     | `a`, `b` ->                   | jumps to `label` if `a` is less than `b`                                                                                                                    |
| `if-ge a b label`                                                                                     | `a`, `b` ->                   | jumps to `label` if `a` is greater than or equal to `b`                                                                                                     |
| `if-gt a b label`                                                                                     | `a`, `b` ->                   | jumps to `label` if `a` is greater than `b`                                                                                                                 |
| `if-le a b label`                                                                                     | `a`, `b` ->                   | jumps to `label` if `a` is less than or equal to `b`                                                                                                        |
| `if-eqz src label`                                                                                    | `src` ->                      | jumps to `label` if `src` is equal to `0`                                                                                                                   |
| `if-nez src label`                                                                                    | `src` ->                      | jumps to `label` if `src` is not equal to `0`                                                                                                               |
| `if-ltz src label`                                                                                    | `src` ->                      | jumps to `label` if `src` is less than `0`                                                                                                                  |
| `if-gez src label`                                                                                    | `src` ->                      | jumps to `label` if `src` is greater than or equal to `0`                                                                                                   |
| `if-gtz src label`                                                                                    | `src` ->                      | jumps to `label` if `src` is greater than `0`                                                                                                               |
| `if-lez src label`                                                                                    | `src` ->                      | jumps to `label` if `src` is less than or equal to `0`                                                                                                      |
| `neg-int dest src`                                                                                    | `src` -> `dest`               | negates the `int` in `src`                                                                                                                                  |
| `not-int dest src`                                                                                    | `src` -> `dest`               | bitwise complements the `int` in `src`                                                                                                                      |
| `neg-long dest src`                                                                                   | `src` -> `dest`               | negates the `long` in `src`                                                                                                                                 |
| `not-long dest src`                                                                                   | `src` -> `dest`               | bitwise complements the `long` in `src`                                                                                                                     |
| `neg-float dest src`                                                                                  | `src` -> `dest`               | negates the `float` in `src`                                                                                                                                |
| `neg-double dest src`                                                                                 | `src` -> `dest`               | negates the `double` in `src`                                                                                                                               |
| `int-to-long dest src`                                                                                | `src` -> `dest`               | converts the `int` in `src` to a `long`                                                                                                                     |
| `int-to-float dest src`                                                                               | `src` -> `dest`               | converts the `int` in `src` to a `float`                                                                                                                    |
| `int-to-double dest src`                                                                              | `src` -> `dest`               | converts the `int` in `src` to a `double`                                                                                                                   |
| `long-to-int dest src`                                                                                | `src` -> `dest`               | converts the `long` in `src` to an `int`                                                                                                                    |
| `long-to-float dest src`                                                                              | `src` -> `dest`               | converts the `long` in `src` to a `float`                                                                                                                   |
| `long-to-double dest src`                                                                             | `src` -> `dest`               | converts the `long` in `src` to a `double`                                                                                                                  |
| `float-to-int dest src`                                                                               | `src` -> `dest`               | converts the `float` in `src` to an `int`                                                                                                                   |
| `float-to-long dest src`                                                                              | `src` -> `dest`               | converts the `float` in `src` to a `long`                                                                                                                   |
| `float-to-double dest src`                                                                            | `src` -> `dest`               | converts the `float` in `src` to a `double`                                                                                                                 |
| `double-to-int dest src`                                                                              | `src` -> `dest`               | converts the `double` in `src` to an `int`                                                                                                                  |
| `double-to-long dest src`                                                                             | `src` -> `dest`               | converts the `double` in `src` to a `long`                                                                                                                  |
| `double-to-float dest src`                                                                            | `src` -> `dest`               | converts the `double` in `src` to a `float`                                                                                                                 |
| `int-to-byte dest src`                                                                                | `src` -> `dest`               | converts the `int` in `src` to a `byte`, sign-extending into `dest`                                                                                         |
| `int-to-char dest src`                                                                                | `src` -> `dest`               | converts the `int` in `src` to a `char`, zero-extending into `dest`                                                                                         |
| `int-to-short dest src`                                                                               | `src` -> `dest`               | converts the `int` in `src` to a `short`, sign-extending into `dest`                                                                                        |
| `add-int dest a b`                                                                                    | `a`, `b` -> `dest`            | adds the `int`s `a` and `b`; `b` may be a [literal](#literal)                                                                                               |
| `sub-int dest a b`                                                                                    | `a`, `b` -> `dest`            | subtracts the `int` `b` from `a`                                                                                                                            |
| `mul-int dest a b`                                                                                    | `a`, `b` -> `dest`            | multiplies the `int`s `a` and `b`; `b` may be a [literal](#literal)                                                                                         |
| `div-int dest a b`                                                                                    | `a`, `b` -> `dest`            | divides the `int` `a` by `b`; `b` may be a [literal](#literal)                                                                                              |
| `rem-int dest a b`                                                                                    | `a`, `b` -> `dest`            | stores the remainder of the `int` `a` divided by `b`; `b` may be a [literal](#literal)                                                                      |
| `and-int dest a b`                                                                                    | `a`, `b` -> `dest`            | bitwise ands the `int`s `a` and `b`; `b` may be a [literal](#literal)                                                                                       |
| `or-int dest a b`                                                                                     | `a`, `b` -> `dest`            | bitwise ors the `int`s `a` and `b`; `b` may be a [literal](#literal)                                                                                        |
| `xor-int dest a b`                                                                                    | `a`, `b` -> `dest`            | bitwise xors the `int`s `a` and `b`; `b` may be a [literal](#literal)                                                                                       |
| `shl-int dest a b`                                                                                    | `a`, `b` -> `dest`            | shifts the `int` `a` left by `b`; `b` may be a [literal](#literal) from `-128` through `127`                                                                |
| `shr-int dest a b`                                                                                    | `a`, `b` -> `dest`            | shifts the `int` `a` right by `b`, sign-extending; `b` may be a [literal](#literal) from `-128` through `127`                                               |
| `ushr-int dest a b`                                                                                   | `a`, `b` -> `dest`            | shifts the `int` `a` right by `b`, zero-extending; `b` may be a [literal](#literal) from `-128` through `127`                                               |
| `rsub-int dest src lit`                                                                               | `src`, `lit` -> `dest`        | subtracts the `int` `src` from the [literal](#literal) `lit`                                                                                                |
| `add-long dest a b`                                                                                   | `a`, `b` -> `dest`            | adds the `long`s `a` and `b`                                                                                                                                |
| `sub-long dest a b`                                                                                   | `a`, `b` -> `dest`            | subtracts the `long` `b` from `a`                                                                                                                           |
| `mul-long dest a b`                                                                                   | `a`, `b` -> `dest`            | multiplies the `long`s `a` and `b`                                                                                                                          |
| `div-long dest a b`                                                                                   | `a`, `b` -> `dest`            | divides the `long` `a` by `b`                                                                                                                               |
| `rem-long dest a b`                                                                                   | `a`, `b` -> `dest`            | stores the remainder of the `long` `a` divided by `b`                                                                                                       |
| `and-long dest a b`                                                                                   | `a`, `b` -> `dest`            | bitwise ands the `long`s `a` and `b`                                                                                                                        |
| `or-long dest a b`                                                                                    | `a`, `b` -> `dest`            | bitwise ors the `long`s `a` and `b`                                                                                                                         |
| `xor-long dest a b`                                                                                   | `a`, `b` -> `dest`            | bitwise xors the `long`s `a` and `b`                                                                                                                        |
| `shl-long dest a b`                                                                                   | `a`, `b` -> `dest`            | shifts the `long` `a` left by the `int` `b`                                                                                                                 |
| `shr-long dest a b`                                                                                   | `a`, `b` -> `dest`            | shifts the `long` `a` right by the `int` `b`, sign-extending                                                                                                |
| `ushr-long dest a b`                                                                                  | `a`, `b` -> `dest`            | shifts the `long` `a` right by the `int` `b`, zero-extending                                                                                                |
| `add-float dest a b`                                                                                  | `a`, `b` -> `dest`            | adds the `float`s `a` and `b`                                                                                                                               |
| `sub-float dest a b`                                                                                  | `a`, `b` -> `dest`            | subtracts the `float` `b` from `a`                                                                                                                          |
| `mul-float dest a b`                                                                                  | `a`, `b` -> `dest`            | multiplies the `float`s `a` and `b`                                                                                                                         |
| `div-float dest a b`                                                                                  | `a`, `b` -> `dest`            | divides the `float` `a` by `b`                                                                                                                              |
| `rem-float dest a b`                                                                                  | `a`, `b` -> `dest`            | stores the remainder of the `float` `a` divided by `b`                                                                                                      |
| `add-double dest a b`                                                                                 | `a`, `b` -> `dest`            | adds the `double`s `a` and `b`                                                                                                                              |
| `sub-double dest a b`                                                                                 | `a`, `b` -> `dest`            | subtracts the `double` `b` from `a`                                                                                                                         |
| `mul-double dest a b`                                                                                 | `a`, `b` -> `dest`            | multiplies the `double`s `a` and `b`                                                                                                                        |
| `div-double dest a b`                                                                                 | `a`, `b` -> `dest`            | divides the `double` `a` by `b`                                                                                                                             |
| `rem-double dest a b`                                                                                 | `a`, `b` -> `dest`            | stores the remainder of the `double` `a` divided by `b`                                                                                                     |
| `aget dest array index`                                                                               | `array`, `index` -> `dest`    | stores the `int` at `index` in the array `array` in `dest`                                                                                                  |
| `aget-wide dest array index`                                                                          | `array`, `index` -> `dest`    | stores the wide value at `index` in the array `array` in `dest`                                                                                             |
| `aget-object dest array index`                                                                        | `array`, `index` -> `dest`    | stores the object at `index` in the array `array` in `dest`                                                                                                 |
| `aget-boolean dest array index`                                                                       | `array`, `index` -> `dest`    | stores the `boolean` at `index` in the array `array` in `dest`, zero-extended                                                                               |
| `aget-byte dest array index`                                                                          | `array`, `index` -> `dest`    | stores the `byte` at `index` in the array `array` in `dest`, sign-extended                                                                                  |
| `aget-char dest array index`                                                                          | `array`, `index` -> `dest`    | stores the `char` at `index` in the array `array` in `dest`, zero-extended                                                                                  |
| `aget-short dest array index`                                                                         | `array`, `index` -> `dest`    | stores the `short` at `index` in the array `array` in `dest`, sign-extended                                                                                 |
| `aput src array index`                                                                                | `src`, `array`, `index` ->    | stores the `int` `src` at `index` in the array `array`                                                                                                      |
| `aput-wide src array index`                                                                           | `src`, `array`, `index` ->    | stores the wide value `src` at `index` in the array `array`                                                                                                 |
| `aput-object src array index`                                                                         | `src`, `array`, `index` ->    | stores the object `src` at `index` in the array `array`                                                                                                     |
| `aput-boolean src array index`                                                                        | `src`, `array`, `index` ->    | stores the `boolean` `src` at `index` in the array `array`                                                                                                  |
| `aput-byte src array index`                                                                           | `src`, `array`, `index` ->    | stores the `byte` `src` at `index` in the array `array`                                                                                                     |
| `aput-char src array index`                                                                           | `src`, `array`, `index` ->    | stores the `char` `src` at `index` in the array `array`                                                                                                     |
| `aput-short src array index`                                                                          | `src`, `array`, `index` ->    | stores the `short` `src` at `index` in the array `array`                                                                                                    |
| `iget dest instance owner.name fieldDescriptor`                                                       | `instance` -> `dest`          | stores the `int` field `owner.name` of `instance` in `dest`                                                                                                 |
| `iget-wide dest instance owner.name fieldDescriptor`                                                  | `instance` -> `dest`          | stores the wide field `owner.name` of `instance` in `dest`                                                                                                  |
| `iget-object dest instance owner.name fieldDescriptor`                                                | `instance` -> `dest`          | stores the object field `owner.name` of `instance` in `dest`                                                                                                |
| `iget-boolean dest instance owner.name fieldDescriptor`                                               | `instance` -> `dest`          | stores the `boolean` field `owner.name` of `instance` in `dest`, zero-extended                                                                              |
| `iget-byte dest instance owner.name fieldDescriptor`                                                  | `instance` -> `dest`          | stores the `byte` field `owner.name` of `instance` in `dest`, sign-extended                                                                                 |
| `iget-char dest instance owner.name fieldDescriptor`                                                  | `instance` -> `dest`          | stores the `char` field `owner.name` of `instance` in `dest`, zero-extended                                                                                 |
| `iget-short dest instance owner.name fieldDescriptor`                                                 | `instance` -> `dest`          | stores the `short` field `owner.name` of `instance` in `dest`, sign-extended                                                                                |
| `iput src instance owner.name fieldDescriptor`                                                        | `src`, `instance` ->          | stores the `int` `src` in the field `owner.name` of `instance`                                                                                              |
| `iput-wide src instance owner.name fieldDescriptor`                                                   | `src`, `instance` ->          | stores the wide value `src` in the field `owner.name` of `instance`                                                                                         |
| `iput-object src instance owner.name fieldDescriptor`                                                 | `src`, `instance` ->          | stores the object `src` in the field `owner.name` of `instance`                                                                                             |
| `iput-boolean src instance owner.name fieldDescriptor`                                                | `src`, `instance` ->          | stores the `boolean` `src` in the field `owner.name` of `instance`                                                                                          |
| `iput-byte src instance owner.name fieldDescriptor`                                                   | `src`, `instance` ->          | stores the `byte` `src` in the field `owner.name` of `instance`                                                                                             |
| `iput-char src instance owner.name fieldDescriptor`                                                   | `src`, `instance` ->          | stores the `char` `src` in the field `owner.name` of `instance`                                                                                             |
| `iput-short src instance owner.name fieldDescriptor`                                                  | `src`, `instance` ->          | stores the `short` `src` in the field `owner.name` of `instance`                                                                                            |
| `sget dest owner.name fieldDescriptor`                                                                | -> `dest`                     | stores the `int` static field `owner.name` in `dest`                                                                                                        |
| `sget-wide dest owner.name fieldDescriptor`                                                           | -> `dest`                     | stores the wide static field `owner.name` in `dest`                                                                                                         |
| `sget-object dest owner.name fieldDescriptor`                                                         | -> `dest`                     | stores the object static field `owner.name` in `dest`                                                                                                       |
| `sget-boolean dest owner.name fieldDescriptor`                                                        | -> `dest`                     | stores the `boolean` static field `owner.name` in `dest`, zero-extended                                                                                     |
| `sget-byte dest owner.name fieldDescriptor`                                                           | -> `dest`                     | stores the `byte` static field `owner.name` in `dest`, sign-extended                                                                                        |
| `sget-char dest owner.name fieldDescriptor`                                                           | -> `dest`                     | stores the `char` static field `owner.name` in `dest`, zero-extended                                                                                        |
| `sget-short dest owner.name fieldDescriptor`                                                          | -> `dest`                     | stores the `short` static field `owner.name` in `dest`, sign-extended                                                                                       |
| `sput src owner.name fieldDescriptor`                                                                 | `src` ->                      | stores the `int` `src` in the static field `owner.name`                                                                                                     |
| `sput-wide src owner.name fieldDescriptor`                                                            | `src` ->                      | stores the wide value `src` in the static field `owner.name`                                                                                                |
| `sput-object src owner.name fieldDescriptor`                                                          | `src` ->                      | stores the object `src` in the static field `owner.name`                                                                                                    |
| `sput-boolean src owner.name fieldDescriptor`                                                         | `src` ->                      | stores the `boolean` `src` in the static field `owner.name`                                                                                                 |
| `sput-byte src owner.name fieldDescriptor`                                                            | `src` ->                      | stores the `byte` `src` in the static field `owner.name`                                                                                                    |
| `sput-char src owner.name fieldDescriptor`                                                            | `src` ->                      | stores the `char` `src` in the static field `owner.name`                                                                                                    |
| `sput-short src owner.name fieldDescriptor`                                                           | `src` ->                      | stores the `short` `src` in the static field `owner.name`                                                                                                   |
| `invoke-virtual { registers } owner.name methodDescriptor`                                            | `registers` ->                | calls the virtual method `owner.name`, whose result is read with `move-result`                                                                              |
| `invoke-super { registers } owner.name methodDescriptor`                                              | `registers` ->                | calls the super method `owner.name`                                                                                                                         |
| `invoke-direct { registers } owner.name methodDescriptor`                                             | `registers` ->                | calls the instance method `owner.name` without virtual dispatch                                                                                             |
| `invoke-static { registers } owner.name methodDescriptor`                                             | `registers` ->                | calls the static method `owner.name`                                                                                                                        |
| `invoke-interface { registers } owner.name methodDescriptor`                                          | `registers` ->                | calls the interface method `owner.name`                                                                                                                     |
| `invoke-virtual/range { first, last } owner.name methodDescriptor`                                    | `registers` ->                | calls the virtual method `owner.name` with the registers from `first` through `last`                                                                        |
| `invoke-super/range { first, last } owner.name methodDescriptor`                                      | `registers` ->                | calls the super method `owner.name` with the registers from `first` through `last`                                                                          |
| `invoke-direct/range { first, last } owner.name methodDescriptor`                                     | `registers` ->                | calls the instance method `owner.name` without virtual dispatch, with the registers from `first` through `last`                                             |
| `invoke-static/range { first, last } owner.name methodDescriptor`                                     | `registers` ->                | calls the static method `owner.name` with the registers from `first` through `last`                                                                         |
| `invoke-interface/range { first, last } owner.name methodDescriptor`                                  | `registers` ->                | calls the interface method `owner.name` with the registers from `first` through `last`                                                                      |
| <code>invoke-custom { registers } name descriptor [h: [handle](#handle)] { constants }</code>         | `registers` ->                | calls the call site `name` of the method type `descriptor`, bootstrapped by the [handle](#handle) with `constants`, whose result is read with `move-result` |
| <code>invoke-custom/range { first, last } name descriptor [h: [handle](#handle)] { constants }</code> | `registers` ->                | calls the call site `name` with the registers from `first` through `last`                                                                                   |
| `invoke-polymorphic { registers } owner.name methodDescriptor callSiteDescriptor`                     | `registers` ->                | calls the polymorphic method `owner.name` as the signature `callSiteDescriptor`                                                                             |
| `invoke-polymorphic/range { first, last } owner.name methodDescriptor callSiteDescriptor`             | `registers` ->                | calls the polymorphic method `owner.name` as the signature `callSiteDescriptor`, with the registers from `first` through `last`                             |
| `line number`                                                                                         | ->                            | pseudo-instruction recording `number` as the line the following instruction originates from                                                                 |
