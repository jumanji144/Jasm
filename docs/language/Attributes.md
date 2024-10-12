# Attributes

Certain top level declarations can have attributes declared immediately before that will be collected by the declaration

## Members (Class, Method, Field)

### Annotation

Annotation with `RetentionPolicy.RUNTIME`
```
.visible-annotation name {
    key: element
}
```

Annotation with `RetentionPolicy.CLASS`
```
.invisible-annotation name {
    key: element
}
```

Annotations that are embedded in others do not have a concept of being visible or not.
For instance:
```java
@Foo(bar = @Bar(fizz = "buzz"))
```
Could be represented as either:
```
.visible-annotation Foo {
    bar: .annotation Bar {
        fizz: "Buzz"
    }
}
```
or
```
.invisible-annotation Foo {
    bar: .annotation Bar {
        fizz: "Buzz"
    }
}
```
It would depend on how `@Foo` uses `@RetentionPolicy`, but the important note is that the internal `@Bar` usage is just a `.annotation`.

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
Appends an interface to the implemented interfaces, the `interfacetype` must be given in 
internal name format (`java/util/function/Consumer`). 

### Inner class
```
.inner modifiers {
  name: innerName, // optional
  inner: innerClass,
  outer: outerClass // optional
}
```
Appends an inner class to the inner classes, `innerClass` and `outerClass` must be given in internal name format
and name must be an identifier. When `outer` isn't given `name` must also be not given.

### Source File
```
.sourcefile "SourceFile"
```
Specifies the source file of the class.

### Outer class + method
```
.outer-class typename
.outer-method methodName methodDescriptor
```
Specifies the outer class and outer method declaration this inner class is defined within.

### Permitted subclasses
```
.permitted-subclass typename
```
Appends an internal type to the list of allowed subclasses for `sealed` types.

### Nest host + members
```
.nest-host typename
```
Specifies the host of the nest this class belongs to. Typically seen in inner classes where the outer class is the host.

```
.nest-member typename
```
Appends an internal type to the list of members this class is a nest host for.

### Record Components
```
.annotation name { ... }       # Optional attribute to apply to the record component
.signature "generic signature" # Optional attribute to apply to the record component
.record-component name descriptor
```
Appends an entry the list of the record class's components.