# Emery Language Analysis

## Overview

Emery is a domain-specific language (DSL) implemented within Groovy that extends standard Groovy syntax with custom constructs for enterprise application development. This document provides a comprehensive analysis of Emery-specific language constructs identified from the AppStudio codebase.

## Core Emery DSL Structure

### 1. Main Emery Object

The central DSL entry point is the `Emery` class which provides a hierarchical API structure:

```groovy
Emery.<namespace>.<method>(<parameters>)
```

### 2. Primary Namespaces

#### 2.1 Form Operations (`Emery.form`)
- **Purpose**: Form creation, retrieval, and manipulation
- **Key Methods**:
  - `formId(String formName)` - Get form ID from name
  - `formName(long formId)` - Get form name from ID  
  - `workFlowCode(String formName)` - Get workflow code
  - `getBlueprintCode(String formName)` - Get blueprint code
  - `newForm(String formName)` - Create new form instance
  - `getForm(String formName, long pid, long iid)` - Retrieve form data
  - `getLatestFormInstance(String formName, long pid)` - Get latest form instance

#### 2.2 Utility Operations (`Emery.util`)
- **Purpose**: General utility functions
- **Key Methods**:
  - `getUserId(String userName)` - Get user ID from username
  - `userFullName(int userId)` - Get user full name details
  - `formattedUserFullName(int userId)` - Get formatted user name
  - `nextSequence(String sequenceName)` - Generate sequence numbers
  - `nextUniqueRowId()` - Generate unique row IDs
  - `fetchLocales(LocaleType localeType)` - Get locale information

#### 2.3 Context Operations (`Emery.ctx`)
- **Purpose**: Configuration and system context
- **Key Methods**:
  - `configurationParameter(String category, String name)` - Get config parameters
  - `systemConfigurationParameter(String name)` - Get system config
  - `currentUser()` - Get current user ID
  - `isSDOS()` - Check if Single Dimension Organization Structure
  - `isMDOS()` - Check if Multi Dimension Organization Structure

#### 2.4 Data Access (`Emery.dataobject`)
- **Purpose**: Data object operations and queries
- **Key Methods**:
  - `newInstance(String name)` - Create new data object
  - `fetch(SelectQuery query)` - Execute select queries
  - `count(CountQuery query)` - Count records
  - `delete(DeleteQuery query)` - Delete records
  - `update(UpdateQuery query)` - Update records
  - `insertAll(List<DataObject> dataObjectList)` - Bulk insert

#### 2.5 Data Table Operations (`Emery.dataTable`)
- **Purpose**: Data table access
- **Key Methods**:
  - `read(String dataTableName)` - Read data table records

#### 2.6 List of Values (`Emery.lov`)
- **Purpose**: LOV (List of Values) operations
- **Key Methods**:
  - `getLovDetailsForLovName(String lovName, int localeId)`
  - `getLovDisplayValue(String lovName, Integer localeId, String storedValue)`
  - `getLovStoredValue(String lovName, Integer localeId, String displayValue)`

#### 2.7 Logging (`Emery.log`)
- **Purpose**: Logging operations
- **Key Methods**:
  - `info(String format, Object... args)`
  - `warn(String format, Object... args)`
  - `debug(String format, Object... args)`
  - `error(String format, Object... args)`
  - `trace(String format, Object... args)`

#### 2.8 String Utilities (`Emery.stringUtil`)
- **Purpose**: String manipulation
- **Key Methods**:
  - `displayEncode(String value)` - Encode strings with commas/quotes
  - `displayDecode(String value)` - Decode encoded strings
  - `joinForDisplay(List<String> values)` - Join values for display
  - `split(String value, String delimiter)` - Split strings
  - `join(String[] values, String delimiter)` - Join string arrays

#### 2.9 Date Utilities (`Emery.dateUtil`)
- **Purpose**: Date manipulation and formatting
- **Key Methods**:
  - `addDays(Date date, int days)` - Add days to date
  - `addMonths(Date date, int months)` - Add months to date
  - `addYears(Date date, int years)` - Add years to date
  - `isSameDate(Date date1, Date date2)` - Compare dates
  - `convertDateToString(Date date, String format)` - Format dates

#### 2.10 Integration (`Emery.integration`)
- **Purpose**: External system integration
- **Sub-namespaces**:
  - `Emery.integration.cif` - CIF (Common Integration Framework)
  - `Emery.integration.cis` - CIS (Common Integration Service)
  - `Emery.integration.hookChain` - Hook chain processing

