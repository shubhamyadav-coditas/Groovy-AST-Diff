# Recursive AST Parsing & Comparison Flowchart - Groovy

## Overview

This document describes the recursive approach for parsing Groovy source files down to pure statements, comparing nodes and hashes between two ASTs, and generating hierarchical diffs. This implementation follows the JavaScript AST Diff POC approach but is specifically tailored for Groovy language constructs.

## Supported File Extensions

| Extension | Description |
|-----------|-------------|
| `.groovy` | Standard Groovy source files |

---

## 1. High-Level Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         RECURSIVE AST COMPARISON                            │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  INPUT: File A (.groovy/.gradle)  +  File B (.groovy/.gradle)              │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    ▼                               ▼
        ┌──────────────────┐              ┌──────────────────┐
        │   Parse File A   │              │   Parse File B   │
        │  (tree-sitter)   │              │  (tree-sitter)   │
        └────────┬─────────┘              └────────┬─────────┘
                 │                                  │
                 ▼                                  ▼
        ┌──────────────────┐              ┌──────────────────┐
        │     AST A        │              │     AST B        │
        │   (Root Node)    │              │   (Root Node)    │
        └────────┬─────────┘              └────────┬─────────┘
                 │                                  │
                 └─────────────┬────────────────────┘
                               ▼
        ┌─────────────────────────────────────────────────────┐
        │           RECURSIVE BLOCK EXTRACTION                │
        │     (Level 1: Top-level blocks)                     │
        └─────────────────────┬───────────────────────────────┘
                              │
                              ▼
        ┌─────────────────────────────────────────────────────┐
        │         MULTI-PHASE BLOCK MATCHING                  │
        │   Phase 1: Identifier → Phase 2: Content Hash      │
        │   Phase 3: Similarity (≥70%) → Phase 4: Unmatched  │
        └─────────────────────┬───────────────────────────────┘
                              │
                              ▼
        ┌─────────────────────────────────────────────────────┐
        │         RECURSIVE STATEMENT EXTRACTION              │
        │     (For modified blocks: drill down with depth     │
        │      limiting using GroovyRecursiveParser)          │
        └─────────────────────┬───────────────────────────────┘
                              │
                              ▼
        ┌─────────────────────────────────────────────────────┐
        │              HASH COMPUTATION                       │
        │   (Content hash, structure hash, body hash)         │
        └─────────────────────┬───────────────────────────────┘
                              │
                              ▼
        ┌─────────────────────────────────────────────────────┐
        │           COMPARISON & DIFF GENERATION              │
        │   (Hierarchical diffs with nested child_diffs)     │
        └─────────────────────────────────────────────────────┘
```

---

## 2. Recursive Parsing Decision Tree

```
                    ┌──────────────────────────┐
                    │     Start: AST Node      │
                    └────────────┬─────────────┘
                                 │
                                 ▼
                    ┌──────────────────────────┐
                    │   Is Node a Container?   │
                    │  (Has child statements)  │
                    └────────────┬─────────────┘
                                 │
                    ┌────────────┴────────────┐
                    │ YES                     │ NO
                    ▼                         ▼
        ┌─────────────────────┐   ┌─────────────────────────┐
        │ CONTAINER NODE      │   │ PURE STATEMENT (LEAF)   │
        │                     │   │                         │
        │ • class_definition  │   │ • expression_statement  │
        │ • method_definition │   │ • return_statement      │
        │ • function_definition│  │ • throw_statement       │
        │ • if_statement      │   │ • break_statement       │
        │ • for_loop          │   │ • continue_statement    │
        │ • while_loop        │   │ • assert_statement      │
        │ • try_statement     │   │ • import_statement      │
        │ • switch_statement  │   │ • package_statement     │
        │ • closure           │   │ • variable_declaration  │
        │ • statement_block   │   │ • field_declaration     │
        └──────────┬──────────┘   └───────────┬─────────────┘
                   │                          │
                   ▼                          ▼
        ┌─────────────────────┐   ┌─────────────────────────┐
        │ 1. Compute Node Hash│   │ 1. Compute Content Hash │
        │ 2. Extract Children │   │ 2. Store as Leaf        │
        │ 3. RECURSE into each│   │ 3. STOP recursion       │
        └─────────────────────┘   └─────────────────────────┘
