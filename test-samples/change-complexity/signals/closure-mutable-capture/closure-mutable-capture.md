# Closure Mutable Capture Signal

## Overview

The **Closure Mutable Capture** signal detects when closures in Groovy code capture and potentially modify variables from an outer scope. This pattern is a common source of bugs, race conditions, and unexpected behavior in Groovy applications.

## What is a Closure Mutable Capture?

In Groovy, closures can access variables from their enclosing scope. When a closure captures a mutable variable (typically declared with `def`) and modifies it, this creates a side effect that can be difficult to reason about and test.

### Example

```groovy
def counter = 0  // Mutable outer variable

items.each { item ->
    counter++    // Closure captures and modifies 'counter'
}
```

In this example, the closure passed to `each` captures the `counter` variable from the outer scope and modifies it. While this works, it introduces complexity and potential issues.

## Why This Signal Matters

### 1. Unexpected Side Effects

Closures that modify outer state can produce side effects that are not immediately obvious from the code structure. This makes the code harder to understand and maintain.

### 2. Race Conditions

When closures are executed in parallel or asynchronous contexts (e.g., with `GPars`, `Thread.start`, or Jenkins pipeline `parallel`), captured mutable variables can lead to race conditions:

```groovy
def count = 0

items.each { item ->
    Thread.start {
        count++  // Race condition!
    }
}
```

### 3. Testing Complexity

Code with mutable captures is harder to test in isolation because the closure's behavior depends on and affects external state.

### 4. Debugging Difficulty

When bugs occur in code with mutable captures, tracking down the source can be challenging because the state changes are spread across multiple closure executions.

## How Detection Works

### Detection Algorithm

The detector uses a two-phase approach:

#### Phase 1: Collect Outer `def` Variables

```
1. Traverse the AST from the root
2. For each variable_declaration or assignment node:
   - Check if it contains a 'def' keyword
   - If yes, extract the identifier name
   - Add to the set of outer def variables
3. STOP traversal when entering a closure (don't collect variables defined inside closures)
```

#### Phase 2: Check Closures for Captures

```
1. Traverse the AST looking for closure nodes
2. For each closure found:
   - Get the text content of the closure
   - Check if any outer def variable names appear in the closure text
   - If found, increment the count (max 1 per closure)
3. Return total count of closures capturing mutable state
```

### AST Node Types Used

| Node Type | Purpose |
|-----------|---------|
| `closure` | Identifies closure blocks `{ ... }` |
| `variable_declaration` | Finds variable declarations |
| `assignment` | Finds assignment expressions |
| `def` | Identifies mutable variable declarations |
| `identifier` | Extracts variable names |

## Detection Patterns

### Pattern 1: Simple Variable Capture

```groovy
def total = 0  // Outer mutable variable

list.each { item ->
    total += item  // Captured and modified
}
```

**Detection**: Finds `total` in outer scope, detects reference in closure.

### Pattern 2: Collection Capture

```groovy
def results = []  // Outer mutable collection

items.each { item ->
    results.add(item * 2)  // Collection captured and modified
}
```

**Detection**: Finds `results` in outer scope, detects reference in closure.

### Pattern 3: Map Capture

```groovy
def cache = [:]  // Outer mutable map

keys.each { key ->
    cache[key] = computeValue(key)  // Map captured and modified
}
```

**Detection**: Finds `cache` in outer scope, detects reference in closure.

### Pattern 4: Multiple Variable Capture

```groovy
def sum = 0
def count = 0

items.each { item ->
    sum += item
    count++
}
```

**Detection**: Both `sum` and `count` found, closure references both. Counts as 1 closure capture.

### Pattern 5: Nested Closure Capture

```groovy
def total = 0

matrix.each { row ->
    row.each { cell ->
        total += cell  // Inner closure captures outer scope variable
    }
}
```

**Detection**: `total` captured by inner closure. May count multiple closures.

### Pattern 6: Callback Closure Capture

```groovy
def state = 'initial'

def callback = { result ->
    state = result  // Closure assigned to variable captures outer state
}
```

**Detection**: Closure literal captures `state`.

## What is NOT Detected

### 1. Closure-Local Variables

```groovy
list.each { item ->
    def localVar = item * 2  // Local to closure, not captured
    println(localVar)
}
```

### 2. Typed Variable Declarations

