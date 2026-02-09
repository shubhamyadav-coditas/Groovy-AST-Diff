# MetaClass Usage Signal - Test Results

This document explains each test case for the MetaClass Usage signal detection, including the expected and actual results.

---

## Test Summary

| Test | Description | Before | After | Delta | Status |
|------|-------------|--------|-------|-------|--------|
| 01 | Control Case - No metaClass | 0 | 0 | 0 | ✅ Pass |
| 02 | Single metaClass Addition | 0 | 1 | +1 | ✅ Pass |
| 03 | Multiple metaClass Additions | 0 | 3 | +3 | ✅ Pass |
| 04 | metaClass Property Assignment | 0 | 2 | +2 | ✅ Pass |
| 05 | metaClass Block Syntax | 0 | 1 | +1 | ✅ Pass |
| 06 | Modifying Existing metaClass | 1 | 3 | +2 | ✅ Pass |
| 07 | metaClass Inside Class Method | 0 | 1 | +1 | ✅ Pass |
| 08 | getMetaClass() Method Call | 0 | 2 | +2 | ✅ Pass |
| 09 | metaClass Removal | 2 | 0 | -2 | ✅ Pass |
| 10 | ExpandoMetaClass Pattern | 0 | 1 | +1 | ✅ Pass |

**Signal Score:** +15 points per metaClass occurrence

---

## Detailed Test Case Analysis

### Test 01: Control Case - No metaClass

**Purpose:** Establish baseline - ensure detector doesn't false-positive on regular code.

**Files:**
- `01_no_metaclass_before.groovy`
- `01_no_metaclass_after.groovy`

**Before Code:**
```groovy
class SimpleClass {
    String name
    int value
    
    String getName() { return name }
    void setValue(int val) { this.value = val }
}
```

**After Code:**
```groovy
class SimpleClass {
    String name
    int value
    String description  // Added field
    
    String getName() { return name }
    void setValue(int val) { this.value = val }
    String getDescription() { return description ?: "No description" }
}
```

**Change:** Added a field and method, but no metaClass usage.

| Metric | Value |
|--------|-------|
| Before Signal Count | 0 |
| After Signal Count | 0 |
| Delta | 0 |
| Signal Score Contribution | 0 |

**Verification:** ✅ Correctly identifies no metaClass usage in either file.

---

### Test 02: Single metaClass Addition

**Purpose:** Detect a single new metaClass modification.

**Files:**
- `02_single_metaclass_add_before.groovy`
- `02_single_metaclass_add_after.groovy`

**Change Introduced:**
```groovy
// Adding a new method via metaClass
Calculator.metaClass.multiply = { int a, int b ->
    a * b
}
```

**AST Pattern Detected:**
```
dotted_identifier: "Calculator.metaClass.multiply"
  └── Contains ".metaClass"
```

| Metric | Value |
|--------|-------|
| Before Signal Count | 0 |
| After Signal Count | 1 |
| Delta | +1 |
| Signal Score Contribution | +15 |

**Verification:** ✅ Correctly detects the single metaClass addition.

---

### Test 03: Multiple metaClass Additions

**Purpose:** Detect multiple metaClass modifications in a single file.

**Files:**
- `03_multiple_metaclass_before.groovy`
- `03_multiple_metaclass_after.groovy`

**Changes Introduced:**
```groovy
// First metaClass modification
String.metaClass.reverse = { delegate.reverse() }

// Second metaClass modification
StringUtils.metaClass.toLowerCase = { String s -> s?.toLowerCase() }

// Third metaClass modification
NumberUtils.metaClass.tripleIt = { int n -> n * 3 }
```

| Metric | Value |
|--------|-------|
| Before Signal Count | 0 |
| After Signal Count | 3 |
| Delta | +3 |
| Signal Score Contribution | +45 |

**Verification:** ✅ Correctly counts each metaClass modification separately.

---

### Test 04: metaClass Property Assignment

**Purpose:** Detect metaClass used to add properties (not just methods).

**Files:**
- `04_metaclass_property_before.groovy`
- `04_metaclass_property_after.groovy`

**Changes Introduced:**
```groovy
// Adding a property via metaClass
Person.metaClass.age = 0

// Adding a method that uses the new property
Person.metaClass.getAgeDescription = { "Age: ${delegate.age}" }
```

