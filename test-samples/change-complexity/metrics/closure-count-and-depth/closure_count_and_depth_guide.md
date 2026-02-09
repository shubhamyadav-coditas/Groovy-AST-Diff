# Closure Count and Depth Metric in Groovy

## Overview

Closures are a powerful feature in Groovy that allow you to define blocks of code that can be passed around, stored in variables, and executed later. While they provide flexibility, closures also introduce complexity due to:

1. **Scope Capture**: Closures can capture and access variables from their enclosing scope
2. **Deferred Execution**: Code inside closures runs at a different time/context
3. **Nesting Complexity**: Closures can be nested within other closures
4. **Implicit Behavior**: Closures often have implicit parameters (`it`) and delegation

## What is a Closure in Groovy?

A closure is a block of code wrapped in curly braces `{ }` that can:
- Accept parameters
- Access variables from the enclosing scope
- Be assigned to variables
- Be passed as arguments to methods
- Be returned from methods

### Basic Closure Syntax

```groovy
// Simple closure with no parameters
def greet = { println "Hello" }

// Closure with explicit parameter
def greetPerson = { String name -> println "Hello, $name" }

// Closure with implicit 'it' parameter
def greetIt = { println "Hello, $it" }

// Multi-line closure
def process = { input ->
    def result = input.toUpperCase()
    println result
    return result
}
```

## How We Calculate Closure Complexity

### Formula

```
Closure Complexity = Closure Count + (0.5 × Max Nesting Depth)
```

### Components

| Component | Description | Weight |
|-----------|-------------|--------|
| **Closure Count** | Total number of closures in the code | 1.0 per closure |
| **Max Nesting Depth** | Deepest level of closure nesting | 0.5 per level |

### Why This Formula?

1. **Each closure adds complexity** (count): Every closure is a separate execution context
2. **Nesting multiplies complexity** (depth): Nested closures compound scope complexity
3. **Depth has lower weight (0.5)**: Nesting is important but count is primary driver

## Closure Types We Detect

### 1. Standalone Closures

Closures assigned to variables or used independently.

```groovy
// Assigned to variable
def myClosure = { println "Hello" }  // +1

// Immediately invoked
{ println "Hello" }()  // +1
```

### 2. Method Closures

Closures passed as arguments to methods, especially common Groovy methods.

```groovy
// Collection methods
list.each { println it }           // +1
list.find { it > 5 }               // +1
list.collect { it * 2 }            // +1
list.findAll { it % 2 == 0 }       // +1

// Sorting and grouping
list.sort { a, b -> a <=> b }      // +1
list.groupBy { it.category }       // +1

// Filtering and mapping
list.filter { it.active }          // +1
list.map { it.name }               // +1

// Aggregation
list.inject(0) { sum, item -> sum + item }  // +1
list.reduce { a, b -> a + b }      // +1
```

### 3. Nested Closures

Closures defined inside other closures.

```groovy
// Depth 0: outer closure
list.each { item ->                     // +1 (depth 0)
    // Depth 1: nested closure
    item.properties.each { prop ->      // +1 (depth 1)
        println prop
    }
}
// Total: 2 closures, max depth = 1
// Complexity = 2 + (0.5 × 1) = 2.5
```

### 4. Lambda-style Closures

Arrow notation closures.

```groovy
def add = { a, b -> a + b }        // +1
def filter = { x -> x > 0 }        // +1
```

## Nesting Depth Explained

### Depth 0 (Top-level)

```groovy
def closure = { println "Level 0" }  // Depth 0
```

### Depth 1 (One level nested)

```groovy
def outer = {                        // Depth 0
    def inner = { println "Level 1" } // Depth 1
}
```

### Depth 2 (Two levels nested)

```groovy
def outer = {                        // Depth 0
    def middle = {                   // Depth 1
        def inner = { println "Level 2" } // Depth 2
    }
}
```

### Depth 3+ (Deeply nested - High complexity)

```groovy
list.each { item ->                  // Depth 0
    item.children.each { child ->    // Depth 1
        child.attributes.each { attr -> // Depth 2
            attr.values.each { val ->   // Depth 3
                println val
            }
        }
    }
}
// 4 closures, max depth = 3
// Complexity = 4 + (0.5 × 3) = 5.5
```

