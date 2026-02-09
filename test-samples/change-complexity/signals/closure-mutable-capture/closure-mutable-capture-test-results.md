# Closure Mutable Capture Signal - Test Results

## Test Summary

| Test | Description | Before | After | Delta | Status |
|------|-------------|--------|-------|-------|--------|
| 01 | Control Case - No mutable captures | 0 | 0 | 0 | ✅ Pass |
| 02 | Single Closure Mutable Capture | 0 | 1 | +1 | ✅ Pass |
| 03 | Multiple Closures Capturing State | 0 | 3 | +3 | ✅ Pass |
| 04 | Nested Closures | 0 | 2 | +2 | ✅ Pass |
| 05 | Closure Modifying Outer Collection | 0 | 1 | +1 | ✅ Pass |
| 06 | Closure Modifying Outer Map | 0 | 1 | +1 | ✅ Pass |
| 07 | Async/Threaded Closure Capture | 0 | 2 | +2 | ✅ Pass |
| 08 | Callback Closure Pattern | 0 | 1 | +1 | ✅ Pass |
| 09 | Closure in Class Context | 0 | 2 | +2 | ✅ Pass |
| 10 | Removal of Mutable Capture | 1 | 0 | -1 | ✅ Pass |

**Signal Score:** +8 points per closure capturing mutable outer state

**Note:** The count represents the number of **closures** that capture mutable outer state, not the number of variables captured. A single closure capturing multiple outer variables counts as 1.

---

## Test Case Details

### Test 01: Control Case - No Mutable Captures

**Purpose:** Verify no false positives for code without mutable captures.

**Files:**
- `01_no_closure_capture_before.groovy`
- `01_no_closure_capture_after.groovy`

**Before Code:**

```groovy
class DataProcessor {
    void processItems(List items) {
        items.each { item ->
            println(item)
        }
    }
}
```

**After Code:**

```groovy
class DataProcessor {
    void processItems(List items) {
        items.each { item ->
            def localVar = item * 2  // Local to closure
            println(localVar)
        }
    }
}
```

**Changes Made:**
- Added `def localVar` inside closure (not an outer capture)

**Expected Behavior:**
- Before: 0 (no outer def captured)
- After: 0 (def inside closure is not outer capture)

| Metric | Value |
|--------|-------|
| Before Signal Count | 0 |
| After Signal Count | 0 |
| Delta | 0 |
| Signal Score Contribution | 0.0 |

---

### Test 02: Single Closure Mutable Capture

**Purpose:** Detect a single closure capturing mutable outer state.

**Files:**
- `02_single_capture_before.groovy`
- `02_single_capture_after.groovy`

**Before Code:**

```groovy
def processData() {
    def items = [1, 2, 3, 4, 5]
    def result = items.collect { it * 2 }
    println(result)
}
```

**After Code:**

```groovy
def processData() {
    def items = [1, 2, 3, 4, 5]
    def counter = 0  // Mutable outer state
    
    items.each { item ->
        counter = counter + item  // Captures and modifies 'counter'
    }
    
    println("Final count: ${counter}")
}
```

**Changes Made:**
- Added `def counter = 0` outside closure
- Closure captures and modifies `counter`

