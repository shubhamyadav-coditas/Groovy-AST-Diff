# Recursive AST Parsing - Mermaid Diagrams (Groovy)

## 1. Main Recursive Parsing Flow

```mermaid
flowchart TD
    subgraph INPUT["📁 Input Files"]
        A1["File A<br/>(.groovy/.gradle)"]
        A2["File B<br/>(.groovy/.gradle)"]
    end

    subgraph PARSE["🔍 Parsing Phase"]
        B1["Parse with<br/>tree-sitter-groovy"]
        B2["Generate<br/>AST A"]
        B3["Generate<br/>AST B"]
    end

    subgraph RECURSIVE["🔄 Recursive Extraction"]
        C1["Extract Top-Level<br/>Blocks"]
        C2["Check Recursion<br/>Depth (max=10)"]
        C3{"Is Container<br/>Node?"}
        C4["Context-Aware<br/>Closure Handling"]
        C5["Parse Children<br/>Recursively"]
        C6["Mark as<br/>Pure Statement"]
        C7["Compute<br/>Node Hash"]
        C8["Compute<br/>Structure Hash"]
    end

    subgraph COMPARE["⚖️ Multi-Phase Comparison"]
        D1["Phase 1: Match by<br/>Identifier (exact name)"]
        D2["Phase 2: Match by<br/>Content Hash (MOVED)"]
        D3["Phase 3: Match by<br/>Structural Similarity (≥70%)"]
        D4["Phase 4: Process<br/>Unmatched (ADDED/DELETED)"]
        D5["Generate<br/>Hierarchical Diff"]
    end

    subgraph OUTPUT["📊 Output"]
        E1["ComparisonResult<br/>with nested statement_diffs"]
    end

    A1 --> B1
    A2 --> B1
    B1 --> B2
    B1 --> B3
    B2 --> C1
    B3 --> C1
    C1 --> C2
    C2 --> C3
    C3 -->|Yes| C4
    C3 -->|No| C6
    C4 --> C5
    C5 --> C7
    C6 --> C7
    C7 --> C8
    C5 --> C2
    C8 --> D1
    D1 --> D2
    D2 --> D3
    D3 --> D4
    D4 --> D5
    D5 --> E1

    style INPUT fill:#e1f5fe
    style PARSE fill:#fff3e0
    style RECURSIVE fill:#f3e5f5
    style COMPARE fill:#e8f5e9
    style OUTPUT fill:#fce4ec
```

## 2. Container vs Pure Statement Decision (Groovy)

```mermaid
flowchart TD
    START["🔍 Examine Node"] --> DEPTH_CHECK{"Depth > 10?"}
    
    DEPTH_CHECK -->|Yes| MAX_DEPTH["⚠️ Max Depth<br/>Treat as Pure"]
    DEPTH_CHECK -->|No| CHECK{"Node Type?"}
    
    CHECK -->|"class_definition<br/>interface_definition<br/>trait_definition<br/>enum_definition<br/>annotation_definition"| CLASS["📦 CLASS<br/>Container"]
    CHECK -->|"method_definition<br/>function_definition<br/>constructor_definition"| FUNC["📦 METHOD<br/>Container"]
    CHECK -->|"if_statement<br/>else_clause<br/>switch_statement<br/>switch_block<br/>try_statement"| CONTROL["📦 CONTROL FLOW<br/>Container"]
    CHECK -->|"for_loop<br/>for_in_loop<br/>while_loop<br/>do_while_loop"| LOOP["📦 LOOP<br/>Container"]
    CHECK -->|"closure<br/>closure_expression"| CLOSURE["📦 CLOSURE<br/>Context-Aware"]
    CHECK -->|"expression_statement<br/>return_statement<br/>throw_statement"| PURE["🍃 PURE<br/>Statement"]
    CHECK -->|"break_statement<br/>continue_statement<br/>assert_statement"| PURE
    CHECK -->|"import_statement<br/>package_statement<br/>variable_declaration<br/>field_declaration<br/>declaration"| PURE
    CHECK -->|"case<br/>(CRITICAL: prevents loops)"| PURE
    
    CLASS --> RECURSE["↻ Recurse into body"]
    FUNC --> RECURSE
    CONTROL --> RECURSE
    LOOP --> RECURSE
    CLOSURE --> CONTEXT_CHECK{"Check Closure Context"}
    
    CONTEXT_CHECK -->|"CLASS_BODY<br/>FUNCTION_BODY<br/>IF_STATEMENT_BODY<br/>etc."| RECURSE
    CONTEXT_CHECK -->|"REAL_CLOSURE<br/>(passed to functions)"| HASH["#️⃣ Compute Hash"]
    
    PURE --> HASH
    MAX_DEPTH --> HASH
    RECURSE --> CHILDREN["Get child nodes"]
    CHILDREN --> START
    
    HASH --> STOP["⏹️ Stop<br/>Return Signature"]
    
    style CLASS fill:#c8e6c9
    style FUNC fill:#bbdefb
    style CONTROL fill:#fff9c4
    style LOOP fill:#ffccbc
    style CLOSURE fill:#e1bee7
    style PURE fill:#b2dfdb
    style STOP fill:#ffcdd2
```

