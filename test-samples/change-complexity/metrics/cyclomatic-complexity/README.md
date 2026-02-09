# Cyclomatic Complexity Test Samples

## Overview

These test samples demonstrate the **Cyclomatic Complexity (CC)** metric calculation. Cyclomatic complexity measures the number of linearly independent paths through a program's source code, helping to quantify code complexity and testability.

## What is Cyclomatic Complexity?

Cyclomatic Complexity, introduced by Thomas McCabe in 1976, is a software metric that measures the structural complexity of code. It counts the number of decision points in a program, where each decision point creates a new path through the code.

**Formula:**
```
CC = 1 + (number of decision points)
```

The base complexity is 1 (representing the simplest possible path), and each decision point adds 1 to the total.

## Decision Points Counted

| Decision Point Type | Description | Example |
|---------------------|-------------|---------|
| `if` statement | Conditional branch | `if (x) { ... }` |
| `while` loop | Loop with condition | `while (x) { ... }` |
| `do-while` loop | Post-condition loop | `do { ... } while (x)` |
| `for` loop | Traditional for loop | `for (i=0; i<n; i++) { ... }` |
| `for-in` loop | Groovy collection iteration | `for (item in list) { ... }` |
| `switch case` | Each case in switch | `case X:` |
| `catch` clause | Each catch block | `catch (Exception e) { ... }` |
| `&&` operator | Logical AND | `if (a && b)` |
| `||` operator | Logical OR | `if (a \|\| b)` |
| Ternary `?:` | Conditional expression | `x ? a : b` |
| Elvis `?:` | Null-coalescing | `x ?: default` |
| Safe navigation `?.` | Null-safe access | `obj?.property` |
| Spread safe `*?.` | Spread null-safe | `list*.property` |
| `assert` statement | Assertion check | `assert x > 0` |

## Test Sample Pairs

### 1. `minimal` - Basic If Statement

**Focus**: Minimum complexity change

| Aspect | Before | After |
|--------|--------|-------|
| CC | 1 | 2 |
| Delta | - | +1 |
| Tests | Base complexity | Single if statement |

---

### 2. `if_statements` - Multiple Conditionals

**Focus**: Multiple if statements impact

| Aspect | Before | After |
|--------|--------|-------|
| CC | 2 | 5 |
| Delta | - | +3 |
| Tests | Single if | Multiple if statements (4) |

---

### 3. `loops` - Loop Constructs

**Focus**: Different loop types

| Aspect | Before | After |
|--------|--------|-------|
| CC | 1 | 5 |
| Delta | - | +4 |
| Tests | No loops | for, for-in, while, do-while |

---

### 4. `switch` - Switch Statement

**Focus**: Switch with multiple cases

| Aspect | Before | After |
|--------|--------|-------|
| CC | 1 | 5 |
| Delta | - | +4 |
| Tests | No switch | Switch with 4 cases |

---

### 5. `try_catch` - Exception Handling

**Focus**: Multiple catch clauses

| Aspect | Before | After |
|--------|--------|-------|
| CC | 1 | 4 |
| Delta | - | +3 |
| Tests | No try-catch | try with 3 catch clauses |

---

### 6. `logical_operators` - Logical AND/OR

**Focus**: Compound boolean expressions

| Aspect | Before | After |
|--------|--------|-------|
| CC | 2 | 6 |
| Delta | - | +4 |
| Tests | Simple if | Complex boolean (2 &&, 2 ||) |

---

### 7. `ternary` - Ternary and Elvis Operators

**Focus**: Inline conditional expressions

| Aspect | Before | After |
|--------|--------|-------|
| CC | 2 | 5 |
| Delta | - | +3 |
| Tests | if-else | Ternary (? :), Elvis (?:), Nested ternary |

> Note: The nested ternary `a ? b : (c ? d : e)` counts as 2 ternary operators (outer + inner).

---

### 8. `safe_navigation` - Groovy Safe Navigation

**Focus**: Null-safe operators

| Aspect | Before | After |
|--------|--------|-------|
| CC | 4 | 5 |
| Delta | - | +1 |
| Tests | Explicit null checks | Safe navigation (?.) |
| Tests | Explicit null checks | Spread Safe navigation (?.) |


> Note: Safe navigation operators replace multiple if statements with implicit null checks.

---

### 9. `nested_conditions` - Deep Nesting

**Focus**: Deeply nested if statements

| Aspect | Before | After |
|--------|--------|-------|
| CC | 2 | 7 |
| Delta | - | +5 |
| Tests | Single if | 6 nested if statements |

---

### 10. `comprehensive` - All Complexity Types

**Focus**: Combination of all decision point types

| Aspect | Before | After |
|--------|--------|-------|
| CC | 3 | 19 |
| Delta | - | +16 |
| Tests | Simple loop + if | All complexity types combined |

---

### 11. `assert` - Assertion Statements

**Focus**: Groovy assert statements

| Aspect | Before | After |
|--------|--------|-------|
| CC | 1 | 4 |
| Delta | - | +3 |
| Tests | No assertions | 3 assert statements |

## Testing with API

```bash
curl --location 'http://localhost:8000/api/v1/change-complexity' \
--form 'file_a=@"test-samples/change-complexity/metrics/cyclomatic-complexity/loops_before.groovy"' \
--form 'file_b=@"test-samples/change-complexity/metrics/cyclomatic-complexity/loops_after.groovy"'
```

## Complexity Thresholds

| CC Value | Risk Level | Interpretation |
|----------|------------|----------------|
| 1-5 | Low | Simple, easy to test |
| 6-10 | Medium | Moderate complexity |
| 11-20 | High | Complex, harder to test |
| 21+ | Very High | Very complex, refactor recommended |

## Key Observations

1. **Base complexity is 1**: Every code path has at least one execution path
2. **Each `&&` and `||` adds 1**: Compound conditions significantly increase complexity
3. **Switch cases are additive**: Each case adds to the total, making large switches very complex
4. **Safe navigation reduces explicit checks**: Using `?.` instead of if-null checks can reduce CC
5. **Nested conditions multiply paths**: Deeply nested code has exponentially more paths
6. **Try-catch adds per clause**: Each catch block is a separate decision point

## Impact on Change Complexity

Cyclomatic Complexity has the **highest default weight (35%)** in the change complexity formula because:

- Higher CC means more paths to test
- More decision points increase cognitive load
- Complex logic is harder to maintain and debug
- Bugs are more likely to hide in complex code

## Best Practices

1. **Keep methods under CC 10**: Refactor when complexity grows
2. **Use early returns**: Reduces nesting depth
3. **Extract complex conditions**: Move compound booleans to named methods
4. **Prefer polymorphism over switches**: Large switch statements often indicate missing abstraction
5. **Use Groovy operators**: `?.`, `?:` can reduce explicit null checks while maintaining readability
