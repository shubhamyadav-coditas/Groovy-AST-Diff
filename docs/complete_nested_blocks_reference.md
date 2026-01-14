# Complete Nested Code Blocks Reference - Groovy

## ALL Possible Code Blocks Inside Groovy Classes/Methods/Scripts

This is an exhaustive reference of every possible code construct that can appear within Groovy classes, methods, scripts, and closures, including Emery DSL patterns. This document reflects the current implementation in `GroovyRecursiveParser` and `GroovyASTDiff`.

---

## Table of Contents

1. [Nested Classes](#1-nested-classes)
2. [Nested Methods/Functions](#2-nested-methodsfunctions)
3. [Closures - All Types](#3-closures---all-types)
4. [Control Flow - IF/ELSE](#4-control-flow---ifelse)
5. [Loops - All Types](#5-loops---all-types)
6. [Switch Statements](#6-switch-statements)
7. [Try/Catch/Finally](#7-trycatchfinally)
8. [Groovy Collection Methods](#8-groovy-collection-methods)
9. [Object Patterns](#9-object-patterns)
10. [Async Patterns](#10-async-patterns)
11. [Special Groovy Patterns](#11-special-groovy-patterns)
12. [Emery DSL Patterns](#12-emery-dsl-patterns)
13. [Script Patterns](#13-script-patterns)
14. [Complete Node Type Matrix](#14-complete-node-type-matrix)

---

## 1. Nested Classes

### 1.1 Inner Classes Inside Classes

```groovy
class Outer {
    // ✅ Inner class declaration
    class Inner {
        def innerMethod() {
            // body → RECURSE
        }
    }
    
    // ✅ Static nested class
    static class StaticNested {
        def staticMethod() {
            // body → RECURSE
        }
    }
    
    // ✅ Private inner class
    private class PrivateInner {
        def method() {
            // body → RECURSE
        }
    }
    
    // ✅ Anonymous inner class
    def createInstance() {
        return new SomeInterface() {
            def method() {
                // anonymous class method → RECURSE
            }
        }
    }
}
```

**AST Structure:**
```
class_definition (Outer)  ← CONTAINER in GROOVY_RECURSIVE_CONTAINERS
└── class_body
    ├── class_definition (Inner)  ← CONTAINER, RECURSE via _get_parseable_children()
    │   └── class_body
    │       └── method_definition  ← CONTAINER, RECURSE
    └── method_definition  ← CONTAINER, RECURSE
```

**Implementation Details:**
- Handled by `node_type in {"class_definition", "interface_definition", "trait_definition", "enum_definition", "annotation_definition"}` in `_get_parseable_children()`
- Uses `node.child_by_field_name("body")` to extract class body
- Recursively processes all named children within the body

### 1.2 Classes Inside Methods

```groovy
def outerMethod() {
    // ✅ Local class declaration
    class LocalClass {
        def localMethod() {
            // body → RECURSE
        }
    }
    
    // ✅ Anonymous class in method
    def instance = new Runnable() {
        void run() {
            // body → RECURSE
        }
    }
    
    return new LocalClass()
}
```

### 1.3 Interface and Trait Definitions

```groovy
class Container {
    // ✅ Nested interface
    interface NestedInterface {
        def interfaceMethod()  // → RECURSE (abstract method)
    }
    
    // ✅ Nested trait
    trait NestedTrait {
        def traitMethod() {
            // body → RECURSE
        }
    }
    
    // ✅ Nested enum
    enum NestedEnum {
        VALUE1, VALUE2, VALUE3
        
        def enumMethod() {
            // body → RECURSE
        }
    }
    
    // ✅ Nested annotation
    @interface NestedAnnotation {
        String value() default ""  // → RECURSE
    }
}
```

---

## 2. Nested Methods/Functions

### 2.1 Method Declarations Inside Methods

```groovy
def outerMethod() {
    // ✅ Nested method declaration
    def innerMethod() {
        // body → RECURSE
    }
    
    // ✅ Method with parameters
    def parameterizedMethod(param1, param2) {
        // body → RECURSE
    }
    
    // ✅ Method with return type
    String typedMethod() {
        return "result"  // body → RECURSE
    }
    
    // ✅ Private nested method
    private def privateMethod() {
        // body → RECURSE
    }
    
    // ✅ Static nested method (in static context)
    static def staticNestedMethod() {
        // body → RECURSE
    }
}
```

**Implementation Details:**
- Function types in `GROOVY_FUNCTION_TYPES`: `function_definition`, `method_definition`, `constructor_definition`
- Uses `node.child_by_field_name("body")` to extract function body
- If body is `block` or `statement_block`, processes all named children
- Single statement bodies are processed directly

### 2.2 Function Expressions and Assignments

```groovy
def outerMethod() {
    // ✅ Function assigned to variable
    def func = { param ->
        // closure body → CONTEXT-AWARE (REAL_CLOSURE = PURE)
    }
    
    // ✅ Method reference assignment
    def methodRef = this.&someMethod
    
    // ✅ Nested function with multiple parameters
    def multiParam = { a, b, c ->
        return a + b + c  // body → CONTEXT-AWARE (REAL_CLOSURE = PURE)
    }
    
    // ✅ Function returning function (currying)
    def curry = { a ->
        return { b ->
            return { c ->
                return a + b + c  // nested closures → CONTEXT-AWARE
            }
        }
    }
}
```

**Implementation Details:**
- Closures are handled context-aware via `_get_closure_context()`
- Real closures (functional context) are treated as pure statements
- Structural closures (class/method bodies) are recursed into
- Context types: `REAL_CLOSURE`, `CLASS_BODY`, `FUNCTION_BODY`, etc.

### 2.3 Constructor Patterns

```groovy
class MyClass {
    // ✅ Primary constructor
    MyClass(param) {
        // constructor body → RECURSE
    }
    
    // ✅ Multiple constructors
    MyClass() {
        this("default")  // constructor chaining → RECURSE
    }
    
    MyClass(String name, int value) {
        // constructor body → RECURSE
    }
}
```

---

## 3. Closures - All Types

### 3.1 Basic Closure Patterns (Context-Aware)

```groovy
def closurePatterns() {
    // ✅ Simple closure (REAL_CLOSURE context)
    def simple = {
        println "Hello"  // body → PURE (not recursed)
    }
    
    // ✅ Closure with parameter (REAL_CLOSURE context)
    def withParam = { param ->
        println param  // body → PURE (not recursed)
    }
    
    // ✅ Closure with multiple parameters
    def multiParam = { a, b, c ->
        return a + b + c  // body → RECURSE
    }
    
    // ✅ Closure with implicit parameter 'it'
    def implicit = {
        println it  // body → RECURSE
    }
    
    // ✅ Closure with typed parameters
    def typed = { String name, Integer age ->
        // body → RECURSE
    }
    
    // ✅ Closure with default parameters
    def withDefaults = { name = "default", age = 0 ->
        // body → RECURSE
    }
}
```

### 3.2 Collection Method Closures (Combined as Single Statements)

```groovy
def collectionClosures() {
    def list = [1, 2, 3, 4, 5]
    
    // ✅ findAll with closure (Combined: method + closure)
    def filtered = list.findAll { item ->
        return item > 2  // closure body → RECURSE
    }
    
    // ✅ collect with closure (Combined)
    def transformed = list.collect { item ->
        return item * 2  // closure body → RECURSE
    }
    
    // ✅ each with closure (Combined)
    list.each { item ->
        println item  // closure body → RECURSE
    }
    
    // ✅ any with closure (Combined)
    def hasAny = list.any { item ->
        return item > 10  // closure body → RECURSE
    }
    
    // ✅ every with closure (Combined)
    def allMatch = list.every { item ->
        return item < 10  // closure body → RECURSE
    }
    
    // ✅ groupBy with closure (Combined)
    def grouped = list.groupBy { item ->
        return item % 2  // closure body → RECURSE
    }
    
    // ✅ sort with closure (Combined)
    def sorted = list.sort { a, b ->
        return a <=> b  // closure body → RECURSE
    }
    
    // ✅ Chained collection methods
    def result = list
        .findAll { it > 1 }      // Each closure → RECURSE
        .collect { it * 2 }
        .groupBy { it % 3 }
}
```

### 3.3 Closure as Method Parameters

```groovy
def closureParameters() {
    // ✅ Method accepting closure
    def withClosure(closure) {
        closure.call()
    }
    
    // ✅ Calling with closure
    withClosure {
        println "Inside closure"  // closure body → RECURSE
    }
    
    // ✅ Multiple closure parameters
    def withMultipleClosures(onSuccess, onError) {
        try {
            onSuccess.call()
        } catch (Exception e) {
            onError.call(e)
        }
    }
    
    withMultipleClosures(
        { println "Success" },    // first closure → RECURSE
        { error -> println error } // second closure → RECURSE
    )
    
    // ✅ Closure with return value
    def processor = { data ->
        // process data
        return processedData  // body → RECURSE
    }
}
```

### 3.4 Nested Closures

```groovy
def nestedClosures() {
    // ✅ Closure containing closure
    def outer = {
        def inner = {
            println "Nested"  // inner closure → RECURSE
        }
        inner.call()  // outer closure body → RECURSE
    }
    
    // ✅ Deeply nested closures
    def level1 = {
        def level2 = {
            def level3 = {
                println "Deep"  // each level → RECURSE
            }
            level3.call()
        }
        level2.call()
    }
    
    // ✅ Closure returning closure
    def closureFactory = { multiplier ->
        return { value ->
            return value * multiplier  // nested return → RECURSE
        }
    }
}
```

---

## 4. Control Flow - IF/ELSE

### 4.1 Basic If Statements (Container-Based Analysis)

```groovy
def controlFlow() {
    // ✅ if only
    if (condition) {
        // body → RECURSE
    }
    
    // ✅ if-else
    if (condition) {
        // consequence → RECURSE (container-based)
    } else {
        // alternative → RECURSE (container-based)
    }
    
    // ✅ if-else if-else (Container-based: treats as nested if statements)
    if (condition1) {
        // if branch → RECURSE
    } else if (condition2) {
        // else-if branch → RECURSE (nested if_statement in else_body)
    } else if (condition3) {
        // another else-if branch → RECURSE
    } else {
        // final else branch → RECURSE
    }
    
    // ✅ Single statement (no braces)
    if (condition)
        singleStatement()  // still → need to handle
    else
        otherStatement()
    
    // ✅ Nested if inside if
    if (outer) {
        if (inner) {
            // nested → RECURSE
        }
    }
}
```

**AST Structure (Container-Based):**
```
if_statement  ← CONTAINER in GROOVY_RECURSIVE_CONTAINERS
├── condition (parenthesized_expression)
├── body (statement_block | single_statement)  ← RECURSE via _get_block_statements()
└── else_body (statement_block | if_statement | null)  ← RECURSE via _get_block_statements()
    ├── if_statement (for else-if)  ← ELSE-IF BRANCH
    └── statement_block             ← ELSE BRANCH
```

**Implementation Details:**
- `if_statement` is in `GROOVY_RECURSIVE_CONTAINERS`
- Uses `node.child_by_field_name("body")` and `node.child_by_field_name("else_body")`
- Processes statements within each branch via `_get_block_statements()`
- **Note:** Uses container-based comparison, not individual branch extraction like JavaScript

### 4.2 Groovy-Specific If Patterns

```groovy
def groovyIfPatterns() {
    // ✅ Groovy truth (null, empty collections, etc.)
    if (list) {  // true if list is not null and not empty
        // body → RECURSE
    }
    
    if (string) {  // true if string is not null and not empty
        // body → RECURSE
    }
    
    // ✅ Safe navigation with if
    if (obj?.property) {
        // body → RECURSE
    }
    
    // ✅ instanceof checks
    if (obj instanceof MyClass) {
        // body → RECURSE
    }
    
    // ✅ in operator
    if (value in collection) {
        // body → RECURSE
    }
    
    // ✅ Pattern matching (Groovy 3+)
    if (value ==~ /pattern/) {
        // body → RECURSE
    }
}
```

### 4.3 Ternary and Elvis Operators

```groovy
def ternaryPatterns() {
    // ✅ Ternary operator
    def result = condition ? valueA : valueB
    
    // ✅ Nested ternary
    def result = condA ? valueA 
               : condB ? valueB 
               : condC ? valueC 
               : defaultValue
    
    // ✅ Ternary with closures
    def handler = isAsync 
        ? { -> async_process() }     // closure → RECURSE
        : { -> sync_process() }      // closure → RECURSE
    
    // ✅ Elvis operator (?:) - Groovy specific
    def value = maybeNull ?: defaultValue
    
    // ✅ Safe navigation (?.)
    def result = obj?.method?.()
    def property = obj?.nested?.property
    
    // ✅ Combined patterns
    def result = obj?.method?.() ?: fallback?.() ?: defaultValue
}
```

---

## 5. Loops - All Types

### 5.1 For Loops (Groovy-Specific)

```groovy
def forLoops() {
    // ✅ Groovy for-in loop (range)
    for (i in 0..10) {
        // body → RECURSE
    }
    
    // ✅ Groovy for-in loop (collection)
    for (item in collection) {
        // body → RECURSE
    }
    
    // ✅ For-in with list
    for (item in [1, 2, 3, 4, 5]) {
        // body → RECURSE
    }
    
    // ✅ For-in with map
    for (entry in map) {
        // body → RECURSE (entry is Map.Entry)
    }
    
    // ✅ For-in with destructuring
    for ((key, value) in map) {
        // body → RECURSE
    }
    
    // ✅ Classic for loop (C-style)
    for (int i = 0; i < n; i++) {
        // body → RECURSE
    }
    
    // ✅ Multiple variables
    for (int i = 0, j = n; i < j; i++, j--) {
        // body → RECURSE
    }
    
    // ✅ Nested for loops
    for (i in 0..n) {
        for (j in 0..m) {
            for (k in 0..p) {
                // nested → RECURSE
            }
        }
    }
    
    // ✅ For loop without body block
    for (i in 0..n)
        singleStatement()  // still need to handle
}
```

### 5.2 While Loops

```groovy
def whileLoops() {
    // ✅ Basic while
    while (condition) {
        // body → RECURSE
    }
    
    // ✅ While true with break
    while (true) {
        if (done) break
        // body → RECURSE
    }
    
    // ✅ While with complex condition
    while (i < n && !found && hasMore()) {
        // body → RECURSE
    }
    
    // ✅ Single statement body
    while (condition)
        singleStatement()
    
    // ✅ Groovy-specific: while with closure condition
    while ({ -> checkCondition() }()) {
        // body → RECURSE
    }
}
```

### 5.3 Do-While Loops

```groovy
def doWhileLoops() {
    // ✅ Basic do-while
    do {
        // body → RECURSE
    } while (condition)
    
    // ✅ With break
    do {
        if (shouldExit) break
        // body → RECURSE
    } while (hasMore)
    
    // ✅ Guaranteed one iteration
    do {
        tryOnce()  // body → RECURSE
    } while (shouldRetry)
}
```

### 5.4 Enhanced For Loops (Java-style converted to Groovy)

```groovy
def enhancedForLoops() {
    // ✅ Enhanced for (converted from Java syntax)
    // Original: for (def item : collection)
    // Converted to Groovy: for (item in collection)
    for (item in collection) {
        // body → RECURSE
    }
    
    // ✅ With type declaration
    for (String item in stringCollection) {
        // body → RECURSE
    }
    
    // ✅ With final modifier
    for (final item in collection) {
        // body → RECURSE
    }
}
```

---

## 6. Switch Statements

### 6.1 Basic Switch

```groovy
def switchStatements() {
    // ✅ Basic switch with cases
    switch (value) {
        case 1:
            // statements → PURE (case is leaf node)
            break
        case 2:
            // statements → PURE (case is leaf node)
            break
        case 3:
            // statements → PURE (case is leaf node)
            break
        default:
            // statements → PURE (switch_default is leaf node)
    }
    
    // ✅ Fall-through cases
    switch (value) {
        case 1:
        case 2:
        case 3:
            // shared handling → RECURSE
            break
        case 4:
            // specific handling → RECURSE
            // intentional fall-through
        case 5:
            // more handling → RECURSE
            break
    }
    
    // ✅ Return from switch
    switch (value) {
        case 1:
            return resultA  // → RECURSE
        case 2:
            return resultB
        default:
            return defaultResult
    }
}
```

**AST Structure:**
```
switch_statement  ← CONTAINER in GROOVY_RECURSIVE_CONTAINERS
├── value (parenthesized_expression)
└── switch_block  ← CONTAINER in GROOVY_RECURSIVE_CONTAINERS
    ├── case  ← LEAF in GROOVY_LEAF_STATEMENTS (prevents infinite recursion)
    │   └── statements...  ← NOT recursed (case is pure statement)
    ├── case
    │   └── statements...
    └── default
        └── statements...
```

**Implementation Details:**
- `switch_statement` and `switch_block` are in `GROOVY_RECURSIVE_CONTAINERS`
- `case` and `switch_default` are in `GROOVY_LEAF_STATEMENTS` to prevent infinite recursion
- Switch statement uses `node.child_by_field_name("body")` to get switch_block
- Switch block processes named children (cases) but cases are treated as pure statements

### 6.2 Groovy-Specific Switch Patterns

```groovy
def groovySwitch() {
    // ✅ Switch on type
    switch (obj) {
        case String:
            // type matching → RECURSE
            break
        case Integer:
            // type matching → RECURSE
            break
        case List:
            // type matching → RECURSE
            break
    }
    
    // ✅ Switch with closures
    switch (value) {
        case { it > 0 }:
            // closure condition → RECURSE
            break
        case { it < 0 }:
            // closure condition → RECURSE
            break
        default:
            // default case → RECURSE
    }
    
    // ✅ Switch with ranges
    switch (number) {
        case 0..10:
            // range matching → RECURSE
            break
        case 11..20:
            // range matching → RECURSE
            break
    }
    
    // ✅ Switch with regular expressions
    switch (text) {
        case ~/pattern1/:
            // regex matching → RECURSE
            break
        case ~/pattern2/:
            // regex matching → RECURSE
            break
    }
    
    // ✅ Switch with collections
    switch (value) {
        case [1, 2, 3]:
            // list matching → RECURSE
            break
        case ['a', 'b']:
            // list matching → RECURSE
            break
    }
}
```

---

## 7. Try/Catch/Finally

### 7.1 Basic Try-Catch

```groovy
def tryCatch() {
    // ✅ Basic try-catch
    try {
        riskyOperation()  // body → RECURSE
    } catch (Exception error) {
        handleError(error)  // handler → RECURSE
    }
    
    // ✅ Try-finally (no catch)
    try {
        // body → RECURSE
    } finally {
        cleanup()  // finalizer → RECURSE
    }
    
    // ✅ Try-catch-finally
    try {
        // body → RECURSE
    } catch (Exception error) {
        // handler → RECURSE
    } finally {
        // finalizer → RECURSE
    }
    
    // ✅ Multiple catch blocks
    try {
        // body → RECURSE
    } catch (IOException e) {
        // specific handler → RECURSE
    } catch (RuntimeException e) {
        // another handler → RECURSE
    } catch (Exception e) {
        // general handler → RECURSE
    }
}
```

**AST Structure:**
```
try_statement  ← CONTAINER in GROOVY_RECURSIVE_CONTAINERS
├── body (statement_block)  ← RECURSE via _get_block_statements()
├── catch_clause
│   ├── parameter (identifier or pattern)
│   └── body (statement_block)  ← RECURSE via _get_block_statements()
├── catch_clause (multiple possible)
└── finally_clause
    └── body (statement_block)  ← RECURSE via _get_block_statements()
```

**Implementation Details:**
- `try_statement` is in `GROOVY_RECURSIVE_CONTAINERS`
- Uses field names: `body`, `catch_body`, `finally_body` via `child_by_field_name()`
- Each section (try, catch, finally) is processed via `_get_block_statements()`
- Multiple catch clauses are handled individually

### 7.2 Groovy-Specific Try Patterns

```groovy
def groovyTryPatterns() {
    // ✅ Try with resources (Groovy style)
    new File('file.txt').withReader { reader ->
        // automatic resource management → RECURSE
    }
    
    // ✅ Try with multiple resources
    new File('input.txt').withReader { input ->
        new File('output.txt').withWriter { output ->
            // nested resource management → RECURSE
        }
    }
    
    // ✅ Catch with pattern matching
    try {
        // body → RECURSE
    } catch (Exception e) {
        switch (e) {
            case IOException:
                // pattern-based handling → RECURSE
                break
            case RuntimeException:
                // pattern-based handling → RECURSE
                break
        }
    }
    
    // ✅ Nested try-catch
    try {
        try {
            // inner try → RECURSE
        } catch (InnerException e) {
            // inner catch → RECURSE
            throw new OuterException(e)
        }
    } catch (OuterException e) {
        // outer catch → RECURSE
    }
}
```

---

## 8. Groovy Collection Methods

### 8.1 List Methods with Closures

```groovy
def listMethods() {
    def list = [1, 2, 3, 4, 5]
    
    // ✅ findAll (Combined: method + closure)
    def filtered = list.findAll { item ->
        return item > 2  // closure body → RECURSE
    }
    
    // ✅ collect (Combined)
    def transformed = list.collect { item ->
        return item * 2  // closure body → RECURSE
    }
    
    // ✅ each (Combined)
    list.each { item ->
        println item  // closure body → RECURSE
    }
    
    // ✅ eachWithIndex (Combined)
    list.eachWithIndex { item, index ->
        println "$index: $item"  // closure body → RECURSE
    }
    
    // ✅ find (Combined)
    def found = list.find { item ->
        return item > 3  // closure body → RECURSE
    }
    
    // ✅ findIndexOf (Combined)
    def index = list.findIndexOf { item ->
        return item == 3  // closure body → RECURSE
    }
    
    // ✅ any (Combined)
    def hasAny = list.any { item ->
        return item > 10  // closure body → RECURSE
    }
    
    // ✅ every (Combined)
    def allMatch = list.every { item ->
        return item < 10  // closure body → RECURSE
    }
    
    // ✅ inject/reduce (Combined)
    def sum = list.inject(0) { acc, item ->
        return acc + item  // closure body → RECURSE
    }
    
    // ✅ sort (Combined)
    def sorted = list.sort { a, b ->
        return a <=> b  // closure body → RECURSE
    }
    
    // ✅ groupBy (Combined)
    def grouped = list.groupBy { item ->
        return item % 2  // closure body → RECURSE
    }
    
    // ✅ collectEntries (Combined)
    def map = list.collectEntries { item ->
        return [item, item * 2]  // closure body → RECURSE
    }
}
```

### 8.2 Map Methods with Closures

```groovy
def mapMethods() {
    def map = [a: 1, b: 2, c: 3]
    
    // ✅ each (Combined)
    map.each { key, value ->
        println "$key = $value"  // closure body → RECURSE
    }
    
    // ✅ findAll (Combined)
    def filtered = map.findAll { key, value ->
        return value > 1  // closure body → RECURSE
    }
    
    // ✅ collectEntries (Combined)
    def transformed = map.collectEntries { key, value ->
        return [key.toUpperCase(), value * 2]  // closure body → RECURSE
    }
    
    // ✅ any (Combined)
    def hasAny = map.any { key, value ->
        return value > 5  // closure body → RECURSE
    }
    
    // ✅ every (Combined)
    def allMatch = map.every { key, value ->
        return value < 10  // closure body → RECURSE
    }
}
```

### 8.3 Chained Collection Methods

```groovy
def chainedMethods() {
    def list = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
    
    // ✅ Complex chaining (Each method+closure combined)
    def result = list
        .findAll { it > 2 }           // filter → RECURSE
        .collect { it * 2 }           // transform → RECURSE
        .groupBy { it % 3 }           // group → RECURSE
        .collectEntries { k, v ->     // transform entries → RECURSE
            [k, v.sum()]
        }
    
    // ✅ Nested collection operations
    def nested = list
        .collect { outer ->
            (1..outer).collect { inner ->  // nested collect → RECURSE
                outer * inner
            }
        }
        .flatten()
        .findAll { it > 10 }
}
```

---

## 9. Object Patterns

### 9.1 Object Methods and Properties

```groovy
def objectPatterns() {
    // ✅ Object literal with methods (map-based)
    def obj = [
        // Property with closure
        method: { ->
            return "result"  // closure → RECURSE
        },
        
        // Property with value
        property: "value",
        
        // Computed property
        computed: { ->
            return computeValue()  // closure → RECURSE
        }
    ]
    
    // ✅ Object with nested structure
    def nested = [
        outer: [
            inner: [
                deepMethod: { ->
                    return "deep"  // nested closure → RECURSE
                }
            ]
        ]
    ]
    
    // ✅ Dynamic property access
    def dynamic = [:]
    dynamic.someProperty = { ->
        return "dynamic"  // closure → RECURSE
    }
    
    // ✅ Method chaining object
    def builder = [
        setValue: { value ->
            this.value = value
            return this  // method chaining → RECURSE
        },
        build: { ->
            return new Result(this.value)  // closure → RECURSE
        }
    ]
}
```

### 9.2 Groovy Bean Patterns

```groovy
class GroovyBean {
    // ✅ Property with getter/setter
    String name
    
    // ✅ Custom getter
    String getName() {
        return this.@name?.toUpperCase()  // method body → RECURSE
    }
    
    // ✅ Custom setter
    void setName(String name) {
        this.@name = name?.trim()  // method body → RECURSE
    }
    
    // ✅ Computed property
    String getDisplayName() {
        return "Name: ${getName()}"  // method body → RECURSE
    }
    
    // ✅ Property with closure validation
    def setValidatedProperty(value) {
        def validator = { val ->
            return val != null && val.length() > 0  // validation closure → RECURSE
        }
        
        if (validator(value)) {
            this.property = value  // method body → RECURSE
        }
    }
}
```

---

## 10. Async Patterns

### 10.1 Groovy Async Patterns

```groovy
import groovy.transform.CompileStatic
import java.util.concurrent.*

def asyncPatterns() {
    // ✅ CompletableFuture with closures
    def future = CompletableFuture.supplyAsync {
        return computeValue()  // async closure → RECURSE
    }
    
    // ✅ Future chaining
    future
        .thenApply { result ->
            return transform(result)  // transform closure → RECURSE
        }
        .thenAccept { finalResult ->
            process(finalResult)  // accept closure → RECURSE
        }
        .exceptionally { throwable ->
            handleError(throwable)  // error closure → RECURSE
            return null
        }
    
    // ✅ Parallel processing
    def futures = (1..10).collect { i ->
        CompletableFuture.supplyAsync {
            return processItem(i)  // parallel closure → RECURSE
        }
    }
    
    CompletableFuture.allOf(futures as CompletableFuture[])
        .thenRun {
            println "All completed"  // completion closure → RECURSE
        }
}
```

### 10.2 GPars Async Patterns

```groovy
import groovyx.gpars.GParsPool

def gparsPatterns() {
    // ✅ Parallel collection processing
    GParsPool.withPool {
        def results = (1..1000).collectParallel { item ->
            return processItem(item)  // parallel closure → RECURSE
        }
        
        def filtered = results.findAllParallel { result ->
            return result.isValid()  // parallel filter → RECURSE
        }
    }
    
    // ✅ Actor pattern
    def actor = actor {
        loop {
            react { message ->
                processMessage(message)  // actor closure → RECURSE
            }
        }
    }
    
    // ✅ Dataflow variables
    def dataflow = new DataflowVariable()
    
    task {
        def result = computeResult()
        dataflow << result  // task closure → RECURSE
    }
    
    task {
        def value = dataflow.val
        processValue(value)  // consumer task → RECURSE
    }
}
```

---

## 11. Special Groovy Patterns

### 11.1 Meta-Programming Patterns

```groovy
def metaProgramming() {
    // ✅ Method missing
    def obj = new Object() {
        def methodMissing(String name, args) {
            return "Called $name with $args"  // method body → RECURSE
        }
        
        def propertyMissing(String name) {
            return "Property $name"  // method body → RECURSE
        }
    }
    
    // ✅ ExpandoMetaClass
    String.metaClass.reverse = {
        return delegate.reverse()  // added method → RECURSE
    }
    
    // ✅ Category usage
    use(TimeCategory) {
        def tomorrow = 1.day.from.now  // category context → RECURSE
    }
}
```

### 11.2 Builder Patterns

```groovy
def builderPatterns() {
    // ✅ Markup builder
    def xml = new groovy.xml.MarkupBuilder()
    xml.root {
        element1 {
            text "content"  // builder closure → RECURSE
        }
        element2(attribute: "value") {
            nested {
                text "nested content"  // nested builder → RECURSE
            }
        }
    }
    
    // ✅ JSON builder
    def json = new groovy.json.JsonBuilder()
    json {
        name "John"
        age 30
        address {
            street "123 Main St"  // nested JSON → RECURSE
            city "Anytown"
        }
    }
    
    // ✅ SQL builder
    def sql = new groovy.sql.Sql(dataSource)
    sql.eachRow("SELECT * FROM users") { row ->
        processRow(row)  // SQL closure → RECURSE
    }
}
```

### 11.3 DSL Patterns

```groovy
def dslPatterns() {
    // ✅ Custom DSL
    def config = new ConfigBuilder()
    config.database {
        host "localhost"
        port 5432
        credentials {
            username "user"  // DSL closure → RECURSE
            password "pass"
        }
    }
    
    // ✅ Gradle-style DSL
    dependencies {
        compile 'org.apache.commons:commons-lang3:3.0'
        testCompile 'junit:junit:4.12'  // DSL block → RECURSE
    }
    
    // ✅ Spock testing DSL
    def "test method"() {
        given:
        def list = [1, 2, 3]  // test block → RECURSE
        
        when:
        def result = list.sum()
        
        then:
        result == 6
    }
}
```

---

## 12. Emery DSL Patterns

### 12.1 Form Operations

```groovy
def emeryFormOperations() {
    // ✅ Form field access (Combined: F.fieldName)
    F.fieldName = "value"
    
    // ✅ Nested field access (Combined: F.skillsMultiRow.rows)
    F.skillsMultiRow.rows.each { row ->
        processRow(row)  // closure → RECURSE
    }
    
    // ✅ Binary operations (Combined: F.rows << newRow)
    F.skillsMultiRow.rows << newRow
    
    // ✅ Form creation (Combined: Emery.form.newForm())
    USER_PROFILE_FORM formObj = Emery.form.newForm("USER_PROFILE_FORM")
    
    // ✅ Form validation
    if (Emery.form.validate(formObj)) {
        // validation block → RECURSE
    }
    
    // ✅ Form submission
    Emery.form.submit(formObj) { result ->
        if (result.success) {
            handleSuccess(result)  // callback closure → RECURSE
        } else {
            handleError(result.error)
        }
    }
}
```

### 12.2 Data Operations

```groovy
def emeryDataOperations() {
    // ✅ Data table operations (Combined: Emery.dataTable.read())
    def countryData = Emery.dataTable.read("COUNTRY_DATA_TABLE")
    def statusData = Emery.dataTable.read("STATUS_LOOKUP_TABLE")
    
    // ✅ Data processing with closures
    countryData.each { row ->
        println "Country: ${row.countryName}"  // closure → RECURSE
    }
    
    // ✅ Data filtering
    def activeCountries = countryData.findAll { row ->
        return row.isActive == true  // filter closure → RECURSE
    }
    
    // ✅ MDOS operations (Combined: Emery.mdos.getMdos())
    def mdosData = Emery.mdos.getMdosDisplayValuesClob(params)
    
    // ✅ MDOS processing
    if (mdosData) {
        mdosData.each { item ->
            processMdosItem(item)  // processing closure → RECURSE
        }
    }
}
```

### 12.3 Workflow Operations

```groovy
def emeryWorkflowOperations() {
    // ✅ Workflow step definition
    def workflowStep = Emery.workflow.defineStep("APPROVAL") { context ->
        if (context.requiresApproval) {
            // step logic → RECURSE
            return Emery.workflow.requestApproval(context.requestId)
        } else {
            return Emery.workflow.autoApprove()
        }
    }
    
    // ✅ Conditional workflow
    if (Emery.workflow.isActive(workflowId)) {
        Emery.workflow.execute(workflowId) { step ->
            // execution callback → RECURSE
            processWorkflowStep(step)
        }
    }
    
    // ✅ Workflow error handling
    try {
        Emery.workflow.start(processId)
    } catch (WorkflowException e) {
        Emery.workflow.handleError(e) { error ->
            logWorkflowError(error)  // error handler → RECURSE
        }
    }
}
```

### 12.4 Test Operations

```groovy
def emeryTestOperations() {
    // ✅ Test assertions (Combined: Emery.test.assertEquals())
    Emery.test.assertEquals("SUCCESS", result.status)
    Emery.test.assertTrue(userCount > 0)
    Emery.test.assertNotNull(response.data)
    
    // ✅ Test setup with closures
    Emery.test.setup { testContext ->
        // test setup → RECURSE
        testContext.createTestData()
        testContext.initializeServices()
    }
    
    // ✅ Test execution
    Emery.test.run("User Registration Test") { test ->
        // test body → RECURSE
        def user = createTestUser()
        def result = registerUser(user)
        
        test.assertEquals("SUCCESS", result.status)
        test.assertTrue(result.userId != null)
    }
    
    // ✅ Test cleanup
    Emery.test.cleanup { testContext ->
        // cleanup logic → RECURSE
        testContext.clearTestData()
        testContext.resetServices()
    }
    
    // ✅ Test delays and timing
    Emery.test.delay(1000)
    Emery.test.waitFor { condition ->
        return service.isReady()  // condition closure → RECURSE
    }
}
```

### 12.5 Use Statements and Utilities

```groovy
def emeryUtilities() {
    // ✅ Use declarations (Combined: use())
    use("EMERY_UTILITIES")
    use("EMERY_FORM_OPERATIONS")
    use("EMERY_DATA_ACCESS")
    
    // ✅ Utility operations with closures
    Emery.util.withTransaction { transaction ->
        // transaction block → RECURSE
        def result = performDatabaseOperation()
        if (result.success) {
            transaction.commit()
        } else {
            transaction.rollback()
        }
    }
    
    // ✅ Logging operations
    Emery.log.info("Processing started")
    Emery.log.debug { ->
        // lazy logging closure → RECURSE
        return "Debug info: ${computeDebugInfo()}"
    }
    
    // ✅ Configuration access
    def config = Emery.config.get("database.settings")
    Emery.config.withSettings("production") { settings ->
        // configuration block → RECURSE
        processWithSettings(settings)
    }
}
```

---

## 13. Script Patterns

### 13.1 Top-Level Script Constructs

```groovy
// ✅ Script-level imports
import java.util.*
import groovy.transform.*

// ✅ Script-level variables
def globalVar = "value"
String typedGlobal = "typed"

// ✅ Script-level methods
def scriptMethod() {
    // method body → RECURSE
}

// ✅ Script-level classes
class ScriptClass {
    def method() {
        // method body → RECURSE
    }
}

// ✅ Script-level closures
def scriptClosure = {
    // closure body → RECURSE
}

// ✅ Script execution blocks
if (args.length > 0) {
    // conditional execution → RECURSE
}

// ✅ Main execution
args.each { arg ->
    processArgument(arg)  // script-level closure → RECURSE
}
```

### 13.2 Gradle Script Patterns

```groovy
// ✅ Gradle build script constructs
plugins {
    id 'java'
    id 'application'  // plugin block → RECURSE
}

dependencies {
    implementation 'org.apache.commons:commons-lang3:3.0'
    testImplementation 'junit:junit:4.12'  // dependency block → RECURSE
}

task customTask {
    doLast {
        println "Custom task execution"  // task closure → RECURSE
    }
}

// ✅ Gradle configuration
configurations {
    customConfig {
        description = "Custom configuration"  // config block → RECURSE
    }
}
```

---

## 14. Complete Node Type Matrix

### Container Nodes (MUST RECURSE)

| Node Type | Children to Parse | Notes |
|-----------|-------------------|-------|
| `class_definition` | `body` → members | Class with name |
| `interface_definition` | `body` → members | Interface declaration |
| `trait_definition` | `body` → members | Groovy trait |
| `enum_definition` | `body` → constants/methods | Enum declaration |
| `annotation_definition` | `body` → methods | Annotation type |
| `method_definition` | `body` | Instance method |
| `function_definition` | `body` | Function declaration |
| `constructor_definition` | `body` | Constructor method |
| `if_statement` | `body`, `else_body` | Branch-aware analysis |
| `switch_statement` | `body` → cases | Switch body |
| `switch_block` | all cases | Container for cases |
| `case` | statements after `:` | Case statements |
| `for_loop` | `body` | Groovy for-in |
| `for_in_loop` | `body` | Enhanced for |
| `while_loop` | `body` | While loop |
| `do_while_statement` | `body` | Do-while loop |
| `try_statement` | `body`, `catch_clause`, `finally_clause` | All parts |
| `catch_clause` | `body` | Catch block |
| `finally_clause` | `body` | Finally block |
| `statement_block` | all statements | Curly braces block |
| `block` | all statements | Generic block |
| `closure` | body (context-aware) | Groovy closure |
| `closure_expression` | body | Closure expression |
| `labeled_statement` | `body` | Labeled block/stmt |
| `synchronized_statement` | `body` | Synchronized block |

### Pure Statements (STOP RECURSION)

| Node Type | Example | Notes |
|-----------|---------|-------|
| `expression_statement` | `println "Hello"` | Method calls, assignments |
| `return_statement` | `return x` | May have expression |
| `throw_statement` | `throw e` | Has expression |
| `break_statement` | `break` | Optional label |
| `continue_statement` | `continue` | Optional label |
| `assert_statement` | `assert condition` | Groovy assertion |
| `import_statement` | `import java.util.*` | Module import |
| `package_statement` | `package com.example` | Package declaration |
| `variable_declaration` | `def x = value` | Simple variable |
| `field_declaration` | `String field` | Class field |
| `empty_statement` | `;` | Just semicolon |

### Needs Closure Check (MAY CONTAIN CLOSURES)

| Node Type | What to Check | Notes |
|-----------|---------------|-------|
| `declaration` | value field | May have closure |
| `assignment` | right side | May have closure |
| `field_definition` | value | May have closure |
| `property_definition` | value | May have closure |
| `function_call` | arguments | May have closure args |
| `method_call` | arguments | May have closure args |
| `expression_statement` | contained calls | May contain closure calls |
| `juxt_function_call` | arguments | Groovy-style calls |

### Emery DSL Nodes (SPECIAL HANDLING)

| Node Type | Pattern | Special Handling |
|-----------|---------|------------------|
| `binary_op` | `F.rows << newRow` | Left-hand identifier extraction |
| `dotted_identifier` | `F.fieldName` | Emery field access |
| `member_access` | `F.skillsMultiRow.rows` | Nested field access |
| `function_call` | `Emery.form.newForm()` | Dotted function name extraction |
| `juxt_function_call` | `println value` | Groovy-style function call |

### Combined Statements (GROOVY-SPECIFIC)

| Pattern | Example | Handling |
|---------|---------|----------|
| Method + Closure | `list.findAll { condition }` | Combined as single statement |
| Type + Assignment | `USER_PROFILE_FORM obj = ...` | Combined declaration |
| Use Statement | `use("EMERY_UTILITIES")` | Function call |

---

## Summary Checklist

When implementing recursive parsing for Groovy, ensure you handle:

- [ ] **Class Declarations** (class, interface, trait, enum, annotation)
- [ ] **Nested Classes** (inner, static, anonymous)
- [ ] **Method Definitions** (instance, static, constructor)
- [ ] **Nested Methods** (methods within methods)
- [ ] **Closures** (standalone, as parameters, collection methods)
- [ ] **Context-Aware Closures** (structural vs functional)
- [ ] **Collection Method Closures** (findAll, collect, each, etc.)
- [ ] **Closure Chaining** (method.closure.method.closure)
- [ ] **If Statements** (branch-aware: if/else-if/else)
- [ ] **Ternary/Elvis Operators** (?, ?:, ?.)
- [ ] **For Loops** (for-in, range-based, C-style)
- [ ] **While/Do-While Loops**
- [ ] **Switch Statements** (basic, type matching, closures, ranges)
- [ ] **Try/Catch/Finally** (multiple catch, resources)
- [ ] **Groovy Collection Methods** (all variants with closures)
- [ ] **Map Operations** (each, findAll, collectEntries)
- [ ] **Object Patterns** (maps with closures, beans)
- [ ] **Async Patterns** (CompletableFuture, GPars)
- [ ] **Meta-Programming** (methodMissing, ExpandoMetaClass)
- [ ] **Builder Patterns** (MarkupBuilder, JsonBuilder)
- [ ] **DSL Patterns** (custom DSLs, Gradle, Spock)
- [ ] **Emery Form Operations** (F.fieldName, form creation)
- [ ] **Emery Data Operations** (dataTable, MDOS)
- [ ] **Emery Workflow Operations** (steps, execution)
- [ ] **Emery Test Operations** (assertions, setup/cleanup)
- [ ] **Emery Utilities** (use statements, transactions)
- [ ] **Script Patterns** (top-level constructs, Gradle)
- [ ] **Typed Declarations** (USER_PROFILE_FORM obj = ...)
- [ ] **Method Call Combinations** (method + closure as single unit)
- [ ] **Enhanced For Loops** (Java-style converted to Groovy)
- [ ] **Safe Navigation** (?. operator)
- [ ] **Groovy Truth** (null/empty checks)
- [ ] **Pattern Matching** (==~ operator)
- [ ] **Range Operations** (0..10, 1..<10)

### Key Groovy-Specific Features:

- **Closure Method Call Combination** (semantic units)
- **Context-Aware Closure Handling** (structural vs functional)
- **Container-Based Analysis** (recursive container comparison for all control structures)
- **Context-Aware Closure Handling** (structural vs functional closure distinction)
- **Depth-Limited Recursion** (max depth of 10 to prevent infinite loops)
- **Leaf Node Detection** (GROOVY_LEAF_STATEMENTS stops recursion)
- **Emery DSL Compatibility** (handled as standard Groovy constructs)
- **Enhanced Identifier Extraction** (dotted, member access)
- **Groovy Syntax Adaptations** (for-in, elvis, safe navigation)
- **Collection Method Support** (each method treated as separate statement)
- **Meta-Programming Support** (dynamic method/property handling)

---

## Implementation Summary

### Current Groovy Implementation vs JavaScript Reference

**Key Similarities:**
- ✅ Recursive parsing down to pure statements
- ✅ Multi-phase matching strategy
- ✅ Hierarchical diff structure with nested containers
- ✅ Support for complex nested structures

**Key Differences:**
- ❌ **No tree-sitter query optimization** (uses manual traversal)
- ❌ **No individual branch extraction** (uses container-based comparison)
- ❌ **No callback method combination** (treats each statement separately)
- ✅ **Context-aware closure handling** (prevents infinite recursion)
- ✅ **Depth limiting** (configurable max depth protection)

### Recursion Control Strategy

The Groovy implementation uses multiple layers of recursion protection:

1. **Depth Limiting**: Maximum recursion depth of 10 levels (configurable)
2. **Leaf Node Detection**: `GROOVY_LEAF_STATEMENTS` set stops recursion
3. **Context-Aware Closures**: Distinguishes structural vs functional closures
4. **Special Case Handling**: Switch cases treated as leaf nodes to prevent infinite loops

### Container vs Leaf Classification

**Containers (GROOVY_RECURSIVE_CONTAINERS):**
- `class_definition`, `interface_definition`, `trait_definition`, `enum_definition`
- `function_definition`, `method_definition`, `constructor_definition`
- `if_statement`, `switch_statement`, `switch_block`, `try_statement`
- `for_loop`, `for_in_loop`, `while_loop`, `do_while_loop`
- `block`, `statement_block`
- `closure` (context-dependent), `closure_expression`

**Leaf Nodes (GROOVY_LEAF_STATEMENTS):**
- `expression_statement`, `return_statement`, `throw_statement`
- `break_statement`, `continue_statement`, `assert_statement`
- `import_statement`, `package_statement`
- `variable_declaration`, `field_declaration`, `declaration`
- `case`, `switch_default` (special handling to prevent infinite recursion)

This implementation provides comprehensive coverage of Groovy constructs while maintaining performance and preventing infinite recursion through careful container/leaf classification.