## 3. Groovy Code Block Hierarchy

```mermaid
flowchart TD
    subgraph PROGRAM["📄 PROGRAM (Root)"]
        direction TB
        
        subgraph IMPORTS["📦 Package/Import Declarations"]
            PKG["package_statement"]
            IMP["import_statement"]
        end
        
        subgraph CLASSES["🏛️ Class Types"]
            CD["class_definition"]
            ID["interface_definition"]
            TD["trait_definition"]
            ED["enum_definition"]
            AD["annotation_definition"]
        end
        
        subgraph METHODS["🔧 Method/Function Types"]
            MD["method_definition"]
            FD["function_definition"]
            CON["constructor_definition"]
        end
        
        subgraph FIELDS["📝 Field/Property Declarations"]
            FLD["field_declaration"]
            PD["property_definition"]
            VD["variable_declaration"]
            DECL["declaration<br/>(typed declarations)"]
        end
        
        subgraph GROOVY_SPECIFIC["🎯 Groovy-Specific Constructs"]
            CL["closure"]
            CE["closure_expression"]
            SV["script_variable"]
            GCM["groovy_collection_methods<br/>(findAll, collect, each)"]
        end
        
        subgraph EMERY_DSL["🔮 Emery DSL Constructs"]
            F_ACCESS["F.fieldName"]
            EMERY_FORM["Emery.form.newForm()"]
            EMERY_DATA["Emery.dataTable.read()"]
            EMERY_MDOS["Emery.mdos.getMdos()"]
            USE_STMT["use() declarations"]
        end
        
        subgraph EXPRESSIONS["💬 Top-Level Expressions"]
            ES["expression_statement"]
            ASSIGN["assignment"]
            BINOP["binary_op<br/>(F.rows << newRow)"]
            FC["function_call"]
        end
    end
    
    style IMPORTS fill:#e3f2fd
    style CLASSES fill:#e8f5e9
    style METHODS fill:#f3e5f5
    style FIELDS fill:#fff3e0
    style GROOVY_SPECIFIC fill:#e1f5fe
    style EMERY_DSL fill:#f8bbd9
    style EXPRESSIONS fill:#fce4ec
```

## 4. Inside Class/Method Body - Nested Blocks

```mermaid
flowchart TD
    subgraph BODY["📦 CLASS/METHOD BODY"]
        direction TB
        
        subgraph VARS["Variables & Fields"]
            V1["def/var declarations"]
            V2["field_declaration"]
            V3["assignment"]
            V4["typed declarations<br/>(USER_PROFILE_FORM obj = ...)"]
        end
        
        subgraph CONTROL["Control Flow"]
            IF["if_statement"]
            SWITCH["switch_statement"]
            TRY["try_statement"]
            IF --> IF_BRANCHES["Branch-Aware Analysis:<br/>• if condition + body<br/>• else if branches<br/>• else branch"]
            SWITCH --> SWITCH_BODY["switch_block<br/>• case<br/>• switch_default"]
            TRY --> TRY_BODY["body<br/>catch_clause<br/>finally_clause"]
        end
        
        subgraph LOOPS["Loops"]
            FOR["for_loop"]
            FORIN["for_in_loop"]
            WHILE["while_loop"]
            DO["do_while_statement"]
        end
        
        subgraph NESTED["Nested Classes/Methods"]
            NC["class_definition"]
            NM["method_definition"]
            NF["function_definition"]
        end
        
        subgraph CLOSURES["Groovy Closures & Collections"]
            FINDALL["list.findAll { condition }"]
            COLLECT["list.collect { transform }"]
            EACH["list.each { action }"]
            MAP_OPS["map.collectEntries { }"]
            STANDALONE["{ closure_body }"]
        end
        
        subgraph EMERY["Emery DSL Operations"]
            FORM_OPS["Form Operations<br/>F.fieldName access"]
            DATA_OPS["Data Table Operations<br/>Emery.dataTable.*"]
            MDOS_OPS["MDOS Operations<br/>Emery.mdos.*"]
            TEST_OPS["Test Operations<br/>Emery.test.*"]
        end
        
        subgraph PURE["Pure Statements ✓"]
            EXP["expression_statement"]
            RET["return_statement"]
            THR["throw_statement"]
            BRK["break/continue"]
            ASS["assert_statement"]
        end
    end
    
    style VARS fill:#fff9c4
    style CONTROL fill:#ffccbc
    style LOOPS fill:#d1c4e9
    style NESTED fill:#bbdefb
    style CLOSURES fill:#c8e6c9
    style EMERY fill:#f8bbd9
    style PURE fill:#b2dfdb
```

