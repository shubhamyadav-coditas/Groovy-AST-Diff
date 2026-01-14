# Groovy Code Blocks Reference

## Complete Code Block Types for Recursive Parsing

This document provides a comprehensive reference of all Groovy code blocks that can appear within classes, methods, and scripts and need to be parsed recursively. This includes standard Groovy constructs as well as Emery DSL patterns.

---

## 1. Node Type Classification

### 🔵 CONTAINER NODES (Require Recursion)

These nodes contain other statements and must be parsed recursively (from `GROOVY_RECURSIVE_CONTAINERS`):

| Node Type | Example | Children to Parse | Implementation |
|-----------|---------|-------------------|----------------|
| `class_definition` | `class Foo {}` | `body` (class members) | `child_by_field_name("body")` |
| `interface_definition` | `interface Bar {}` | `body` (interface members) | `child_by_field_name("body")` |
| `trait_definition` | `trait Mixable {}` | `body` (trait members) | `child_by_field_name("body")` |
| `enum_definition` | `enum Status {}` | `body` (enum constants/methods) | `child_by_field_name("body")` |
| `annotation_definition` | `@interface MyAnnotation {}` | `body` | `child_by_field_name("body")` |
| `method_definition` | `def methodName() {}` | `body` (statement_block) | `child_by_field_name("body")` |
| `function_definition` | `def functionName() {}` | `body` | `child_by_field_name("body")` |
| `constructor_definition` | `Constructor() {}` | `body` | `child_by_field_name("body")` |
| `if_statement` | `if (cond) {} else {}` | `body`, `else_body` | `_get_block_statements()` |
| `switch_statement` | `switch (x) {}` | `body` (switch_block) | `child_by_field_name("body")` |
| `switch_block` | `switch body container` | named children (cases) | `named_children` |
| `for_loop` | `for (i in 0..10) {}` | `body` | `child_by_field_name("body")` |
| `for_in_loop` | `for (item in collection) {}` | `body` | `child_by_field_name("body")` |
| `while_loop` | `while (condition) {}` | `body` | `child_by_field_name("body")` |
| `do_while_loop` | `do {} while()` | `body` | `child_by_field_name("body")` |
| `do_while_statement` | `do {} while()` | `body` | `child_by_field_name("body")` |
| `try_statement` | `try {} catch {}` | `body`, `catch_body`, `finally_body` | Field-based extraction |
| `statement_block` | `{ ... }` | all child statements | `named_children` |
| `block` | `{ ... }` | all child statements | `named_children` |
| `closure` | `{ it > 0 }` | body (context-aware) | Context detection via `_get_closure_context()` |
| `closure_expression` | `list.findAll { condition }` | closure body | Context-aware |
| `else_clause` | `else {}` | body | `_get_block_statements()` |
| `switch_default` | `default:` | statements | Special handling |
| `synchronized_statement` | `synchronized (obj) {}` | `body` | `child_by_field_name("body")` |
| `labeled_statement` | `label: stmt` | `body` | `child_by_field_name("statement")` |

### 🟢 PURE STATEMENTS (Leaf Nodes - Stop Recursion)

These are atomic statements that don't contain nested blocks (from `GROOVY_LEAF_STATEMENTS`):

| Node Type | Example | Notes | Reason for Leaf Status |
|-----------|---------|-------|------------------------|
| `expression_statement` | `println "Hello"` | Method calls, assignments | Atomic statement |
| `return_statement` | `return value` | May have expression | Atomic statement |
| `throw_statement` | `throw new Exception()` | Has expression | Atomic statement |
| `break_statement` | `break` | Optional label | Atomic statement |
| `continue_statement` | `continue` | Optional label | Atomic statement |
| `assert_statement` | `assert condition` | Groovy assertion | Atomic statement |
| `import_statement` | `import java.util.List` | Module import | Atomic statement |
| `package_statement` | `package com.example` | Package declaration | Atomic statement |
| `variable_declaration` | `def x = value` | Simple variable | Atomic statement |
| `field_declaration` | `private String field` | Class field | Atomic statement |
| `empty_statement` | `;` | Just semicolon | Atomic statement |
| `declaration` | `def x = value` | Groovy declarations | Atomic in grammar |
| `case` | `case 1: statements` | Switch case | **Prevents infinite recursion** |

