# Comprehensive Test Samples

## Overview

These 5 test pairs are designed to comprehensively test all aspects of the Change Complexity calculation, including all 5 metrics and all 5 signals.

## Metrics Tested

| Metric | Weight | Description |
|--------|--------|-------------|
| Cyclomatic Complexity | 0.35 | Decision points: if, loops, switch, try-catch, &&, \|\|, ternary |
| Dynamic Typing Score | 0.25 | def usage, annotations, meta-programming methods, metaClass |
| Closure Complexity | 0.20 | Closure count + nesting depth |
| Imports Count | 0.15 | Number of import statements |
| Lines of Code | 0.05 | Significant lines of code |

## Signals Tested

| Signal | Score | Description |
|--------|-------|-------------|
| metaClass_usage | 15.0 | metaClass modifications, ExpandoMetaClass |
| dynamic_method_resolution | 10.0 | invokeMethod, methodMissing, propertyMissing, getProperty, setProperty |
| pipeline_step | 12.0 | sh, node, stage, pipeline, checkout, withCredentials, etc. |
| closure_mutable_capture | 8.0 | Closures capturing mutable outer variables |
| new_dependency | 10.0 | New imports for kafka, mongodb, redis, elasticsearch, etc. |

## Test Cases

### Test 01: Low Complexity

**Focus**: Minimal change with low impact

| Aspect | Before | After | Expected |
|--------|--------|-------|----------|
| Cyclomatic | 1 | 2 | +1 (added if) |
| Dynamic Typing | 0 | 0 | 0 |
| Closures | 0 | 0 | 0 |
| Imports | 0 | 0 | 0 |
| LOC | ~10 | ~15 | +5 |
| Signals | 0 | 0 | 0 |
| Risk Level | - | - | **Low** |

---

### Test 02: Medium Metrics

**Focus**: All 5 metrics with moderate values

| Aspect | Before | After | Expected |
|--------|--------|-------|----------|
| Cyclomatic | ~2 | ~6 | +4 (if, for, &&) |
| Dynamic Typing | 0 | ~10 | +10 (def, annotations) |
| Closures | 0 | ~3 | +3 (findAll, collect) |
| Imports | 1 | 3 | +2 |
| LOC | ~15 | ~40 | +25 |
| Signals | 0 | 0 | 0 |
| Risk Level | - | - | **Medium** |

---

### Test 03: All Signals

**Focus**: Triggers all 5 signals

| Signal | Before | After | Expected |
|--------|--------|-------|----------|
| metaClass_usage | 0 | 1 | Score: 15 |
| dynamic_method_resolution | 0 | 2 | Score: 20 |
| pipeline_step | 0 | 4 | Score: 48 |
| closure_mutable_capture | 0 | 1 | Score: 8 |
| new_dependency | 0 | 1 | Score: 10 |
| **Total Signal Score** | 0 | - | **~101** |
| Risk Level | - | - | **High** |

---

### Test 04: High Complexity

**Focus**: High values for all metrics and multiple signals

| Aspect | Before | After | Expected |
|--------|--------|-------|----------|
| Cyclomatic | ~3 | ~20+ | Very high (nested if, switch, try-catch, loops, &&, \|\|) |
| Dynamic Typing | ~5 | ~25+ | High (many def, annotations, meta-programming) |
| Closures | 0 | ~10+ | High (nested closures, chained methods) |
| Imports | 2 | 6 | +4 |
| LOC | ~20 | ~130+ | Very high |
| metaClass_usage | 0 | 2 | Score: 30 |
| dynamic_method_resolution | 0 | 4 | Score: 40 |
| pipeline_step | 0 | 8+ | Score: 96+ |
| closure_mutable_capture | 0 | 2+ | Score: 16+ |
| new_dependency | 0 | 1 | Score: 10 |
| Risk Level | - | - | **Very High** |

---

### Test 05: Extreme Complexity

**Focus**: Maximum complexity - stress test all features

| Aspect | Before | After | Expected |
|--------|--------|-------|----------|
| Cyclomatic | 1 | 30+ | Maximum (all decision types) |
| Dynamic Typing | 0 | 40+ | Maximum (many def, annotations, all meta methods) |
| Closures | 0 | 15+ | Maximum (deeply nested, many closures) |
| Imports | 0 | 15 | Many new imports |
| LOC | ~8 | ~250+ | Very large delta |
| metaClass_usage | 0 | 8+ | Many metaClass modifications |
| dynamic_method_resolution | 0 | 8+ | All resolution methods |
| pipeline_step | 0 | 15+ | Full pipeline with all steps |
| closure_mutable_capture | 0 | 5+ | Multiple mutable captures |
| new_dependency | 0 | 5+ | Multiple new infra deps |
| Risk Level | - | - | **Critical** |

## Testing Commands

### Test 01: Low Complexity
```bash
curl --location 'http://localhost:8000/api/v1/change-complexity' \
--form 'file_a=@"test-samples/change-complexity/comprehensive/01_low_complexity_before.groovy"' \
--form 'file_b=@"test-samples/change-complexity/comprehensive/01_low_complexity_after.groovy"'
```

### Test 02: Medium Metrics
```bash
curl --location 'http://localhost:8000/api/v1/change-complexity' \
--form 'file_a=@"test-samples/change-complexity/comprehensive/02_medium_metrics_before.groovy"' \
--form 'file_b=@"test-samples/change-complexity/comprehensive/02_medium_metrics_after.groovy"'
```

### Test 03: All Signals
```bash
curl --location 'http://localhost:8000/api/v1/change-complexity' \
--form 'file_a=@"test-samples/change-complexity/comprehensive/03_all_signals_before.groovy"' \
--form 'file_b=@"test-samples/change-complexity/comprehensive/03_all_signals_after.groovy"'
```

### Test 04: High Complexity
```bash
curl --location 'http://localhost:8000/api/v1/change-complexity' \
--form 'file_a=@"test-samples/change-complexity/comprehensive/04_high_complexity_before.groovy"' \
--form 'file_b=@"test-samples/change-complexity/comprehensive/04_high_complexity_after.groovy"'
```

### Test 05: Extreme Complexity
```bash
curl --location 'http://localhost:8000/api/v1/change-complexity' \
--form 'file_a=@"test-samples/change-complexity/comprehensive/05_extreme_before.groovy"' \
--form 'file_b=@"test-samples/change-complexity/comprehensive/05_extreme_after.groovy"'
```

## Expected Risk Levels

| Test | Expected Risk | Overall Score Range |
|------|---------------|---------------------|
| 01 | Low | 0 - 15 |
| 02 | Medium | 15 - 40 |
| 03 | High | 50 - 120 |
| 04 | Very High | 100 - 200 |
| 05 | Critical | 200+ |

## Formula Reference

```
Block Change Complexity = Target Complexity + Delta Score + Signal Score

Where:
- Target Complexity = Σ(metric_value × weight)
- Delta Score = max(0, target - source) × 1.6
- Signal Score = Σ(signal_count × signal_score)

Overall Score = Σ(Block Change Complexity) for all changed blocks
```