**Expected Behavior:**
- Before: 0 (collect closure doesn't capture outer def)
- After: 1 (each closure captures `counter`)

| Metric | Value |
|--------|-------|
| Before Signal Count | 0 |
| After Signal Count | 1 |
| Delta | +1 |
| Signal Score Contribution | 8.0 |

---

### Test 03: Multiple Closures Capturing State

**Purpose:** Detect multiple closures each capturing different mutable variables.

**Files:**
- `03_multiple_captures_before.groovy`
- `03_multiple_captures_after.groovy`

**Before Code:**

```groovy
def analyzeData() {
    def numbers = [10, 20, 30, 40, 50]
    def doubled = numbers.collect { it * 2 }
    def filtered = numbers.findAll { it > 20 }
}
```

**After Code:**

```groovy
def analyzeData() {
    def numbers = [10, 20, 30, 40, 50]
    def sum = 0
    def count = 0
    def maxValue = 0
    
    // Closure 1: captures 'sum'
    numbers.each { num -> sum = sum + num }
    
    // Closure 2: captures 'count'
    numbers.findAll { num -> count++; return num > 20 }
    
    // Closure 3: captures 'maxValue'
    numbers.each { num -> if (num > maxValue) { maxValue = num } }
}
```

**Changes Made:**
- Added 3 mutable variables: `sum`, `count`, `maxValue`
- Added 3 closures that each capture one variable

| Metric | Value |
|--------|-------|
| Before Signal Count | 0 |
| After Signal Count | 3 |
| Delta | +3 |
| Signal Score Contribution | 24.0 |

---

### Test 04: Nested Closures

**Purpose:** Detect nested closures capturing outer scope variables.

**Files:**
- `04_nested_closure_before.groovy`
- `04_nested_closure_after.groovy`

**Before Code:**

```groovy
def processMatrix() {
    def matrix = [[1, 2], [3, 4], [5, 6]]
    
    matrix.each { row ->
        row.each { cell ->
            println(cell)
        }
    }
}
```

**After Code:**

```groovy
def processMatrix() {
    def matrix = [[1, 2], [3, 4], [5, 6]]
    def total = 0
    def rowSums = []
    
    matrix.each { row ->
        def rowSum = 0
        row.each { cell ->
            total = total + cell  // Inner captures outer 'total'
        }
        rowSums.add(rowSum)
    }
}
```

**Changes Made:**
- Added `def total` and `def rowSums` outside closures
- Outer closure captures `rowSums`
- Inner closure captures `total` from outermost scope

| Metric | Value |
|--------|-------|
| Before Signal Count | 0 |
| After Signal Count | 2 |
| Delta | +2 |
| Signal Score Contribution | 16.0 |

---

### Test 05: Closure Modifying Outer Collection

**Purpose:** Detect closures that capture and modify outer collections.

**Files:**
- `05_closure_with_collection_before.groovy`
- `05_closure_with_collection_after.groovy`

**Before Code:**

```groovy
def buildReport() {
    def items = ['apple', 'banana', 'cherry']
    def report = items.collect { "Item: ${it}" }
}
```

**After Code:**

```groovy
def buildReport() {
    def items = ['apple', 'banana', 'cherry']
    def results = []
    def errors = []
    
    items.each { item ->
        if (item.length() > 5) {
            results.add("Valid: ${item}")
        } else {
            errors.add("Too short: ${item}")
        }
    }
}
```

**Changes Made:**
- Added `def results = []` and `def errors = []`
- One closure captures and modifies both collections

| Metric | Value |
|--------|-------|
| Before Signal Count | 0 |
| After Signal Count | 1 |
| Delta | +1 |
| Signal Score Contribution | 8.0 |

**Note:** Single closure captures multiple variables, counted as 1.

---

### Test 06: Closure Modifying Outer Map

**Purpose:** Detect closures that capture and modify outer maps.

**Files:**
- `06_closure_with_map_before.groovy`
- `06_closure_with_map_after.groovy`

**Before Code:**

```groovy
def categorizeItems() {
    def items = [[name: 'apple', type: 'fruit'], ...]
    def fruits = items.findAll { it.type == 'fruit' }
}
```

**After Code:**

```groovy
def categorizeItems() {
    def items = [[name: 'apple', type: 'fruit'], ...]
    def categories = [:]
    
    items.each { item ->
        def type = item.type
        if (!categories.containsKey(type)) {
            categories[type] = []
        }
        categories[type].add(item.name)
    }
}
```

**Changes Made:**
- Added `def categories = [:]` (mutable map)
- One closure captures and modifies the map

| Metric | Value |
|--------|-------|
| Before Signal Count | 0 |
| After Signal Count | 1 |
| Delta | +1 |
| Signal Score Contribution | 8.0 |

---

### Test 07: Async/Threaded Closure Capture

**Purpose:** Detect potentially dangerous mutable captures in async contexts.

**Files:**
- `07_async_closure_before.groovy`
- `07_async_closure_after.groovy`

**Before Code:**

```groovy
def processAsync() {
    def tasks = ['task1', 'task2', 'task3']
    tasks.each { task -> println("Processing: ${task}") }
}
```

**After Code:**

```groovy
def processAsync() {
    def tasks = ['task1', 'task2', 'task3']
    def completedCount = 0
    def results = [:]
    
    tasks.each { task ->
        Thread.start {
            results[task] = "completed"
            completedCount++  // Race condition!
        }
    }
}
```

**Changes Made:**
- Added `def completedCount` and `def results`
- Closure with `Thread.start` captures mutable state (race condition risk)

| Metric | Value |
|--------|-------|
| Before Signal Count | 0 |
| After Signal Count | 2 |
| Delta | +2 |
| Signal Score Contribution | 16.0 |

**Risk Note:** This pattern is particularly dangerous as it can cause race conditions.

---

### Test 08: Callback Closure Pattern

**Purpose:** Detect mutable captures in callback/closure patterns.

**Files:**
- `08_callback_closure_before.groovy`
- `08_callback_closure_after.groovy`

**Before Code:**

```groovy
def executeWithCallback() {
    // Direct callback without capturing outer state
    performOperation { result ->
        println("Callback received: ${result}")
    }
}
```

**After Code:**

```groovy
def executeWithCallback() {
    def state = 'initial'
    def callCount = 0
    
    def callback = { result ->
        callCount++
        state = result
    }
    
    performOperation(callback)
    performOperation(callback)
}
```

**Changes Made:**
- Added `def state` and `def callCount` outside closure
- Callback closure captures and modifies both

| Metric | Value |
|--------|-------|
| Before Signal Count | 0 |
| After Signal Count | 1 |
| Delta | +1 |
| Signal Score Contribution | 8.0 |

---

### Test 09: Closure in Class Context

**Purpose:** Detect mutable captures within class methods.

**Files:**
- `09_class_closure_before.groovy`
- `09_class_closure_after.groovy`

**Before Code:**

```groovy
class EventProcessor {
    void processEvents() {
        events.each { event -> println("Processing: ${event}") }
    }
}
```

**After Code:**

```groovy
class EventProcessor {
    Map<String, Integer> countByType() {
        def counts = [:]
        events.each { event ->
            def type = event.split(':')[0]
            counts[type] = (counts[type] ?: 0) + 1
        }
        return counts
    }
    
    void processWithStats() {
        def successCount = 0
        def failCount = 0
        events.each { event ->
            if (event.contains('success')) { successCount++ }
            else if (event.contains('fail')) { failCount++ }
        }
    }
}
```

**Changes Made:**
- Added `countByType()` method with `def counts` captured by closure (1 capture)
- Added `processWithStats()` with `def successCount` and `def failCount` captured (1 capture)

| Metric | Value |
|--------|-------|
| Before Signal Count | 0 |
| After Signal Count | 2 |
| Delta | +2 |
| Signal Score Contribution | 16.0 |

**Note:** Two new closures capture mutable state; the other closures in the class (in `processEvents` and `getFilteredEvents`) don't capture any outer `def` variables.

---

### Test 10: Removal of Mutable Capture

**Purpose:** Verify detection of mutable capture removal (refactoring to functional style).

**Files:**
- `10_removal_before.groovy`
- `10_removal_after.groovy`

**Before Code:**

```groovy
def aggregateData() {
    def items = [1, 2, 3, 4, 5]
    def runningSum = 0
    def runningMax = 0
    
    items.each { item ->
        runningSum += item
        if (item > runningMax) { runningMax = item }
    }
}
```

**After Code:**

```groovy
def aggregateData() {
    def items = [1, 2, 3, 4, 5]
    
    // Functional approach - no mutable capture
    def sum = items.sum()
    def max = items.max()
}
```

**Changes Made:**
- Removed mutable captures `runningSum` and `runningMax`
- Refactored to use functional `sum()` and `max()` methods

| Metric | Value |
|--------|-------|
| Before Signal Count | 1 |
| After Signal Count | 0 |
| Delta | -1 |
| Signal Score Contribution | 0.0 |

**Note:** Negative delta indicates improvement (removal of risky pattern). The before file has 1 closure that captures both `runningSum` and `runningMax`.

---

## Actual Test Results

All tests have been executed. Results are verified and match expected values.

| Test | Before | After | Delta | Signal Score | Status |
|------|--------|-------|-------|--------------|--------|
| 01 | 0 | 0 | 0 | 0.0 | ✅ Pass |
| 02 | 0 | 1 | +1 | 8.0 | ✅ Pass |
| 03 | 0 | 3 | +3 | 24.0 | ✅ Pass |
| 04 | 0 | 2 | +2 | 16.0 | ✅ Pass |
| 05 | 0 | 1 | +1 | 8.0 | ✅ Pass |
| 06 | 0 | 1 | +1 | 8.0 | ✅ Pass |
| 07 | 0 | 2 | +2 | 16.0 | ✅ Pass |
| 08 | 0 | 1 | +1 | 8.0 | ✅ Pass |
| 09 | 0 | 2 | +2 | 16.0 | ✅ Pass |
| 10 | 1 | 0 | -1 | 0.0 | ✅ Pass |

---

## Summary

All 10 test cases passed successfully. The Closure Mutable Capture signal detector correctly identifies:

1. **Control cases** (01): No false positives for normal code
2. **Single captures** (02): Basic mutable variable capture
3. **Multiple captures** (03): Multiple closures with multiple variables
4. **Nested closures** (04): Inner closures capturing outer scope
5. **Collection mutations** (05): Lists being modified via closures
6. **Map mutations** (06): Maps being modified via closures
7. **Async contexts** (07): Dangerous patterns with threads
8. **Callback patterns** (08): Closure variables capturing state
9. **Class methods** (09): Captures within class method context
10. **Removals** (10): Correctly tracks reduction in captures

### Coverage Summary

| Scenario | Covered |
|----------|---------|
| No mutable capture (control) | ✅ |
| Single closure capture | ✅ |
| Multiple closure captures | ✅ |
| Nested closure capture | ✅ |
| Collection modification | ✅ |
| Map modification | ✅ |
| Async/Thread context | ✅ |
| Callback pattern | ✅ |
| Class method context | ✅ |
| Capture removal (refactoring) | ✅ |

### Verification Checklist

- [x] All test files created and valid Groovy syntax
- [x] Before/After pairs properly structured
- [x] Expected counts verified against actual API results
- [x] Control case produces no false positives
- [x] Removal case correctly shows negative delta
- [x] Class context captures detected correctly

*Tests executed on: 2026-01-21*