| Metric | Value |
|--------|-------|
| Before Signal Count | 0 |
| After Signal Count | 2 |
| Delta | +2 |
| Signal Score Contribution | +30 |

**Verification:** ✅ Correctly detects both property and method additions.

---

### Test 05: metaClass Block Syntax

**Purpose:** Detect metaClass block syntax (multiple methods in single block).

**Files:**
- `05_metaclass_block_before.groovy`
- `05_metaclass_block_after.groovy`

**Change Introduced:**
```groovy
// Using block syntax to add multiple methods at once
DataService.metaClass {
    fetchFilteredData = { String filter -> ... }
    countItems = { delegate.fetchData().size() }
    clearCache = { println "Cache cleared" }
}
```

| Metric | Value |
|--------|-------|
| Before Signal Count | 0 |
| After Signal Count | 1 |
| Delta | +1 |
| Signal Score Contribution | +15 |

**Note:** The block syntax is counted as a single metaClass access, even though it defines multiple methods. This is because there's only one `.metaClass` property access in the code.

**Verification:** ✅ Correctly identifies the block syntax as one metaClass usage.

---

### Test 06: Modifying Existing metaClass Definition

**Purpose:** Detect changes to existing metaClass and additions of new ones.

**Files:**
- `06_metaclass_modification_before.groovy`
- `06_metaclass_modification_after.groovy`

**Before (1 existing):**
```groovy
Logger.metaClass.debug = { String message -> println "[DEBUG] ${message}" }
```

**After (3 total - 1 modified + 2 new):**
```groovy
// Modified existing metaClass - now includes timestamp
Logger.metaClass.debug = { String message ->
    def timestamp = new Date().format("yyyy-MM-dd HH:mm:ss")
    println "[DEBUG ${timestamp}] ${message}"
}

// New metaClass addition
Logger.metaClass.error = { String message -> System.err.println "[ERROR] ${message}" }

// Another new metaClass addition
Logger.metaClass.warn = { String message -> println "[WARN] ${message}" }
```

| Metric | Value |
|--------|-------|
| Before Signal Count | 1 |
| After Signal Count | 3 |
| Delta | +2 |
| Signal Score Contribution (Target) | +45 |

**Verification:** ✅ Correctly counts all three metaClass usages in the target file.

---

### Test 07: metaClass Inside Class Method

**Purpose:** Detect metaClass usage inside a method (dynamic enhancement).

**Files:**
- `07_metaclass_in_class_before.groovy`
- `07_metaclass_in_class_after.groovy`

**Change Introduced:**
```groovy
// New method that uses metaClass dynamically
void enhancePlugin(String pluginName, Closure behavior) {
    // Dynamically add behavior to String class for this plugin
    String.metaClass."${pluginName}Action" = behavior
    println "Enhanced plugin: ${pluginName}"
}
```

| Metric | Value |
|--------|-------|
| Before Signal Count | 0 |
| After Signal Count | 1 |
| Delta | +1 |
| Signal Score Contribution | +15 |

**Note:** This is particularly risky because the metaClass modification happens at runtime when the method is called, making it harder to trace.

**Verification:** ✅ Correctly detects metaClass usage inside a method.

---

### Test 08: getMetaClass() Method Call

**Purpose:** Detect both getMetaClass() calls and .metaClass property access.

**Files:**
- `08_getmetaclass_before.groovy`
- `08_getmetaclass_after.groovy`

**Changes Introduced:**
```groovy
// Using getMetaClass()
void inspectMeta(Object obj) {
    def mc = obj.getMetaClass()  // Detection #1
    println "MetaClass: ${mc.theClass.name}"
}

// Modify via metaClass
void addMethod(Class clazz, String methodName, Closure impl) {
    clazz.metaClass."${methodName}" = impl  // Detection #2
}
```

| Metric | Value |
|--------|-------|
| Before Signal Count | 0 |
| After Signal Count | 2 |
| Delta | +2 |
| Signal Score Contribution | +30 |

**Verification:** ✅ Correctly detects both getMetaClass() call and .metaClass access.

---

### Test 09: metaClass Removal (Refactoring)

**Purpose:** Verify behavior when metaClass code is removed (refactored to proper methods).

**Files:**
- `09_metaclass_removal_before.groovy`
- `09_metaclass_removal_after.groovy`