```

---

## 3. Groovy Code Block Hierarchy

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       PROGRAM (Root)                                        │
└─────────────────────────────────────────────────────────────────────────────┘
         │
         ├── PACKAGE/IMPORT DECLARATIONS ──────────────────────────────────────
         │   ├── package_statement           │ package com.example
         │   └── import_statement            │ import java.util.List
         │
         ├── CLASS TYPES ──────────────────────────────────────────────────────
         │   ├── class_definition            │ class Foo {}
         │   ├── interface_definition        │ interface Bar {}
         │   ├── trait_definition            │ trait Mixable {}
         │   ├── enum_definition             │ enum Status {}
         │   └── annotation_definition       │ @interface MyAnnotation {}
         │
         ├── METHOD/FUNCTION TYPES ────────────────────────────────────────────
         │   ├── method_definition           │ def methodName() {}
         │   ├── function_definition         │ def functionName() {}
         │   └── constructor_definition      │ Constructor methods
         │
         ├── FIELD/PROPERTY DECLARATIONS ──────────────────────────────────────
         │   ├── field_declaration           │ private int field = 0
         │   ├── property_definition         │ String property
         │   ├── variable_declaration        │ def variable = value
         │   └── declaration                 │ USER_PROFILE_FORM formObj = ...
         │
         ├── GROOVY-SPECIFIC CONSTRUCTS ───────────────────────────────────────
         │   ├── closure                     │ { it > 0 }
         │   ├── closure_expression          │ list.findAll { condition }
         │   ├── script_variable             │ Top-level variables
         │   └── groovy_collection_methods   │ list.each {}, map.collect {}
         │
         └── TOP-LEVEL EXPRESSIONS ────────────────────────────────────────────
             ├── expression_statement       │ println "Hello"; method calls
             ├── assignment                 │ variable = value
             ├── binary_op                  │ F.skillsMultiRow.rows << newRow
             └── function_call              │ Emery.form.newForm(...)
```

---

## 4. Inside a Class/Method: Nested Code Blocks

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     CLASS/METHOD BODY (statement_block)                     │
└─────────────────────────────────────────────────────────────────────────────┘
         │
         ├── VARIABLE DECLARATIONS ────────────────────────────────────────────
         │   ├── variable_declaration        │ def x = value
         │   ├── field_declaration           │ private String field
         │   └── assignment                  │ x = value
         │
         ├── CONTROL FLOW ─────────────────────────────────────────────────────
         │   ├── if_statement ────────────►──┬── body (statement_block)
         │   │                               ├── else_body (else clause)
         │   │                               └── branch-aware analysis
         │   │
         │   ├── switch_statement ────────►──┬── switch_block
         │   │                               ├── case (individual cases)
         │   │                               └── switch_default
         │   │
         │   └── try_statement ───────────►──┬── try_body
         │                                   ├── catch_clause
         │                                   └── finally_clause
         │
         ├── LOOPS (ITERATIVE) ────────────────────────────────────────────────
         │   ├── for_loop ────────────────►── body (statement_block)
         │   ├── for_in_loop ─────────────►── body (for-in iteration)
         │   ├── while_loop ──────────────►── body
         │   └── do_while_statement ──────►── body
         │
         ├── NESTED CLASSES/METHODS ───────────────────────────────────────────
         │   ├── class_definition            │ class InnerClass {}
         │   ├── method_definition           │ def innerMethod() {}
         │   └── function_definition         │ def localFunction() {}
         │
         ├── GROOVY CLOSURES & COLLECTIONS ────────────────────────────────────
         │   ├── list.findAll { condition }  │ Collection method with closure
         │   ├── list.collect { transform }  │ Transformation closures
         │   ├── list.each { action }        │ Iteration closures
         │   ├── map.collectEntries { }      │ Map operations
         │   └── { closure_body }            │ Standalone closures
         │
         ├── EMERY DSL CONSTRUCTS ─────────────────────────────────────────────
         │   ├── F.fieldName                 │ Form field access
         │   ├── Emery.form.newForm()        │ Form operations
         │   ├── Emery.dataTable.read()      │ Data table operations
         │   ├── Emery.mdos.getMdos()        │ MDOS operations
         │   └── use() declarations          │ Emery use statements
         │
         └── PURE STATEMENTS (LEAF NODES) ─────────────────────────────────────
             ├── expression_statement       │ println "Hello"; method calls
             ├── return_statement           │ return value;
             ├── throw_statement            │ throw new Exception();
             ├── break_statement            │ break;
             ├── continue_statement         │ continue;
             ├── assert_statement           │ assert condition;
             └── empty_statement            │ ; (empty)
