# Groovy Code Blocks Reference

## Complete Code Block Types for Recursive Parsing

This document provides a comprehensive reference of all Groovy code blocks that can appear within classes, methods, and scripts and need to be parsed recursively. This includes standard Groovy constructs as well as Emery DSL patterns.

---

## 1. Node Type Classification

### 🔵 CONTAINER NODES (Require Recursion)

These nodes contain other statements and must be parsed recursively:

| Node Type | Example | Children to Parse |
|-----------|---------|-------------------|
| `class_definition` | `class Foo {}` | `body` (class members) |
| `interface_definition` | `interface Bar {}` | `body` (interface members) |
| `trait_definition` | `trait Mixable {}` | `body` (trait members) |
| `enum_definition` | `enum Status {}` | `body` (enum constants/methods) |
| `annotation_definition` | `@interface MyAnnotation {}` | `body` |
| `method_definition` | `def methodName() {}` | `body` (statement_block) |
| `function_definition` | `def functionName() {}` | `body` |
| `constructor_definition` | `Constructor() {}` | `body` |
| `if_statement` | `if (cond) {} else {}` | `body`, `else_body` |
| `switch_statement` | `switch (x) {}` | `body` (switch_block) |
| `switch_block` | `switch body container` | `case`, `default` statements |
| `case` | `case 1:` | statements after colon |
| `for_loop` | `for (i in 0..10) {}` | `body` |
| `for_in_loop` | `for (item in collection) {}` | `body` |
| `while_loop` | `while (condition) {}` | `body` |
| `do_while_statement` | `do {} while()` | `body` |
| `try_statement` | `try {} catch {}` | `body`, `catch_clause`, `finally_clause` |
| `catch_clause` | `catch (Exception e) {}` | `body` |
| `finally_clause` | `finally {}` | `body` |
| `statement_block` | `{ ... }` | all child statements |
| `block` | `{ ... }` | all child statements |
| `closure` | `{ it > 0 }` | body (context-aware) |
| `closure_expression` | `list.findAll { condition }` | closure body |
| `synchronized_statement` | `synchronized (obj) {}` | `body` |
| `labeled_statement` | `label: stmt` | `body` |

### 🟢 PURE STATEMENTS (Leaf Nodes - Stop Recursion)

These are atomic statements that don't contain nested blocks:

| Node Type | Example | Notes |
|-----------|---------|-------|
| `expression_statement` | `println "Hello"` | Method calls, assignments |
| `return_statement` | `return value` | May have expression |
| `throw_statement` | `throw new Exception()` | Has expression |
| `break_statement` | `break` | Optional label |
| `continue_statement` | `continue` | Optional label |
| `assert_statement` | `assert condition` | Groovy assertion |
| `import_statement` | `import java.util.List` | Module import |
| `package_statement` | `package com.example` | Package declaration |
| `variable_declaration` | `def x = value` | Simple variable |
| `field_declaration` | `private String field` | Class field |
| `empty_statement` | `;` | Just semicolon |

### 🟡 DECLARATION NODES (May Contain Functions/Closures)

These need special handling - check if value contains function or closure:

| Node Type | Example | Check For |
|-----------|---------|-----------|
| `declaration` | `def x = { closure }` | Closure/function in value |
| `assignment` | `x = { it > 0 }` | Closure/function in value |
| `field_definition` | `def field = { closure }` | Closure as field value |
| `property_definition` | `String prop = getValue()` | Method call or closure |

### 🔮 EMERY DSL NODES (Special Handling)

Emery-specific constructs that require enhanced parsing:

| Node Type | Example | Special Handling |
|-----------|---------|------------------|
| `binary_op` | `F.rows << newRow` | Left-hand identifier extraction |
| `function_call` | `Emery.form.newForm()` | Dotted function name extraction |
| `dotted_identifier` | `F.fieldName` | Emery field access |
| `member_access` | `F.skillsMultiRow.rows` | Nested field access |
| `juxt_function_call` | `println value` | Groovy-style function call |

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
├── GROOVY CLOSURES (Context-Aware Handling)
│   ├── closure
│   │   └── [Check context: structural block vs functional closure]
│   │       ├── Structural (method body) → RECURSE
│   │       └── Functional (callback)   → PURE or RECURSE based on complexity
│   │
│   ├── closure_expression
│   │   └── [Usually functional - check for nested statements]
│   │
│   └── Collection Methods with Closures
│       ├── list.findAll { condition }   ← Combined as single statement
│       ├── list.collect { transform }   ← Combined as single statement
│       ├── list.each { action }         ← Combined as single statement
│       └── map.collectEntries { }       ← Combined as single statement
│
├── EMERY DSL CONSTRUCTS (Special Parsing)
│   ├── Form Operations
│   │   ├── F.fieldName              ← dotted_identifier
│   │   ├── F.skillsMultiRow.rows    ← member_access
│   │   └── F.rows << newRow         ← binary_op (left-hand extraction)
│   │
│   ├── Emery Function Calls
│   │   ├── Emery.form.newForm()     ← function_call (dotted)
│   │   ├── Emery.dataTable.read()   ← function_call (dotted)
│   │   ├── Emery.mdos.getMdos()     ← function_call (dotted)
│   │   └── Emery.test.assertEquals() ← function_call (dotted)
│   │
│   ├── Typed Declarations
│   │   ├── USER_PROFILE_FORM obj = ... ← Combined identifier + assignment
│   │   └── EMPLOYEE_FORM emp = ...     ← Combined identifier + assignment
│   │
│   └── Use Statements
│       └── use("EMERY_UTILITIES")   ← function_call
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

## Summary

When implementing recursive parsing for Groovy:

1. **Start at program root** and extract top-level declarations, classes, methods
2. **Check each node type** against Groovy container/pure classification
3. **For containers**, get child nodes and recurse appropriately
4. **For declarations**, check if value contains closures or functions
5. **For pure statements**, compute hash and stop recursion
6. **Handle Groovy closures** with context-aware analysis (structural vs functional)
7. **Combine method calls with closures** for semantic accuracy
8. **Handle Emery DSL constructs** with specialized identifier extraction
9. **Use branch-aware if analysis** for detailed control flow comparison
10. **Track depth and path** for context in hierarchical diffs
11. **Handle typed declarations** by combining type identifiers with assignments
12. **Apply Groovy-specific node type corrections** (for_loop vs for_statement)

### Key Groovy-Specific Features:

- **Context-aware closure handling** (structural blocks vs functional closures)
- **Collection method combination** (method + closure as single statement)
- **Emery DSL pattern recognition** (F.fieldName, binary operations, typed declarations)
- **Branch-aware if statement analysis** (individual if/else-if/else comparison)
- **Enhanced identifier extraction** (dotted identifiers, member access, function calls)
- **Groovy syntax adaptations** (for-in loops, juxt function calls, implicit parameters)