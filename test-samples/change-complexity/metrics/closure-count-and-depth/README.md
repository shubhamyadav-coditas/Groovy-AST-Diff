# Closure Count and Depth Test Samples

## Overview

These test samples demonstrate the closure complexity metric calculation. The metric measures **TRUE closure complexity** by counting only:
- Closures assigned to variables (`def fn = { }`)
- Closures passed as arguments to methods (`list.each { }`)
- Closures returned from methods

**NOT counted** (structural blocks):
- Class bodies
- Method bodies  
- Loop bodies (for, while, etc.)
- If/else bodies
- Try/catch bodies

## Formula

```
Closure Complexity = Closure Count + (0.5 × Max Nesting Depth)
```

## Test Sample Pairs

### 1. `simple_closures_before.groovy` → `simple_closures_after.groovy`

**Focus**: Basic closures vs procedural code

| Aspect | Before | After |
|--------|--------|-------|
| Purpose | No explicit closures | Simple closures with collection methods |
| Delta | - | +2.0 |

---

### 2. `nested_closures_before.groovy` → `nested_closures_after.groovy`

**Focus**: Single-level vs nested closures

| Aspect | Before | After |
|--------|--------|-------|
| Purpose | Single `.each {}` | Nested `.each { .each {} }` |
| Delta | - | +1.5 |

---

### 3. `deep_nesting_before.groovy` → `deep_nesting_after.groovy`

**Focus**: Impact of increasing nesting depth

| Aspect | Before | After |
|--------|--------|-------|
| Purpose | 2 levels of nesting | 4 levels of nesting |
| Delta | - | +3.0 |

---

### 4. `collection_methods_before.groovy` → `collection_methods_after.groovy`

**Focus**: Procedural loops vs Groovy collection methods

| Aspect | Before | After |
|--------|--------|-------|
| Purpose | Traditional for loops | `findAll`, `collect`, `find`, `any`, `every`, `groupBy` |
| Delta | - | +6 |

---

### 5. `mixed_complexity_before.groovy` → `mixed_complexity_after.groovy`

**Focus**: Combination of standalone closures, nested closures, and chained methods

| Aspect | Before | After |
|--------|--------|-------|
| Purpose | Simple closures | Standalone + nested + chained |
| Delta | - | +6.5 |

---

### 6. `standalone_closures_before.groovy` → `standalone_closures_after.groovy`

**Focus**: Closures assigned to variables

| Aspect | Before | After |
|--------|--------|-------|
| Purpose | No closures | Multiple closure variables |
| Delta | - | +4.5 |

## Testing with API

```bash
curl --location 'http://localhost:8000/api/v1/change-complexity' \
--form 'file_a=@"test-samples/closure-count-and-depth/nested_closures_before.groovy"' \
--form 'file_b=@"test-samples/closure-count-and-depth/nested_closures_after.groovy"' \
--form 'weight_closures="0.20"'
```

## Key Observations

1. **Only TRUE closures are counted**: Class bodies, method bodies, and loop bodies are NOT counted
2. **Nesting has multiplicative effect**: The 0.5 weight on depth adds complexity for nested closures
3. **Collection methods add complexity**: Each `.each`, `.findAll`, `.collect` adds a closure
4. **Chained methods are flat**: `data.findAll{}.collect{}.sort{}` has depth 0 for the chained closures
5. **Standalone closures**: Closures assigned to variables (`def fn = { }`) are counted

## Understanding the Results

The closure complexity metric provides insight into:
- How many block constructs exist in the code
- How deeply nested those blocks are
- The overall structural complexity of the code

Higher scores indicate:
- More block structures to understand
- Deeper nesting requiring more context tracking
- Potentially harder to maintain code