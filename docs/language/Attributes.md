# Attributes

Certain top level declarations can have attributes declared immediately before that will be collected by the declaration

## Members (Class, Method, Field)

### Annotation
```
.annotation name {
    key: element
}
```

#### Element
An element can be one of the following, with following interpretation:
- identifer: a type (`Ljava/lang/String;` / `I`)
- number: a numeric type (`10.3f`)
- string: a string value
- character: a character value
- array: an array containing elements (`{ element, element }`)
- declaration: there are two possible declaration types allowed
    - annotation: a sub annotation
    - enum: a enum value (`.enum class name`)

### Signature
```
.signature "generic signature"
```

## Class

### Super
```
.super supertype
```
Specifies the supertype of the class, the `supertype` must be given in internal name format (`java/lang/Object`).
### Implements
```
.implements interfacetype
```
Appends a interface to the implemented interfaces, the `interfacetype` must be given in 
internal name format (`java/util/function/Consumer`). 
### Inner class
```
.inner modifiers {
  name: innerName, // optional
  inner: innerClass,
  outer: outerClass // optional
}
```
Appends a inner class to the inner classes, `innerClass` and `outerClass` must be given in internal name format
and name must be an identifier. When `outer` isn't given `name` must also be not given.
### Source File
```
.sourcefile "SourceFile"
```
Specifies the source file of the class.