#### 2.11 MDOS Operations (`Emery.mdos`)
- **Purpose**: Multi-Dimensional Organization Structure operations
- **Key Methods**:
  - `getUsersBasedOnActivityAndOrganization(List<String> tupleIds, List<String> activityNames, HierarchyAccess accessLevel)`
  - `getUsersBasedOnRole(List<String> tupleIds, List<String> roleIds, HierarchyAccess accessLevel)`
  - `getMdosDisplayValuesClob(String storedValue, String delimiter, String displayValueDelimiter, int localeId)`

#### 2.12 Email Operations (`Emery.email`)
- **Purpose**: Email sending functionality
- **Key Methods**:
  - `sendEmail(String templateName, String module, String messageSender, List<String> toRecipients, ...)`

#### 2.13 Master Table Operations (`Emery.masterTable`)
- **Purpose**: Master table management
- **Key Methods**:
  - `updateInstanceId(String formName, long instanceId, String objectIdFieldName, String objectId)`

#### 2.14 Public View Operations (`Emery.pubview`)
- **Purpose**: Public view data access
- **Key Methods**:
  - `fetch(SelectQuery query)` - Execute select queries on public views
  - `count(CountQuery query)` - Count records in public views

#### 2.15 Test Operations (`Emery.test`)
- **Purpose**: Testing and assertion utilities
- **Key Methods**:
  - `assertEquals(def lhs, def rhs)` - Assert equality
  - `assertNotNull(def value)` - Assert not null
  - `assertNull(def value)` - Assert null
  - `assertTrue(boolean condition)` - Assert true
  - `assertFalse(boolean condition)` - Assert false
  - `delay(long duration)` - Introduce delay in tests

## Custom Data Types and Classes

### 1. Form-Related Classes
- `Form` - Abstract base class for form objects
- `MultiRow` - Abstract class for multi-row regions
- `AbstractRow` - Base class for row objects

### 2. Data Access Classes
- `DataObject` - Abstract base class for data objects
- `DataObjectRegion` - Multi-row regions in data objects
- `AbstractRegionRow` - Rows within data object regions

### 3. Query Classes
- `SelectQuery` - Query builder for select operations
- `CountQuery` - Query builder for count operations
- `DeleteQuery` - Query builder for delete operations
- `UpdateQuery` - Query builder for update operations
- `Condition` - Query condition builder
- `SortCondition` - Sorting specifications
- `GroupCondition` - Grouping specifications

### 4. Response Classes
- `SelectQueryResponse` - Response from select queries
- `FormResponse` - Response from form operations
- `EmeryFormResponse` - Enhanced form response

## Special Syntax Patterns

### 1. Form Field Access
```groovy
F.fieldName = value              // Set field value
F.multiRowName.newRow()          // Create new row
F.multiRowName.rows << row       // Add row to multi-row
F.getField("fieldName")          // Get field value
F.process_instance_id            // Access form properties
```

### 2. Data Object Creation and Manipulation
```groovy
DataObject dataobject = Emery.dataobject.newInstance("OBJECT_NAME")
dataobject.fieldName = value
dataobject.save()
```

### 3. Query Builder Pattern
```groovy
SelectQuery query = new SelectQuery.Builder("OBJECT_NAME")
    .addCondition(Condition.eq("FIELD", "value"))
    .addSortCondition(SortCondition.asc("FIELD"))
    .limit(10)
    .build()
```

### 4. Multi-Row and Region Access
```groovy
def region = dataobject.regionAcronym
def row = region.newRow()
row.fieldName = value
region.addRow(row)
```

### 5. Context Variables and Form Declaration
```groovy
use("FORM_NAME")                 // Declare form usage
useDataObject("OBJECT_NAME")     // Declare data object usage
FORM_NAME formObj = Emery.form.newForm("FORM_NAME")  // Create typed form object
```

### 6. Context Variables
- `CONTEXT` - Global context map
- `CURRENT_STAGE` - Current workflow stage
- `TARGET_STAGE` - Target workflow stage
- `PROCESS_CODE` - Process code
- `TRANSITION_CODE` - Transition code

## Import Statements

### Standard Emery Imports
```groovy
import com.metricstream.appstudio.dsl.engine.domain.Form
import com.metricstream.appstudio.dsl.engine.domain.MultiRow
import com.metricstream.appstudio.dsl.engine.domain.AbstractRow
import com.metricstream.appstudio.emery.dataaccess.domain.model.DataObject
import com.metricstream.appstudio.emery.dataaccess.query.type.SelectQuery
import com.metricstream.appstudio.emery.dataaccess.query.type.CountQuery
import com.metricstream.appstudio.emery.dataaccess.query.type.DeleteQuery
import com.metricstream.appstudio.emery.dataaccess.query.model.Condition
import com.metricstream.appstudio.emery.dataaccess.query.model.SortCondition
```

