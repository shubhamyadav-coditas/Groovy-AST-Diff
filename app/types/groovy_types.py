"""
Enums and types for Groovy AST comparison.

This module contains all enum definitions used in Groovy AST comparison,
following the same structure as the JavaScript AST Diff POC.
"""

from enum import Enum


class ChangeType(Enum):
    """Types of changes detected during AST comparison."""

    ADDED = "added"                    # Block exists only in new file
    DELETED = "deleted"                # Block exists only in old file
    MODIFIED = "modified"              # Same identifier, different content
    MOVED = "moved"                    # Same content, different position
    MOVED_MODIFIED = "moved_modified"  # Different position AND content changed
    UNCHANGED = "unchanged"            # Identical in both files


class BlockType(Enum):
    """
    Core block types for Groovy top-level comparison.
    
    Focused on classes, methods, fields, and other Groovy constructs.
    """

    # =========================================================================
    # CLASS TYPES
    # =========================================================================
    CLASS = "class"                    # class Foo {}
    INTERFACE = "interface"            # interface Foo {}
    TRAIT = "trait"                    # trait Foo {}
    ENUM = "enum"                      # enum Status {}
    ANNOTATION = "annotation"          # @interface Foo {}
    
    # =========================================================================
    # METHOD TYPES
    # =========================================================================
    METHOD = "method"                  # def methodName() {}
    CONSTRUCTOR = "constructor"        # Constructor method
    STATIC_METHOD = "static_method"    # static def methodName() {}
    ABSTRACT_METHOD = "abstract_method" # abstract def methodName()
    
    # =========================================================================
    # FIELD/PROPERTY TYPES
    # =========================================================================
    FIELD = "field"                    # private int field = 0
    PROPERTY = "property"              # String property
    STATIC_FIELD = "static_field"      # static final String CONSTANT
    
    # =========================================================================
    # CLOSURE TYPES
    # =========================================================================
    CLOSURE = "closure"                # { -> code }
    
    # =========================================================================
    # SCRIPT ELEMENTS
    # =========================================================================
    SCRIPT_METHOD = "script_method"    # Top-level method in script
    SCRIPT_VARIABLE = "script_variable" # Top-level variable in script
    
    # =========================================================================
    # IMPORT/PACKAGE
    # =========================================================================
    IMPORT = "import"                  # import statement
    PACKAGE = "package"                # package declaration
    
    # =========================================================================
    # FUNCTION CALLS AND EXPRESSIONS
    # =========================================================================
    FUNCTION_CALL = "function_call"    # Function/method calls
    EXPRESSION = "expression"          # Top-level expression
    DECLARATION = "declaration"        # Variable declarations
    
    # =========================================================================
    # OTHER
    # =========================================================================
    STATEMENT = "statement"            # Generic statement
    COMMENT = "comment"                # Comments
    UNKNOWN = "unknown"                # Unrecognized block type


class StatementChangeType(Enum):
    """Types of changes for individual statements within a block."""

    ADDED = "added"                    # Statement exists only in new version
    DELETED = "deleted"                # Statement exists only in old version
    MODIFIED = "modified"              # Statement changed (similar but not identical)
    MOVED = "moved"                    # Same statement, different position
    MOVED_MODIFIED = "moved_modified"  # Moved AND content changed
    UNCHANGED = "unchanged"            # Identical statement at same position


# Groovy node type mappings
GROOVY_NODE_TYPE_TO_BLOCK_TYPE = {
    # Class-related
    "class_definition": BlockType.CLASS,
    "interface_definition": BlockType.INTERFACE,
    "trait_definition": BlockType.TRAIT,
    "enum_definition": BlockType.ENUM,
    "annotation_definition": BlockType.ANNOTATION,
    
    # Method-related
    "function_definition": BlockType.METHOD,
    "method_definition": BlockType.METHOD,
    "constructor_definition": BlockType.CONSTRUCTOR,
    
    # Field-related
    "field_definition": BlockType.FIELD,
    "property_definition": BlockType.PROPERTY,
    "variable_definition": BlockType.FIELD,
    
    # Closure
    "closure": BlockType.CLOSURE,
    "closure_expression": BlockType.CLOSURE,
    
    # Script elements
    "script_method": BlockType.SCRIPT_METHOD,
    "script_variable": BlockType.SCRIPT_VARIABLE,
    "declaration": BlockType.DECLARATION,  # Variable declarations -> declaration
    
    # Function calls and expressions (based on actual tree-sitter node types)
    "juxt_function_call": BlockType.FUNCTION_CALL,  # println "text" -> function_call
    "function_call": BlockType.EXPRESSION,          # Math.max(5, 10) -> expression
    "method_call": BlockType.FUNCTION_CALL,         # obj.method() -> function_call
    "assignment": BlockType.EXPRESSION,             # value += number -> expression
    "binary_op": BlockType.EXPRESSION,              # list << item, a + b -> expression
    "increment_op": BlockType.EXPRESSION,           # counter++ -> expression
    
    # Import/Package (using actual tree-sitter node names)
    "groovy_import": BlockType.IMPORT,
    "groovy_package": BlockType.PACKAGE,
    
    # Control flow statements
    "for_loop": BlockType.STATEMENT,        # for loops -> statement
    "for_in_loop": BlockType.STATEMENT,     # for-in loops -> statement  
    "while_loop": BlockType.STATEMENT,      # while loops -> statement
    "do_while_loop": BlockType.STATEMENT,   # do-while loops -> statement
    "if_statement": BlockType.STATEMENT,    # if statements -> statement
    "switch_statement": BlockType.STATEMENT, # switch statements -> statement
    "try_statement": BlockType.STATEMENT,   # try-catch -> statement
    
    # Other
    "expression_statement": BlockType.EXPRESSION,
    "comment": BlockType.COMMENT,
}

# Groovy function/method types
GROOVY_FUNCTION_TYPES = {
    "function_definition",
    "method_definition", 
    "constructor_definition",
}

# Groovy class types
GROOVY_CLASS_TYPES = {
    "class_definition",
    "interface_definition",
    "trait_definition",
    "enum_definition",
    "annotation_definition",
}

# Groovy field/property types
GROOVY_FIELD_TYPES = {
    "field_definition",
    "property_definition", 
    "variable_definition",
    "declaration",  # Groovy field declarations
}

# Pure statement types (leaf nodes)
GROOVY_PURE_STATEMENT_TYPES = {
    "expression_statement",
    "return_statement",
    "throw_statement",
    "break_statement",
    "continue_statement",
    "assert_statement",
    "import_statement",
    "package_statement",
}

# Container types that need recursive parsing
GROOVY_CONTAINER_TYPES = {
    # Class/Interface containers
    "class_definition",
    "interface_definition", 
    "trait_definition",
    "enum_definition",
    
    # Function/Method containers
    "function_definition",
    "method_definition",
    "constructor_definition",
    
    # Control flow containers (CORRECTED node types)
    "if_statement",
    "for_loop",           # FIXED: was for_statement
    "for_in_loop",       # FIXED: was for_in_statement  
    "while_loop",        # FIXED: was while_statement
    "switch_statement",
    "switch_block",      # NEW: container for switch cases
    "case",              # NEW: switch case container
    "try_statement",
    
    # Block containers
    "closure",
    "closure_expression",
    "block",
    "statement_block",
}
