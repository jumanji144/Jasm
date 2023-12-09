# Basics

In Jasm, attributes and classes, methods and fields are represented via [declarations]()

The most simple declarations are:

## Class
<pre>
.class modifiers name { [members] }
</pre>

Members is a [list of declarations](Syntax.md#declaration-list)

Valid members for a class declaration are:
- [method](#method)
- [field](#field)

### Modifiers
Valid modifiers for the `class` type are:   
`public private protected static final native
abstract interface synthetic strict annotation
enum super module`

## Method
<pre>
.method modifiers name descriptor {
    parameters: { parameter... }, // optional
    exceptions: { <a href="#exception">exception</a>... }, // optional
    code: { // optional
        <a href="Instructions.md#instruction">instruction</a>...
    }
}
</pre>

### Modifiers
Valid modifiers for the `method` type are:
`public private protected static final native
abstract interface synthetic strict annotation
enum synchronized bridge varargs`

### Exception
A exception is a try catch handler descriptor object, it is structured like this:
```
{ start, end, handler, exception }
```
start, end and handler are identifiers of the label they represent, so `A`, `Q` or `AB`.
the exception is either the internal of the class, or to have an all catch block simply use `*`

## Field
<pre>
.field <a>modifiers</a> name descriptor
</pre>

### Modifiers
Valid modifiers for the `field` type are:
`public private protected static final native
abstract interface synthetic strict annotation
enum volatile transient`

