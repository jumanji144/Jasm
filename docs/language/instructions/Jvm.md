# JVM Instruction set
The jvm instruction set for jasm 

## Generic
Generic objects reused accords multiple instructions:

### Constant
A constant is a object present in the constant pool and can be represented by the following expressions:
- identifier:
  - class type (`Lsome/package/Class;` `[Lsome/package/Array` `[I` `I`)
  - a method type (`(Lsome/package/Argument;IIJJZZ)Lsome/package/Return;`)
- number:
    represents a numeric constant, depending on context only certain number kinds are allowed
- string:
    a string constant
- array:
    - handle:
        The jvm takes the [handle](#handle-1) and resolves it to a MethodHandle
    - constant dynamic: The jvm evaluates the constant dynamic using the handle to resolve a method handle and then invokes it

### Handle
Format:
```
{ kind, owner.member, descriptor }
```      
A handle is a way to describe a way for the jvm to obtain a `java/lang/invoke/MethodHandle` from the instructions given.
The way the jvm obtains this is determined by the kind:
- invokevirtual (equivalent to: `invokevirtual owner.member descriptor`)
- invokestatic (equivalent to: `invokestatic owner.member descriptor`)
- invokespecial (equivalent to: `invokespecial owner.member descriptor`)
- getfield (equivalent to: `getfield owner.member descriptor`)
- putfield (equivalent to: `putfield owner.member descriptor`)
- getstatic (equivalent to: `getstatic owner.member descriptor`)
- putstatic (equivalent to: `putstatic owner.member descriptor`)
- invokeinterface (equivalent to: `invokeinterface owner.member descriptor`)
- newinvokespecial (equivalent to: `new owner; dup; invokespecial owner.<init> descriptor`

### Constant dynamic
Format:
```
{ name, descriptor { kind, owner.member, descriptor } { arguments } }
```
Constant dynamic is the constant equivalent of the [invokedynamic](#invokedynamic) instruction, as it evaluates the same
but counts as a constant, thus can be used in any other place a constant of its type is permitted.

It behaves the same as a invokedynamic instruction, but evaluates the value directly in place, instead of on the stack

## Instructions