## 5. Multi-Phase Block Matching Strategy

```mermaid
flowchart TD
    subgraph BLOCKS_A["📄 Blocks A (File A)"]
        BA1["Block A1<br/>class Calculator<br/>id: Calculator"]
        BA2["Block A2<br/>def calculate(x)<br/>id: calculate"]
        BA3["Block A3<br/>def result = x * 2<br/>id: content_hash_abc"]
    end
    
    subgraph BLOCKS_B["📄 Blocks B (File B)"]
        BB1["Block B1<br/>class Calculator<br/>id: Calculator"]
        BB2["Block B2<br/>def calculate(x)<br/>id: calculate"]
        BB3["Block B3<br/>def result = x * 3<br/>id: content_hash_xyz"]
        BB4["Block B4<br/>def newMethod()<br/>id: newMethod"]
    end
    
    subgraph PHASE1["🎯 Phase 1: Identifier Match"]
        P1_MATCH1["Calculator ↔ Calculator<br/>SAME IDENTIFIER"]
        P1_MATCH2["calculate ↔ calculate<br/>SAME IDENTIFIER"]
        P1_NO_MATCH["newMethod ← NO MATCH"]
    end
    
    subgraph PHASE2["🔍 Phase 2: Content Hash Match"]
        P2_CHECK["Check remaining blocks<br/>for exact content match"]
        P2_NO_MATCH["No exact matches found"]
    end
    
    subgraph PHASE3["📊 Phase 3: Structural Similarity"]
        P3_HYBRID["Hybrid Best-Match Approach:<br/>1. High-confidence matches (≥90%)<br/>2. Best-match for remaining blocks"]
        P3_RESULT["result = x * 2 ↔ result = x * 3<br/>85% similarity → MODIFIED"]
    end
    
    subgraph PHASE4["➕➖ Phase 4: Unmatched"]
        P4_ADDED["newMethod → ADDED"]
        P4_DELETED["(none in this example)"]
    end
    
    BA1 --> P1_MATCH1
    BB1 --> P1_MATCH1
    BA2 --> P1_MATCH2
    BB2 --> P1_MATCH2
    BB4 --> P1_NO_MATCH
    
    BA3 --> P2_CHECK
    BB3 --> P2_CHECK
    P2_CHECK --> P2_NO_MATCH
    
    P2_NO_MATCH --> P3_HYBRID
    P3_HYBRID --> P3_RESULT
    
    P1_NO_MATCH --> P4_ADDED
    
    style BLOCKS_A fill:#e3f2fd
    style BLOCKS_B fill:#f3e5f5
    style PHASE1 fill:#c8e6c9
    style PHASE2 fill:#fff9c4
    style PHASE3 fill:#ffccbc
    style PHASE4 fill:#d1c4e9
```

## 6. Groovy-Specific Enhancements