## Common Closure Method Patterns

We specifically track closures used with these common Groovy methods:

| Category | Methods |
|----------|---------|
| **Iteration** | `each`, `forEach` |
| **Searching** | `find`, `findAll`, `any`, `every` |
| **Transformation** | `collect`, `map`, `filter`, `select`, `reject` |
| **Sorting** | `sort`, `groupBy` |
| **Aggregation** | `inject`, `fold`, `reduce` |
| **Utilities** | `withDefault` |

## Examples with Complexity Calculations

### Example 1: Simple Closures

```groovy
def greet = { println "Hello" }
def add = { a, b -> a + b }
```

**Calculation:**
- Closure count: 2
- Max nesting depth: 0
- **Complexity: 2 + (0.5 × 0) = 2.0**

### Example 2: Collection Processing

```groovy
def numbers = [1, 2, 3, 4, 5]
numbers.each { println it }
numbers.findAll { it % 2 == 0 }
numbers.collect { it * 2 }
```

**Calculation:**
- Closure count: 3
- Max nesting depth: 0
- **Complexity: 3 + (0.5 × 0) = 3.0**

### Example 3: Nested Processing

```groovy
def data = [[1, 2], [3, 4], [5, 6]]
data.each { row ->
    row.each { cell ->
        println cell
    }
}
```

**Calculation:**
- Closure count: 2 (outer each + inner each)
- Max nesting depth: 1 (inner is inside outer)
- **Complexity: 2 + (0.5 × 1) = 2.5**

### Example 4: Complex Nested Processing

```groovy
def process = { data ->
    data.each { item ->
        item.children.findAll { it.active }.each { child ->
            child.properties.collect { it.value }
        }
    }
}
```

**Calculation:**
- Closure count: 4 (process, each, findAll, each, collect)
- Max nesting depth: 3
- **Complexity: 5 + (0.5 × 3) = 6.5**

## Why Closure Complexity Matters

### 1. **Cognitive Load**
- Each closure creates a new mental context
- Nested closures require tracking multiple scopes

### 2. **Debugging Difficulty**
- Stack traces with closures are harder to read
- Variable shadowing can cause confusion

### 3. **Performance Considerations**
- Closures create objects at runtime
- Deeply nested closures may impact performance

### 4. **Testability**
- Closures may be harder to test in isolation
- Mocking becomes more complex

### 5. **Maintainability**
- Deeply nested code is harder to modify
- Refactoring nested closures requires care

## Best Practices

### 1. **Limit Nesting Depth**
```groovy
// Avoid (depth 3)
list.each { a ->
    a.items.each { b ->
        b.values.each { c ->
            process(c)
        }
    }
}

// Better: Extract to methods
list.each { processItems(it) }

def processItems(item) {
    item.items.each { processValues(it) }
}
```

### 2. **Name Complex Closures**
```groovy
// Avoid
list.findAll { it.active && it.date > now && it.type == 'A' }

// Better
def activeRecentTypeA = { item ->
    item.active && item.date > now && item.type == 'A'
}
list.findAll(activeRecentTypeA)
```

### 3. **Use Method References When Possible**
```groovy
// Closure
list.each { println it }

// Method reference (simpler)
list.each(this.&println)
```

### 4. **Keep Closures Small**
- Single responsibility
- Minimal logic
- Easy to understand at a glance

## Impact on Change Complexity

When analyzing code changes:

1. **Adding closures** increases complexity
2. **Increasing nesting depth** has compounding effect
3. **Flattening nested closures** reduces complexity
4. **Extracting closures to methods** can reduce perceived complexity

Higher closure complexity scores indicate code that may be:
- Harder to understand and maintain
- More prone to scope-related bugs
- Requiring more careful testing
- Potentially benefiting from refactoring

## Summary

| Aspect | Low Complexity | High Complexity |
|--------|----------------|-----------------|
| Closure Count | 0-2 | 5+ |
| Nesting Depth | 0-1 | 3+ |
| Total Score | < 3 | > 5 |
| Risk Level | Low | High |

The closure complexity metric helps identify code that may benefit from:
- Breaking out nested closures into separate methods
- Simplifying collection processing chains
- Reducing scope complexity
- Improving code readability