### 🟡 DECLARATION NODES (Block Type Mapping)

These are mapped to specific block types in `GROOVY_NODE_TYPE_TO_BLOCK_TYPE`:

| Node Type | Example | Maps To | Notes |
|-----------|---------|---------|-------|
| `declaration` | `def x = { closure }` | `BlockType.DECLARATION` | Leaf node in `GROOVY_LEAF_STATEMENTS` |
| `assignment` | `x = { it > 0 }` | `BlockType.EXPRESSION` | Assignment operations |
| `field_definition` | `def field = { closure }` | `BlockType.FIELD` | Class field definitions |
| `property_definition` | `String prop = getValue()` | `BlockType.PROPERTY` | Property definitions |
| `variable_definition` | `String var = value` | `BlockType.FIELD` | Variable definitions |

### 🔮 EMERY DSL NODES (Block Type Mapping)

Emery-specific constructs mapped to standard block types in `GROOVY_NODE_TYPE_TO_BLOCK_TYPE`:

| Node Type | Example | Maps To | Implementation Notes |
|-----------|---------|---------|---------------------|
| `binary_op` | `F.rows << newRow` | `BlockType.EXPRESSION` | Standard expression handling |
| `function_call` | `Emery.form.newForm()` | `BlockType.EXPRESSION` | Standard expression handling |
| `juxt_function_call` | `println value` | `BlockType.FUNCTION_CALL` | Groovy-style function call |
| `method_call` | `obj.method()` | `BlockType.FUNCTION_CALL` | Method invocation |
| `assignment` | `F.fieldName = value` | `BlockType.EXPRESSION` | Assignment operations |
| `increment_op` | `counter++` | `BlockType.EXPRESSION` | Increment/decrement |

**Note:** Emery DSL constructs are handled as standard Groovy syntax - no special DSL-specific parsing logic.

---

## 2. Recursive Parsing Tree

