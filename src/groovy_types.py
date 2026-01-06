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
    # OTHER
    # =========================================================================
    EXPRESSION = "expression"          # Top-level expression
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
    
    # Import/Package
    "import_statement": BlockType.IMPORT,
    "package_statement": BlockType.PACKAGE,
    
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
    "class_definition",
    "interface_definition", 
    "trait_definition",
    "enum_definition",
    "function_definition",
    "method_definition",
    "constructor_definition",
    "if_statement",
    "for_statement",
    "while_statement",
    "switch_statement",
    "try_statement",
    "catch_clause",
    "finally_clause",
    "closure",
    "closure_expression",
    "block",
}
