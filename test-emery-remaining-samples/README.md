# Emery DSL Remaining Test Samples

This directory contains additional test sample pairs for Emery DSL constructs that were identified as missing from the initial test coverage analysis.

## Overview

These test samples cover the **remaining 50% of Emery DSL constructs** that were not included in the original `test-emery-samples/` directory. They provide comprehensive coverage of advanced and extended Emery functionality.

## Test Sample Pairs (8 pairs, 16 files)

### 1. Data Table Operations (`emery_datatable_*`)
- **Tests**: `Emery.dataTable.read()`, table data iteration, filtering operations
- **Constructs Covered**: 
  - Data table access patterns
  - Row iteration and processing
  - Collection filtering operations

### 2. Integration Framework (`emery_integration_framework_*`)
- **Tests**: `Emery.integration.cif.*`, `Emery.integration.cis.*`, `Emery.integration.hookChain.*`
- **Constructs Covered**: 
  - CIF (Common Integration Framework) operations
  - CIS (Common Integration Service) operations
  - Hook chain processing
  - Complex payload structures

### 3. Public View Operations (`emery_pubview_*`)
- **Tests**: `Emery.pubview.fetch()`, `Emery.pubview.count()`, advanced query patterns
- **Constructs Covered**: 
  - Public view queries
  - `GroupCondition` usage
  - Complex query builders with multiple conditions

### 4. Advanced Query Operations (`emery_advanced_queries_*`)
- **Tests**: `CountQuery`, `DeleteQuery`, `UpdateQuery`, `insertAll()`, complex conditions
- **Constructs Covered**: 
  - All query types beyond basic `SelectQuery`
  - Bulk operations (`insertAll`)
  - Multi-field updates
  - Complex condition chaining

### 5. Extended Utilities (`emery_extended_utilities_*`)
- **Tests**: Additional util/ctx/string/date/lov/log methods not covered in basic tests
- **Constructs Covered**: 
  - **Utility Extensions**: `formattedUserFullName()`, `nextUniqueRowId()`
  - **Context Extensions**: `systemConfigurationParameter()`, `isSDOS()`, `isMDOS()`
  - **String Extensions**: `displayDecode()`, `split()`, `join()`
  - **Date Extensions**: `addMonths()`, `addYears()`
  - **LOV Extensions**: `getLovStoredValue()`
  - **Logging Extensions**: `warn()`, `error()`, `trace()`

### 6. Advanced Form Operations (`emery_advanced_form_*`)
- **Tests**: `getForm()`, `getLatestFormInstance()`, `getBlueprintCode()`, advanced field operations
- **Constructs Covered**: 
  - Form retrieval operations
  - Advanced field operations (`getField()`, `updateField()`)
  - Form lifecycle methods
  - Multiple form usage patterns

### 7. Advanced MDOS Operations (`emery_advanced_mdos_*`)
- **Tests**: `getUsersBasedOnActivityAndOrganization()`, multiple hierarchy access patterns
- **Constructs Covered**: 
  - Activity-based user queries
  - Multiple hierarchy access levels
  - Complex tuple handling
  - Advanced MDOS display value operations

### 8. Extended Testing Operations (`emery_extended_test_*`)
- **Tests**: `assertNull()`, `assertFalse()`, multiple delays, complex test scenarios
- **Constructs Covered**: 
  - All assertion types (`assertNull`, `assertFalse`)
  - Error condition testing
  - Multiple delay patterns
  - Comprehensive test scenarios

## Purpose

These samples were created to achieve **100% coverage** of all Emery DSL constructs identified in the `emery_language_analysis.md` file. They complement the original test samples to provide comprehensive testing of:

1. **Advanced API Coverage**: Extended methods in each Emery namespace
2. **Complex Patterns**: Advanced usage patterns and method combinations  
3. **Edge Cases**: Less common but important Emery constructs
4. **Integration Points**: Framework integration and external system connectivity

## Usage

These test samples can be used to:

1. **Verify Grammar Compatibility**: Ensure all Emery constructs parse correctly
2. **Test AST Diff Accuracy**: Validate diff detection for advanced patterns
3. **Performance Testing**: Test parser performance with complex Emery code
4. **Regression Testing**: Ensure changes don't break advanced Emery support

## Status

✅ **All samples are ready for testing** - Each pair represents realistic before/after scenarios with meaningful changes that test the AST diff tool's ability to detect and analyze modifications in advanced Emery DSL constructs.

## Combined Coverage

When combined with the original `test-emery-samples/` directory:
- **Total Test Pairs**: 16 pairs (32 files)
- **Emery Construct Coverage**: 100%
- **Namespace Coverage**: All 15+ Emery namespaces fully covered
- **Pattern Coverage**: All major Emery usage patterns included