```
program (root)
├── PACKAGE/IMPORTS
│   ├── package_statement           ← PURE (package declaration)
│   └── import_statement            ← PURE (module import)
│
├── CLASSES (CONTAINER - parse members)
│   ├── class_definition
│   │   └── class_body
│   │       ├── method_definition    ← CONTAINER (has body)
│   │       ├── field_declaration    ← May have closure value
│   │       ├── property_definition  ← May have closure value
│   │       └── class_definition     ← NESTED class (recurse)
│   │
│   ├── interface_definition
│   │   └── interface_body
│   │       ├── method_definition    ← CONTAINER (abstract methods)
│   │       └── field_declaration    ← Interface constants
│   │
│   ├── trait_definition
│   │   └── trait_body
│   │       ├── method_definition    ← CONTAINER (trait methods)
│   │       └── field_declaration    ← Trait fields
│   │
│   ├── enum_definition
│   │   └── enum_body
│   │       ├── enum_constant        ← May have constructor args
│   │       └── method_definition    ← CONTAINER (enum methods)
│   │
│   └── annotation_definition
│       └── annotation_body
│           └── method_definition    ← CONTAINER (annotation methods)
│
├── METHODS/FUNCTIONS (CONTAINER - parse body)
│   ├── method_definition
│   │   └── statement_block (body)
│   │       ├── [any statement type below]
│   │       └── [RECURSE into each]
│   │
│   ├── function_definition
│   │   └── statement_block (body)
│   │       └── [RECURSE into statements]
│   │
│   └── constructor_definition
│       └── statement_block (body)
│           └── [RECURSE into statements]
│
├── CONTROL FLOW (CONTAINER - parse branches)
│   ├── if_statement
│   │   ├── body                    ← statement_block or single statement
│   │   └── else_body               ← else branch (if present)
│   │       └── [may contain nested if_statement for else-if]
│   │
│   ├── switch_statement
│   │   └── switch_block
│   │       ├── case                ← Multiple statements per case
│   │       └── default             ← Default case statements
│   │
│   └── try_statement
│       ├── body                    ← statement_block
│       ├── catch_clause            ← CONTAINER with body
│       └── finally_clause          ← CONTAINER with body
│
├── LOOPS (CONTAINER - parse body)
│   ├── for_loop                    → body
│   ├── for_in_loop                 → body (Groovy for-in)
│   ├── while_loop                  → body
│   └── do_while_statement          → body
│
├── GROOVY CLOSURES (Context-Aware via _get_closure_context())
│   ├── closure
│   │   └── [Context Detection]
│   │       ├── CLASS_BODY/FUNCTION_BODY/etc. → Parse children directly (structural)
│   │       └── REAL_CLOSURE → PURE (treated as pure statement)
│   │
│   ├── closure_expression
│   │   └── [Context-aware - usually PURE for functional closures]
│   │
│   └── Collection Methods with Closures
│       ├── list.findAll { condition }   ← Separate statements (not combined)
│       ├── list.collect { transform }   ← Separate statements (not combined)
│       ├── list.each { action }         ← Separate statements (not combined)
│       └── map.collectEntries { }       ← Separate statements (not combined)
│
├── EMERY DSL CONSTRUCTS (Standard Groovy Handling)
│   ├── Form Operations
│   │   ├── F.fieldName              ← assignment (BlockType.EXPRESSION)
│   │   ├── F.skillsMultiRow.rows    ← assignment (BlockType.EXPRESSION)
│   │   └── F.rows << newRow         ← binary_op (BlockType.EXPRESSION)
│   │
│   ├── Emery Function Calls
│   │   ├── Emery.form.newForm()     ← function_call (BlockType.EXPRESSION)
│   │   ├── Emery.dataTable.read()   ← function_call (BlockType.EXPRESSION)
│   │   ├── Emery.mdos.getMdos()     ← function_call (BlockType.EXPRESSION)
│   │   └── Emery.test.assertEquals() ← function_call (BlockType.EXPRESSION)
│   │
│   ├── Typed Declarations
│   │   ├── USER_PROFILE_FORM obj = ... ← declaration (BlockType.DECLARATION)
│   │   └── EMPLOYEE_FORM emp = ...     ← declaration (BlockType.DECLARATION)
│   │
│   └── Use Statements
│       └── use("EMERY_UTILITIES")   ← juxt_function_call (BlockType.FUNCTION_CALL)
│
└── EXPRESSIONS (Check for closures/callbacks)
    ├── expression_statement
    │   └── function_call/method_call
    │       └── arguments
    │           └── closure
    │               └── [RECURSE into closure body if complex]
    │
    ├── assignment
    │   └── right side may be closure/function
    │
    ├── binary_op
    │   └── [Special handling for << operator and field access]
    │
    └── juxt_function_call
        └── [Groovy-style function calls without parentheses]
```

---

## 3. Statement Types Within Method/Class Body

When parsing a method or class body, you'll encounter these statement types:

### Control Flow Statements

```groovy
// if_statement with branch-aware analysis
if (condition) {
    // body → RECURSE
} else if (otherCondition) {
    // nested if_statement in else_body → RECURSE
    // Branch extraction: separate if/else-if/else analysis
} else {
    // final else → RECURSE
}

// switch_statement
switch (expression) {
    case value1:      // case → RECURSE into statements
        break
    case value2:
        // fall through
    default:          // default case → RECURSE
        break
}

// try_statement
try {
    // body → RECURSE
} catch (Exception error) {
    // catch_clause.body → RECURSE
} finally {
    // finally_clause.body → RECURSE
}
```

### Loop Statements

```groovy
// for_loop (Groovy range-based)
for (i in 0..10) {
    // body → RECURSE
}

// for_in_loop (Groovy collection iteration)
for (item in collection) {
    // body → RECURSE
}

// Enhanced for loop (Java-style, converted to Groovy for-in)
for (item in collection) {  // Converted from: for (def item : collection)
    // body → RECURSE
}

// while_loop
while (condition) {
    // body → RECURSE
}

// do_while_statement
do {
    // body → RECURSE
} while (condition)
```