```

---

## 5. Recursive Parsing Algorithm

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    RECURSIVE PARSE ALGORITHM                                │
└─────────────────────────────────────────────────────────────────────────────┘

    parse_recursive(node, source, depth=0, parent_hash=None, path="")
        │
        ▼
    ┌───────────────────────────────────────┐
    │  1. Compute node_hash = hash(node)    │
    │     • Include: type + content         │
    │     • Normalize whitespace            │
    └───────────────────┬───────────────────┘
                        │
                        ▼
    ┌───────────────────────────────────────┐
    │  2. Create RecursiveNodeSignature     │
    │     • node_type                       │
    │     • identifier (if named)           │
    │     • content_hash                    │
    │     • start_line, end_line            │
    │     • depth_level                     │
    │     • parent_hash                     │
    │     • path (e.g., "class:Foo/method:bar")│
    └───────────────────┬───────────────────┘
                        │
                        ▼
    ┌───────────────────────────────────────┐
    │  3. Check: is_pure_statement(node)?   │
    │     • GROOVY_LEAF_STATEMENTS          │
    │     • Context-aware closure handling  │
    └───────────────────┬───────────────────┘
                        │
            ┌───────────┴───────────┐
            │ YES                   │ NO
            ▼                       ▼
    ┌─────────────────┐   ┌─────────────────────────────┐
    │ Return node     │   │ 4. Get parseable children:  │
    │ as LEAF         │   │    • statement_block        │
    │                 │   │    • body/else_body (if)    │
    │                 │   │    • cases (switch)         │
    └─────────────────┘   │    • body (loops)           │
                          │    • class members          │
                          │    • method body            │
                          └─────────────┬───────────────┘
                                        │
                                        ▼
                          ┌─────────────────────────────┐
                          │ 5. For each child_node:     │
                          │    child = parse_recursive( │
                          │        child_node,          │
                          │        source,              │
                          │        depth + 1,           │
                          │        content_hash,        │
                          │        current_path         │
                          │    )                        │
                          │    signature.children.append(│
                          │        child                │
                          │    )                        │
                          └─────────────┬───────────────┘
                                        │
                                        ▼
                          ┌─────────────────────────────┐
                          │ 6. Compute structure_hash   │
                          │    = hash(                  │
                          │        node_type +          │
                          │        children_hashes      │
                          │    )                        │
                          └─────────────┬───────────────┘
                                        │
                                        ▼
                          ┌─────────────────────────────┐
                          │ 7. Compute body_hash        │
                          │    (for functions/methods)  │
                          └─────────────┬───────────────┘
                                        │
                                        ▼
                          ┌─────────────────────────────┐
                          │ 8. Return node with all     │
                          │    recursive children       │
                          └─────────────────────────────┘
```

---

## 6. Pure Statement Detection

```python
# Pure statement types (leaf nodes) - stop recursion here
GROOVY_LEAF_STATEMENTS = {
    # Pure expressions
    "expression_statement",      # println "Hello"; method calls
    
    # Control flow terminals
    "return_statement",          # return value;
    "throw_statement",           # throw new Exception();
    "break_statement",           # break;
    "continue_statement",        # continue;
    "assert_statement",          # assert condition;
    
    # Declarations/imports
    "import_statement",          # import java.util.List;
    "package_statement",         # package com.example;
    "variable_declaration",      # def x = value (simple)
    "field_declaration",         # private String field
    "empty_statement",           # ; (empty)
}

# Container types that need recursive parsing
GROOVY_RECURSIVE_CONTAINERS = {
    # Class/Interface containers
    "class_definition",
    "interface_definition",
    "trait_definition",
    "enum_definition",
    "annotation_definition",
    
    # Method/Function containers
    "method_definition",
    "function_definition",
    "constructor_definition",
    
    # Control flow containers
    "if_statement",
    "for_loop",              # Corrected from for_statement
    "for_in_loop",           # Corrected from for_in_statement
    "while_loop",            # Corrected from while_statement
    "switch_statement",
    "switch_block",          # Container for switch cases
    "case",                  # Individual switch case
    "try_statement",
    "catch_clause",
    "finally_clause",
    
    # Block containers
    "statement_block",
    "block",
    
    # Groovy-specific containers (context-aware)
    "closure",               # Handled contextually
    "closure_expression",    # Real closures vs structural blocks
}
```

---

