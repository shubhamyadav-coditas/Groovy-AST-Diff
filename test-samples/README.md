# Groovy AST Diff - Comprehensive Test Samples

This directory contains comprehensive test samples for all Groovy constructs defined in `src/groovy_types.py`.

## Test File Pairs

Each pair of files (`*_before.groovy` and `*_after.groovy`) tests specific Groovy constructs and demonstrates all change types:

- **ADDED**: New constructs in the after file
- **DELETED**: Constructs removed from the before file  
- **MODIFIED**: Same construct with different content
- **MOVED**: Same construct in different position
- **MOVED_MODIFIED**: Different position AND content changed
- **UNCHANGED**: Identical constructs in both files

## Test Categories

### 1. Class-Related Constructs
**Files**: `class_before.groovy` ↔ `class_after.groovy`
- **class_definition**: Regular classes
- **interface_definition**: Interfaces
- **enum_definition**: Enumerations
- **trait_definition**: Groovy traits
- **annotation_definition**: Custom annotations

### 2. Method-Related Constructs  
**Files**: `method_before.groovy` ↔ `method_after.groovy`
- **function_definition**: Regular methods
- **constructor_definition**: Class constructors
- **static methods**: Static method definitions

### 3. Field/Property Constructs
**Files**: `field_before.groovy` ↔ `field_after.groovy`
- **field_definition**: Private/public fields
- **property_definition**: Groovy properties
- **static fields**: Static and final fields

### 4. Closure Constructs
**Files**: `closure_before.groovy` ↔ `closure_after.groovy`
- **closure**: Groovy closures `{ -> }`
- **closure_expression**: Closure expressions

### 5. Script-Level Constructs
**Files**: `script_before.groovy` ↔ `script_after.groovy`
- **script_method**: Top-level methods
- **script_variable**: Top-level variables

### 6. Import/Package Constructs
**Files**: `import_before.groovy` ↔ `import_after.groovy`
- **import_statement**: Import declarations
- **package_statement**: Package declarations

### 7. Expression/Statement Constructs
**Files**: `expression_before.groovy` ↔ `expression_after.groovy`
- **expression_statement**: Top-level expressions
- **statement**: Generic statements

### 8. Comment Constructs
**Files**: `comment_before.groovy` ↔ `comment_after.groovy`
- **comment**: Single-line and multi-line comments

### 9. Comprehensive Test
**Files**: `comprehensive_before.groovy` ↔ `comprehensive_after.groovy`
- **Mixed constructs**: Combines multiple construct types in one test
- **Real-world scenario**: Simulates actual code changes

## Usage

Run AST diff on any pair:

```bash
# Test specific construct
python3 groovy_ast_diff.py test-samples/class_before.groovy test-samples/class_after.groovy --output results.json

# Test comprehensive scenario
python3 groovy_ast_diff.py test-samples/comprehensive_before.groovy test-samples/comprehensive_after.groovy --simple
```

## Expected Results

Each test pair should demonstrate:
1. **Block-level detection**: Correct identification of construct types
2. **Change type classification**: Accurate ADDED/DELETED/MODIFIED/MOVED/MOVED_MODIFIED detection
3. **Statement-level analysis**: Detailed changes within modified constructs
4. **Position tracking**: Correct line number and position reporting
5. **Similarity scoring**: Appropriate Sørensen-Dice coefficient calculations

## Validation Checklist

- [ ] All BlockType enums are covered
- [ ] All ChangeType scenarios are tested
- [ ] Statement-level diffs work correctly
- [ ] Position changes are detected
- [ ] Similarity thresholds work as expected
- [ ] JSON output structure is correct
