# Groovy AST Comparison Service - Covered Scenarios

## Version Overview

This document describes all scenarios covered by the Groovy AST Comparison service in this codebase. The system provides comprehensive comparison capabilities for Groovy code including:

1. **Block-Level Comparison** (`GroovyASTDiff`) - Top-level structure comparison (classes, methods, closures)
2. **Recursive Comparison** (`GroovyRecursiveParser`) - Deep nested structure comparison with statement-level analysis
3. **Emery DSL Support** - Specialized handling for Emery language constructs
4. **Multi-Phase Matching Strategy** - Sophisticated matching with 4 distinct phases
5. **Hierarchical Diff Structure** - Nested diffs showing container relationships

---

## Table of Contents

1. [Change Types](#change-types)
2. [Block Types](#block-types)
3. [Block-Level Scenarios](#block-level-scenarios)
4. [Statement-Level Scenarios](#statement-level-scenarios)
5. [Nested Structure Scenarios](#nested-structure-scenarios)
6. [Control Flow Scenarios](#control-flow-scenarios)
7. [Closure & Collection Method Detection](#closure--collection-method-detection)
8. [Groovy-Specific Patterns](#groovy-specific-patterns)
9. [Emery DSL Scenarios](#emery-dsl-scenarios)
10. [Response Structure](#response-structure)

---

## Change Types

### Block-Level Change Types (`ChangeType`)

| Change Type | Description | Example |
|-------------|-------------|---------|
| `ADDED` | Block exists only in new file | New method added |
| `DELETED` | Block exists only in old file | Method removed |
| `MODIFIED` | Same identifier, different content | Method body changed |
| `MOVED` | Same content, different position | Method reordered in class |
| `MOVED_MODIFIED` | Different position AND content changed | Method moved and edited |
| `UNCHANGED` | Identical in both files | No changes |

### Statement-Level Change Types (`StatementChangeType`)

Same types as block-level, but applied to individual statements within blocks.

---

## Block Types

The system recognizes the following Groovy block types:

### Class and Interface Types
- `CLASS` - `class Foo {}`
- `INTERFACE` - `interface Foo {}`
- `TRAIT` - `trait Foo {}`
- `ENUM` - `enum Status {}`
- `ANNOTATION` - `@interface Foo {}`

### Method Types
- `FUNCTION` - `def foo() {}`
- `METHOD` - Class method definition
- `CONSTRUCTOR` - Class constructor
- `STATIC_METHOD` - Static method
- `PRIVATE_METHOD` - Private method

### Variable and Property Types
- `DECLARATION` - `def x = value`, typed declarations
- `FIELD` - Class field declaration
- `PROPERTY` - Groovy property with getter/setter

### Control Flow Types
- `STATEMENT` - Generic statement (if, for, while, switch, try)
- `EXPRESSION` - Top-level expressions
- `BINARY_OPERATION` - Binary operations like `F.rows << newRow`

### Groovy-Specific Types
- `CLOSURE` - `{ param -> body }`
- `SCRIPT` - Top-level script constructs
- `FUNCTION_CALL` - Method calls including Emery DSL calls

---

## Groovy Container Types

The system recognizes these container types that require recursive analysis:

### Method Containers
- `class_definition` - Class body with methods
- `interface_definition` - Interface with method signatures
- `trait_definition` - Trait with default implementations
- `method_definition` - Method body
- `function_definition` - Function body
- `constructor_definition` - Constructor body

### Control Flow Containers
- `if_statement` - If/else branches (branch-aware analysis)
- `switch_statement` - Switch cases
- `switch_block` - Switch body container
- `case` - Individual switch cases
- `for_loop` - For-in loops
- `for_in_loop` - Enhanced for loops
- `while_loop` - While loops
- `do_while_statement` - Do-while loops

### Exception Handling Containers
- `try_statement` - Try-catch-finally blocks
- `catch_clause` - Catch blocks
- `finally_clause` - Finally blocks

### Groovy-Specific Containers
- `closure` - Closure bodies (context-aware)
- `closure_expression` - Closure expressions
- `statement_block` - Generic statement blocks

---

## Block-Level Scenarios

### Scenario 1: Named Block Matching

**Covered:** Classes, methods, and variables are matched by their identifier (name).

```groovy
// File A
def add(a, b) {
    return a + b
}

// File B - MODIFIED (same name, different content)
def add(a, b, c) {
    return a + b + c
}
```

### Scenario 2: Content-Based Block Matching

**Covered:** Expressions, statements, and anonymous blocks are matched by content similarity when names aren't available.

```groovy
// File A
println add(5, 3)

// File B - MODIFIED (same position, content changed)
println add(5, 3, 6)
```

### Scenario 3: Multi-Phase Matching Strategy

**Covered:** Sophisticated matching with multiple phases for optimal accuracy.

**Matching Phases:**
1. **Phase 1:** Match by identifier (exact name match for same type)
2. **Phase 2:** Match by content hash (exact content match → MOVED)
3. **Phase 3:** Match by structural similarity (≥70% similarity → MODIFIED/MOVED_MODIFIED)
4. **Phase 4:** Remaining unmatched → ADDED/DELETED

**Implementation Details:**
- Uses `_compare_blocks()` method in `GroovyASTDiff` class
- Recursive matching in `GroovyRecursiveParser._compare_container_children()`
- Similarity threshold of 0.7 (70%) for modified block detection

### Scenario 4: Structural Similarity Scoring

**Covered:** Uses advanced similarity calculation for Groovy AST structures.

```groovy
// Similarity calculation based on:
// - Identifier matching
// - Content hash comparison
// - Structural tree similarity
// - Line position analysis
```

### Scenario 5: Typed Declaration Handling

**Covered:** Properly handles Groovy typed declarations including Emery DSL types.

```groovy
// File A
USER_PROFILE_FORM formObj = Emery.form.newForm("USER_PROFILE_FORM")

// File B

USER_PROFILE_FORM formObj = Emery.form.newForm("EMPLOYEE_FORM")
```

---

## Statement-Level Scenarios

### Scenario 6: Statement Movement Detection

**Covered:** Detects when statements are reordered within a method.

```groovy
// File A
def process() {
    def a = 1
    def b = 2
    return a + b
}

// File B - Statement MOVED
def process() {
    def b = 2  // Moved up
    def a = 1  // Moved down
    return a + b
}
```

### Scenario 7: Statement Modification Detection

**Covered:** Detects when a statement's content changes while position remains similar.

```groovy
// File A
count = count + 2

// File B - MODIFIED (equivalent operation)
count += 2
```

### Scenario 8: Mixed Statement Changes

**Covered:** Handles combinations of added, deleted, modified, and moved statements.

```groovy
// File A
def process(items) {
    println "start"
    for (i in 0..<items.size()) {
        processItem(items[i])
    }
    println "end"
}

// File B
def process(items) {
    println "processing"  // MODIFIED
    items.each { item ->  // ADDED (old for loop DELETED)
        processItem(item)
    }
    println "end"         // UNCHANGED
}
```

---

## Nested Structure Scenarios

### Scenario 9: Nested Class Detection

**Covered:** Detects and compares nested class declarations.

```groovy
class Outer {
    class Inner {  // Nested class detected
        def innerMethod() {
            return true
        }
    }
    
    static class StaticNested {
        def staticMethod() {
            return "static"
        }
    }
}
```

### Scenario 10: Closure Detection and Context Analysis

**Covered:** Context-aware closure handling distinguishing structural vs functional closures.

```groovy
// Functional closures (treated as pure statements)
list.findAll { item -> item > 2 }
list.collect { item -> item * 2 }
list.each { item -> println item }

// Structural closures (treated as containers for recursion)
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

### Scenario 11: Method-in-Method Detection

**Covered:** Detects and compares nested method declarations.

```groovy
def outerMethod() {
    def innerMethod() {  // Nested method detected
        return "inner"
    }
    
    def anotherInner = { param ->  // Closure assignment
        return param.toUpperCase()
    }
    
    return innerMethod() + anotherInner("test")
}
```

### Scenario 12: Class Method Detection

**Covered:** Compares class methods individually with proper inheritance handling.

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

---

## Control Flow Scenarios

### Scenario 13: If-Else-If Chain Comparison

**Covered:** Recursive comparison of if/else-if/else statements as containers.

```groovy
// File A
if (x > 10) {
    println "large"
} else if (x > 5) {
    println "medium"
} else {
    println "small"
}

// File B - Branches reordered and modified
if (x > 5) {           // Different condition
    println "medium"
} else if (x > 10) {   // Different condition
    println "very large"  // MODIFIED body
} else {
    println "small"       // UNCHANGED
}
```

**Implementation Details:**
- If statements are treated as containers in `GROOVY_RECURSIVE_CONTAINERS`
- Uses `_get_parseable_children()` to extract if/else bodies
- Recursive comparison via `GroovyRecursiveParser.compare_recursive_statements()`

### Scenario 14: Switch Statement Comparison

**Covered:** Compares individual switch cases with Groovy-specific patterns.

```groovy
switch (value) {
    case String:           // Type matching
        handleString(value)
        break
    case { it > 10 }:      // Closure condition
        handleLarge(value)
        break
    case 1..5:             // Range matching
        handleSmall(value)
        break
    case [1, 2, 3]:        // List matching
        handleList(value)
        break
    default:
        handleDefault(value)
}
```

### Scenario 15: Try-Catch-Finally Comparison

**Covered:** Compares try, catch, and finally blocks separately.

```groovy
try {
    riskyOperation()
} catch (IOException e) {
    handleIOError(e)
} finally {
    cleanup()
}
```

### Scenario 16: Loop Body Comparison

**Covered:** Compares loop bodies for all Groovy loop types.

```groovy
// For-in loop (Groovy style)
for (item in collection) {
    processItem(item)
}

// Range-based for loop
for (i in 0..10) {
    println i
}

// While loop
while (condition) {
    // Body compared recursively
}

// Enhanced for loop (converted from Java style)
for (String item in stringCollection) {
    processString(item)
}
```

---

## Closure & Collection Method Detection

### Recognized Collection Methods

The system recognizes closures in these Groovy collection methods:

| Category | Methods |
|----------|---------|
| **Filtering** | `findAll`, `find`, `findIndexOf`, `grep` |
| **Transformation** | `collect`, `collectEntries`, `collectMany`, `flatten` |
| **Iteration** | `each`, `eachWithIndex`, `reverseEach` |
| **Aggregation** | `inject`, `reduce`, `sum`, `min`, `max` |
| **Testing** | `any`, `every`, `contains` |
| **Sorting** | `sort`, `sortBy` |
| **Grouping** | `groupBy`, `countBy` |
| **Unique** | `unique`, `uniqueBy` |

### Collection Method Handling

**Covered:** Collection methods with closures are treated as expression statements.

```groovy
// These are treated as expression statements (not combined):
def filtered = list.findAll { item ->
    return item.isActive && item.value > 10  // Closure treated as pure statement
}

def transformed = list.collect { item ->
    return [id: item.id, name: item.name.toUpperCase()]  // Closure treated as pure statement
}

// Method chaining
def result = list
    .findAll { it.active }      // Each line is a separate expression
    .collect { it.transform() }
    .groupBy { it.category }
```

**Implementation Details:**
- Collection methods are mapped to `BlockType.EXPRESSION` in `GROOVY_NODE_TYPE_TO_BLOCK_TYPE`
- Closures in functional contexts are treated as pure statements to avoid deep recursion
- No special "combination" logic - each statement is analyzed independently

### Closure Context Analysis

**Covered:** Context-aware closure handling distinguishes structural vs functional closures.

```groovy
// Structural closure (control flow) - Treated as container
if (condition) {
    // Block body - not a functional closure
}

// Functional closure (collection method) - Combined with method call
list.each { item ->
    // Functional closure body → RECURSE
}
```

---

## Groovy-Specific Patterns

### Scenario 17: Groovy Truth Evaluation

**Covered:** Handles Groovy's truth evaluation in conditions.

```groovy
// Groovy truth patterns
if (list) {          // true if list is not null and not empty
    processItems()
}

if (string) {        // true if string is not null and not empty
    processString()
}

if (map) {           // true if map is not null and not empty
    processMap()
}
```

### Scenario 18: Safe Navigation Operator

**Covered:** Handles safe navigation (?.) and elvis operator (?:).

```groovy
// Safe navigation
def result = obj?.method?.()
def property = obj?.nested?.property

// Elvis operator
def value = maybeNull ?: defaultValue
def result = obj?.method?.() ?: fallback?.() ?: defaultValue
```

### Scenario 19: Groovy String Interpolation

**Covered:** Handles GString expressions and interpolation.

```groovy
// String interpolation
def name = "World"
def greeting = "Hello, ${name}!"
def complex = "Result: ${calculate(a, b)}"

// Multiline strings
def multiline = """
    Line 1: ${value1}
    Line 2: ${value2}
"""
```

### Scenario 20: Range Operations

**Covered:** Handles Groovy range expressions.

```groovy
// Range creation and usage
def range1 = 0..10        // Inclusive range
def range2 = 1..<10       // Exclusive range
def charRange = 'a'..'z'  // Character range

// Range in loops
for (i in 0..items.size()-1) {
    processItem(items[i])
}
```

### Scenario 21: Meta-Programming Patterns

**Covered:** Handles dynamic method calls and property access.

```groovy
// Dynamic method calls
obj."${methodName}"(params)
obj.invokeMethod(methodName, params)

// Dynamic property access
obj."${propertyName}" = value
def value = obj."${propertyName}"

// Method missing
def methodMissing(String name, args) {
    return "Called ${name} with ${args}"
}
```

---

## Emery DSL Scenarios

### Scenario 22: Form Field Operations

**Covered:** Emery form field access patterns handled as expressions.

```groovy
// Form field access (treated as expressions)
F.userName = "john.doe"
F.userAge = 25

// Nested field access
F.skillsMultiRow.rows.each { row ->
    processSkillRow(row)  // Closure treated as pure statement
}

// Binary operations (mapped to expressions)
F.skillsMultiRow.rows << newRow1
F.skillsMultiRow.rows << newRow2
```

**Implementation Details:**
- `F.fieldName` patterns are handled as regular assignment expressions
- Binary operations (`<<`) are mapped to `BlockType.EXPRESSION`
- No special "combination" logic for Emery constructs
- Uses standard expression handling from `GROOVY_NODE_TYPE_TO_BLOCK_TYPE`

### Scenario 23: Emery Data Operations

**Covered:** Emery data table and MDOS operations handled as declarations and expressions.

```groovy
// Data table operations (treated as variable declarations)
def countryData = Emery.dataTable.read("COUNTRY_DATA_TABLE")
def statusData = Emery.dataTable.read("STATUS_LOOKUP_TABLE")

// Data processing with closures
def activeCountries = countryData.findAll { row ->
    return row.isActive == true  // Closure treated as pure statement
}

// MDOS operations (treated as variable declarations)
def mdosData = Emery.mdos.getMdosDisplayValuesClob(params)
```

**Implementation Details:**
- `Emery.dataTable.read()` calls are handled as regular method calls in declarations
- Variable declarations with `def` are mapped to `BlockType.DECLARATION`
- No special handling for Emery namespace - treated as standard Groovy syntax

### Scenario 24: Emery Test Operations

**Covered:** Emery test framework constructs handled as function calls and expressions.

```groovy
// Test assertions (treated as function calls)
Emery.test.assertEquals("SUCCESS", result.status)
Emery.test.assertTrue(userCount > 0)
Emery.test.assertNotNull(response.data)

// Test delays and timing
Emery.test.delay(1000)
Emery.test.waitFor { condition ->
    return service.isReady()  // Closure treated as pure statement
}
```

**Implementation Details:**
- `Emery.test.*` calls are mapped to `BlockType.FUNCTION_CALL` or `BlockType.EXPRESSION`
- Uses standard function call handling from tree-sitter grammar
- No special test framework recognition - treated as regular method calls

### Scenario 25: Emery Workflow Operations

**Covered:** Handles Emery workflow step definitions and execution.

```groovy
// Workflow step definition
def workflowStep = Emery.workflow.defineStep("APPROVAL") { context ->
    if (context.requiresApproval) {
        return Emery.workflow.requestApproval(context.requestId)  // Closure → RECURSE
    } else {
        return Emery.workflow.autoApprove()
    }
}
```

### Scenario 26: Use Statement Handling

**Covered:** Emery use declarations handled as function calls.

```groovy
// Use declarations (treated as function calls)
use("EMERY_UTILITIES")
use("EMERY_FORM_OPERATIONS")
use("EMERY_DATA_ACCESS")
```

**Implementation Details:**
- `use()` statements are mapped to `BlockType.FUNCTION_CALL`
- Treated as standard function calls with string parameters
- No special DSL handling - uses regular tree-sitter parsing

---

## Response Structure

### Hierarchical Diff Output

```
BlockDiff (method: processData) - MODIFIED
├── StatementDiff (declaration) - UNCHANGED
├── StatementDiff (if_statement) - MODIFIED [is_container=True]
│   ├── IfBranch (if) - MODIFIED
│   │   ├── StatementDiff (expression_statement) - UNCHANGED
│   │   └── StatementDiff (for_loop) - MODIFIED [is_container=True]
│   │       ├── StatementDiff (expression_statement) - ADDED
│   │       └── StatementDiff (expression_statement) - UNCHANGED
│   ├── IfBranch (else_if) - ADDED
│   └── IfBranch (else) - DELETED
└── StatementDiff (return_statement) - UNCHANGED
```

### Key Data Structures

- **`RecursiveNodeSignature`**: Hierarchical signature for matching code blocks (type, identifier, content_hash, structure_hash, children, depth, path)
- **`BlockSignature`**: Top-level block signature (type, identifier, content_hash, start_line, end_line, code, modifiers)
- **`BlockDiff`**: Block-level difference with statement diffs and metadata
- **`StatementDiff`**: Statement-level difference with optional child diffs and container support
- **`ComparisonResult`**: Complete comparison result with statistics and error handling

### Emery DSL Support

- **Typed Declaration Support**: Full capture of `USER_PROFILE_FORM obj = ...` patterns via `_extract_typed_declaration()`
- **Standard Expression Handling**: `F.fieldName`, `Emery.module.method()` treated as regular expressions
- **Binary Operation Support**: `F.rows << newRow` patterns mapped to `BlockType.EXPRESSION`
- **Use Statement Recognition**: `use("MODULE_NAME")` handled as function calls
- **No Special Combination Logic**: Emery constructs use standard Groovy parsing rules

---

## Limitations

### Not Covered

1. **Dynamic Groovy features** - Runtime-generated methods/properties
2. **Cross-file analysis** - Each comparison is file-scoped
3. **Rename detection** - Variable/method renames are detected as DELETED + ADDED
4. **AST transformation** - @CompileStatic and other AST transformations
5. **Gradle-specific syntax** - Build script DSL patterns (partially covered)


### Groovy-Specific Challenges

- **Closure context determination** - Distinguishing structural vs functional closures
- **Collection method chaining** - Maintaining semantic units across chains
- **Dynamic typing** - Type inference limitations
- **Meta-programming** - Runtime behavior analysis

---

## API Usage

### Direct API Usage

```python
from app.domain.groovy_ast_diff import GroovyASTDiff

# Initialize the comparison service
service = GroovyASTDiff()

# Compare files
result = service.compare_files("file_a.groovy", "file_b.groovy")

# Or compare from source
result = service.compare_sources(source_a.encode('utf-8'), source_b.encode('utf-8'))

print(f"Similarity: {result.structural_similarity:.2%}")
for diff in result.diffs:
    print(f"{diff.change_type.value}: {diff.identifier}")
```

### Service Layer Usage

```python
from app.services.groovy_comparison_service import GroovyComparisonService

# Initialize the service
service = GroovyComparisonService()

# Compare files (automatically performs recursive analysis)
result = service.compare_files("file_a.groovy", "file_b.groovy")

for diff in result.diffs:
    print(f"{diff.change_type.value}: {diff.identifier}")
    
    # Statement-level diffs (hierarchical)
    for stmt_diff in diff.statement_diffs:
        print(f"  {stmt_diff.change_type.value}: {stmt_diff.node_type}")
        
        # Child diffs for containers (recursive)
        for child in stmt_diff.child_diffs:
            print(f"    {child.change_type.value}: {child.node_type}")
            
            # Container information
            if stmt_diff.is_container:
                print(f"      Container: {stmt_diff.description}")
```

### FastAPI Endpoint Usage

```python
# Via HTTP API endpoint
import requests

files = {
    'file_a': open('emery_before.groovy', 'rb'),
    'file_b': open('emery_after.groovy', 'rb')
}

response = requests.post('http://localhost:8000/api/v1/compare', files=files)
result = response.json()

# Access comparison results
print(f"Similarity: {result['summary']['structural_similarity']:.2%}")
for diff in result['differences']:
    print(f"{diff['change_type']}: {diff['identifier']}")
    
    # Statement-level changes
    for stmt in diff['statement_diffs']:
        print(f"  {stmt['change_type']}: {stmt['node_type']}")
```

---

## Version History

- **Current Version**: Full recursive comparison with hierarchical diff structure
- **Multi-Phase Matching**: 4-phase matching strategy (identifier, content hash, similarity, unmatched)
- **Context-Aware Closures**: Distinction between structural and functional closures
- **Recursion Protection**: Depth limiting and leaf node detection
- **Emery DSL Support**: Standard handling of Emery constructs as regular Groovy syntax
- **FastAPI Integration**: RESTful API with file upload and comparison endpoints
- **Hierarchical Diffs**: Nested StatementDiff structure showing container relationships

---



### Validation Results

- **Comprehensive Groovy Support**: All major Groovy constructs (classes, methods, closures, control flow) properly handled
- **Context-Aware Closure Handling**: Structural vs functional closures correctly distinguished
- **Recursion Safety**: Depth limiting and leaf detection prevent infinite recursion
- **Multi-Phase Matching**: Sophisticated matching strategy with 70% similarity threshold
- **Emery DSL Compatibility**: Emery constructs handled as standard Groovy syntax
- **API Integration**: Full FastAPI service with file upload and comparison endpoints

### Current Implementation Status

**Fully Implemented:**
- ✅ Block-level comparison with multi-phase matching
- ✅ Recursive statement-level analysis
- ✅ Context-aware closure handling
- ✅ Hierarchical diff structure
- ✅ Emery DSL support (as standard Groovy)
- ✅ FastAPI service integration

This implementation provides accurate and reliable AST comparison for Groovy codebases while maintaining compatibility with Emery DSL patterns.