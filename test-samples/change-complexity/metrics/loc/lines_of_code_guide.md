# Lines of Code (LOC) Metric in Groovy

## Overview

The Lines of Code (LOC) metric measures the number of **significant lines** within a code block. It counts only lines that contain actual code, excluding blank lines and comment-only lines. This provides a more accurate measure of code volume than raw line counts.

## What We Count

### Counted (Significant Lines)
- Pure code lines
- Lines with code AND comments (mixed lines)

### Not Counted
- Blank lines (empty or whitespace only)
- Single-line comment lines (`// comment`)
- Block comment lines (`/* ... */`)

## Line Categories

| Category | Description | Example | Counted? |
|----------|-------------|---------|----------|
| **Code Line** | Contains only executable code | `int x = 5` | ✅ Yes |
| **Mixed Line** | Code + inline comment | `int x = 5 // initialize` | ✅ Yes |
| **Blank Line** | Empty or whitespace only | ` ` | ❌ No |
| **Comment Line** | Only comments | `// This is a comment` | ❌ No |
| **Block Comment** | Multi-line comment | `/* comment */` | ❌ No |

## Formula

```
Significant LOC = Code Lines + Mixed Lines
```

Or equivalently:

```
Significant LOC = Total Lines - Blank Lines - Comment-Only Lines
```

## Comment Handling

### Single-Line Comments (`//`)

```groovy
// This is a comment                    → NOT counted
int x = 5                               → Counted (code)
int y = 10 // inline comment            → Counted (mixed - has code before //)
```

### Block Comments (`/* */`)

```groovy
/* This is a                            → NOT counted
   multi-line                           → NOT counted
   block comment */                     → NOT counted

int code = 1 /* inline block */ + 2     → Counted (code with inline block comment)

int x = 5 /* start                      → Counted (code before comment)
  comment                               → NOT counted
  end */ int y = 6                      → Counted (code after comment)
```

## Examples with Calculations

### Example 1: Pure Code

```groovy
class Calculator {
    int add(int a, int b) {
        return a + b
    }
}
```

**Breakdown:**
- Line 1: `class Calculator {` → Code ✅
- Line 2: `int add(int a, int b) {` → Code ✅
- Line 3: `return a + b` → Code ✅
- Line 4: `}` → Code ✅
- Line 5: `}` → Code ✅

**Significant LOC: 5**

### Example 2: With Comments

```groovy
// Calculator class
class Calculator {
    
    // Add two numbers
    int add(int a, int b) {
        return a + b  // Return sum
    }
}
```

**Breakdown:**
- Line 1: `// Calculator class` → Comment ❌
- Line 2: `class Calculator {` → Code ✅
- Line 3: `` → Blank ❌
- Line 4: `// Add two numbers` → Comment ❌
- Line 5: `int add(int a, int b) {` → Code ✅
- Line 6: `return a + b  // Return sum` → Mixed ✅
- Line 7: `}` → Code ✅
- Line 8: `}` → Code ✅

**Significant LOC: 5** (2 code + 1 mixed + 2 braces)

### Example 3: Block Comments

```groovy
/*
 * This is a header comment
 * describing the class
 */
class DataProcessor {
    
    void process() {
        /* inline */ doWork()
    }
}
```

**Breakdown:**
- Lines 1-4: Block comment → Comment ❌ (4 lines)
- Line 5: `class DataProcessor {` → Code ✅
- Line 6: `` → Blank ❌
- Line 7: `void process() {` → Code ✅
- Line 8: `/* inline */ doWork()` → Code ✅ (has code after block comment)
- Line 9: `}` → Code ✅
- Line 10: `}` → Code ✅

**Significant LOC: 5**

### Example 4: Heavily Commented Code

```groovy
/**
 * Service class for user operations.
 * 
 * @author Developer
 * @version 1.0
 */
class UserService {
    
    // User repository
    private UserRepository repo
    
    /**
     * Find user by ID.
     * @param id User ID
     * @return User or null
     */
    User findById(Long id) {
        // Query database
        return repo.find(id)  // Return result
    }
    
    // End of class
}
```

**Breakdown:**
- Lines 1-6: Block comment → Comment ❌ (6 lines)
- Line 7: `class UserService {` → Code ✅
- Line 8: `` → Blank ❌
- Line 9: `// User repository` → Comment ❌
- Line 10: `private UserRepository repo` → Code ✅
- Line 11: `` → Blank ❌
- Lines 12-16: Block comment → Comment ❌ (5 lines)
- Line 17: `User findById(Long id) {` → Code ✅
- Line 18: `// Query database` → Comment ❌
- Line 19: `return repo.find(id)  // Return result` → Mixed ✅
- Line 20: `}` → Code ✅
- Line 21: `` → Blank ❌
- Line 22: `// End of class` → Comment ❌
- Line 23: `}` → Code ✅

**Total Lines: 23**
**Significant LOC: 6** (5 code + 1 mixed)

## AST-Based Line Counting

The LOC calculator uses AST node boundaries for precise counting:

```python
start_line = ast_node.start_point[0]  # 0-based line number
end_line = ast_node.end_point[0]      # 0-based line number
```

This means:
- For a class, LOC counts lines from `class {` to the closing `}`
- For a method, LOC counts only lines within that method
- Nested blocks are scoped to their specific range

## Impact on Change Complexity

The LOC metric is weighted at **0.05** (5%) in the default configuration because:

1. **Supporting Metric**: LOC supplements other complexity metrics
2. **Volume Indicator**: Shows the size of change, not complexity
3. **Context Provider**: Large LOC deltas may indicate significant structural changes

### Delta Calculation

```
LOC Delta = LOC_after - LOC_before
```

| Delta | Meaning |
|-------|---------|
| Positive | Code added (more lines) |
| Zero | Same line count (refactoring) |
| Negative | Code removed (less lines) |

## Detailed Breakdown

The calculator provides a detailed breakdown:

```python
{
    'total_lines': 15,           # All lines in range
    'blank_lines': 3,            # Empty lines
    'comment_lines': 5,          # Comment-only lines
    'code_lines': 6,             # Pure code lines
    'mixed_lines': 1,            # Code + comment lines
    'significant_lines': 7,      # code_lines + mixed_lines
    'line_range': '1-15',        # Line range (1-based display)
    'breakdown_percentage': {
        'blank': 20.0,
        'comments': 33.3,
        'code': 40.0,
        'mixed': 6.7
    }
}
```

## Best Practices

### 1. Keep Methods Short
- Smaller methods are easier to understand and test
- High LOC in a single method may indicate need for refactoring

### 2. Balance Comments and Code
- Too few comments: Code may be hard to understand
- Too many comments: May indicate overly complex code

### 3. Use LOC as a Guide
- LOC alone doesn't measure quality
- Combine with other metrics for full picture

## Summary

The Lines of Code metric provides:
- Accurate count of significant code lines
- Exclusion of blank lines and comments
- AST-based scoping for precise measurement
- Detailed breakdown for analysis

It serves as a volume indicator that, combined with other complexity metrics, gives a complete picture of code change impact.
