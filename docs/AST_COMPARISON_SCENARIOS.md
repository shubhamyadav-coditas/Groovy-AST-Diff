# Groovy AST Comparison Service - Covered Scenarios

## Version Overview

This document describes all scenarios covered by the Groovy AST Comparison service in this codebase. The system provides comprehensive comparison capabilities for Groovy code including:

1. **Block-Level Comparison** - Top-level structure comparison (classes, methods, closures)
2. **Recursive Comparison** - Deep nested structure comparison with statement-level analysis
3. **Emery DSL Support** - Specialized handling for Emery language constructs
4. **Branch-Aware Analysis** - Individual if/else-if/else branch comparison

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
10. [Performance Optimizations](#performance-optimizations)

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
1. **Phase 0:** Match by identifier (exact name match)
2. **Phase 1:** Match by content hash (exact content match → MOVED)
3. **Phase 2:** Match moved blocks by identifier with different position
4. **Phase 3:** Hybrid similarity matching:
   - First pass: High-confidence matches (≥90% similarity)
   - Second pass: Best match for remaining blocks
5. **Phase 4:** Remaining unmatched → ADDED/DELETED

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

// File B - MOVED_MODIFIED
EMPLOYEE_FORM empForm = Emery.form.newForm("EMPLOYEE_FORM")
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

### Scenario 10: Closure Detection in Method Calls

**Covered:** Detects closures passed as parameters and combined with method calls.

```groovy
// Collection methods with closures (Combined as single statements)
list.findAll { item -> item > 2 }
list.collect { item -> item * 2 }
list.each { item -> println item }

// Nested closures
list.groupBy { item ->
    item.category
}.each { category, items ->
    processCategory(category, items)
}
```

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

### Scenario 13: Branch-Aware If-Else-If Analysis

**Covered:** Individual branch comparison for if/else-if/else statements.

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
if (x > 5) {           // MOVED from position 1 to 0
    println "medium"
} else if (x > 10) {   // MOVED from position 0 to 1
    println "very large"  // MODIFIED body
} else {
    println "small"       // UNCHANGED
}
```

**Branch Matching Strategy:**
1. Extract individual if/else-if/else branches
2. Match by condition hash → UNCHANGED/MOVED
3. Match by condition similarity → MODIFIED/MOVED_MODIFIED
4. Match else blocks by type
5. Remaining unmatched → ADDED/DELETED

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
} catch (RuntimeException e) {
    handleRuntimeError(e)
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

### Collection Method Closure Detection

**Covered:** All collection methods with closures are combined as single semantic units.

```groovy
// These are treated as single combined statements:
def filtered = list.findAll { item ->
    return item.isActive && item.value > 10  // Closure body → RECURSE
}

def transformed = list.collect { item ->
    return [id: item.id, name: item.name.toUpperCase()]  // Closure body → RECURSE
}

// Chained collection methods
def result = list
    .findAll { it.active }      // Each method+closure combined
    .collect { it.transform() }
    .groupBy { it.category }
    .collectEntries { k, v ->
        [k, v.size()]
    }
```

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

**Covered:** Specialized handling for Emery form field access patterns.

```groovy
// Form field access (Combined: F.fieldName)
F.userName = "john.doe"
F.userAge = 25

// Nested field access (Combined: F.skillsMultiRow.rows)
F.skillsMultiRow.rows.each { row ->
    processSkillRow(row)  // Closure → RECURSE
}

// Binary operations (Combined: F.rows << newRow)
F.skillsMultiRow.rows << newRow1
F.skillsMultiRow.rows << newRow2
```

### Scenario 23: Emery Data Operations

**Covered:** Handles Emery data table and MDOS operations.

```groovy
// Data table operations (Combined: Emery.dataTable.read())
def countryData = Emery.dataTable.read("COUNTRY_DATA_TABLE")
def statusData = Emery.dataTable.read("STATUS_LOOKUP_TABLE")

// Data processing with closures
def activeCountries = countryData.findAll { row ->
    return row.isActive == true  // Filter closure → RECURSE
}

// MDOS operations (Combined: Emery.mdos.getMdos())
def mdosData = Emery.mdos.getMdosDisplayValuesClob(params)
```

### Scenario 24: Emery Test Operations

**Covered:** Handles Emery test framework constructs.

```groovy
// Test assertions (Combined: Emery.test.assertEquals())
Emery.test.assertEquals("SUCCESS", result.status)
Emery.test.assertTrue(userCount > 0)
Emery.test.assertNotNull(response.data)

// Test delays and timing
Emery.test.delay(1000)
Emery.test.waitFor { condition ->
    return service.isReady()  // Condition closure → RECURSE
}
```

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

**Covered:** Handles Emery use declarations.

```groovy
// Use declarations (Combined: use())
use("EMERY_UTILITIES")
use("EMERY_FORM_OPERATIONS")
use("EMERY_DATA_ACCESS")
```

---

## Performance Optimizations

### Tree-sitter Query Usage

The system uses compiled tree-sitter queries for efficient Groovy AST traversal:

| Query Purpose | Target Nodes |
|---------------|--------------|
| Container Detection | `class_definition`, `method_definition`, `if_statement`, `for_loop`, `while_loop`, `try_statement` |
| Closure Detection | `closure`, `closure_expression` |
| Statement Extraction | All statement types within containers |
| Control Flow Analysis | `if_statement`, `switch_statement`, `case` |
| Method Call Detection | `function_call`, `juxt_function_call` |

### Smart Detection Strategy

For nested structure detection:
- **Small nodes (<200 bytes):** Uses manual tree traversal (lower overhead)
- **Larger nodes:** Uses compiled queries (faster for complex structures)
- **Context-aware processing:** Different strategies for closures vs control flow

```python
def _should_recurse_into_node(self, node: Node) -> bool:
    if node.type in self.GROOVY_LEAF_STATEMENTS:
        return False
    if node.type == 'closure' and self._is_structural_closure(node):
        return False
    return node.type in self.GROOVY_RECURSIVE_CONTAINERS
```

### Recursion Protection

**Covered:** Multiple layers of recursion protection:

```python
# Depth limiting
MAX_RECURSION_DEPTH = 50

# Circular reference detection
visited = set()

# Python recursion limit increase
sys.setrecursionlimit(5000)
```

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

- **`RecursiveNodeSignature`**: Signature for matching code blocks (type, identifier, hash, lines, children)
- **`BlockDiff`**: Block-level difference with statement diffs
- **`StatementDiff`**: Statement-level difference with optional child diffs
- **`IfBranch`**: Individual if/else-if/else branch representation
- **`ComparisonResult`**: Complete comparison result with statistics

### Emery DSL Enhancements

- **Combined Statement Handling**: Method calls with closures treated as single units
- **Typed Declaration Support**: Full capture of `USER_PROFILE_FORM obj = ...` patterns
- **Enhanced Identifier Extraction**: Proper handling of `F.fieldName`, `Emery.module.method()`
- **Binary Operation Support**: Special handling for `F.rows << newRow` patterns

---

## Limitations

### Not Covered

1. **Dynamic Groovy features** - Runtime-generated methods/properties
2. **Cross-file analysis** - Each comparison is file-scoped
3. **Semantic equivalence** - `a + b + c` vs `c + b + a` are treated as different
4. **Rename detection** - Variable/method renames are detected as DELETED + ADDED
5. **AST transformation** - @CompileStatic and other AST transformations
6. **Gradle-specific syntax** - Build script DSL patterns (partially covered)

### Known Node Type Limitations

These patterns require special handling due to tree-sitter-groovy grammar limitations:

- **Java-style enhanced for loops** - Converted to Groovy for-in syntax
- **Array type casting** - `as String[]` syntax not supported
- **Some closure contexts** - Distinction between structural and functional closures
- **Dynamic method calls** - `obj."${methodName}"()` patterns

### Groovy-Specific Challenges

- **Closure context determination** - Distinguishing structural vs functional closures
- **Collection method chaining** - Maintaining semantic units across chains
- **Dynamic typing** - Type inference limitations
- **Meta-programming** - Runtime behavior analysis

---

## API Usage

### Block-Level Comparison

```python
from groovy_ast_diff import GroovyASTDiff

# Initialize the comparison service
service = GroovyASTDiff()

# Compare files
result = service.compare_files("file_a.groovy", "file_b.groovy")

# Or compare from source
result = service.compare_from_source(source_a, source_b)

print(f"Similarity: {result.structural_similarity:.2%}")
for diff in result.differences:
    print(f"{diff.change_type}: {diff.identifier}")
```

### Recursive Comparison with Statement Analysis

```python
# The service automatically performs recursive analysis
result = service.compare_files("file_a.groovy", "file_b.groovy")

for diff in result.differences:
    print(f"{diff.change_type}: {diff.identifier}")
    
    # Statement-level diffs
    for stmt_diff in diff.statement_diffs:
        print(f"  {stmt_diff.change_type}: {stmt_diff.node_type}")
        
        # Child diffs for containers (recursive)
        for child in stmt_diff.child_diffs:
            print(f"    {child.change_type}: {child.node_type}")
            
            # Branch-level diffs for if statements
            if hasattr(child, 'branch_label') and child.branch_label:
                print(f"      Branch: {child.branch_label}")
```

### Emery DSL Analysis

```python
# Emery DSL patterns are automatically detected and handled
result = service.compare_files("emery_before.groovy", "emery_after.groovy")

# Form field operations
# F.fieldName access patterns
# Emery.module.method() calls
# Use statement handling
# All automatically processed with proper identifier extraction
```

---

## Version History

- **Current Version**: Full recursive comparison with branch-aware if-else analysis
- **Emery DSL Support**: Specialized handling for Emery language constructs
- **Closure Method Combination**: Collection methods with closures as semantic units
- **Typed Declaration Handling**: Full capture of typed variable declarations
- **Enhanced Matching**: Hybrid similarity matching with multiple phases
- **Context-Aware Closures**: Distinction between structural and functional closures
- **Recursion Protection**: Multiple layers of infinite recursion prevention

---

## Testing Coverage

### Test Scenarios Covered

| Category | Test Files | Scenarios |
|----------|------------|-----------|
| **Basic Groovy** | `class_*.groovy`, `function_*.groovy` | Class/method comparison |
| **Control Flow** | `added_*.groovy`, `modified_*.groovy` | If/for/while/switch statements |
| **Collections** | `expression_*.groovy` | Collection methods with closures |
| **Emery Core** | `emery_form_*.groovy`, `emery_util_*.groovy` | Basic Emery constructs |
| **Emery Extended** | `emery_advanced_*.groovy` | Complex Emery patterns |
| **Edge Cases** | `moved_*.groovy`, `unchanged_*.groovy` | Movement and similarity detection |

### Validation Results

- **100% Emery DSL Coverage**: All identified Emery constructs properly handled
- **Branch-Aware Analysis**: If/else-if/else branches individually compared
- **Closure Combination**: Collection methods with closures treated as semantic units
- **Recursion Safety**: No infinite recursion in complex nested structures
- **Performance**: Efficient handling of large Groovy files with deep nesting

This comprehensive coverage ensures accurate and reliable AST comparison for all Groovy and Emery DSL patterns encountered in real-world codebases.