## 7. Multi-Phase Block Matching Strategy

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       MULTI-PHASE BLOCK COMPARISON                          │
└─────────────────────────────────────────────────────────────────────────────┘

    ┌─────────────────┐                    ┌─────────────────┐
    │    Blocks A     │                    │    Blocks B     │
    │   (File A)      │                    │   (File B)      │
    └────────┬────────┘                    └────────┬────────┘
             │                                      │
             └──────────────┬───────────────────────┘
                            │
                            ▼
    ┌─────────────────────────────────────────────────────────────────────────┐
    │                     MATCHING PHASES                                     │
    └─────────────────────────────────────────────────────────────────────────┘
                            │
         ┌──────────────────┼──────────────────┬──────────────────┐
         │                  │                  │                  │
         ▼                  ▼                  ▼                  ▼
    ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  ┌─────────────┐
    │  Phase 1:   │  │  Phase 2:   │  │  Phase 3:           │  │  Phase 4:   │
    │  Match by   │  │  Match by   │  │  Match by           │  │  Process    │
    │  IDENTIFIER │  │  CONTENT    │  │  STRUCTURAL         │  │  UNMATCHED  │
    │  + TYPE     │  │  HASH       │  │  SIMILARITY         │  │             │
    │             │  │             │  │                     │  │             │
    │  "Same name │  │  "Same code │  │  "Similar structure │  │  "Added or  │
    │  same type" │  │  anywhere"  │  │  ≥70% similarity"   │  │  Deleted"   │
    │             │  │             │  │                     │  │             │
    │  → MODIFIED │  │  → MOVED    │  │  → MODIFIED         │  │  → ADDED/   │
    │  (if diff   │  │             │  │  (hybrid approach:  │  │    DELETED  │
    │  content)   │  │             │  │  best match finder) │  │             │
    └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘  └──────┬──────┘
           │                │                     │                     │
           └────────────────┴─────────────────────┴─────────────────────┘
                            │
                            ▼
    ┌─────────────────────────────────────────────────────────────────────────┐
    │                   RECURSIVE STATEMENT ANALYSIS                          │
    └─────────────────────────────────────────────────────────────────────────┘
                            │
                            ▼
    ┌───────────────────────────────────────┐
    │  For each matched MODIFIED pair:      │
    │  ─────────────────────────────        │
    │  1. Use GroovyRecursiveParser         │
    │  2. Build hierarchical signatures     │
    │  3. Compare children recursively      │
    │  4. Generate statement-level diffs    │
    │  5. Detect moves within containers    │
    └───────────────────────────────────────┘
```

---

## 8. Groovy-Specific Enhancements

### 8.1 Closure Method Call Handling

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    CLOSURE METHOD CALL COMBINATION                          │
└─────────────────────────────────────────────────────────────────────────────┘

PROBLEM: Tree-sitter parses this as separate nodes:
    def filtered = list.findAll { it > 0 }
    
    ├── declaration: "def filtered = list.findAll"
    └── closure: "{ it > 0 }"

SOLUTION: Detect and combine method calls with closure arguments:

    ┌─────────────────────────────────────────────────────────────────────────┐
    │ Pattern Detection                                                       │
    ├─────────────────────────────────────────────────────────────────────────┤
    │ if (child.type in {'declaration', 'expression_statement'} and           │
    │     i + 1 < len(children) and                                           │
    │     children[i + 1].type == 'closure'):                                 │
    │                                                                         │
    │     # Combine into single statement                                     │
    │     combined_code = method_call_code + " " + closure_code               │
    │                                                                         │
    │ Result: "def filtered = list.findAll { it > 0 }"                        │
    └─────────────────────────────────────────────────────────────────────────┘

Affected Groovy Collection Methods:
    • list.findAll { condition }
    • list.collect { transformation }
    • list.each { action }
    • list.any { condition }
    • list.every { condition }
    • map.collectEntries { }
```

### 8.2 Emery DSL Support

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        EMERY DSL CONSTRUCTS                                │
└─────────────────────────────────────────────────────────────────────────────┘

Emery Language Patterns Supported:
    ┌─────────────────────────────────────────────────────────────────────────┐
    │ Pattern                           │ AST Handling                        │
    ├─────────────────────────────────────────────────────────────────────────┤
    │ F.fieldName                       │ dotted_identifier / member_access   │
    │ F.skillsMultiRow.rows << newRow   │ binary_op (left-hand extraction)    │
    │ Emery.form.newForm("TYPE")        │ function_call (dotted function)     │
    │ USER_PROFILE_FORM formObj = ...   │ typed_declaration (combined nodes)  │
    │ Emery.test.assertEquals(...)      │ function_call (nested identifiers) │
    │ use("EMERY_UTILITIES")            │ function_call                       │
    └─────────────────────────────────────────────────────────────────────────┘
```

### 8.3 Branch-Aware If Statement Analysis

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    IF STATEMENT BRANCH ANALYSIS                             │
└─────────────────────────────────────────────────────────────────────────────┘

Standard if_statement handling:
    if (condition) {
        // body
    } else if (condition2) {
        // else_body contains nested if_statement
    } else {
        // final else_body
    }

Branch Extraction:
    ┌─────────────────────────────────────────────────────────────────────────┐
    │ 1. Extract main if branch: condition + body                             │
    │ 2. Extract else if branches: nested if_statements in else_body          │
    │ 3. Extract final else branch: remaining else_body content               │
    │                                                                         │
    │ Result: Individual branch comparison like JavaScript POC                │
    │   • if ((condition)) → modified/unchanged                               │
    │   • else_if ((condition2)) → added/deleted/modified                     │
    │   • else → modified/unchanged                                           │
    └─────────────────────────────────────────────────────────────────────────┘
```