```mermaid
flowchart TD
    subgraph ENHANCEMENTS["🎯 Groovy-Specific Enhancements"]
        
        subgraph CLOSURE_COMBO["🔗 Closure Method Call Combination"]
            PROBLEM["PROBLEM:<br/>def filtered = list.findAll { it > 0 }<br/>Parsed as separate nodes"]
            SOLUTION["SOLUTION:<br/>Detect & combine method + closure<br/>into single statement"]
            RESULT["RESULT:<br/>Complete semantic unit preserved"]
        end
        
        subgraph TYPED_DECL["📝 Typed Declaration Handling"]
            EMERY_PROBLEM["PROBLEM:<br/>USER_PROFILE_FORM formObj = ...<br/>Split into identifier + assignment"]
            EMERY_SOLUTION["SOLUTION:<br/>Detect type + assignment pattern<br/>Combine into synthetic signature"]
            EMERY_RESULT["RESULT:<br/>Full typed declaration captured"]
        end
        
        subgraph BRANCH_AWARE["🌿 Branch-Aware If Analysis"]
            IF_PROBLEM["PROBLEM:<br/>if/else if/else treated as single block"]
            IF_SOLUTION["SOLUTION:<br/>Extract individual branches<br/>Compare like JavaScript POC"]
            IF_RESULT["RESULT:<br/>Branch-level change detection"]
        end
        
        subgraph EMERY_DSL["🔮 Emery DSL Support"]
            DSL_FEATURES["• F.fieldName access<br/>• Binary operations (<<)<br/>• Dotted function calls<br/>• Custom type declarations"]
            DSL_HANDLING["Context-aware parsing<br/>Enhanced identifier extraction"]
        end
    end
    
    PROBLEM --> SOLUTION
    SOLUTION --> RESULT
    EMERY_PROBLEM --> EMERY_SOLUTION
    EMERY_SOLUTION --> EMERY_RESULT
    IF_PROBLEM --> IF_SOLUTION
    IF_SOLUTION --> IF_RESULT
    DSL_FEATURES --> DSL_HANDLING
    
    style CLOSURE_COMBO fill:#c8e6c9
    style TYPED_DECL fill:#fff9c4
    style BRANCH_AWARE fill:#ffccbc
    style EMERY_DSL fill:#f8bbd9
```

## 7. Recursive Comparison Algorithm (Groovy)

```mermaid
flowchart TD
    START["compare_recursive<br/>(nodeA, nodeB)"] --> HASH_CHECK{"content_hash<br/>equal?"}
    
    HASH_CHECK -->|"Yes"| UNCHANGED["UNCHANGED<br/>at this level"]
    HASH_CHECK -->|"No"| ID_CHECK{"Same<br/>identifier?<br/>(Phase 0)"}
    
    ID_CHECK -->|"Yes"| LINE_CHECK{"Different<br/>line position?"}
    LINE_CHECK -->|"Yes"| MOVED_MOD["MOVED_MODIFIED<br/>same name, diff content & position"]
    LINE_CHECK -->|"No"| MODIFIED["MODIFIED<br/>same name, diff content"]
    
    ID_CHECK -->|"No"| SIMILARITY_CHECK{"Structural<br/>similarity ≥ 70%?<br/>(Phase 3)"}
    
    SIMILARITY_CHECK -->|"Yes"| STRUCT_MOD["MODIFIED<br/>(structural similarity)"]
    SIMILARITY_CHECK -->|"No"| ADD_DEL["ADDED or<br/>DELETED"]
    
    UNCHANGED --> RECURSE["Recurse into<br/>children"]
    MODIFIED --> RECURSE
    MOVED_MOD --> RECURSE
    STRUCT_MOD --> RECURSE
    
    RECURSE --> SPECIAL_HANDLING{"Special Node<br/>Type?"}
    SPECIAL_HANDLING -->|"if_statement"| IF_BRANCHES["Compare if/else if/else<br/>branches individually"]
    SPECIAL_HANDLING -->|"class/method"| CLASS_METHOD["Extract and compare<br/>members/statements"]
    SPECIAL_HANDLING -->|"other"| GENERIC["Generic container<br/>comparison"]
    
    IF_BRANCHES --> MATCH_CHILDREN
    CLASS_METHOD --> MATCH_CHILDREN
    GENERIC --> MATCH_CHILDREN
    
    MATCH_CHILDREN["Multi-phase matching<br/>on children"] --> FOR_EACH{"For each<br/>matched pair"}
    FOR_EACH -->|"More pairs"| CALL_RECURSIVE["compare_recursive<br/>(childA, childB)"]
    CALL_RECURSIVE --> FOR_EACH
    
    FOR_EACH -->|"Done"| UNMATCHED["Handle unmatched:<br/>A-only = DELETED<br/>B-only = ADDED"]
    
    UNMATCHED --> RESULT["Return<br/>StatementDiff with<br/>child_diffs"]
    ADD_DEL --> RESULT
    
    style START fill:#e3f2fd
    style UNCHANGED fill:#c8e6c9
    style MODIFIED fill:#fff9c4
    style MOVED_MOD fill:#e1bee7
    style STRUCT_MOD fill:#d1c4e9
    style ADD_DEL fill:#ffccbc
    style RESULT fill:#fce4ec
```