### Nested Classes/Methods

```groovy
// Nested class_definition
class Outer {
    class Inner {           // → RECURSE into inner class
        def method() {      // → RECURSE into method body
            // method body
        }
    }
}

// Nested method_definition
class MyClass {
    def outerMethod() {
        def innerMethod() { // → RECURSE into inner method
            // inner method body
        }
    }
}

// Nested function_definition
def outerFunction() {
    def innerFunction() {   // → RECURSE into inner function
        // inner function body
    }
}
```

### Groovy Closure Patterns

```groovy
// Collection method closures (Combined as single statements)
list.findAll { item ->          // → Combined: method call + closure
    return item.isValid()       // → RECURSE into closure body if complex
}

list.collect { it * 2 }         // → Combined: method call + closure
                                // → Expression body → PURE

list.each { item ->             // → Combined: method call + closure
    println item                // → RECURSE into closure body
    processItem(item)
}

// Standalone closures
def myClosure = { param ->      // → RECURSE into closure body
    return param * 2
}

// Closure as method parameter
processData({ item ->           // → RECURSE into closure body
    return transform(item)
})

// Groovy's implicit closure parameter
list.findAll { it.isActive }   // → Expression body → PURE
```

### Emery DSL Patterns

```groovy
// Field access patterns
F.fieldName                     // → dotted_identifier → PURE
F.skillsMultiRow.rows          // → member_access → PURE
F.skillsMultiRow.rows << newRow // → binary_op → PURE (with left-hand extraction)

// Emery function calls
Emery.form.newForm("USER_PROFILE_FORM")        // → function_call → PURE
Emery.dataTable.read("TABLE_NAME")             // → function_call → PURE
Emery.mdos.getMdosDisplayValuesClob(params)    // → function_call → PURE
Emery.test.assertEquals(expected, actual)      // → function_call → PURE

// Typed declarations (Combined handling)
USER_PROFILE_FORM formObj = Emery.form.newForm("USER_PROFILE_FORM")
// → Combined: identifier + assignment → PURE

// Use statements
use("EMERY_UTILITIES")          // → function_call → PURE
use("EMERY_FORM_OPERATIONS")    // → function_call → PURE
```

### Pure Statements (No Recursion)

```groovy
// expression_statement
println "Hello World"           // PURE - function call
methodCall()                    // PURE - method call
obj.method()                    // PURE - method call

// assignment
x = 5                          // PURE - assignment
obj.field = value              // PURE - field assignment

// return_statement
return value                   // PURE
return                         // PURE

// throw_statement
throw new Exception("error")   // PURE

// break_statement
break                          // PURE
break labelName                // PURE

// continue_statement
continue                       // PURE
continue labelName             // PURE

// assert_statement
assert condition               // PURE - Groovy assertion
assert x > 0, "x must be positive" // PURE

// variable_declaration
def variable = value           // PURE - simple declaration

// empty_statement
;                              // PURE
```

---

## 4. Groovy-Specific Enhancements

### Closure Method Call Combination

```groovy
// PROBLEM: Tree-sitter parses as separate nodes
def filtered = list.findAll { it > 0 }

// Parsed as:
// ├── declaration: "def filtered = list.findAll"
// └── closure: "{ it > 0 }"

// SOLUTION: Detect and combine
def filtered = list.findAll { it > 0 }  // → Single combined statement

// Affected patterns:
list.findAll { condition }
list.collect { transformation }
list.each { action }
list.any { condition }
list.every { condition }
map.collectEntries { keyValue }
```

### Typed Declaration Handling

```groovy
// PROBLEM: Emery typed declarations split
USER_PROFILE_FORM formObj = Emery.form.newForm("USER_PROFILE_FORM")

// Parsed as:
// ├── identifier: "USER_PROFILE_FORM"
// └── assignment: "formObj = Emery.form.newForm(...)"

// SOLUTION: Detect and combine
USER_PROFILE_FORM formObj = Emery.form.newForm("USER_PROFILE_FORM")
// → Single combined declaration

// Affected patterns:
EMPLOYEE_FORM empForm = ...
CUSTOM_TYPE variable = ...
```