---

## 9. Complete Recursive Comparison Flow

```
                    ┌────────────────────────────────┐
                    │  compare_recursive(nodeA, nodeB) │
                    └───────────────┬────────────────┘
                                    │
                                    ▼
                    ┌────────────────────────────────┐
                    │  Content Hash Match?           │
                    │  nodeA.content_hash ==         │
                    │  nodeB.content_hash            │
                    └───────────────┬────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │ YES                           │ NO
                    ▼                               ▼
        ┌───────────────────────┐    ┌────────────────────────────┐
        │ UNCHANGED             │    │ Content differs...         │
        │ (Exact match at       │    │ Check identifier match     │
        │ this level)           │    └─────────────┬──────────────┘
        │                       │                  │
        │ Still compare children│                  ▼
        │ for position changes  │    ┌────────────────────────────┐
        └──────────┬────────────┘    │ Same identifier?           │
                   │                 │ (Phase 0: Identifier)      │
                   │                 └─────────────┬──────────────┘
                   │                               │
                   │                 ┌─────────────┴─────────────┐
                   │                 │ YES                       │ NO
                   │                 ▼                           ▼
                   │    ┌─────────────────────┐    ┌─────────────────────┐
                   │    │ MODIFIED            │    │ Check structure     │
                   │    │ • Same name         │    │ similarity          │
                   │    │ • Different content │    │ (Phase 3)           │
                   │    │ • May be MOVED_MOD  │    └──────────┬──────────┘
                   │    │   if line differs   │               │
                   │    └─────────┬───────────┘               ▼
                   │              │              ┌────────────────────────┐
                   │              ▼              │ Similarity ≥ 70%?     │
                   │    ┌─────────────────────┐  └────────────┬───────────┘
                   │    │ Compare children    │               │
                   │    │ recursively to find │    ┌──────────┴──────────┐
                   │    │ what changed inside │    │ YES                 │ NO
                   │    └─────────────────────┘    ▼                     ▼
                   │                  ┌─────────────────┐   ┌─────────────────┐
                   │                  │ MODIFIED        │   │ ADDED/DELETED   │
                   │                  │ (Structural     │   │ (No match found)│
                   │                  │ similarity)     │   └─────────────────┘
                   │                  └─────────────────┘
                   │
                   ▼
        ┌───────────────────────────────────────────────────────────────────┐
        │                    RECURSE INTO CHILDREN                          │
        ├───────────────────────────────────────────────────────────────────┤
        │  children_a = extract_children(nodeA)                             │
        │  children_b = extract_children(nodeB)                             │
        │                                                                   │
        │  Multi-phase matching on children:                                │
        │    Phase 1: Identifier matching (same type + name)               │
        │    Phase 2: Content hash matching (exact content → MOVED)        │
        │    Phase 3: Similarity matching (≥70% → MODIFIED)                │
        │    Phase 4: Remaining unmatched → ADDED/DELETED                   │
        │                                                                   │
        │  For each matched pair (childA, childB):                          │
        │      compare_recursive(childA, childB)                            │
        │                                                                   │
        │  Unmatched in A → DELETED                                         │
        │  Unmatched in B → ADDED                                           │
        └───────────────────────────────────────────────────────────────────┘
```

---

## 10. Data Structures for Recursive Parsing

