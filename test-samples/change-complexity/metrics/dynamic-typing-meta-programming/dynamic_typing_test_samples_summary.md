# Dynamic Typing & Meta-Programming Test Samples

## Overview

This document describes the test samples created to validate the dynamic typing and meta-programming complexity metric. Each sample pair demonstrates specific aspects of Groovy's dynamic features.

## Test Sample Pairs

### 1. `dynamic_typing_before.groovy` → `dynamic_typing_after.groovy`

**Focus**: Basic dynamic typing with annotations and meta-programming introduction

**Before (Score: 3.0)**:
- 3 `def` usages (method return types)
- No annotations
- No meta-programming

**After (Score: 16.5)**:
- 8 `def` usages (parameters, variables, return types)
- 3 annotations (`@CompileStatic`, `@Override`, `@Deprecated`)
- 2 meta-programming methods (`methodMissing`, `propertyMissing`)

**Delta: +13.5** - Demonstrates transition from basic dynamic typing to comprehensive dynamic features.

---

### 2. `meta_programming_before.groovy` → `meta_programming_after.groovy`

**Focus**: Meta-programming capabilities and runtime behavior modification

**Before (Score: 0.0)**:
- Static typing throughout
- No dynamic features

**After (Score: 30.5)**:
- 15 `def` usages (extensive dynamic typing)
- 1 annotation (`@Singleton`)
- 7 meta-programming methods (`invokeMethod`, `methodMissing`, `getProperty`, `setProperty`)

**Delta: +30.5** - Shows transformation from static to heavily meta-programmed code.

---

### 3. `annotations_before.groovy` → `annotations_after.groovy`

**Focus**: Annotation usage and framework integration patterns

**Before (Score: 0.0)**:
- Static typing
- No annotations
- Simple method signatures

**After (Score: 21.0)**:
- 9 `def` usages (dynamic parameters and variables)
- 8 annotations (Spring framework annotations: `@Service`, `@Transactional`, `@Autowired`, `@Override`, `@Cacheable`, `@Deprecated`, `@PostConstruct`)
- No meta-programming methods

**Delta: +21.0** - Demonstrates annotation-heavy framework integration patterns.

---

### 4. `comprehensive_before.groovy` → `comprehensive_after.groovy`

**Focus**: Complete transformation showcasing all dynamic typing aspects

**Before (Score: 0.0)**:
- Pure static typing
- No dynamic features whatsoever

**After (Score: 48.0)**:
- 21 `def` usages (extensive dynamic typing throughout)
- 6 annotations (class and method level)
- 9 meta-programming methods (complete MOP implementation)

**Delta: +48.0** - Ultimate example showing maximum dynamic typing complexity.

## Metric Breakdown by Category

### Def Usage Examples (Weight: 1.0 each)
```groovy
def variable = "value"           // Variable declaration
def method(def param) { ... }    // Method parameter + return type
def result = computation()       // Local variable
```

### Annotation Examples (Weight: 1.5 each)
```groovy
@Override                        // Method annotation
@Component                       // Class annotation
@Transactional(rollbackFor = Exception.class)  // Parameterized annotation
@Autowired                       // Field annotation
```

### Meta-Programming Examples (Weight: 2.0 each)
```groovy
def methodMissing(String name, args)     // Missing method handling
def invokeMethod(String name, args)      // Method interception
def propertyMissing(String name)         // Missing property handling
def getProperty(String name)             // Property getter interception
def setProperty(String name, value)      // Property setter interception
```

## Expected API Results

When testing these samples through the change complexity API, you should see:

1. **Dynamic Typing Score**: Properly calculated based on the weighted sum
2. **Metric Deltas**: Accurate difference between before/after states
3. **Overall Complexity**: Appropriate weighting applied (default 0.25 for dynamic typing)

### Example API Response Structure:
```json
{
  "metrics_before": {
    "dynamic_typing_score": 3.0
  },
  "metrics_after": {
    "dynamic_typing_score": 16.5
  },
  "metric_deltas": {
    "dynamic_typing_score": 13.5
  },
  "weighted_deltas": {
    "dynamic_typing_score": 3.375  // 13.5 × 0.25
  }
}
```

## Usage Instructions

### Testing Individual Samples:
```bash
curl --location 'http://localhost:8000/api/v1/change-complexity' \
--form 'file_a=@"test-samples/change-complexity/dynamic_typing_before.groovy"' \
--form 'file_b=@"test-samples/change-complexity/dynamic_typing_after.groovy"' \
--form 'weight_dynamic_typing="0.25"' \
# ... other weights
```

### Direct Calculator Testing:
```python
from app.domain.metrics.dynamic_typing_calculator import DynamicTypingCalculator

calculator = DynamicTypingCalculator(language)
score = calculator.calculate(ast_node, source_bytes)
breakdown = calculator.get_dynamic_typing_breakdown(ast_node, source_bytes)
```

## Validation Checklist

When testing, verify:

- [ ] `def` keywords are correctly counted (including in parameters, return types, variables)
- [ ] Annotations are detected regardless of parameters or placement
- [ ] Meta-programming methods are identified by name
- [ ] Scores match expected calculations (count × weight)
- [ ] API integration works correctly with proper delta calculations
- [ ] Different complexity levels are properly differentiated

## Key Insights

These samples demonstrate that:

1. **Dynamic typing complexity grows exponentially** with feature adoption
2. **Meta-programming has the highest impact** on complexity scores
3. **Annotations represent moderate complexity** but are common in framework code
4. **Gradual adoption patterns** can be tracked through delta analysis
5. **Framework integration** typically increases dynamic typing scores significantly

The metric successfully captures the trade-off between Groovy's flexibility and code complexity/maintainability.