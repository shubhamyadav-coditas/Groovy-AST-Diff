# MetaClass Usage Signal

## Overview

The **MetaClass Usage** signal detects runtime metaClass modifications in Groovy code. This is one of the most impactful change signals because metaClass modifications can alter program behavior at runtime in ways that are difficult to predict, test, and debug.

---

## What is MetaClass in Groovy?

In Groovy, every class has an associated `MetaClass` that handles method dispatch at runtime. By modifying the metaClass, developers can:

- **Add new methods** to existing classes (including JDK classes like `String`, `Integer`)
- **Add new properties** to classes
- **Override existing methods** at runtime
- **Change method resolution behavior**

### Example: Adding a Method via MetaClass

```groovy
// Add a 'reverse' method to all Strings
String.metaClass.reverse = {
    delegate.reverse()
}

// Now this works
println "hello".reverse()  // Output: "olleh"
```

---

## Why MetaClass Usage Matters for Change Complexity

MetaClass modifications are **high-risk patterns** because:

| Risk Factor | Description |
|-------------|-------------|
| **Runtime behavior change** | Methods are added/modified at runtime, not compile time |
| **Global impact** | Changes to a class's metaClass affect ALL instances |
| **Difficult to trace** | No static analysis can fully predict behavior |
| **Testing challenges** | Tests may not cover all dynamic invocation paths |
| **Merge conflicts** | Multiple PRs modifying metaClass can cause subtle bugs |
| **Production issues** | MetaClass changes can cause ClassCastExceptions or MethodMissingExceptions |

---

## Signal Detection Logic

### Detection Patterns

The MetaClass signal detector identifies the following patterns:

#### 1. Dotted Identifier with `.metaClass`

```groovy
// Pattern: ClassName.metaClass.methodName = closure
String.metaClass.toSnakeCase = { ... }

// Pattern: ClassName.metaClass = value
ApiClient.metaClass = customMetaClass

// Pattern: instance.metaClass.method = closure
obj.metaClass.helper = { ... }
```

#### 2. MetaClass Block Syntax

```groovy
// Pattern: ClassName.metaClass { ... }
DataService.metaClass {
    fetchData = { ... }
    saveData = { ... }
}
```

#### 3. getMetaClass() Method Calls

```groovy
// Pattern: object.getMetaClass()
def mc = obj.getMetaClass()
println mc.theClass.name
```

### AST Traversal Logic

The detector uses Tree-sitter to parse Groovy code and traverses the AST looking for specific node types:

```
┌─────────────────────────────────────────────────────────────────┐
│                    AST Traversal Logic                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. Find 'dotted_identifier' nodes containing ".metaClass"     │
│     └── e.g., "String.metaClass.reverse"                       │
│     └── Only count the outermost occurrence                    │
│                                                                 │
│  2. Find 'property_access' or 'field_access' with metaClass    │
│     └── Fallback for alternative AST representations           │
│                                                                 │
│  3. Find 'function_call' containing "getMetaClass()"           │
│     └── e.g., "obj.getMetaClass()"                             │
│                                                                 │
│  4. Avoid double-counting nested patterns                      │
│     └── Track counted node positions                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Implementation

```python
# From metaclass_signal_detector.py

def detect(self, root_node: Node, source_bytes: bytes, **kwargs) -> int:
    count = 0
    counted_nodes = set()  # Track counted nodes to avoid double-counting
    
    def traverse(node: Node):
        nonlocal count
        
        # Check for dotted_identifier containing 'metaClass'
        if node.type == 'dotted_identifier':
            text = self._get_node_text(node, source_bytes)
            if '.metaClass' in text:
                # Only count if parent doesn't also contain metaClass
                # (avoid counting both "String.metaClass" and "String.metaClass.reverse")
                ...
        
        # Check for getMetaClass() method calls
        if node.type in ['function_call', 'method_call']:
            text = self._get_node_text(node, source_bytes)
            if 'getMetaClass()' in text:
                ...