```python
@dataclass
class RecursiveNodeSignature:
    """Signature for a node at any depth in the Groovy AST."""
    
    # Identity
    node_type: str                    # tree-sitter node type
    identifier: str | None           # class/method/variable name
    
    # Hashes for comparison
    content_hash: str                 # hash of node's text content
    structure_hash: str               # hash of type + children's hashes
    body_hash: str | None            # hash of body only (for methods)
    
    # Position info
    start_line: int
    end_line: int
    depth: int                        # nesting level (0 = top-level)
    
    # Parent reference for context
    parent_hash: str | None           # hash of parent node
    path: str                         # e.g., "class:Foo/method:bar/if:0"
    
    # Children (recursive)
    children: list['RecursiveNodeSignature']
    
    # Flags
    is_pure_statement: bool           # True if leaf node
    is_container: bool                # True if has nested blocks
    
    # Original code
    code: str                         # Source code text


@dataclass 
class StatementDiff:
    """Diff for a statement that includes nested diffs."""
    
    change_type: StatementChangeType  # ADDED, DELETED, MODIFIED, UNCHANGED
    code: str                         # New code (file B)
    node_type: str                    # tree-sitter node type
    file_a_line: int | None          # Line in file A
    file_b_line: int | None          # Line in file B
    old_code: str | None             # Original code (file A)
    similarity_score: float | None   # Similarity percentage
    
    # Nested diffs (recursive)
    child_diffs: list['StatementDiff'] = field(default_factory=list)
    
    # Metadata
    is_container: bool = False        # True if has child_diffs
    branch_label: str | None = None   # For if/else branches
    description: str = ""             # Human-readable description


@dataclass
class BlockSignature:
    """Signature for a top-level Groovy code block."""
    
    block_type: BlockType             # CLASS, METHOD, FIELD, etc.
    identifier: str                   # Name or content hash
    content_hash: str                 # Hash of block content
    start_line: int
    end_line: int
    code: str                         # Source code
    node_type: str                    # tree-sitter node type
    children_count: int = 0           # Number of child nodes
    modifiers: list[str] = field(default_factory=list)  # static, private, etc.
```

---

## 11. Algorithm Pseudocode

```python
def recursive_parse(node: Node, source: bytes, depth: int = 0, 
                    parent_hash: str = None, path: str = "") -> RecursiveNodeSignature:
    """
    Recursively parse a Groovy node and all its children until pure statements.
    """
    # 1. Get node content and compute content hash
    code = source[node.start_byte:node.end_byte].decode('utf-8', errors='replace')
    content_hash = hash_content(code)
    
    # 2. Determine if this is a pure statement (leaf)
    is_pure = node.type in GROOVY_LEAF_STATEMENTS
    is_container = node.type in GROOVY_RECURSIVE_CONTAINERS
    
    # 3. Extract identifier if named (class, method, variable names)
    identifier = extract_identifier(node, source)
    
    # 4. Build path for context
    current_path = f"{path}/{node.type}"
    if identifier:
        current_path += f":{identifier}"
    
    # 5. Create signature
    signature = RecursiveNodeSignature(
        node_type=node.type,
        identifier=identifier,
        content_hash=content_hash,
        structure_hash="",  # computed after children
        body_hash=None,
        start_line=node.start_point.row + 1,
        end_line=node.end_point.row + 1,
        depth=depth,
        parent_hash=parent_hash,
        path=current_path,
        children=[],
        is_pure_statement=is_pure,
        is_container=is_container,
        code=code,
    )
    
    # 6. If pure statement, stop recursion
    if is_pure:
        signature.structure_hash = content_hash
        return signature
    
    # 7. Otherwise, recurse into children
    child_nodes = get_parseable_children(node)
    
    for child in child_nodes:
        child_sig = recursive_parse(
            child, 
            source, 
            depth + 1,
            content_hash,
            current_path
        )
        signature.children.append(child_sig)
    
    # 8. Compute structure hash from children
    child_hashes = [c.structure_hash for c in signature.children]
    signature.structure_hash = hash_structure(node.type, child_hashes)
    
    # 9. Compute body hash for methods/functions
    if is_groovy_method_type(node):
        signature.body_hash = compute_body_hash(node, source)
    
    return signature


def get_parseable_children(node: Node) -> list[Node]:
    """
    Get children that should be parsed recursively for Groovy nodes.
    """
    children = []
    
    if node.type in ("method_definition", "function_definition", "constructor_definition"):
        body = node.child_by_field_name("body")
        if body:
            # Parse statements inside method body
            for child in body.named_children:
                children.append(child)
    
    elif node.type in ("class_definition", "interface_definition", "trait_definition"):
        body = node.child_by_field_name("body")
        if body:
            # Parse class members (methods, fields, nested classes)
            for member in body.named_children:
                children.append(member)
    
    elif node.type == "if_statement":
        # Handle if/else branches
        body = node.child_by_field_name("body")
        else_body = node.child_by_field_name("else_body")
        if body:
            children.extend(get_block_statements(body))
        if else_body:
            children.extend(get_block_statements(else_body))
    
    elif node.type == "switch_statement":
        body = node.child_by_field_name("body")
        if body:
            for case in body.named_children:
                children.append(case)
    
    elif node.type in ("for_loop", "for_in_loop", "while_loop"):
        body = node.child_by_field_name("body")
        if body:
            children.extend(get_block_statements(body))
    
    elif node.type == "try_statement":
        body = node.child_by_field_name("body")
        handler = node.child_by_field_name("handler")
        finalizer = node.child_by_field_name("finalizer")
        if body:
            children.extend(get_block_statements(body))
        if handler:
            children.append(handler)
        if finalizer:
            children.append(finalizer)
    
    elif node.type == "closure":
        # Context-aware closure handling
        if is_structural_closure(node):
            # Treat as container
            for child in node.named_children:
                children.append(child)
        # else: treat as pure statement (no children)
    
    return children
```