### Utility Imports
```groovy
import com.metricstream.appstudio.constants.LocaleType
import com.metricstream.appstudio.dsl.engine.domain.InputType
import com.metricstream.appstudio.dsl.engine.domain.Violation
import com.metricstream.systemi.services.mdos.enums.HierarchyAccess
```

## Special Keywords and Identifiers

### 1. Reserved Keywords
- `useDataObject("OBJECT_NAME")` - Declare data object usage
- `use("FORM_NAME")` - Declare form usage
- `F` - Form object reference
- `Emery` - Main DSL entry point

### 2. Predefined Variables
- `CONTEXT` - Global context map
- `CURRENT_STAGE` - Current workflow stage
- `TARGET_STAGE` - Target workflow stage
- `PROCESS_CODE` - Process code
- `TRANSITION_CODE` - Transition code
- `CURRENT_USER` - Current user context
- `ACTION` - Action context for workflow actions

### 3. Method Patterns
- `.newRow()` - Create new row in multi-row
- `.addRow(row)` - Add row to multi-row
- `.allRows()` - Get all rows from multi-row
- `.save()` - Persist object
- `.submit()` - Submit form
- `.validate()` - Validate object
- `.updateField(fieldName, value)` - Update field value
- `.getField(fieldName)` - Get field value
- `.nextProcessInstanceId()` - Generate process instance ID
- `.nextInstanceId()` - Generate instance ID

## Configuration and Constants

### 1. Configuration Parameter Access
```groovy
Emery.ctx.configurationParameter("CATEGORY", "PARAMETER_NAME")
Emery.ctx.systemConfigurationParameter("PARAMETER_NAME")
```

### 2. Locale Types
- `LocaleType.ALL`
- `LocaleType.ENABLED`

### 3. Input Types
- `InputType.XML`
- `InputType.JSON`

### 4. Hierarchy Access Types
- `HierarchyAccess.FLOW_UP`
- `HierarchyAccess.FLOW_DOWN`
- `HierarchyAccess.FLAT`
- `HierarchyAccess.EXACT`

## Error Handling

### Exception Types
- `EmeryException` - General Emery exceptions
- `DataAccessException` - Data access related exceptions
- `ServiceException` - Service layer exceptions
- `CifException` - Integration framework exceptions

## Additional Emery Patterns

### 1. Typed Form Objects
```groovy
use("MS_ATD_NEW_FORM")
MS_ATD_NEW_FORM formObj = Emery.form.newForm("MS_ATD_NEW_FORM")
formObj.field1 = "value"
formObj.save()
formObj.submit()
```

### 2. Mixed Java/Groovy API Usage
```groovy
// Groovy-style API
def row = F.multiRow.newRow()
F.multiRow.rows << row

// Java-style API
MultiRow multiRow = (MultiRow) F.getField("multiRow")
AbstractRow newRow = (AbstractRow) multiRow.newRow()
newRow.updateField("fieldName", "value")
multiRow.addRow(newRow)
```

### 3. Workflow Hook Configuration
- Scripts are configured in JSON files with execution order
- Pre-hooks and post-hooks are supported
- Scripts can be enabled/disabled via configuration

### 4. Testing Assertions
```groovy
Emery.test.assertEquals(expected, actual)
Emery.test.assertTrue(condition)
Emery.test.assertNotNull(value)
```

## Summary

The Emery DSL provides a comprehensive framework for enterprise application development with:

1. **Hierarchical API Structure**: Organized into 15+ logical namespaces (form, util, ctx, dataobject, etc.)
2. **Builder Patterns**: Extensive use of builder patterns for query construction
3. **Domain-Specific Abstractions**: High-level abstractions for forms, data objects, and workflows
4. **Integration Capabilities**: Built-in support for external system integration (CIF, CIS, hookChain)
5. **Utility Functions**: Comprehensive utility functions for strings, dates, LOVs, and more
6. **Type Safety**: Strong typing with custom classes and enums
7. **Context Awareness**: Built-in context management for workflow and user information
8. **Testing Support**: Built-in testing utilities and assertion methods
9. **Dual API Style**: Support for both Groovy-style and Java-style API usage
10. **Form Declarations**: Special syntax for declaring form and data object usage

This DSL extends Groovy syntax while maintaining compatibility, providing a domain-specific vocabulary for enterprise application development within the MetricStream platform.