```groovy
int counter = 0  // Typed, not 'def'

list.each { item ->
    counter++  // Not detected (only 'def' variables are tracked)
}
```

**Note**: This is a current limitation. Typed variables can also be mutable.

### 3. Class Fields

```groovy
class MyClass {
    def counter = 0  // Field, not local variable
    
    void process() {
        items.each { counter++ }  // Not detected as local capture
    }
}
```

### 4. Read-Only Access

The current implementation counts any reference to an outer variable, even if it's only read (not modified). This is a conservative approach.

## Examples

### Example 1: Accumulator Pattern (Risky)

```groovy
// BEFORE: No mutable capture
def calculateSum(List<Integer> numbers) {
    return numbers.sum()
}

// AFTER: Mutable capture introduced
def calculateSum(List<Integer> numbers) {
    def total = 0
    numbers.each { num ->
        total += num  // Mutable capture!
    }
    return total
}
```

**Signal Count**: +1

### Example 2: Building Collections (Risky)

```groovy
// BEFORE: Functional approach
def filterValid(List items) {
    return items.findAll { it.isValid() }
}

// AFTER: Imperative with mutable capture
def filterValid(List items) {
    def valid = []
    def invalid = []
    
    items.each { item ->
        if (item.isValid()) {
            valid.add(item)    // Capture 1
        } else {
            invalid.add(item)  // Same closure, counts as 1
        }
    }
    
    return valid
}
```

**Signal Count**: +1 (one closure captures mutable state)

### Example 3: Async Context (High Risk)

```groovy
// BEFORE: Sequential
def processTasks(List tasks) {
    tasks.each { task ->
        println("Processing: ${task}")
    }
}

// AFTER: Parallel with mutable capture (DANGEROUS!)
def processTasks(List tasks) {
    def completed = 0
    def results = [:]
    
    tasks.each { task ->
        Thread.start {
            results[task] = process(task)
            completed++  // Race condition!
        }
    }
}
```

**Signal Count**: +1 (but this is particularly dangerous)

## Impact on Change Complexity Score

### Scoring

| Metric | Value |
|--------|-------|
| Base Score per Occurrence | **+8 points** |
| Risk Category | **Medium-High** |

### Why +8 Points?

The score of +8 reflects:

1. **Moderate-to-High Risk**: Mutable captures can cause subtle bugs
2. **Testing Difficulty**: Code becomes harder to test in isolation
3. **Concurrency Hazards**: Potential for race conditions
4. **Maintenance Burden**: Future changes may introduce bugs

### Score Calculation

```
Signal Score = Count of Closures with Mutable Capture × 8
```

### Example Scoring

| Scenario | Closures with Capture | Signal Score |
|----------|----------------------|--------------|
| No mutable captures | 0 | 0 |
| Single closure captures variable | 1 | 8 |
| Three closures each capture state | 3 | 24 |
| Nested closures (counts each) | 2 | 16 |

## Best Practices

### 1. Prefer Functional Approaches

```groovy
// Instead of:
def sum = 0
items.each { sum += it }

// Use:
def sum = items.sum()
```

### 2. Use inject/reduce for Accumulation

```groovy
// Instead of:
def result = []
items.each { result.add(transform(it)) }

// Use:
def result = items.collect { transform(it) }
```

### 3. Avoid Mutable Captures in Parallel Contexts

```groovy
// Instead of:
def count = 0
items.parallelStream().forEach { count++ }  // BROKEN!

// Use:
def count = items.parallelStream().count()
```

### 4. Use @Synchronized or Atomic Types for Shared State

```groovy
import java.util.concurrent.atomic.AtomicInteger

def count = new AtomicInteger(0)
items.each { count.incrementAndGet() }
```

## Limitations

1. **Only tracks `def` variables**: Typed variables (`int`, `String`, etc.) are not tracked
2. **Text-based matching**: May have false positives if variable name appears in a string
3. **No data flow analysis**: Cannot determine if variable is actually modified
4. **Counts any reference**: Read-only access is also counted (conservative)

## Related Signals

- **MetaClass Usage**: Also indicates dynamic/hard-to-predict behavior
- **Dynamic Method Resolution**: Similar category of runtime complexity

## References

- [Groovy Closures Documentation](https://groovy-lang.org/closures.html)
- [Closure Delegation and Owner](https://groovy-lang.org/closures.html#closure-owner)
- [GPars Concurrency Library](http://gpars.org/)
