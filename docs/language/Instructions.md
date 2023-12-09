# Instructions
Code objects contain instruction. These instructions depend on the platform, there are currently 2 instruction sets
avaiable:
- [jvm](instructions/Jvm)
- [dalvik](instructions/Dalvik)

## Generic
There are some things generic across both instructions

### Labels
```
label:
```
Labels are used for jumps, location information and flow blocks. A label must always be placed between two instructions:
```
lint 3
label:
jmp label
```
Labels can be referenced before they were defined.