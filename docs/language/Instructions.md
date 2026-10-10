# Instructions

Code objects contain instruction. These instructions depend on the platform, there are currently 2 instruction sets
available:

- [jvm](instructions/Jvm.md)
- [dalvik](instructions/Dalvik.md)

## Generic

There are some things generic across both instructions.

### Labels

```
label:
```

Labels are used for jumps, location information and flow blocks. A label must always be placed between two instructions:

```
label:
iload index
ifeq label // jump backwards if 'index == 0'
```

Labels can be referenced before they were defined.