## 8. Groovy Node Type Classifications

```mermaid
flowchart TD
    subgraph CONTAINERS["📦 GROOVY_RECURSIVE_CONTAINERS"]
        CLASS_TYPES["🏛️ Class Types<br/>• class_definition<br/>• interface_definition<br/>• trait_definition<br/>• enum_definition<br/>• annotation_definition"]
        
        METHOD_TYPES["🔧 Method Types<br/>• method_definition<br/>• function_definition<br/>• constructor_definition"]
        
        CONTROL_TYPES["🎛️ Control Flow<br/>• if_statement<br/>• else_clause<br/>• switch_statement<br/>• switch_block<br/>• switch_default<br/>• try_statement"]
        
        LOOP_TYPES["🔄 Loop Types<br/>• for_loop<br/>• for_in_loop<br/>• while_loop<br/>• do_while_loop<br/>• do_while_statement"]
        
        BLOCK_TYPES["📋 Block Types<br/>• statement_block<br/>• block<br/>• synchronized_statement<br/>• labeled_statement"]
        
        GROOVY_TYPES["🎯 Groovy-Specific<br/>• closure (context-aware)<br/>• closure_expression"]
    end
    
    subgraph LEAF_STATEMENTS["🍃 GROOVY_LEAF_STATEMENTS"]
        PURE_TYPES["🔚 Pure Statements<br/>• expression_statement<br/>• return_statement<br/>• throw_statement<br/>• break_statement<br/>• continue_statement<br/>• assert_statement<br/>• import_statement<br/>• package_statement<br/>• variable_declaration<br/>• field_declaration<br/>• declaration<br/>• empty_statement<br/>• case (CRITICAL: prevents loops)"]
    end
    
    subgraph BLOCK_TYPE_MAPPING["🗺️ Block Type Mapping"]
        MAPPING["GROOVY_NODE_TYPE_TO_BLOCK_TYPE:<br/>• class_definition → CLASS<br/>• method_definition → METHOD<br/>• declaration → DECLARATION<br/>• function_call → EXPRESSION<br/>• binary_op → BINARY_OPERATION<br/>• juxt_function_call → FUNCTION_CALL<br/>• for_loop → STATEMENT<br/>• if_statement → STATEMENT<br/>• closure → CLOSURE<br/>• comment → COMMENT"]
    end
    
    style CLASS_TYPES fill:#e8f5e9
    style METHOD_TYPES fill:#f3e5f5
    style CONTROL_TYPES fill:#fff9c4
    style LOOP_TYPES fill:#ffccbc
    style BLOCK_TYPES fill:#d1c4e9
    style GROOVY_TYPES fill:#e1f5fe
    style PURE_TYPES fill:#b2dfdb
    style MAPPING fill:#f8bbd9
```

## 9. File Extension Support

```mermaid
flowchart LR
    subgraph EXTENSIONS["Supported Extensions"]
        GROOVY[".groovy<br/>Standard Groovy"]
        GRADLE[".gradle<br/>Gradle Build Scripts"]
    end
    
    GROOVY --> PARSER["tree-sitter<br/>Groovy Parser"]
    GRADLE --> PARSER
    
    PARSER --> FEATURES["All features supported:<br/>• Classes & Traits<br/>• Methods & Closures<br/>• Control Flow<br/>• Groovy Collections<br/>• Emery DSL<br/>• Gradle Scripts"]
    
    style EXTENSIONS fill:#e8f5e9
    style PARSER fill:#fff3e0
    style FEATURES fill:#e3f2fd
```

## 10. Complete Processing Pipeline