**Before (metaClass usage):**
```groovy
Formatter.metaClass.formatUpper = { String text -> text?.toUpperCase()?.trim() }
Formatter.metaClass.formatLower = { String text -> text?.toLowerCase()?.trim() }
```

**After (proper class methods):**
```groovy
class Formatter {
    String formatUpper(String text) { return text?.toUpperCase()?.trim() }
    String formatLower(String text) { return text?.toLowerCase()?.trim() }
}
```

| Metric | Value |
|--------|-------|
| Before Signal Count | 2 |
| After Signal Count | 0 |
| Delta | -2 |

**Important Note on Deletion Behavior:**

When metaClass code is DELETED (not present in target), the signal detection rules are:
- For `deleted` blocks: signals are detected in the **SOURCE** (before) file
- This is intentional - we want to capture what risky code was removed

In this test case:
- 2 metaClass expressions are **deleted** (removed from source)
- The signal detector finds them in the source blocks being deleted
- The overall score reflects that risky code is being removed

**Integration Test Result:**
- Total metaClass count: 2 (from deleted source blocks)
- Signal score: 30.0 (2 × 15)
- Overall score: 39.70 (Medium risk)

**Verification:** ✅ Correctly detects metaClass in deleted source blocks.

---

### Test 10: ExpandoMetaClass Pattern

**Purpose:** Detect metaClass assignment when using ExpandoMetaClass.

**Files:**
- `10_expandometaclass_before.groovy`
- `10_expandometaclass_after.groovy`

**Change Introduced:**
```groovy
// Create ExpandoMetaClass and add methods
def emc = new ExpandoMetaClass(ApiClient)
emc.put = { ... }
emc.delete = { ... }
emc.initialize()

// Apply the new metaClass - THIS IS DETECTED
ApiClient.metaClass = emc
```

| Metric | Value |
|--------|-------|
| Before Signal Count | 0 |
| After Signal Count | 1 |
| Delta | +1 |
| Signal Score Contribution | +15 |

**Note:** The `ExpandoMetaClass` class name is NOT counted - only the `.metaClass` property access/assignment is counted.

**Verification:** ✅ Correctly detects only the metaClass assignment, not the class name.

---

## Edge Cases Covered

| Edge Case | Test | Result |
|-----------|------|--------|
| No metaClass at all | 01 | ✅ 0 detected |
| Single occurrence | 02 | ✅ 1 detected |
| Multiple occurrences | 03 | ✅ 3 detected |
| Property addition | 04 | ✅ Detected |
| Block syntax | 05 | ✅ 1 detected (not per-method) |
| Modification of existing | 06 | ✅ All counted |
| Inside class method | 07 | ✅ Detected |
| getMetaClass() call | 08 | ✅ Detected |
| Removal/refactoring | 09 | ✅ 0 in target |
| metaClass assignment | 10 | ✅ Detected |

---

## Running the Tests

To run the metaClass signal tests:

```bash
cd /path/to/Change-Complexity-with-AST-Diff

poetry run python -c "
from app.domain.signals.metaclass_signal_detector import MetaClassSignalDetector
from app.domain.groovy_ast_diff import GroovyASTDiff
import glob

differ = GroovyASTDiff()
parser = differ.parser
detector = MetaClassSignalDetector()

test_dir = 'test-samples/change-complexity/signals/metaclass-usage'

for before_file in sorted(glob.glob(f'{test_dir}/*_before.groovy')):
    after_file = before_file.replace('_before.groovy', '_after.groovy')
    
    with open(before_file, 'r') as f:
        before_code = f.read()
    with open(after_file, 'r') as f:
        after_code = f.read()
    
    before_tree = parser.parse(before_code.encode('utf-8'))
    after_tree = parser.parse(after_code.encode('utf-8'))
    
    before_count = detector.detect(before_tree.root_node, before_code.encode('utf-8'))
    after_count = detector.detect(after_tree.root_node, after_code.encode('utf-8'))
    
    print(f'{before_file.split(\"/\")[-1]}: {before_count} -> {after_count}')
"
```

---

## Conclusion

The MetaClass Usage signal detector correctly identifies all common patterns of metaClass modification in Groovy code. The test suite covers:

- **Positive cases** - Various ways to use metaClass
- **Negative cases** - Code without metaClass
- **Edge cases** - Block syntax, dynamic names, getMetaClass()
- **Refactoring cases** - Removal of metaClass usage

All tests pass with expected results, validating that the signal detection logic is working correctly.
