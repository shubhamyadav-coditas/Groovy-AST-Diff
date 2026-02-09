# Dynamic Typing & Meta-Programming Test Samples

## Overview

These test samples demonstrate the **Dynamic Typing & Meta-Programming** metric calculation. This metric measures the complexity introduced by Groovy's dynamic language features that bypass static type checking and enable runtime behavior modification.

## What is Measured

The metric counts occurrences of four categories of dynamic features:

| Category | Weight | Description |
|----------|--------|-------------|
| `def` keyword | 1.0 | Dynamic typing via `def` keyword |
| Annotations | 1.0 | All annotation usages (`@`) |
| Meta-programming methods | 1.0 | Runtime behavior interception methods |
| metaClass modifications | 1.0 | Runtime class structure modifications |

**Formula:**
```
Score = def_count + annotation_count + meta_programming_count + metaclass_count
```

## Features Detected

### 1. `def` Keyword Usage

Variables and method parameters/returns declared with `def` instead of explicit types.

```groovy
def value = "hello"           // Counted
def processItem(def item) {}  // Counted (return type + parameter = 2)
String name = "typed"         // NOT counted
```

### 2. Annotations

All annotation usages on classes, methods, fields, and parameters.

```groovy
@Service                      // Counted
@Override                     // Counted
@Transactional(rollbackFor = Exception.class)  // Counted
```

### 3. Meta-Programming Methods

Methods that intercept and modify runtime behavior:

| Method | Description |
|--------|-------------|
| `invokeMethod` | Intercepts all method calls |
| `methodMissing` | Called when method not found |
| `propertyMissing` | Called when property not found |
| `getProperty` | Intercepts property reads |
| `setProperty` | Intercepts property writes |
| `getMetaClass` | Gets object's meta class |
| `setMetaClass` | Sets object's meta class |

### 4. metaClass Modifications

Direct metaClass manipulation for runtime class modification:

| Pattern | Description |
|---------|-------------|
| `metaClass` | Direct metaClass access/modification |
| `ExpandoMetaClass` | Explicit ExpandoMetaClass usage |

```groovy
String.metaClass.shout = { -> delegate.toUpperCase() }  // Counted (metaClass)
new ExpandoMetaClass(String)  // Counted (ExpandoMetaClass)
```

## Test Sample Pairs

### Test 01: `def` Keyword Usage

**Focus**: Dynamic typing via `def` keyword

| File | def | annotations | meta | metaClass | Total |
|------|-----|-------------|------|-----------|-------|
| Before | 0 | 0 | 0 | 0 | 0 |
| After | 11 | 0 | 0 | 0 | 11 |
| **Delta** | +11 | 0 | 0 | 0 | **+11** |

---

### Test 02: Annotations

**Focus**: Annotation usage on classes and methods

| File | def | annotations | meta | metaClass | Total |
|------|-----|-------------|------|-----------|-------|
| Before | 0 | 0 | 0 | 0 | 0 |
| After | 0 | 8 | 0 | 0 | 8 |
| **Delta** | 0 | +8 | 0 | 0 | **+8** |

---

### Test 03: Meta-Programming Methods

**Focus**: invokeMethod, methodMissing, propertyMissing, getProperty, setProperty

| File | def | annotations | meta | metaClass | Total |
|------|-----|-------------|------|-----------|-------|
| Before | 0 | 0 | 0 | 0 | 0 |
| After | 0 | 0 | 9 | 0 | 9 |
| **Delta** | 0 | 0 | +9 | 0 | **+9** |

---

### Test 04: metaClass Modifications

**Focus**: metaClass and ExpandoMetaClass usage

| File | def | annotations | meta | metaClass | Total |
|------|-----|-------------|------|-----------|-------|
| Before | 0 | 0 | 0 | 0 | 0 |
| After | 0 | 0 | 0 | 7 | 7 |
| **Delta** | 0 | 0 | 0 | +7 | **+7** |

---

### Test 05: Combined Features

**Focus**: All four categories combined

| File | def | annotations | meta | metaClass | Total |
|------|-----|-------------|------|-----------|-------|
| Before | 0 | 0 | 0 | 0 | 0 |
| After | 14 | 5 | 4 | 1 | 24 |
| **Delta** | +14 | +5 | +4 | +1 | **+24** |

---

### Test 06: No Change (Control)

**Focus**: Identical files - delta should be zero

| File | def | annotations | meta | metaClass | Total |
|------|-----|-------------|------|-----------|-------|
| Before | 4 | 2 | 0 | 0 | 7 |
| After | 4 | 2 | 0 | 0 | 7 |
| **Delta** | 0 | 0 | 0 | 0 | **0** |

## Testing with API

```bash
curl --location 'http://localhost:8000/api/v1/change-complexity' \
--form 'file_a=@"test-samples/change-complexity/metrics/dynamic-typing-meta-programming/03_meta_programming_before.groovy"' \
--form 'file_b=@"test-samples/change-complexity/metrics/dynamic-typing-meta-programming/03_meta_programming_after.groovy"'
```

## Why These Features Matter

### Risk Factors

1. **Type Safety**: `def` bypasses compile-time type checking
2. **Predictability**: Meta-programming changes behavior at runtime
3. **Debugging**: Stack traces can be misleading with intercepted methods
4. **IDE Support**: Limited auto-completion and refactoring support
5. **Testing**: Harder to mock and verify behavior

### Impact on Code Review

Code with high dynamic typing scores requires:
- More careful review of type assumptions
- Additional testing for edge cases
- Documentation of expected types
- Verification of meta-programming side effects

## Best Practices

1. **Use @CompileStatic** when possible to enable static compilation
2. **Limit metaClass modifications** to initialization code
3. **Document meta-programming methods** thoroughly
4. **Prefer explicit types** for public APIs
5. **Use `def` judiciously** - prefer typed variables when type is known

## Impact on Change Complexity

Dynamic Typing has a **default weight of 25%** in the change complexity formula because:

- Dynamic code is harder to reason about during review
- Type-related bugs may not surface until runtime
- Meta-programming can have far-reaching effects
- Changes to meta-methods affect all intercepted calls