```mermaid
sequenceDiagram
    participant User
    participant CLI as GroovyASTDiff CLI
    participant Parser as TreeSitter Groovy Parser
    participant Extractor as Recursive Extractor
    participant Combiner as Enhancement Processor
    participant Comparator as Multi-Phase Comparator
    participant Differ as Hierarchical Diff Generator
    
    User->>CLI: python3 groovy_ast_diff.py fileA.groovy fileB.groovy
    
    par Parse Files
        CLI->>Parser: parse(fileA.groovy)
        Parser-->>CLI: AST_A
        CLI->>Parser: parse(fileB.groovy)
        Parser-->>CLI: AST_B
    end
    
    CLI->>Extractor: _extract_recursive_signatures(AST_A)
    
    loop For each top-level node
        Extractor->>Extractor: check recursion depth (max=10)
        
        alt Depth > 10
            Extractor->>Extractor: treat as pure statement
        else Normal Processing
            Extractor->>Extractor: context-aware closure handling
            Extractor->>Extractor: is_container?
            alt Container Node
                Extractor->>Extractor: get_parseable_children()
                Extractor->>Extractor: parse_recursive()
            else Pure Statement
                Extractor->>Extractor: compute_hashes()
                Extractor->>Extractor: return signature
            end
        end
    end
    
    Extractor-->>CLI: RecursiveSignatures_A
    
    CLI->>Extractor: _extract_recursive_signatures(AST_B)
    Extractor-->>CLI: RecursiveSignatures_B
    
    CLI->>Comparator: _compare_blocks(Signatures_A, Signatures_B)
    
    loop Multi-phase matching
        Comparator->>Comparator: Phase 1: match_by_identifier()
        Comparator->>Comparator: Phase 2: match_by_content_hash()
        Comparator->>Comparator: Phase 3: match_by_similarity() (hybrid)
        Comparator->>Comparator: Phase 4: process_unmatched()
        
        alt Modified Block Found
            Comparator->>Differ: _compare_statements(blockA, blockB)
            Differ->>Differ: recursive statement analysis
            Differ-->>Comparator: StatementDiff[]
        end
    end
    
    Comparator-->>CLI: BlockDiff[]
    
    CLI->>Differ: _create_json_result(BlockDiff[])
    Differ-->>CLI: ComparisonResult JSON
    
    CLI-->>User: JSON output / file saved
```

## 11. Data Structures (Groovy Implementation)

```mermaid
classDiagram
    class RecursiveNodeSignature {
        +str node_type
        +str identifier
        +str content_hash
        +str structure_hash
        +str body_hash
        +int start_line
        +int end_line
        +int depth
        +str parent_hash
        +str path
        +bool is_pure_statement
        +bool is_container
        +str code
        +List~RecursiveNodeSignature~ children
    }
    
    class BlockSignature {
        +BlockType block_type
        +str identifier
        +str content_hash
        +int start_line
        +int end_line
        +str code
        +str node_type
        +int children_count
        +List~str~ modifiers
    }
    
    class StatementDiff {
        +StatementChangeType change_type
        +str code
        +str node_type
        +int file_a_line
        +int file_b_line
        +str old_code
        +float similarity_score
        +List~StatementDiff~ child_diffs
        +bool is_container
        +str branch_label
        +str description
    }
    
    class ChangeType {
        <<enumeration>>
        ADDED
        DELETED
        MODIFIED
        MOVED
        MOVED_MODIFIED
        UNCHANGED
    }
    
    class BlockType {
        <<enumeration>>
        CLASS
        INTERFACE
        TRAIT
        ENUM
        METHOD
        FIELD
        PROPERTY
        VARIABLE
        DECLARATION
        STATEMENT
        EXPRESSION
        FUNCTION_CALL
        BINARY_OPERATION
        CLOSURE
        COMMENT
    }
    
    RecursiveNodeSignature "1" *-- "0..*" RecursiveNodeSignature : children
    StatementDiff "1" *-- "0..*" StatementDiff : child_diffs
    StatementDiff --> ChangeType
    BlockSignature --> BlockType
```

## 12. Emery DSL Pattern Recognition

