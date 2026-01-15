# 🚀 Groovy AST Comparison Service - Covered Scenarios

## 📋 What Does This Tool Do?

This tool compares two versions of a Groovy file and tells you **exactly what changed** between them. It's like a smart "track changes" feature that understands Groovy code structure, including closures, DSL patterns, and Emery constructs.

---

## 🔧 Version Overview

This document describes all scenarios covered by the Groovy AST Comparison service in this codebase. The system provides comprehensive comparison capabilities for Groovy code including:

1. **🏗️ Block-Level Comparison** (`GroovyASTDiff`) - Top-level structure comparison (classes, methods, closures)
2. **🔄 Recursive Comparison** (`GroovyRecursiveParser`) - Deep nested structure comparison with statement-level analysis
3. **🎯 Emery DSL Support** - Specialized handling for Emery language constructs
4. **🎪 Multi-Phase Matching Strategy** - Sophisticated matching with 4 distinct phases
5. **🌳 Hierarchical Diff Structure** - Nested diffs showing container relationships

---

## 📚 Table of Contents

1. [🏷️ Change Types](#change-types)
2. [🧱 Block Types](#block-types)
3. [🏗️ Block-Level Scenarios](#block-level-scenarios)
4. [📝 Statement-Level Scenarios](#statement-level-scenarios)
5. [🪆 Nested Structure Scenarios](#nested-structure-scenarios)
6. [🔀 Control Flow Scenarios](#control-flow-scenarios)
7. [🔒 Closure & Collection Method Detection](#closure--collection-method-detection)
8. [🎨 Groovy-Specific Patterns](#groovy-specific-patterns)
9. [🎯 Emery DSL Scenarios](#emery-dsl-scenarios)
10. [📊 Response Structure](#response-structure)

---

## 🏷️ Types of Changes We Detect

### Block-Level Change Types (`ChangeType`)

| 🏷️ Change Type | 📝 What It Means | 🎯 Example |
|---------------|------------------|------------|
| ✅ **UNCHANGED** | Code is exactly the same | No changes made |
| ➕ **ADDED** | Block exists only in new file | New method added |
| ➖ **DELETED** | Block exists only in old file | Method removed |
| ✏️ **MODIFIED** | Same identifier, different content | Method body changed |
| ↔️ **MOVED** | Same content, different position | Method reordered in class |
| ↔️✏️ **MOVED_MODIFIED** | Different position AND content changed | Method moved and edited |

### Statement-Level Change Types (`StatementChangeType`)

Same types as block-level, but applied to individual statements within blocks.

---

## 🧱 Block Types

The system recognizes the following Groovy block types:

### 🏛️ Class and Interface Types
| Type | Example | Description |
|------|---------|-------------|
| 🏛️ `CLASS` | `class Foo {}` | Class definitions |
| 🔌 `INTERFACE` | `interface Foo {}` | Interface definitions |
| 🧬 `TRAIT` | `trait Foo {}` | Groovy traits |
| 📋 `ENUM` | `enum Status {}` | Enumeration types |
| 🏷️ `ANNOTATION` | `@interface Foo {}` | Annotation definitions |

### ⚙️ Method Types
| Type | Example | Description |
|------|---------|-------------|
| ⚙️ `FUNCTION` | `def foo() {}` | Function definitions |
| 🔧 `METHOD` | Class method definition | Instance methods |
| 🏗️ `CONSTRUCTOR` | Class constructor | Object constructors |
| 🔒 `STATIC_METHOD` | Static method | Class-level methods |
| 🔐 `PRIVATE_METHOD` | Private method | Private access methods |

### 📦 Variable and Property Types
| Type | Example | Description |
|------|---------|-------------|
| 📦 `DECLARATION` | `def x = value` | Variable declarations |
| 🏠 `FIELD` | Class field declaration | Class fields |
| 🎛️ `PROPERTY` | Groovy property | Properties with getter/setter |

### 🔀 Control Flow Types
| Type | Example | Description |
|------|---------|-------------|
| 📝 `STATEMENT` | Generic statement | if, for, while, switch, try |
| 🎯 `EXPRESSION` | Top-level expressions | Expression statements |
| ⚡ `BINARY_OPERATION` | `F.rows << newRow` | Binary operations |

### 🎨 Groovy-Specific Types
| Type | Example | Description |
|------|---------|-------------|
| 🔒 `CLOSURE` | `{ param -> body }` | Closure definitions |
| 📜 `SCRIPT` | Top-level script constructs | Script-level code |
| 📞 `FUNCTION_CALL` | Method calls | Including Emery DSL calls |

---

## 🪆 Groovy Container Types

The system recognizes these container types that require recursive analysis:

### ⚙️ Method Containers
| Container | Description | Purpose |
|-----------|-------------|---------|
| 🏛️ `class_definition` | Class body with methods | Contains class members |
| 🔌 `interface_definition` | Interface with method signatures | Contains method signatures |
| 🧬 `trait_definition` | Trait with default implementations | Contains trait methods |
| ⚙️ `method_definition` | Method body | Contains method statements |
| 🔧 `function_definition` | Function body | Contains function statements |
| 🏗️ `constructor_definition` | Constructor body | Contains constructor logic |

### 🔀 Control Flow Containers
| Container | Description | Analysis Type |
|-----------|-------------|---------------|
| 🔀 `if_statement` | If/else branches | Branch-aware analysis |
| 🎛️ `switch_statement` | Switch cases | Case-by-case comparison |
| 📦 `switch_block` | Switch body container | Contains switch cases |
| 🏷️ `case` | Individual switch cases | Case-specific logic |
| 🔄 `for_loop` | For-in loops | Loop body analysis |
| 🔁 `for_in_loop` | Enhanced for loops | Enhanced iteration |
| ⏳ `while_loop` | While loops | Condition-based loops |
| 🔂 `do_while_statement` | Do-while loops | Post-condition loops |

### ⚠️ Exception Handling Containers
| Container | Description | Purpose |
|-----------|-------------|---------|
| 🛡️ `try_statement` | Try-catch-finally blocks | Exception handling |
| 🚨 `catch_clause` | Catch blocks | Error handling |
| 🧹 `finally_clause` | Finally blocks | Cleanup logic |

### 🎨 Groovy-Specific Containers
| Container | Description | Context |
|-----------|-------------|---------|
| 🔒 `closure` | Closure bodies | Context-aware analysis |
| 🎯 `closure_expression` | Closure expressions | Expression closures |
| 📦 `statement_block` | Generic statement blocks | General containers |

---

## 🏗️ Block-Level Scenarios

### 1️⃣ Named Block Matching

We detect when functions are added, removed, or changed by matching their names.

**Before:**
```groovy
def add(a, b) {
    return a + b
}
```

**After:**
```groovy
def add(a, b, c) {
    return a + b + c
}
```

✅ **Detected as:** `MODIFIED` - Function `add` was changed (new parameter added, logic updated)

---

### 2️⃣ Content-Based Block Matching

We detect when code blocks are matched by content similarity when names aren't available.

**Before:**
```groovy
println add(5, 3)
```

**After:**
```groovy
println add(5, 3, 6)
```

✅ **Detected as:** `MODIFIED` - Expression statement content changed

---

### 3️⃣ Multi-Phase Matching Strategy

We use a sophisticated 4-phase matching strategy for optimal accuracy.

**🎪 Matching Phases:**
1. **🎯 Phase 1:** Match by identifier (exact name match for same type)
2. **🔄 Phase 2:** Match by content hash (exact content match → MOVED)
3. **📊 Phase 3:** Match by structural similarity (≥70% similarity → MODIFIED/MOVED_MODIFIED)
4. **🏁 Phase 4:** Remaining unmatched → ADDED/DELETED

**Implementation Details:**
- Uses `_compare_blocks()` method in `GroovyASTDiff` class
- Recursive matching in `GroovyRecursiveParser._compare_container_children()`
- Similarity threshold of 0.7 (70%) for modified block detection

---

### 4️⃣ Structural Similarity Scoring

We use advanced similarity calculation for Groovy AST structures.

```groovy
// 📊 Similarity calculation based on:
// - 🎯 Identifier matching
// - 🔍 Content hash comparison  
// - 🌳 Structural tree similarity
// - 📍 Line position analysis
```

---

### 5️⃣ Typed Declaration Handling

We properly handle Groovy typed declarations including Emery DSL types.

**Before:**
```groovy
USER_PROFILE_FORM formObj = Emery.form.newForm("USER_PROFILE_FORM")
```

**After:**
```groovy
USER_PROFILE_FORM formObj = Emery.form.newForm("EMPLOYEE_FORM")
```

✅ **Detected as:** `MODIFIED` - Typed declaration with different parameter value

---

## 📝 Statement-Level Scenarios

### 6️⃣ Statement Movement Detection

We detect when statements are reordered within a method.

**Before:**
```groovy
def process() {
    def a = 1
    def b = 2
    return a + b
}
```

**After:**
```groovy
def process() {
    def b = 2  // ← Moved up
    def a = 1  // ← Moved down
    return a + b
}
```

✅ **Detected as:** `MOVED` - Statements reordered but content unchanged

---

### 7️⃣ Statement Modification Detection

We detect when a statement's content changes while position remains similar.

**Before:**
```groovy
count = count + 2
```

**After:**
```groovy
count += 2
```

✅ **Detected as:** `MODIFIED` - Statement content changed (equivalent operation)

---

### 8️⃣ Mixed Statement Changes

We handle combinations of added, deleted, modified, and moved statements.

**Before:**
```groovy
def process(items) {
    println "start"
    for (i in 0..<items.size()) {
        processItem(items[i])
    }
    println "end"
}
```

**After:**
```groovy
def process(items) {
    println "processing"  // ← MODIFIED
    items.each { item ->  // ← ADDED (old for loop DELETED)
        processItem(item)
    }
    println "end"         // ← UNCHANGED
}
```

✅ **Detected as:** Mixed changes with:
- `println "start"` → `MODIFIED`
- `for` loop → `DELETED`
- `items.each` → `ADDED`
- `println "end"` → `UNCHANGED`

---

## 🪆 Nested Structure Scenarios

### 9️⃣ Nested Class Detection

We detect and compare nested class declarations.

```groovy
class Outer {
    class Inner {  // ← Nested class detected
        def innerMethod() {
            return true
        }
    }
    
    static class StaticNested {  // ← Static nested class
        def staticMethod() {
            return "static"
        }
    }
}
```

✅ **Detected as:** 
- Class `Outer` → Container with nested classes
- Class `Inner` → Nested class with methods
- Class `StaticNested` → Static nested class

---

### 🔟 Closure Detection and Context Analysis

We use context-aware closure handling to distinguish structural vs functional closures.

```groovy
// 🔧 Functional closures (treated as pure statements)
list.findAll { item -> item > 2 }
list.collect { item -> item * 2 }
list.each { item -> println item }

// 🏗️ Structural closures (treated as containers for recursion)
class MyClass {
    // This closure is a structural block, not a functional closure
    def method() {
        // Method body statements
    }
}
```

**Implementation Details:**
- Uses `_get_closure_context()` to determine closure type
- Structural closures (CLASS_BODY, FUNCTION_BODY, etc.) are parsed recursively
- Functional closures (REAL_CLOSURE) are treated as pure statements
- Context detection prevents infinite recursion in structural blocks

---

### 1️⃣1️⃣ Method-in-Method Detection

We detect and compare nested method declarations.

```groovy
def outerMethod() {
    def innerMethod() {  // ← Nested method detected
        return "inner"
    }
    
    def anotherInner = { param ->  // ← Closure assignment
        return param.toUpperCase()
    }
    
    return innerMethod() + anotherInner("test")
}
```

✅ **Detected as:**
- Method `outerMethod` → Container with nested methods
- Method `innerMethod` → Nested method definition
- Closure `anotherInner` → Closure assignment

---

### 1️⃣2️⃣ Class Method Detection

We compare class methods individually with proper inheritance handling.

```groovy
class Calculator {
    def add(a, b) {
        return a + b
    }
    
    def subtract(a, b) {
        return a - b
    }
    
    static def multiply(a, b) {
        return a * b
    }
    
    private def validate(value) {
        return value != null
    }
}
```

✅ **Detected as:**
- Class `Calculator` → Container with multiple methods
- Method `add` → Instance method
- Method `subtract` → Instance method  
- Method `multiply` → Static method
- Method `validate` → Private method

---

## 🔀 Control Flow Scenarios

### 1️⃣3️⃣ If-Else-If Chain Comparison

We look **inside** if-else conditions to show exactly which branch changed.

**Before:**
```groovy
if (x > 10) {
    println "large"
} else if (x > 5) {
    println "medium"
} else {
    println "small"
}
```

**After:**
```groovy
if (x > 5) {           // ← Different condition
    println "medium"
} else if (x > 10) {   // ← Different condition
    println "very large"  // ← MODIFIED body
} else {
    println "small"       // ← UNCHANGED
}
```

✅ **Detected as:**
- `if` branch → `MODIFIED` (condition and body changed)
- `else if` branch → `MODIFIED` (condition and body changed)
- `else` branch → `UNCHANGED`

**Implementation Details:**
- If statements are treated as containers in `GROOVY_RECURSIVE_CONTAINERS`
- Uses `_get_parseable_children()` to extract if/else bodies
- Recursive comparison via `GroovyRecursiveParser.compare_recursive_statements()`

---

### 1️⃣4️⃣ Switch Statement Comparison

We compare each case individually with Groovy-specific patterns.

```groovy
switch (value) {
    case String:           // ← Type matching
        handleString(value)
        break
    case { it > 10 }:      // ← Closure condition
        handleLarge(value)
        break
    case 1..5:             // ← Range matching
        handleSmall(value)
        break
    case [1, 2, 3]:        // ← List matching
        handleList(value)
        break
    default:               // ← Default case
        handleDefault(value)
}
```

✅ **Detected as:**
- `case String` → Type-based matching
- `case { it > 10 }` → Closure condition
- `case 1..5` → Range matching
- `case [1, 2, 3]` → List matching
- `default` → Default case

---

### 1️⃣5️⃣ Try-Catch-Finally Comparison

We compare each part of error handling separately.

**Before:**
```groovy
try {
    riskyOperation()
} catch (IOException e) {
    handleIOError(e)
}
```

**After:**
```groovy
try {
    riskyOperation()
    validateData()          // ← New line added
} catch (IOException e) {
    reportError(e)          // ← Changed
} finally {
    cleanup()               // ← New section added
}
```

✅ **Detected as:**
- `try` block → `MODIFIED`
- `catch` block → `MODIFIED`
- `finally` block → `ADDED`

---

### 1️⃣6️⃣ Loop Body Comparison

We detect changes inside for loops, while loops, and other repetitive structures.

**Before:**
```groovy
for (item in collection) {
    processItem(item)
}
```

**After:**
```groovy
for (item in collection) {
    processItem(item)
    logProcessing(item)     // ← New line added
}
```

✅ **Detected as:** Loop body `MODIFIED` with:
- `processItem(item)` → `UNCHANGED`
- `logProcessing(item)` → `ADDED`

**🔄 Supported Loop Types:**
- **For-in loop:** `for (item in collection)`
- **Range-based:** `for (i in 0..10)`
- **While loop:** `while (condition)`
- **Enhanced for:** `for (String item in stringCollection)`

---

## 🔒 Closure & Collection Method Detection

### 🎯 Recognized Collection Methods

The system recognizes closures in these Groovy collection methods:

| 🏷️ Category | 📋 Methods | 🎯 Purpose |
|-------------|------------|------------|
| 🔍 **Filtering** | `findAll`, `find`, `findIndexOf`, `grep` | Find matching elements |
| 🔄 **Transformation** | `collect`, `collectEntries`, `collectMany`, `flatten` | Transform data |
| 🔁 **Iteration** | `each`, `eachWithIndex`, `reverseEach` | Process each element |
| 📊 **Aggregation** | `inject`, `reduce`, `sum`, `min`, `max` | Combine values |
| ✅ **Testing** | `any`, `every`, `contains` | Test conditions |
| 📈 **Sorting** | `sort`, `sortBy` | Order elements |
| 📦 **Grouping** | `groupBy`, `countBy` | Group by criteria |
| 🎯 **Unique** | `unique`, `uniqueBy` | Remove duplicates |

### 🎯 Collection Method Handling

We detect changes in collection methods and their closures.

**Before:**
```groovy
def filtered = list.findAll { item ->
    return item.isActive
}
```

**After:**
```groovy
def filtered = list.findAll { item ->
    return item.isActive && item.value > 10  // ← New condition added
}
```

✅ **Detected as:** Collection method `MODIFIED` with closure body changed

**🔗 Method Chaining:**
```groovy
def result = list
    .findAll { it.active }      // ← Each line is a separate expression
    .collect { it.transform() }
    .groupBy { it.category }
```

**Implementation Details:**
- Collection methods are mapped to `BlockType.EXPRESSION` in `GROOVY_NODE_TYPE_TO_BLOCK_TYPE`
- Closures in functional contexts are treated as pure statements to avoid deep recursion
- No special "combination" logic - each statement is analyzed independently

### 🧠 Closure Context Analysis

We use context-aware closure handling to distinguish structural vs functional closures.

```groovy
// 🏗️ Structural closure (control flow) - Treated as container
if (condition) {
    // Block body - not a functional closure
}

// 🔧 Functional closure (collection method) - Combined with method call
list.each { item ->
    // Functional closure body → RECURSE
}
```

---

## 🎨 Groovy-Specific Patterns

### 1️⃣7️⃣ Groovy Truth Evaluation

We handle Groovy's truth evaluation in conditions.

```groovy
// 🎯 Groovy truth patterns
if (list) {          // ← true if list is not null and not empty
    processItems()
}

if (string) {        // ← true if string is not null and not empty
    processString()
}

if (map) {           // ← true if map is not null and not empty
    processMap()
}
```

✅ **Detected as:** Groovy truth conditions properly parsed and compared

---

### 1️⃣8️⃣ Safe Navigation Operator

We handle safe navigation (?.) and elvis operator (?:).

```groovy
// 🛡️ Safe navigation
def result = obj?.method?.()
def property = obj?.nested?.property

// 👑 Elvis operator
def value = maybeNull ?: defaultValue
def result = obj?.method?.() ?: fallback?.() ?: defaultValue
```

✅ **Detected as:** Safe navigation and elvis operators properly parsed

---

### 1️⃣9️⃣ Groovy String Interpolation

We handle GString expressions and interpolation.

```groovy
// 💬 String interpolation
def name = "World"
def greeting = "Hello, ${name}!"
def complex = "Result: ${calculate(a, b)}"

// 📄 Multiline strings
def multiline = """
    Line 1: ${value1}
    Line 2: ${value2}
"""
```

✅ **Detected as:** GString expressions with embedded code properly parsed

---

### 2️⃣0️⃣ Range Operations

We handle Groovy range expressions.

```groovy
// 📏 Range creation and usage
def range1 = 0..10        // ← Inclusive range
def range2 = 1..<10       // ← Exclusive range
def charRange = 'a'..'z'  // ← Character range

// 🔄 Range in loops
for (i in 0..items.size()-1) {
    processItem(items[i])
}
```

✅ **Detected as:** Range expressions and range-based loops properly handled

---

### 2️⃣1️⃣ Meta-Programming Patterns

We handle dynamic method calls and property access.

```groovy
// 🎭 Dynamic method calls
obj."${methodName}"(params)
obj.invokeMethod(methodName, params)

// 🔧 Dynamic property access
obj."${propertyName}" = value
def value = obj."${propertyName}"

// 🎪 Method missing
def methodMissing(String name, args) {
    return "Called ${name} with ${args}"
}
```

✅ **Detected as:** Meta-programming patterns with dynamic access properly parsed

---

## 🎯 Emery DSL Scenarios

### 2️⃣2️⃣ Form Field Operations

We handle Emery form field access patterns as expressions.

**Before:**
```groovy
F.userName = "john.doe"
F.userAge = 25
```

**After:**
```groovy
F.userName = "jane.smith"  // ← Changed value
F.userAge = 30             // ← Changed value
F.department = "IT"        // ← New field added
```

✅ **Detected as:**
- `F.userName` → `MODIFIED` (value changed)
- `F.userAge` → `MODIFIED` (value changed)
- `F.department` → `ADDED` (new field)

**🔗 Nested Field Access:**
```groovy
F.skillsMultiRow.rows.each { row ->
    processSkillRow(row)  // ← Closure treated as pure statement
}

// ⚡ Binary operations (mapped to expressions)
F.skillsMultiRow.rows << newRow1
F.skillsMultiRow.rows << newRow2
```

**Implementation Details:**
- `F.fieldName` patterns are handled as regular assignment expressions
- Binary operations (`<<`) are mapped to `BlockType.EXPRESSION`
- No special "combination" logic for Emery constructs
- Uses standard expression handling from `GROOVY_NODE_TYPE_TO_BLOCK_TYPE`

---

### 2️⃣3️⃣ Emery Data Operations

We handle Emery data table and MDOS operations as declarations and expressions.

**Before:**
```groovy
def countryData = Emery.dataTable.read("COUNTRY_DATA_TABLE")
def statusData = Emery.dataTable.read("STATUS_LOOKUP_TABLE")
```

**After:**
```groovy
def countryData = Emery.dataTable.read("UPDATED_COUNTRY_TABLE")  // ← Changed table name
def statusData = Emery.dataTable.read("STATUS_LOOKUP_TABLE")     // ← Unchanged
def regionData = Emery.dataTable.read("REGION_DATA_TABLE")       // ← New data source
```

✅ **Detected as:**
- `countryData` → `MODIFIED` (different table name)
- `statusData` → `UNCHANGED`
- `regionData` → `ADDED` (new data source)

**🔄 Data Processing:**
```groovy
def activeCountries = countryData.findAll { row ->
    return row.isActive == true  // ← Closure treated as pure statement
}

// 📊 MDOS operations (treated as variable declarations)
def mdosData = Emery.mdos.getMdosDisplayValuesClob(params)
```

**Implementation Details:**
- `Emery.dataTable.read()` calls are handled as regular method calls in declarations
- Variable declarations with `def` are mapped to `BlockType.DECLARATION`
- No special handling for Emery namespace - treated as standard Groovy syntax

---

### 2️⃣4️⃣ Emery Test Operations

We handle Emery test framework constructs as function calls and expressions.

```groovy
// 🧪 Test assertions (treated as function calls)
Emery.test.assertEquals("SUCCESS", result.status)
Emery.test.assertTrue(userCount > 0)
Emery.test.assertNotNull(response.data)

// ⏰ Test delays and timing
Emery.test.delay(1000)
Emery.test.waitFor { condition ->
    return service.isReady()  // ← Closure treated as pure statement
}
```

✅ **Detected as:** Test framework calls handled as regular method calls

**Implementation Details:**
- `Emery.test.*` calls are mapped to `BlockType.FUNCTION_CALL` or `BlockType.EXPRESSION`
- Uses standard function call handling from tree-sitter grammar
- No special test framework recognition - treated as regular method calls

---

### 2️⃣5️⃣ Emery Workflow Operations

We handle Emery workflow step definitions and execution.

```groovy
// 🔄 Workflow step definition
def workflowStep = Emery.workflow.defineStep("APPROVAL") { context ->
    if (context.requiresApproval) {
        return Emery.workflow.requestApproval(context.requestId)  // ← Closure → RECURSE
    } else {
        return Emery.workflow.autoApprove()
    }
}
```

✅ **Detected as:** Workflow definitions with closure recursion

---

### 2️⃣6️⃣ Use Statement Handling

We handle Emery use declarations as function calls.

```groovy
// 📦 Use declarations (treated as function calls)
use("EMERY_UTILITIES")
use("EMERY_FORM_OPERATIONS")
use("EMERY_DATA_ACCESS")
```

✅ **Detected as:** Use statements handled as standard function calls

**Implementation Details:**
- `use()` statements are mapped to `BlockType.FUNCTION_CALL`
- Treated as standard function calls with string parameters
- No special DSL handling - uses regular tree-sitter parsing

---

## 📊 Response Structure

### 🌳 Hierarchical Diff Output

When you compare two files, you get a structured response like this:

```
📊 Comparison Summary
├── Similarity: 75%
├── Added: 2 blocks
├── Deleted: 1 block
├── Modified: 3 blocks
└── Unchanged: 8 blocks

📝 Differences Found:

BlockDiff (method: processData) - ✏️ MODIFIED
├── StatementDiff (declaration) - ✅ UNCHANGED
├── StatementDiff (if_statement) - ✏️ MODIFIED [is_container=True]
│   ├── IfBranch (if) - ✏️ MODIFIED
│   │   ├── StatementDiff (expression_statement) - ✅ UNCHANGED
│   │   └── StatementDiff (for_loop) - ✏️ MODIFIED [is_container=True]
│   │       ├── StatementDiff (expression_statement) - ➕ ADDED
│   │       └── StatementDiff (expression_statement) - ✅ UNCHANGED
│   ├── IfBranch (else_if) - ➕ ADDED
│   └── IfBranch (else) - ➖ DELETED
└── StatementDiff (return_statement) - ✅ UNCHANGED
```

### 🔧 Key Data Structures

| 🏗️ Structure | 📝 Description | 🎯 Purpose |
|-------------|----------------|------------|
| 🧬 **`RecursiveNodeSignature`** | Hierarchical signature for matching code blocks | Contains type, identifier, content_hash, structure_hash, children, depth, path |
| 🏷️ **`BlockSignature`** | Top-level block signature | Contains type, identifier, content_hash, start_line, end_line, code, modifiers |
| 📦 **`BlockDiff`** | Block-level difference with statement diffs | Contains change type, metadata, and nested statement diffs |
| 📝 **`StatementDiff`** | Statement-level difference with optional child diffs | Supports container relationships and recursive nesting |
| 📊 **`ComparisonResult`** | Complete comparison result | Contains statistics, error handling, and all differences |

### 🎯 Emery DSL Support

| 🏷️ Feature | 📝 Description | 🔧 Implementation |
|------------|----------------|-------------------|
| 🏗️ **Typed Declaration Support** | Full capture of `USER_PROFILE_FORM obj = ...` patterns | Via `_extract_typed_declaration()` |
| 🎯 **Standard Expression Handling** | `F.fieldName`, `Emery.module.method()` treated as regular expressions | Standard Groovy parsing |
| ⚡ **Binary Operation Support** | `F.rows << newRow` patterns mapped to `BlockType.EXPRESSION` | Expression mapping |
| 📦 **Use Statement Recognition** | `use("MODULE_NAME")` handled as function calls | Function call handling |
| 🎨 **No Special Combination Logic** | Emery constructs use standard Groovy parsing rules | Standard parsing approach |

---

## ⚠️ Limitations

### ❌ Not Covered

| 🚫 Limitation | 📝 Description | 💡 Reason |
|---------------|----------------|-----------|
| 🎭 **Dynamic Groovy features** | Runtime-generated methods/properties | Requires runtime analysis |
| 🔗 **Cross-file analysis** | Each comparison is file-scoped | Single-file focus |
| 🔄 **Rename detection** | Variable/method renames are detected as DELETED + ADDED | Name-based matching |
| 🔧 **AST transformation** | @CompileStatic and other AST transformations | Compile-time transformations |
| 🏗️ **Gradle-specific syntax** | Build script DSL patterns (partially covered) | Limited DSL support |

### 🎯 Groovy-Specific Challenges

| 🎨 Challenge | 📝 Description | 🔧 Current Approach |
|-------------|----------------|-------------------|
| 🔒 **Closure context determination** | Distinguishing structural vs functional closures | Context-aware analysis |
| 🔗 **Collection method chaining** | Maintaining semantic units across chains | Individual expression handling |
| 🎭 **Dynamic typing** | Type inference limitations | Syntax-based analysis |
| 🎪 **Meta-programming** | Runtime behavior analysis | Static structure analysis |

---

## 🚀 API Usage

### 🔧 Direct API Usage

```python
from app.domain.groovy_ast_diff import GroovyASTDiff

# 🏗️ Initialize the comparison service
service = GroovyASTDiff()

# 📊 Compare files
result = service.compare_files("file_a.groovy", "file_b.groovy")

# 📝 Or compare from source
result = service.compare_sources(source_a.encode('utf-8'), source_b.encode('utf-8'))

print(f"📊 Similarity: {result.structural_similarity:.2%}")
for diff in result.diffs:
    print(f"{diff.change_type.value}: {diff.identifier}")
```

### 🏢 Service Layer Usage

```python
from app.services.groovy_comparison_service import GroovyComparisonService

# 🏗️ Initialize the service
service = GroovyComparisonService()

# 🔄 Compare files (automatically performs recursive analysis)
result = service.compare_files("file_a.groovy", "file_b.groovy")

for diff in result.diffs:
    print(f"📝 {diff.change_type.value}: {diff.identifier}")
    
    # 🌳 Statement-level diffs (hierarchical)
    for stmt_diff in diff.statement_diffs:
        print(f"  📋 {stmt_diff.change_type.value}: {stmt_diff.node_type}")
        
        # 🪆 Child diffs for containers (recursive)
        for child in stmt_diff.child_diffs:
            print(f"    🔸 {child.change_type.value}: {child.node_type}")
            
            # 📦 Container information
            if stmt_diff.is_container:
                print(f"      🏗️ Container: {stmt_diff.description}")
```

### 🌐 FastAPI Endpoint Usage

```python
# 🌐 Via HTTP API endpoint
import requests

files = {
    'file_a': open('emery_before.groovy', 'rb'),
    'file_b': open('emery_after.groovy', 'rb')
}

response = requests.post('http://localhost:8000/api/v1/compare', files=files)
result = response.json()

# 📊 Access comparison results
print(f"📊 Similarity: {result['summary']['structural_similarity']:.2%}")
for diff in result['differences']:
    print(f"📝 {diff['change_type']}: {diff['identifier']}")
    
    # 🌳 Statement-level changes
    for stmt in diff['statement_diffs']:
        print(f"  📋 {stmt['change_type']}: {stmt['node_type']}")
```

---

## 📚 Version History

| 🏷️ Feature | 📝 Description | ✅ Status |
|------------|----------------|-----------|
| 🔄 **Current Version** | Full recursive comparison with hierarchical diff structure | ✅ Complete |
| 🎪 **Multi-Phase Matching** | 4-phase matching strategy (identifier, content hash, similarity, unmatched) | ✅ Complete |
| 🧠 **Context-Aware Closures** | Distinction between structural and functional closures | ✅ Complete |
| 🛡️ **Recursion Protection** | Depth limiting and leaf node detection | ✅ Complete |
| 🎯 **Emery DSL Support** | Standard handling of Emery constructs as regular Groovy syntax | ✅ Complete |
| 🌐 **FastAPI Integration** | RESTful API with file upload and comparison endpoints | ✅ Complete |
| 🌳 **Hierarchical Diffs** | Nested StatementDiff structure showing container relationships | ✅ Complete |

---



### 🔍 Validation Results

| ✨ Feature | 💡 Benefit | ✅ Status |
|-----------|-----------|-----------|
| 🏗️ **Comprehensive Groovy Support** | All major Groovy constructs properly handled | ✅ Validated |
| 🧠 **Context-Aware Closure Handling** | Structural vs functional closures correctly distinguished | ✅ Validated |
| 🛡️ **Recursion Safety** | Depth limiting and leaf detection prevent infinite recursion | ✅ Validated |
| 🎪 **Multi-Phase Matching** | Sophisticated matching strategy with 70% similarity threshold | ✅ Validated |
| 🎯 **Emery DSL Compatibility** | Emery constructs handled as standard Groovy syntax | ✅ Validated |
| 🌐 **API Integration** | Full FastAPI service with file upload and comparison endpoints | ✅ Validated |

### 🚀 Current Implementation Status

**✅ Fully Implemented:**
- ✅ Block-level comparison with multi-phase matching
- ✅ Recursive statement-level analysis
- ✅ Context-aware closure handling
- ✅ Hierarchical diff structure
- ✅ Emery DSL support (as standard Groovy)
- ✅ FastAPI service integration

---

## 🎯 Benefits

| ✨ Feature | 💡 Benefit |
|-----------|-----------|
| 🔍 **Deep Comparison** | Sees changes inside nested structures |
| 🧠 **Smart Matching** | Recognizes moved code, not just additions/deletions |
| 🎨 **Pattern Aware** | Understands Groovy and Emery DSL patterns |
| 📊 **Detailed Output** | Shows exactly what changed and where |
| 🌳 **Hierarchical View** | Displays changes in a tree structure |

---

## 🎯 Use Cases

| 👤 Who | 🎯 Use Case |
|--------|------------|
| 👩‍💻 **Developers** | Review code changes before merging |
| 👨‍🔬 **Code Reviewers** | Quickly understand what changed |
| 🧪 **QA Teams** | Verify specific functionality was modified |
| 📊 **Project Managers** | Track scope of changes between versions |
| 📋 **Auditors** | Document changes for compliance |

---

*This tool provides intelligent code comparison that goes beyond simple text diff, understanding the structure and meaning of your Groovy code including Emery DSL patterns.*