# Lines of Code (LOC) Test Samples

This folder contains test samples for the Lines of Code metric calculation.

## Test Cases

### 1. `minimal` - Adding code lines
- **Before**: Empty method body (LOC: 4)
- **After**: Method with 3 println statements (LOC: 7)
- **Delta**: +3
- **Tests**: Basic code addition

### 2. `comments` - Single-line comments impact
- **Before**: Pure code (LOC: 5)
- **After**: Same code with many comments (LOC: 5, includes 1 mixed line)
- **Delta**: +0
- **Tests**: Comments don't inflate LOC, only code-bearing lines count

### 3. `block_comments` - Block comment handling
- **Before**: Simple class (LOC: 5)
- **After**: Class with block comments (LOC: 5)
- **Delta**: +0
- **Tests**: Multi-line `/* */` comments are excluded

### 4. `blank_lines` - Blank line handling
- **Before**: Compact code (LOC: 5)
- **After**: Same code expanded with blank lines (LOC: 11)
- **Delta**: +6
- **Tests**: Blank lines don't count, but expanded braces do

### 5. `mixed` - Comprehensive example
- **Before**: Small class (LOC: 7)
- **After**: Expanded class with comments, blanks, new methods (LOC: 16)
- **Delta**: +9
- **Tests**: Combination of all line types

### 6. `code_removal` - Negative delta
- **Before**: Class with 5 methods (LOC: 17)
- **After**: Class with 1 method (LOC: 5)
- **Delta**: -12
- **Tests**: Negative LOC delta (code reduction)

## What Counts as Significant Lines

| Line Type | Counted? |
|-----------|----------|
| Pure code | ✅ Yes |
| Code + inline comment | ✅ Yes |
| Blank line | ❌ No |
| Single-line comment (`//`) | ❌ No |
| Block comment (`/* */`) | ❌ No |

## Formula

```
Significant LOC = Code Lines + Mixed Lines
             = Total Lines - Blank Lines - Comment-Only Lines
```