```

---

## Types of MetaClass Patterns

### 1. Class-Level MetaClass Modification

Adding methods to a class that apply to all instances:

```groovy
// All String instances get this method
String.metaClass.shout = { delegate.toUpperCase() + "!" }
```

### 2. Instance-Level MetaClass Modification

Adding methods to a specific instance only:

```groovy
def myString = "hello"
myString.metaClass.special = { "This is special: ${delegate}" }
// Only myString has the 'special' method
```

### 3. MetaClass Replacement

Replacing the entire metaClass with a custom one:

```groovy
def emc = new ExpandoMetaClass(MyClass)
emc.newMethod = { ... }
emc.initialize()
MyClass.metaClass = emc
```

### 4. Dynamic Method Names

Using GString to create dynamic method names:

```groovy
String.metaClass."${methodName}" = { ... }
```

---

## Signal Scoring

| Signal | Score per Occurrence |
|--------|---------------------|
| `metaClass_usage` | **+15 points** |

### Rationale for High Score

The +15 score (highest among all signals) reflects:

1. **High blast radius** - Changes affect all instances of a class
2. **Runtime unpredictability** - Behavior changes at runtime
3. **Testing difficulty** - Hard to achieve full coverage
4. **Debugging challenges** - Stack traces may be misleading
5. **Merge risk** - Multiple metaClass changes can conflict silently

---

## Examples from Test Files

### Example 1: Single MetaClass Addition (Test 02)

**Before:**
```groovy
class Calculator {
    int add(int a, int b) { return a + b }
}
```

**After:**
```groovy
class Calculator {
    int add(int a, int b) { return a + b }
}

// Adding a new method via metaClass
Calculator.metaClass.multiply = { int a, int b -> a * b }
```

**Signal Count:** 0 → 1 = **+1 occurrence** = **+15 points**

### Example 2: Multiple MetaClass Additions (Test 03)

**After:**
```groovy
String.metaClass.reverse = { delegate.reverse() }
StringUtils.metaClass.toLowerCase = { String s -> s?.toLowerCase() }
NumberUtils.metaClass.tripleIt = { int n -> n * 3 }
```

**Signal Count:** 0 → 3 = **+3 occurrences** = **+45 points**

### Example 3: MetaClass Removal (Test 09)

**Before:** 2 metaClass usages
**After:** 0 metaClass usages (refactored to proper class methods)

**Signal Count:** 2 → 0 = **-2 occurrences**

*Note: The signal is detected on the TARGET file, so if metaClass is removed, the signal score contribution is 0 for the target.*

---

## Impact on Change Complexity Score

The MetaClass signal contributes to the overall block change complexity:

```
BlockScore = TargetComplexity + DeltaScore + SignalScore
                                              ↑
                            metaClass_usage count × 15
```

### Example Calculation

For a block with:
- Target Complexity: 10.0
- Delta Score: 5.0
- MetaClass Usage: 2 occurrences

```
SignalScore = 2 × 15 = 30
BlockScore = 10.0 + 5.0 + 30.0 = 45.0 (High Risk)
```

---

## Best Practices

### When MetaClass is Acceptable

- **Testing/Mocking** - Temporarily modifying behavior in tests
- **Legacy Integration** - Adding methods to third-party classes
- **DSL Building** - Creating domain-specific languages

### When to Avoid MetaClass

- **Production code paths** - Prefer proper class extension
- **Performance-critical code** - MetaClass lookup has overhead
- **Multi-threaded code** - MetaClass modifications may not be thread-safe

### Refactoring Away from MetaClass

**Instead of:**
```groovy
String.metaClass.sanitize = { delegate.trim().toLowerCase() }
```

**Prefer:**
```groovy
class StringUtils {
    static String sanitize(String s) { s?.trim()?.toLowerCase() }
}
```

---

## Related Signals

| Signal | Relationship |
|--------|--------------|
| `dynamic_method_resolution` | Often used with metaClass |
| `closure_mutable_capture` | Closures assigned to metaClass |

---

## References

- [Groovy MetaClass Documentation](https://groovy-lang.org/metaprogramming.html)
- [ExpandoMetaClass](https://docs.groovy-lang.org/latest/html/api/groovy/lang/ExpandoMetaClass.html)