### Context-Aware Closure Handling

```groovy
// Structural closures (method bodies) - RECURSE
def method() {
    // This is a structural block, not a functional closure
    if (condition) {
        // RECURSE into nested statements
    }
}

// Functional closures - Context-dependent
list.findAll { item ->
    // Simple condition → PURE
    item.isValid
}

list.each { item ->
    // Complex body → RECURSE
    if (item.isValid) {
        processItem(item)
    }
}
```

### Branch-Aware If Statement Analysis

```groovy
// Standard if_statement
if (condition) {
    // if branch
} else if (condition2) {
    // else-if branch (nested if_statement in else_body)
} else {
    // else branch
}

// Branch extraction and comparison:
// 1. Extract main if: condition + body
// 2. Extract else-if branches: nested if_statements in else_body  
// 3. Extract final else: remaining else_body content
// 4. Compare branches individually like JavaScript POC
```

---

## 5. Emery DSL Pattern Recognition

### Field Access Patterns

```groovy
// Simple field access
F.fieldName                     // → dotted_identifier
// AST: dotted_identifier with parts: ["F", "fieldName"]

// Nested field access  
F.skillsMultiRow.rows          // → member_access
// AST: member_access with object and property chains

// Binary operations with field access
F.skillsMultiRow.rows << newRow // → binary_op
// AST: binary_op with left (field access) and right (value)
// Special handling: extract left-hand side as identifier
```

### Function Call Patterns

```groovy
// Emery namespace function calls
Emery.form.newForm("TYPE")              // → function_call
Emery.dataTable.read("TABLE")           // → function_call  
Emery.mdos.getMdosDisplayValuesClob()   // → function_call
Emery.test.assertEquals(exp, act)       // → function_call

// AST: function_call with dotted function name
// Special handling: extract full dotted name as identifier
```

### Use Statement Patterns

```groovy
// Emery use declarations
use("EMERY_UTILITIES")          // → function_call
use("EMERY_FORM_OPERATIONS")    // → function_call

// AST: function_call with simple function name "use"
```

---

## 6. Detection Functions