---

## 12. Groovy-Specific Node Type Mappings

```python
# Block type mappings for top-level classification
GROOVY_NODE_TYPE_TO_BLOCK_TYPE = {
    # Class types
    "class_definition": BlockType.CLASS,
    "interface_definition": BlockType.INTERFACE,
    "trait_definition": BlockType.TRAIT,
    "enum_definition": BlockType.ENUM,
    "annotation_definition": BlockType.ANNOTATION,
    
    # Method types
    "method_definition": BlockType.METHOD,
    "function_definition": BlockType.METHOD,
    "constructor_definition": BlockType.CONSTRUCTOR,
    
    # Field/Property types
    "field_definition": BlockType.FIELD,
    "property_definition": BlockType.PROPERTY,
    "variable_declaration": BlockType.VARIABLE,
    "declaration": BlockType.DECLARATION,
    
    # Statement types
    "if_statement": BlockType.STATEMENT,
    "for_loop": BlockType.STATEMENT,
    "while_loop": BlockType.STATEMENT,
    "switch_statement": BlockType.STATEMENT,
    "try_statement": BlockType.STATEMENT,
    
    # Expression types
    "expression_statement": BlockType.EXPRESSION,
    "assignment": BlockType.EXPRESSION,
    "function_call": BlockType.EXPRESSION,
    "juxt_function_call": BlockType.FUNCTION_CALL,
    "binary_op": BlockType.BINARY_OPERATION,
    
    # Other
    "closure": BlockType.CLOSURE,
    "comment": BlockType.COMMENT,
}

# Function/Method types for body hash computation
GROOVY_FUNCTION_TYPES = {
    "function_definition",
    "method_definition", 
    "constructor_definition",
}

# Class types for member extraction
GROOVY_CLASS_TYPES = {
    "class_definition",
    "interface_definition",
    "trait_definition",
    "enum_definition",
    "annotation_definition",
}
```

---

## 13. Visual Example

```
INPUT:
─────────────────────────────────────────────────────────────────

// File A                          // File B
class Calculator {                 class Calculator {
  def calculate(x) {                 def calculate(x) {
    if (x > 0) {          ←─────→      if (x > 0) {
      return x * 2;                      def result = x * 2;  // Added
    }                                    return result;        // Moved Modified
    return 0;             ←─────→      }
  }                                    return 0;               // UNCHANGED
}                                    }
                                   }


COMPARISON OUTPUT:
─────────────────────────────────────────────────────────────────

{
    "differences": [
    {
      "change_type": "modified",
      "block_type": "class",
      "identifier": "Calculator",
      "file_a_start_line": 1,
      "file_a_end_line": 8,
      "file_a_code": "class Calculator {\n    def calculate(x) {\n        if (x > 0) {\n            return x * 2; \n        }\n        return 0; \n    }\n}",
      "file_b_start_line": 1,
      "file_b_end_line": 9,
      "file_b_code": "class Calculator {\n    def calculate(x) {\n        if (x > 0) {\n            def result = x * 2; \n            return result;\n        }\n        return 0;\n    }\n}",
      "similarity_score": 93.69369369369369,
      "description": "Modified class 'Calculator' (93.7% similar)",
      "modifiers": [],
      "statement_diffs": [
        {
          "change_type": "modified",
          "code": "def calculate(x) {\n        if (x > 0) {\n            def result = x * 2; \n            return result;\n        }\n        return 0;\n    }",
          "node_type": "function_definition",
          "file_a_line": 2,
          "file_a_index": 0,
          "file_b_line": 2,
          "file_b_index": 0,
          "description": "Statement modified: function_definition 'calculate' (91.3% similar)",
          "old_code": "def calculate(x) {\n        if (x > 0) {\n            return x * 2; \n        }\n        return 0; \n    }",
          "similarity_score": 0.9130434782608695,
          "is_container": true,
          "branch_label": null,
          "child_diffs": [
            {
              "change_type": "modified",
              "code": "if (x > 0) {\n            def result = x * 2; \n            return result;\n        }",
              "node_type": "if_statement",
              "file_a_line": 3,
              "file_a_index": 1,
              "file_b_line": 3,
              "file_b_index": 1,
              "description": "Statement modified: if_statement 'anonymous_if_statement' (82.5% similar)",
              "old_code": "if (x > 0) {\n            return x * 2; \n        }",
              "similarity_score": 0.8253968253968254,
              "is_container": true,
              "branch_label": null,
              "child_diffs": [
                {
                  "change_type": "modified",
                  "code": "if (x > 0) {\n            def result = x * 2; \n            return result;\n        }",
                  "node_type": "if_branch",
                  "file_a_line": 3,
                  "file_a_index": 0,
                  "file_b_line": 3,
                  "file_b_index": 0,
                  "description": "if ((x > 0)) body modified",
                  "old_code": "if (x > 0) {\n            return x * 2; \n        }",
                  "similarity_score": null,
                  "is_container": true,
                  "branch_label": "if((x > 0))",
                  "child_diffs": [
                    {
                      "change_type": "moved_modified",
                      "code": "return result",
                      "node_type": "return",
                      "file_a_line": 4,
                      "file_a_index": 0,
                      "file_b_line": 5,
                      "file_b_index": 1,
                      "description": "Statement moved and modified: return from position 1 to 2 (54.5% similar)",
                      "old_code": "return x * 2",
                      "similarity_score": 0.5454545454545454,
                      "is_container": false,
                      "branch_label": null,
                      "child_diffs": []
                    },
                    {
                      "change_type": "added",
                      "code": "def result = x * 2",
                      "node_type": "declaration",
                      "file_a_line": null,
                      "file_a_index": null,
                      "file_b_line": 4,
                      "file_b_index": 0,
                      "description": "Statement added: declaration (lines 4-4)",
                      "old_code": null,
                      "similarity_score": null,
                      "is_container": false,
                      "branch_label": null,
                      "child_diffs": []
                    }
                  ]
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}
```

