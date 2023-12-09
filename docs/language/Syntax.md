# Syntax Reference

## Tokens

The language grammar is made up off five main tokens: 
### identifier 
    - Everything that is not any other token
    - Accepts all of unicode directly, except:
        - Operator characters (`{ } , :`)
        - Whitespaces (`<space> <newline> <cariage return>`)
        - String or character triggers (`' "`)
    - Allows for escape sequences (\uXXXX or any other escape sequence),
      which will be turned into the respective character before being evaluated
        - Example `Hello\u0020World!`, would become `Hello World!`
        - Example `Hello\n\"World\"!`, would become `Hello<newline>"World"!`
### number
    - supports integers, longs, floats and doubles using the respective suffix
        - l/L for longs
        - d/D for doubles
        - f/F for floats
        - nothing for integers
        - default interpretation is:
            - float (when decimal point is present)
            - integer (when no other condition is met)
    - supports hexadecimal (0xX), scientific floats (X.XeY), hexadecimal floats (0xX.XpY)
    - `nan`, `-nan`, `infinity` and `-infinity` count as numbers
### string
    - anything within `""` is a string
    - supports all java string escape sequences
### character
    - anything within `''` is a character
    - supports all java character escape sequences
### operator
    - any of: `{ } , :`

## Expressions

The language has three main categories of `expression`s:
- The [Declaration](#Declaration)
- The [Object](#Objects)
- The [Value](#Value)

### Declaration
Declarations are the core foundation of the language, as they declare components of the class file.    
Declarations are structured like the following:
```
.identifier <arguments>
```
Where `arguments` can be any expression.    
The end of the arguments is determined by multiple conditions:
- another declaration is found
- next value in object or array
- closing `}` for object or array
- end of file

### Objects
Objects are used as arguments for declarations, instructions or other values as they can be interpreted as values.    
The Object has two structures, first the simple [key -> value] structure:
```
{
    identifer: expression,
    identifer: expression,
    ...
    identifer: expression
}
```
And the second is the array structure:
```
{ expression, expression ... expression }
```

#### Code
Under objects is a type of object which contain the instructions of a code object,
which are structured like this:
```
{ 
label:
    instruction
    instruction
label:
...
}
```
Where labels and instructions can be in arbitrary order

#### Declaration list
Under objects is a type of object which contains a list of declarations, 
which are structured like this:
```
{
    .declaration <arguments>
    .declaration <arguments>
    ...
    .declaration <arguments>
}
```


### Value
Values can be any of the four tokens (without operators) where they represent the following:
- identifier: 
    - a class/field/method path
    - a class/method type
    - a field/method descriptor
- number: a number
- string: a string
- character: a character 