```python
def is_pure_statement(node_type: str) -> bool:
    """Check if node is a pure statement (leaf node) in Groovy."""
    return node_type in {
        "expression_statement",
        "return_statement", 
        "throw_statement",
        "break_statement",
        "continue_statement",
        "assert_statement",
        "import_statement",
        "package_statement",
        "variable_declaration",
        "field_declaration",
        "empty_statement",
    }


def is_container_node(node_type: str) -> bool:
    """Check if node contains other statements in Groovy."""
    return node_type in {
        # Classes/Interfaces
        "class_definition",
        "interface_definition",
        "trait_definition",
        "enum_definition",
        "annotation_definition",
        
        # Methods/Functions
        "method_definition",
        "function_definition",
        "constructor_definition",
        
        # Control flow
        "if_statement",
        "switch_statement",
        "switch_block",
        "case",
        "try_statement",
        "catch_clause",
        "finally_clause",
        
        # Loops
        "for_loop",
        "for_in_loop", 
        "while_loop",
        "do_while_statement",
        
        # Blocks
        "statement_block",
        "block",
        "labeled_statement",
        "synchronized_statement",
        
        # Groovy-specific (context-aware)
        "closure",           # Check context
        "closure_expression", # Check context
    }


def needs_closure_check(node_type: str) -> bool:
    """Check if we need to examine for closures in Groovy."""
    return node_type in {
        "declaration",
        "assignment",
        "field_definition",
        "property_definition",
        "function_call",      # May have closure arguments
        "method_call",        # May have closure arguments
        "expression_statement", # May contain closure calls
    }


def is_emery_dsl_node(node_type: str) -> bool:
    """Check if node is an Emery DSL construct."""
    return node_type in {
        "binary_op",         # F.rows << newRow
        "dotted_identifier", # F.fieldName
        "member_access",     # F.skillsMultiRow.rows
        "function_call",     # Emery.form.newForm()
        "juxt_function_call", # Groovy-style calls
    }


def should_combine_with_closure(node, next_node) -> bool:
    """Check if method call should be combined with following closure."""
    if not next_node or next_node.type != "closure":
        return False
        
    # Check if current node is a method call that could take a closure
    if node.type in {"declaration", "expression_statement"}:
        # Look for method calls like list.findAll, list.collect, etc.
        code = get_node_text(node)
        return any(method in code for method in [
            "findAll", "collect", "each", "any", "every",
            "collectEntries", "findResults", "groupBy"
        ])
    
    return False


def should_combine_typed_declaration(node, next_node) -> bool:
    """Check if identifier should be combined with following assignment."""
    return (node.type == "identifier" and 
            next_node and next_node.type == "assignment")


def get_children_to_parse(node) -> list:
    """Get the child nodes that should be parsed recursively in Groovy."""
    children = []
    node_type = node.type
    
    if node_type in ("method_definition", "function_definition", "constructor_definition"):
        body = node.child_by_field_name("body")
        if body:
            if body.type in ("statement_block", "block"):
                children.extend(body.named_children)
            else:
                children.append(body)
    
    elif node_type in ("class_definition", "interface_definition", "trait_definition", 
                       "enum_definition", "annotation_definition"):
        body = node.child_by_field_name("body")
        if body:
            children.extend(body.named_children)
    
    elif node_type == "if_statement":
        # Handle branch-aware analysis
        body = node.child_by_field_name("body")
        else_body = node.child_by_field_name("else_body")
        if body:
            children.extend(get_block_statements(body))
        if else_body:
            children.extend(get_block_statements(else_body))
    
    elif node_type == "switch_statement":
        body = node.child_by_field_name("body")
        if body:
            children.extend(body.named_children)  # switch_block contents
    
    elif node_type == "switch_block":
        # All case and default statements
        children.extend(node.named_children)
    
    elif node_type == "case":
        # All statements after the case label
        for child in node.named_children:
            if child.type not in ("case_label", ":"):
                children.append(child)
    
    elif node_type == "try_statement":
        body = node.child_by_field_name("body")
        handler = node.child_by_field_name("handler")
        finalizer = node.child_by_field_name("finalizer")
        if body:
            children.append(body)
        if handler:
            children.append(handler)
        if finalizer:
            children.append(finalizer)
    
    elif node_type in ("catch_clause", "finally_clause"):
        body = node.child_by_field_name("body")
        if body:
            children.append(body)
    
    elif node_type in ("for_loop", "for_in_loop", "while_loop", "do_while_statement"):
        body = node.child_by_field_name("body")
        if body:
            children.extend(get_block_statements(body))
    
    elif node_type in ("statement_block", "block"):
        children.extend(node.named_children)
    
    elif node_type == "closure":
        # Context-aware closure handling
        if is_structural_closure(node):
            # Treat as container - recurse into statements
            for child in node.named_children:
                children.append(child)
        # else: treat as pure statement (no children)
    
    elif node_type == "closure_expression":
        # Usually functional, but check for complexity
        if has_complex_statements(node):
            children.extend(node.named_children)
    
    return children


def get_block_statements(block_node) -> list:
    """Extract individual statements from a block, handling Groovy-specific cases."""
    if block_node.type in ("statement_block", "block"):
        return list(block_node.named_children)
    else:
        # Single statement (not in block)
        return [block_node]


def is_structural_closure(closure_node) -> bool:
    """Determine if closure is structural (method body) vs functional (callback)."""
    parent = closure_node.parent
    if not parent:
        return False
    
    # Check parent context
    parent_type = parent.type
    
    # Structural contexts (method/class bodies)
    if parent_type in ("method_definition", "function_definition", "constructor_definition",
                       "class_definition", "if_statement", "for_loop", "while_loop"):
        return True
    
    # Functional contexts (method arguments, assignments)
    if parent_type in ("function_call", "method_call", "assignment", "declaration"):
        return False
    
    # Default to functional for ambiguous cases
    return False


def has_complex_statements(node) -> bool:
    """Check if node contains complex statements requiring recursion."""
    for child in node.named_children:
        if child.type in GROOVY_RECURSIVE_CONTAINERS:
            return True
        if child.type in ("if_statement", "for_loop", "while_loop", "try_statement"):
            return True
    return False
```