---

## Implementation Summary

### Current Groovy Recursive Parsing Implementation

The Groovy recursive parsing approach follows these key principles:

1. **Starts at the root** (program node) using tree-sitter-groovy
2. **Extracts top-level blocks** (classes, methods, fields, variables, expressions)
3. **Applies multi-phase matching** (identifier → content hash → similarity ≥70%)
4. **For modified blocks**, recursively parses children using `GroovyRecursiveParser`
5. **Continues until pure statements** are reached (leaf nodes) with depth limiting
6. **Computes multiple hash types** (content, structure, body) for comparison
7. **Generates hierarchical diffs** showing exactly what changed at each nesting level

### Key Implementation Features:

**Recursion Control:**
- **Leaf node detection**: `GROOVY_LEAF_STATEMENTS` stops recursion
- **Special case handling**: Switch cases are leaf nodes to prevent infinite loops
- **Context-aware closures**: `_get_closure_context()` distinguishes structural vs functional

**Multi-Phase Matching:**
- **Phase 1**: Identifier matching (same type + name)
- **Phase 2**: Content hash matching (exact content → MOVED)
- **Phase 3**: Similarity matching (≥70% → MODIFIED/MOVED_MODIFIED)
- **Phase 4**: Remaining unmatched → ADDED/DELETED

**Groovy-Specific Handling:**
- **Context-aware closure handling**: Structural blocks parsed, functional closures treated as pure
- **Emery DSL compatibility**: Uses standard Groovy node type mappings
- **Container-based comparison**: If statements use container approach, not individual branch extraction
- **Collection methods**: Each method treated as separate statement (not combined with closures)

**Data Structures:**
- `RecursiveNodeSignature`: Hierarchical signatures with depth, path, children
- `GROOVY_RECURSIVE_CONTAINERS`: 25 container types requiring recursion
- `GROOVY_LEAF_STATEMENTS`: 12 leaf types that stop recursion

### Differences from JavaScript Implementation:

**✅ Similarities:**
- Recursive parsing down to pure statements
- Multi-phase matching strategy
- Hierarchical diff structure with nested containers
- Hash-based comparison (content, structure, body)


### Recursion Protection Mechanisms:

```python
# 1. Leaf node detection
if node_type in GROOVY_LEAF_STATEMENTS:
    return signature(is_pure_statement=True)

# 2. Context-aware closure handling
if node_type == "closure":
    context = _get_closure_context(node)
    if context == "REAL_CLOSURE":
        return signature(is_pure_statement=True)  # Don't recurse
    else:  # Structural block
        parse_children_directly()  # Skip closure wrapper

# 3. Special case handling
if node_type == "case":  # Always leaf to prevent infinite loops
    return signature(is_pure_statement=True)
```

This provides fine-grained comparison that can detect:
- Moved statements within methods/classes
- Renamed methods with identical bodies  
- Added/deleted blocks at any nesting level
- Structural changes (e.g., wrapping code in new control flow)
- Groovy-specific language pattern modifications
- Emery DSL construct changes