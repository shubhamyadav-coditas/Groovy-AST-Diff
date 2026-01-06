# Test Samples for Groovy AST Diff

This directory contains organized test sample pairs to demonstrate each change type supported by the Groovy AST Diff tool.

## File Pairs by Change Type

### 1. ADDED Changes
- `added_before.groovy` → `added_after.groovy`
- **Demonstrates**: New top-level functions added to existing script
- **Expected Result**: `subtract()` and `getValue()` functions detected as ADDED

### 2. DELETED Changes  
- `deleted_before.groovy` → `deleted_after.groovy`
- **Demonstrates**: Top-level functions removed from existing script
- **Expected Result**: `calculatePerimeter()` and `printResult()` functions detected as DELETED

### 3. MODIFIED Changes
- `modified_before.groovy` → `modified_after.groovy` 
- **Demonstrates**: Same functions with different implementations
- **Expected Result**: `setText()` and `processText()` functions detected as MODIFIED

### 4. MOVED Changes
- `moved_before.groovy` → `moved_after.groovy`
- **Demonstrates**: Same functions reordered within script
- **Expected Result**: Functions detected as MOVED (same content, different positions)

### 5. MOVED_MODIFIED Changes
- `moved_modified_before.groovy` → `moved_modified_after.groovy`
- **Demonstrates**: Functions both moved and modified
- **Expected Result**: `error()` and `warn()` functions detected as MOVED_MODIFIED

### 6. UNCHANGED Changes
- `unchanged_before.groovy` → `unchanged_after.groovy`
- **Demonstrates**: Identical files with no changes
- **Expected Result**: All blocks detected as UNCHANGED

## Usage

Test individual change types:
```bash
python3 groovy_ast_diff.py test_samples/added_before.groovy test_samples/added_after.groovy --simple
python3 groovy_ast_diff.py test_samples/deleted_before.groovy test_samples/deleted_after.groovy --simple
python3 groovy_ast_diff.py test_samples/modified_before.groovy test_samples/modified_after.groovy --simple
python3 groovy_ast_diff.py test_samples/moved_before.groovy test_samples/moved_after.groovy --simple
python3 groovy_ast_diff.py test_samples/moved_modified_before.groovy test_samples/moved_modified_after.groovy --simple
python3 groovy_ast_diff.py test_samples/unchanged_before.groovy test_samples/unchanged_after.groovy --simple
```

Get JSON output:
```bash
python3 groovy_ast_diff.py test_samples/added_before.groovy test_samples/added_after.groovy --json
```

Save to file:
```bash
python3 groovy_ast_diff.py test_samples/modified_before.groovy test_samples/modified_after.groovy --output results.json
```