---

## 7. Groovy vs Java Syntax Differences

### Loop Syntax Corrections

```groovy
// Java-style enhanced for (NOT supported by tree-sitter-groovy)
for (def item : collection) { }     // ❌ Syntax Error

// Groovy for-in loop (Correct)
for (item in collection) { }        // ✅ Supported

// Groovy range-based for
for (i in 0..10) { }               // ✅ Groovy-specific

// Groovy collection iteration
for (item in [1, 2, 3]) { }        // ✅ Groovy-specific
```

### Closure vs Lambda Differences

```groovy
// Groovy closures
def closure = { param -> param * 2 }    // Groovy closure syntax
list.findAll { it > 0 }                 // Implicit parameter 'it'

// Java lambdas (for comparison - not in Groovy AST)
// list.stream().filter(item -> item > 0)  // Java 8+ lambda
```

### Method Call Syntax

```groovy
// Groovy method calls (multiple forms)
println "Hello"                    // juxt_function_call (no parentheses)
println("Hello")                   // function_call (with parentheses)
obj.method "param"                 // method_call (no parentheses)
obj.method("param")                // method_call (with parentheses)
```

---

## Implementation Summary

### Current Groovy Implementation Details

**Core Classification Sets:**
- `GROOVY_RECURSIVE_CONTAINERS`: 25 container types that require recursion
- `GROOVY_LEAF_STATEMENTS`: 12 leaf types that stop recursion
- `GROOVY_NODE_TYPE_TO_BLOCK_TYPE`: Maps tree-sitter nodes to BlockType enums

**Recursion Control:**
1. **Depth limiting**: Maximum recursion depth of 10 levels (configurable)
2. **Context-aware closures**: `_get_closure_context()` distinguishes structural vs functional
3. **Leaf node detection**: `case` statements are leaf nodes to prevent infinite recursion
4. **Special handling**: Switch blocks are containers but cases within are pure

**Key Implementation Methods:**
- `_get_parseable_children()`: Extracts children for each container type
- `_get_closure_context()`: Determines closure context (CLASS_BODY, REAL_CLOSURE, etc.)
- `_get_block_statements()`: Handles block vs single statement bodies
- `child_by_field_name()`: Accesses specific fields (body, else_body, etc.)

### Actual vs Documented Behavior

**✅ Correctly Implemented:**
- Context-aware closure handling (structural blocks parsed, functional closures treated as pure)
- Multi-phase matching strategy with similarity thresholds
- Hierarchical diff structure with nested containers
- Depth limiting and recursion protection

### Node Type Mapping Examples

```python
# From GROOVY_NODE_TYPE_TO_BLOCK_TYPE
"juxt_function_call": BlockType.FUNCTION_CALL,  # println "text"
"function_call": BlockType.EXPRESSION,          # Math.max(5, 10)
"binary_op": BlockType.EXPRESSION,              # F.rows << newRow
"declaration": BlockType.DECLARATION,           # def x = value
"case": # In GROOVY_LEAF_STATEMENTS (not mapped to BlockType)
```

### Context Detection Logic

```python
def _get_closure_context(self, closure_node):
    parent_type = closure_node.parent.type
    if parent_type == "class_definition":
        return "CLASS_BODY"  # Structural - recurse
    elif parent_type in ["function_call", "juxt_function_call"]:
        return "REAL_CLOSURE"  # Functional - pure statement
    # ... other contexts
```

This implementation provides comprehensive Groovy AST parsing while maintaining performance through careful recursion control and context-aware handling of language-specific constructs.