```mermaid
flowchart TD
    subgraph EMERY_PATTERNS["🔮 Emery DSL Pattern Recognition"]
        
        subgraph FIELD_ACCESS["Field Access Patterns"]
            F_SIMPLE["F.fieldName"]
            F_NESTED["F.skillsMultiRow.rows"]
            F_BINARY["F.skillsMultiRow.rows << newRow"]
        end
        
        subgraph FUNCTION_CALLS["Function Call Patterns"]
            EMERY_FORM["Emery.form.newForm('TYPE')"]
            EMERY_DATA["Emery.dataTable.read('TABLE')"]
            EMERY_MDOS["Emery.mdos.getMdosDisplayValuesClob()"]
            EMERY_TEST["Emery.test.assertEquals(expected, actual)"]
        end
        
        subgraph TYPED_DECLARATIONS["Typed Declaration Patterns"]
            USER_FORM["USER_PROFILE_FORM formObj = ..."]
            EMPLOYEE_FORM["EMPLOYEE_FORM empForm = ..."]
            CUSTOM_TYPE["CUSTOM_TYPE variable = ..."]
        end
        
        subgraph USE_STATEMENTS["Use Statement Patterns"]
            USE_UTIL["use('EMERY_UTILITIES')"]
            USE_FORM["use('EMERY_FORM_OPERATIONS')"]
        end
    end
    
    subgraph AST_HANDLING["🎯 AST Handling Strategy"]
        F_SIMPLE --> DOTTED_ID["dotted_identifier"]
        F_NESTED --> MEMBER_ACCESS["member_access"]
        F_BINARY --> BINARY_OP["binary_op<br/>(left-hand extraction)"]
        
        EMERY_FORM --> FUNC_CALL["function_call<br/>(dotted function)"]
        EMERY_DATA --> FUNC_CALL
        EMERY_MDOS --> FUNC_CALL
        EMERY_TEST --> FUNC_CALL
        
        USER_FORM --> COMBINED_DECL["Combined Declaration<br/>(synthetic signature)"]
        EMPLOYEE_FORM --> COMBINED_DECL
        CUSTOM_TYPE --> COMBINED_DECL
        
        USE_UTIL --> SIMPLE_FUNC["function_call"]
        USE_FORM --> SIMPLE_FUNC
    end
    
    style FIELD_ACCESS fill:#e1f5fe
    style FUNCTION_CALLS fill:#f3e5f5
    style TYPED_DECLARATIONS fill:#fff9c4
    style USE_STATEMENTS fill:#e8f5e9
    style AST_HANDLING fill:#f8bbd9
```

## 13. Visual Example: Groovy Class Modification

```mermaid
flowchart TD
    subgraph FILE_A["📄 File A (Before)"]
        A_CLASS["class Calculator {"]
        A_METHOD["  def calculate(x) {"]
        A_IF["    if (x > 0) {"]
        A_RETURN["      return x * 2;"]
        A_IF_END["    }"]
        A_RETURN2["    return 0;"]
        A_METHOD_END["  }"]
        A_CLASS_END["}"]
    end
    
    subgraph FILE_B["📄 File B (After)"]
        B_CLASS["class Calculator {"]
        B_METHOD["  def calculate(x) {"]
        B_IF["    if (x > 0) {"]
        B_VAR["      def result = x * 2;"]
        B_RETURN["      return result;"]
        B_IF_END["    }"]
        B_RETURN2["    return 0;"]
        B_METHOD_END["  }"]
        B_CLASS_END["}"]
    end
    
    subgraph ANALYSIS["🔍 Recursive Analysis"]
        CLASS_MATCH["class:Calculator<br/>MATCHED (same identifier)"]
        METHOD_MATCH["method:calculate<br/>MATCHED → MODIFIED<br/>(different content)"]
        IF_MATCH["if_statement<br/>MATCHED → MODIFIED<br/>(different body)"]
        STMT_CHANGES["Statement Changes:<br/>• return x * 2 → DELETED<br/>• def result = x * 2 → ADDED<br/>• return result → ADDED"]
        RETURN_UNCHANGED["return 0 → UNCHANGED"]
    end
    
    subgraph RESULT["📊 Result Structure"]
        JSON_STRUCTURE["{<br/>  'change_type': 'modified',<br/>  'block_type': 'class',<br/>  'identifier': 'Calculator',<br/>  'statement_diffs': [<br/>    {<br/>      'change_type': 'modified',<br/>      'node_type': 'method_definition',<br/>      'child_diffs': [<br/>        {<br/>          'change_type': 'modified',<br/>          'node_type': 'if_statement',<br/>          'child_diffs': [...]<br/>        }<br/>      ]<br/>    }<br/>  ]<br/>}"]
    end
    
    A_CLASS --> CLASS_MATCH
    B_CLASS --> CLASS_MATCH
    A_METHOD --> METHOD_MATCH
    B_METHOD --> METHOD_MATCH
    A_IF --> IF_MATCH
    B_IF --> IF_MATCH
    A_RETURN --> STMT_CHANGES
    B_VAR --> STMT_CHANGES
    B_RETURN --> STMT_CHANGES
    A_RETURN2 --> RETURN_UNCHANGED
    B_RETURN2 --> RETURN_UNCHANGED
    
    CLASS_MATCH --> JSON_STRUCTURE
    METHOD_MATCH --> JSON_STRUCTURE
    IF_MATCH --> JSON_STRUCTURE
    STMT_CHANGES --> JSON_STRUCTURE
    
    style FILE_A fill:#e3f2fd
    style FILE_B fill:#f3e5f5
    style ANALYSIS fill:#fff9c4
    style RESULT fill:#e8f5e9
```

---

## Implementation Summary

### Current Groovy Recursive Parsing Implementation

This documentation reflects the actual implementation in GroovyRecursiveParser and GroovyASTDiff as of the latest codebase analysis.

#### Core Features

1. **Depth Limiting (max_depth=10)**
   - Critical safety mechanism to prevent infinite recursion
   - Nodes beyond max depth are treated as pure statements
   - Configurable parameter with sensible default

2. **Context-Aware Closure Handling**
   - `_get_closure_context()` determines closure behavior
   - Structural closures (CLASS_BODY, FUNCTION_BODY, etc.) → Containers (recurse)
   - Functional closures (REAL_CLOSURE) → Pure statements (stop recursion)
   - Prevents infinite loops while maintaining accuracy

3. **Multi-Phase Matching Strategy**
   - **Phase 1**: Match by identifier (exact name match)
   - **Phase 2**: Match by content hash (exact content → MOVED)
   - **Phase 3**: Match by structural similarity (≥70% → MODIFIED)
   - **Phase 4**: Process unmatched blocks (ADDED/DELETED)

4. **Accurate Node Classifications**
   - `GROOVY_RECURSIVE_CONTAINERS`: Containers that need recursion
   - `GROOVY_LEAF_STATEMENTS`: Pure statements that stop recursion
   - Special handling: `case` statements are leaf nodes (prevents infinite loops)

#### Key Differences from JavaScript Implementation

1. **No Zhang-Shasha Algorithm**: Uses custom similarity calculation
2. **Context-Aware Closures**: JavaScript doesn't have this concept
3. **Groovy-Specific Node Types**: Different tree-sitter grammar
4. **Emery DSL Support**: Domain-specific language handling
5. **Manual Traversal**: No tree-sitter queries, uses manual node traversal

#### Recursion Protection Mechanisms

1. **Max Depth Limiting**: Prevents runaway recursion
2. **Case Statement Classification**: Prevents infinite loops in switch statements
3. **Context-Aware Closures**: Functional closures don't recurse
4. **Leaf Node Detection**: Pure statements immediately stop recursion

#### Performance Optimizations

1. **Early Termination**: Pure statements stop recursion immediately
2. **Hash-Based Comparison**: Content and structure hashes for efficiency
3. **Context Caching**: Closure contexts computed once and reused
4. **Depth Tracking**: Efficient depth management during traversal

This implementation provides robust, context-aware recursive parsing for Groovy with proper handling of closures, Emery DSL constructs, and comprehensive recursion protection.

---

## Usage

These Mermaid diagrams can be rendered in:
- GitHub README/Markdown files
- GitLab README/Markdown files
- VS Code with Mermaid extensions
- Obsidian
- Notion
- Any Mermaid-compatible viewer

To view locally, you can use:
```bash
# Install mermaid-cli
npm install -g @mermaid-js/mermaid-cli

# Generate PNG/SVG
mmdc -i recursive_parsing_mermaid.md -o groovy_ast_diff_diagrams.png
```

## Key Differences from JavaScript Implementation

1. **Groovy-Specific Node Types**: Uses actual tree-sitter-groovy node types (e.g., `for_loop` instead of `for_statement`)
2. **Context-Aware Closure Handling**: Distinguishes structural blocks from functional closures using `_get_closure_context()`
3. **Emery DSL Support**: Specialized handling for custom DSL constructs (uses standard Groovy node mappings)
4. **Depth Limiting**: Configurable max_depth (default 10) with safety mechanisms
5. **Case Statement Protection**: `case` nodes are leaf statements to prevent infinite recursion
6. **Multi-Phase Matching**: 4-phase strategy with custom similarity calculation (no Zhang-Shasha)
7. **Manual Traversal**: Uses `_get_parseable_children()` instead of tree-sitter queries
8. **Method+Closure Combination**: Limited combination logic for Groovy collection methods (not a separate phase)