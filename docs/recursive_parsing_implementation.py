"""
Complete Recursive AST Parsing Implementation Reference - Groovy

This module provides the complete implementation logic for recursively
parsing Groovy ASTs until reaching pure statements, including specialized
handling for Emery DSL constructs.

Use this as a reference for implementing the recursive comparison service
for Groovy code.
"""

from dataclasses import dataclass, field
from enum import Enum
from typing import Optional, List, Dict, Any
import hashlib
import sys


# =============================================================================
# NODE TYPE CLASSIFICATIONS
# =============================================================================

# Pure statements - these are leaf nodes, stop recursion
GROOVY_PURE_STATEMENT_TYPES = frozenset({
    # Expression statements
    "expression_statement",       # println "hello"; methodCall();
    "juxt_function_call",        # println value (Groovy-style function call)
    
    # Control flow terminals
    "return_statement",           # return value
    "throw_statement",            # throw new Exception()
    "break_statement",            # break; break label
    "continue_statement",         # continue; continue label
    "assert_statement",           # assert condition
    
    # Empty/special
    "empty_statement",            # ;
    
    # Module declarations (no body to parse)
    "import_statement",           # import java.util.*
    "package_statement",          # package com.example
    
    # Variable declarations (simple)
    "field_declaration",          # String field
    "variable_declaration",       # def x = value (when no closure)
    
    # Groovy-specific pure statements
    "property_definition",        # Simple property definitions
    "annotation",                 # @Override, @Test
    
    # Closures treated as pure in certain contexts
    "closure",                    # { param -> body } (context-dependent)
    "closure_expression",         # Closure expressions (context-dependent)
})


# Container nodes - these have nested statements, MUST recurse
GROOVY_CONTAINER_NODE_TYPES = frozenset({
    # =========================================================================
    # CLASS TYPES
    # =========================================================================
    "class_definition",               # class Foo {}
    "interface_definition",           # interface Foo {}
    "trait_definition",               # trait Foo {}
    "enum_definition",                # enum Status {}
    "annotation_definition",          # @interface Foo {}
    "class_body",                     # { methods, fields... }
    
    # =========================================================================
    # METHOD TYPES
    # =========================================================================
    "method_definition",              # def method() {}
    "function_definition",            # def function() {}
    "constructor_definition",         # Constructor() {}
    
    # =========================================================================
    # CONTROL FLOW
    # =========================================================================
    "if_statement",                   # if (cond) {} else {}
    "switch_statement",               # switch (x) { cases }
    "switch_block",                   # container for cases
    "case",                          # case x: statements
    
    # =========================================================================
    # LOOPS
    # =========================================================================
    "for_loop",                       # for (item in collection) {}
    "for_in_loop",                    # Enhanced for loop
    "while_loop",                     # while (cond) {}
    "do_while_statement",             # do {} while (cond)
    
    # =========================================================================
    # EXCEPTION HANDLING
    # =========================================================================
    "try_statement",                  # try {} catch {} finally {}
    "catch_clause",                   # catch (e) {}
    "finally_clause",                 # finally {}
    
    # =========================================================================
    # BLOCKS AND SPECIAL
    # =========================================================================
    "statement_block",                # { statements }
    "block",                         # Generic block
    "labeled_statement",              # label: statement
    "synchronized_statement",         # synchronized (obj) {}
    
    # =========================================================================
    # GROOVY-SPECIFIC CONTAINERS (context-dependent)
    # =========================================================================
    # Note: closures can be containers OR pure statements depending on context
    # They are handled specially in the parsing logic
})


# Nodes that may contain closures or functions in their values
GROOVY_VALUE_CHECK_NODE_TYPES = frozenset({
    "declaration",                    # def x = { closure }
    "assignment",                     # x = { closure }
    "field_definition",               # field = { closure }
    "property_definition",            # property = { closure }
    "function_call",                  # method(closure)
    "method_call",                    # obj.method(closure)
    "juxt_function_call",            # method closure (Groovy style)
    "expression_statement",           # May contain method calls with closures
    "binary_op",                     # F.rows << newRow (Emery DSL)
    "member_access",                 # obj.property (may be method call)
    "dotted_identifier",             # F.fieldName (Emery DSL)
})


# Function/method types that have a body to parse
GROOVY_FUNCTION_TYPES = frozenset({
    "method_definition",
    "function_definition",
    "constructor_definition",
    "closure",                       # Context-dependent
    "closure_expression",            # Context-dependent
})


# =============================================================================
# GROOVY COLLECTION METHOD PATTERNS
# =============================================================================

# Collection methods that typically take closures as arguments
GROOVY_COLLECTION_METHODS = frozenset({
    # Filtering
    "findAll", "find", "findIndexOf", "grep",
    
    # Transformation
    "collect", "collectEntries", "collectMany", "flatten",
    
    # Iteration
    "each", "eachWithIndex", "reverseEach",
    
    # Aggregation
    "inject", "reduce", "sum", "min", "max",
    
    # Testing
    "any", "every", "contains",
    
    # Sorting
    "sort", "sortBy",
    
    # Grouping
    "groupBy", "countBy",
    
    # Unique
    "unique", "uniqueBy",
    
    # Other common methods
    "with", "tap", "use",
})


# =============================================================================
# EMERY DSL PATTERNS
# =============================================================================

# Emery DSL method patterns
EMERY_DSL_PATTERNS = {
    # Form operations
    "form": {"newForm", "validate", "submit", "clear"},
    
    # Data operations
    "dataTable": {"read", "write", "update", "delete"},
    "mdos": {"getMdos", "getMdosDisplayValuesClob", "setMdos"},
    
    # Workflow operations
    "workflow": {"defineStep", "execute", "start", "stop", "isActive"},
    
    # Test operations
    "test": {"assertEquals", "assertTrue", "assertFalse", "assertNotNull", 
             "setup", "cleanup", "run", "delay", "waitFor"},
    
    # Utility operations
    "util": {"withTransaction", "log", "config"},
    "log": {"info", "debug", "warn", "error"},
    "config": {"get", "set", "withSettings"},
}


# =============================================================================
# DATA STRUCTURES
# =============================================================================

class GroovyNodeCategory(Enum):
    """Category of a Groovy node for processing decisions."""
    PURE_STATEMENT = "pure"          # Leaf node, stop recursion
    CONTAINER = "container"          # Has children, must recurse
    VALUE_CHECK = "value_check"      # May contain closure in value
    CLOSURE_COMBINED = "combined"    # Method+closure combined statement
    UNKNOWN = "unknown"              # Unclassified


@dataclass
class IfBranch:
    """Represents an individual if/else-if/else branch."""
    branch_type: str                 # "if", "else_if", "else"
    condition: Optional[str] = None  # Condition code (None for else)
    condition_hash: Optional[str] = None
    body_code: str = ""
    body_hash: str = ""
    start_line: int = 0
    end_line: int = 0


@dataclass
class RecursiveNodeSignature:
    """
    Signature for a Groovy node at any depth in the AST.
    
    This is the main data structure used for recursive parsing and comparison.
    Enhanced for Groovy-specific patterns and Emery DSL support.
    """
    
    # Identity
    node_type: str                              # tree-sitter node type
    identifier: Optional[str] = None            # method/class/variable name
    
    # Hashes for comparison
    content_hash: str = ""                      # hash of node's text content
    structure_hash: str = ""                    # hash of type + children's hashes
    body_hash: Optional[str] = None             # hash of body only (for methods)
    
    # Position info
    start_line: int = 0
    end_line: int = 0
    start_byte: int = 0
    end_byte: int = 0
    
    # Hierarchy info
    depth: int = 0                              # nesting level (0 = top-level)
    parent_hash: Optional[str] = None           # hash of parent node
    path: str = ""                              # e.g., "class:Foo/method:bar/if:0"
    
    # Children (recursive)
    children: List["RecursiveNodeSignature"] = field(default_factory=list)
    
    # Flags
    is_pure_statement: bool = False             # True if leaf node
    is_container: bool = False                  # True if has nested blocks
    is_closure_combined: bool = False           # True if method+closure combined
    is_emery_dsl: bool = False                  # True if Emery DSL construct
    
    # Groovy-specific
    is_collection_method: bool = False          # True if collection method call
    closure_context: Optional[str] = None       # Context for closure handling
    
    # Branch analysis (for if statements)
    if_branches: List[IfBranch] = field(default_factory=list)
    
    # Original code
    code: str = ""
    
    # Emery DSL specific
    emery_module: Optional[str] = None          # e.g., "form", "dataTable"
    emery_method: Optional[str] = None          # e.g., "newForm", "read"


# =============================================================================
# CLASSIFICATION FUNCTIONS
# =============================================================================

def classify_groovy_node(node_type: str, context: Dict[str, Any] = None) -> GroovyNodeCategory:
    """
    Classify a Groovy node type into a processing category.
    
    Args:
        node_type: The tree-sitter node type string
        context: Additional context for classification
        
    Returns:
        GroovyNodeCategory indicating how to process this node
    """
    context = context or {}
    
    # Special handling for closures - context-dependent
    if node_type in ("closure", "closure_expression"):
        # If it's part of a collection method call, it's combined
        if context.get("is_collection_method"):
            return GroovyNodeCategory.CLOSURE_COMBINED
        # If it's a structural closure (in control flow), it's pure
        if context.get("is_structural"):
            return GroovyNodeCategory.PURE_STATEMENT
        # Otherwise, it's a container
        return GroovyNodeCategory.CONTAINER
    
    if node_type in GROOVY_PURE_STATEMENT_TYPES:
        return GroovyNodeCategory.PURE_STATEMENT
    
    if node_type in GROOVY_CONTAINER_NODE_TYPES:
        return GroovyNodeCategory.CONTAINER
    
    if node_type in GROOVY_VALUE_CHECK_NODE_TYPES:
        return GroovyNodeCategory.VALUE_CHECK
    
    return GroovyNodeCategory.UNKNOWN


def is_groovy_pure_statement(node_type: str, context: Dict[str, Any] = None) -> bool:
    """Check if node is a pure statement (leaf node) in Groovy context."""
    context = context or {}
    
    # Context-dependent closure handling
    if node_type in ("closure", "closure_expression"):
        return context.get("is_structural", False)
    
    return node_type in GROOVY_PURE_STATEMENT_TYPES


def is_groovy_container(node_type: str, context: Dict[str, Any] = None) -> bool:
    """Check if node contains other statements in Groovy context."""
    context = context or {}
    
    # Context-dependent closure handling
    if node_type in ("closure", "closure_expression"):
        return not context.get("is_structural", False) and not context.get("is_collection_method", False)
    
    return node_type in GROOVY_CONTAINER_NODE_TYPES


def is_groovy_function_type(node_type: str) -> bool:
    """Check if node is a function/method type."""
    return node_type in GROOVY_FUNCTION_TYPES


def needs_groovy_value_check(node_type: str) -> bool:
    """Check if we need to examine the value for nested closures."""
    return node_type in GROOVY_VALUE_CHECK_NODE_TYPES


def is_collection_method_call(node, source_bytes: bytes) -> bool:
    """
    Check if a node represents a collection method call.
    
    Args:
        node: tree-sitter Node object
        source_bytes: Original source code as bytes
        
    Returns:
        True if this is a collection method call
    """
    if node.type not in ("function_call", "method_call", "member_access"):
        return False
    
    # Extract method name
    method_name = extract_method_name(node, source_bytes)
    return method_name in GROOVY_COLLECTION_METHODS


def is_emery_dsl_call(node, source_bytes: bytes) -> tuple[bool, Optional[str], Optional[str]]:
    """
    Check if a node represents an Emery DSL call.
    
    Args:
        node: tree-sitter Node object
        source_bytes: Original source code as bytes
        
    Returns:
        Tuple of (is_emery, module, method)
    """
    if node.type not in ("function_call", "dotted_identifier", "member_access", "binary_op"):
        return False, None, None
    
    code = source_bytes[node.start_byte:node.end_byte].decode("utf-8", errors="replace")
    
    # Check for Emery.module.method pattern
    if "Emery." in code:
        parts = code.split(".")
        if len(parts) >= 3 and parts[0] == "Emery":
            module = parts[1]
            method = parts[2].split("(")[0]  # Remove parameters
            if module in EMERY_DSL_PATTERNS and method in EMERY_DSL_PATTERNS[module]:
                return True, module, method
    
    # Check for F.fieldName pattern
    if code.startswith("F."):
        return True, "form", "field_access"
    
    # Check for use() statements
    if "use(" in code:
        return True, "util", "use"
    
    return False, None, None


def is_structural_closure(node, parent_node) -> bool:
    """
    Determine if a closure is structural (part of control flow) or functional.
    
    Args:
        node: The closure node
        parent_node: The parent node
        
    Returns:
        True if this is a structural closure (should not recurse)
    """
    if not parent_node:
        return False
    
    # If parent is a control flow statement, closure is structural
    structural_parents = {
        "if_statement", "for_loop", "while_loop", "switch_statement",
        "try_statement", "catch_clause", "finally_clause"
    }
    
    return parent_node.type in structural_parents


# =============================================================================
# IDENTIFIER EXTRACTION FUNCTIONS
# =============================================================================

def extract_groovy_identifier(node, source_bytes: bytes) -> Optional[str]:
    """
    Extract the identifier (name) from a Groovy node if it has one.
    Enhanced for Groovy-specific patterns and Emery DSL.
    
    Args:
        node: tree-sitter Node object
        source_bytes: Original source code as bytes
        
    Returns:
        The identifier string or None
    """
    node_type = node.type
    
    # Handle binary operations (e.g., F.rows << newRow)
    if node_type == "binary_op":
        # Extract left-hand side as identifier
        left_child = None
        for child in node.children:
            if child.type in ("dotted_identifier", "member_access", "identifier"):
                left_child = child
                break
        
        if left_child:
            return source_bytes[left_child.start_byte:left_child.end_byte].decode("utf-8", errors="replace")
    
    # Handle function calls (including Emery DSL)
    if node_type == "function_call":
        function_field = node.child_by_field_name("function")
        if function_field:
            if function_field.type == "dotted_identifier":
                # Full dotted name like Emery.form.newForm
                return source_bytes[function_field.start_byte:function_field.end_byte].decode("utf-8", errors="replace")
            elif function_field.type == "member_access":
                # Member access like obj.method
                return source_bytes[function_field.start_byte:function_field.end_byte].decode("utf-8", errors="replace")
            elif function_field.type == "identifier":
                # Simple identifier
                return function_field.text.decode("utf-8") if function_field.text else None
    
    # Handle method definitions with return types
    if node_type == "method_definition":
        # Look for method name, avoiding return type
        for child in node.children:
            if child.type == "identifier":
                # Check if this is the method name (not return type)
                # Method name usually comes after return type or parameters
                method_name = child.text.decode("utf-8") if child.text else None
                if method_name and method_name != "String":  # Avoid return types
                    return method_name
    
    # Try 'name' field (functions, classes, variables)
    name_node = node.child_by_field_name("name")
    if name_node and name_node.text:
        return name_node.text.decode("utf-8")
    
    # For variable declarators and declarations
    if node_type in ("variable_declarator", "declaration"):
        name = node.child_by_field_name("name")
        if name and name.type == "identifier" and name.text:
            return name.text.decode("utf-8")
    
    # For property definitions
    if node_type == "property_definition":
        for child in node.children:
            if child.type == "identifier" and child.text:
                return child.text.decode("utf-8")
    
    # For juxt function calls (Groovy-style)
    if node_type == "juxt_function_call":
        # First child is usually the function name
        if node.children and node.children[0].type == "identifier":
            return node.children[0].text.decode("utf-8") if node.children[0].text else None
    
    return None


def extract_method_name(node, source_bytes: bytes) -> Optional[str]:
    """
    Extract method name from a method call node.
    
    Args:
        node: tree-sitter Node object
        source_bytes: Original source code as bytes
        
    Returns:
        The method name or None
    """
    if node.type == "function_call":
        function_field = node.child_by_field_name("function")
        if function_field:
            if function_field.type == "member_access":
                # obj.method() - get the method part
                property_field = function_field.child_by_field_name("property")
                if property_field and property_field.text:
                    return property_field.text.decode("utf-8")
            elif function_field.type == "identifier":
                return function_field.text.decode("utf-8") if function_field.text else None
    
    elif node.type == "member_access":
        property_field = node.child_by_field_name("property")
        if property_field and property_field.text:
            return property_field.text.decode("utf-8")
    
    return None


# =============================================================================
# CHILD EXTRACTION FUNCTIONS
# =============================================================================

def get_groovy_children_to_parse(node, source_bytes: bytes, context: Dict[str, Any] = None) -> List:
    """
    Get the child nodes that should be parsed recursively for Groovy.
    
    This is the core function that determines what to recurse into
    for each container node type in Groovy.
    
    Args:
        node: tree-sitter Node object
        source_bytes: Original source code as bytes
        context: Additional context for parsing decisions
        
    Returns:
        List of child nodes to parse
    """
    children = []
    node_type = node.type
    context = context or {}
    
    # =========================================================================
    # CLASSES AND INTERFACES
    # =========================================================================
    if node_type in ("class_definition", "interface_definition", "trait_definition", 
                     "enum_definition", "annotation_definition"):
        body = node.child_by_field_name("body")
        if body:
            # Parse all class members (methods, fields, nested classes)
            for child in body.named_children:
                children.append(child)
    
    elif node_type == "class_body":
        # All members
        for child in node.named_children:
            children.append(child)
    
    # =========================================================================
    # METHODS AND FUNCTIONS
    # =========================================================================
    elif node_type in GROOVY_FUNCTION_TYPES:
        if node_type in ("closure", "closure_expression"):
            # Context-dependent closure handling
            if context.get("is_structural"):
                # Structural closure - don't recurse
                return []
            elif context.get("is_collection_method"):
                # Collection method closure - don't recurse (handled as combined)
                return []
        
        body = node.child_by_field_name("body")
        if body:
            if body.type in ("statement_block", "block"):
                # Block body - parse all statements
                for child in body.named_children:
                    children.append(child)
            else:
                # Expression body - parse the expression
                children.append(body)
    
    # =========================================================================
    # IF STATEMENT (Branch-Aware)
    # =========================================================================
    elif node_type == "if_statement":
        # Parse body
        body = node.child_by_field_name("body")
        if body:
            if body.type in ("statement_block", "block"):
                for child in body.named_children:
                    children.append(child)
            else:
                children.append(body)
        
        # Parse else_body (could be else or else-if)
        else_body = node.child_by_field_name("else_body")
        if else_body:
            children.append(else_body)
    
    # =========================================================================
    # SWITCH STATEMENT
    # =========================================================================
    elif node_type == "switch_statement":
        body = node.child_by_field_name("body")
        if body:
            # Parse all cases
            for child in body.named_children:
                children.append(child)
    
    elif node_type == "switch_block":
        for child in node.named_children:
            children.append(child)
    
    elif node_type == "case":
        # All statements after the case label
        for child in node.named_children:
            # Skip case expression (first child), get the statements
            if child.type not in ("number", "string", "identifier", "string_literal"):
                children.append(child)
    
    # =========================================================================
    # LOOPS
    # =========================================================================
    elif node_type in ("for_loop", "for_in_loop", "while_loop", "do_while_statement"):
        body = node.child_by_field_name("body")
        if body:
            if body.type in ("statement_block", "block"):
                for child in body.named_children:
                    children.append(child)
            else:
                # Single statement body
                children.append(body)
    
    # =========================================================================
    # TRY/CATCH/FINALLY
    # =========================================================================
    elif node_type == "try_statement":
        body = node.child_by_field_name("body")
        handler = node.child_by_field_name("handler")
        finalizer = node.child_by_field_name("finalizer")
        
        if body:
            children.append(body)
        if handler:
            children.append(handler)
        if finalizer:
            children.append(finalizer)
    
    elif node_type == "catch_clause":
        body = node.child_by_field_name("body")
        if body:
            if body.type in ("statement_block", "block"):
                for child in body.named_children:
                    children.append(child)
            else:
                children.append(body)
    
    elif node_type == "finally_clause":
        body = node.child_by_field_name("body")
        if body:
            if body.type in ("statement_block", "block"):
                for child in body.named_children:
                    children.append(child)
            else:
                children.append(body)
    
    # =========================================================================
    # BLOCKS
    # =========================================================================
    elif node_type in ("statement_block", "block"):
        for child in node.named_children:
            children.append(child)
    
    elif node_type == "labeled_statement":
        body = node.child_by_field_name("body")
        if body:
            children.append(body)
    
    elif node_type == "synchronized_statement":
        body = node.child_by_field_name("body")
        if body:
            children.append(body)
    
    # =========================================================================
    # VARIABLE DECLARATIONS (check for closure values)
    # =========================================================================
    elif node_type in ("declaration", "variable_declaration"):
        # Check if the value is a closure
        value = node.child_by_field_name("value")
        if value and is_groovy_closure_or_function(value):
            children.append(value)
    
    # =========================================================================
    # FUNCTION CALLS (check for closure arguments)
    # =========================================================================
    elif node_type in ("function_call", "method_call"):
        # Check if this is a collection method call
        if is_collection_method_call(node, source_bytes):
            # For collection methods, closures are combined with the method call
            # Don't recurse into them separately
            return []
        
        # For other function calls, check arguments for closures
        args = node.child_by_field_name("arguments")
        if args:
            for arg in args.named_children:
                if is_groovy_closure_or_function(arg):
                    children.append(arg)
    
    elif node_type == "juxt_function_call":
        # Groovy-style function call - check for closure arguments
        for child in node.named_children:
            if child.type in ("closure", "closure_expression"):
                # Check if this should be combined or parsed separately
                method_name = extract_method_name(node, source_bytes)
                if method_name in GROOVY_COLLECTION_METHODS:
                    # Combined - don't recurse
                    return []
                else:
                    children.append(child)
    
    # =========================================================================
    # ASSIGNMENTS (check for closure values)
    # =========================================================================
    elif node_type == "assignment":
        right = node.child_by_field_name("right")
        if right and is_groovy_closure_or_function(right):
            children.append(right)
    
    return children


def is_groovy_closure_or_function(node) -> bool:
    """
    Check if a node is a closure or contains closures.
    
    Args:
        node: tree-sitter Node object
        
    Returns:
        True if the node is or contains a closure/function
    """
    if node.type in GROOVY_FUNCTION_TYPES:
        return True
    
    if node.type in ("function_call", "method_call", "juxt_function_call"):
        # May contain closures as arguments
        return True
    
    if node.type == "parenthesized_expression":
        # Check what's inside
        for child in node.named_children:
            if is_groovy_closure_or_function(child):
                return True
    
    return False


# =============================================================================
# IF BRANCH EXTRACTION
# =============================================================================

def extract_if_branches(node, source_bytes: bytes) -> List[IfBranch]:
    """
    Extract individual if/else-if/else branches from an if_statement.
    
    Args:
        node: if_statement node
        source_bytes: Original source code as bytes
        
    Returns:
        List of IfBranch objects
    """
    branches = []
    
    # Extract main if branch
    condition = node.child_by_field_name("condition")
    body = node.child_by_field_name("body")
    
    if condition and body:
        condition_code = source_bytes[condition.start_byte:condition.end_byte].decode("utf-8", errors="replace")
        body_code = source_bytes[body.start_byte:body.end_byte].decode("utf-8", errors="replace")
        
        branches.append(IfBranch(
            branch_type="if",
            condition=condition_code,
            condition_hash=hash_content(condition_code),
            body_code=body_code,
            body_hash=hash_content(body_code),
            start_line=body.start_point[0] + 1,
            end_line=body.end_point[0] + 1
        ))
    
    # Extract else/else-if branches
    else_body = node.child_by_field_name("else_body")
    while else_body:
        if else_body.type == "if_statement":
            # else if
            condition = else_body.child_by_field_name("condition")
            body = else_body.child_by_field_name("body")
            
            if condition and body:
                condition_code = source_bytes[condition.start_byte:condition.end_byte].decode("utf-8", errors="replace")
                body_code = source_bytes[body.start_byte:body.end_byte].decode("utf-8", errors="replace")
                
                branches.append(IfBranch(
                    branch_type="else_if",
                    condition=condition_code,
                    condition_hash=hash_content(condition_code),
                    body_code=body_code,
                    body_hash=hash_content(body_code),
                    start_line=body.start_point[0] + 1,
                    end_line=body.end_point[0] + 1
                ))
            
            # Continue with nested else
            else_body = else_body.child_by_field_name("else_body")
        else:
            # final else
            body_code = source_bytes[else_body.start_byte:else_body.end_byte].decode("utf-8", errors="replace")
            
            branches.append(IfBranch(
                branch_type="else",
                condition=None,
                condition_hash=None,
                body_code=body_code,
                body_hash=hash_content(body_code),
                start_line=else_body.start_point[0] + 1,
                end_line=else_body.end_point[0] + 1
            ))
            break
    
    return branches


# =============================================================================
# HASH COMPUTATION
# =============================================================================

def hash_content(content: str) -> str:
    """
    Create a normalized hash of content for comparison.
    
    Normalizes whitespace to match semantically equivalent code.
    """
    if not content:
        return ""
    
    # Normalize whitespace and remove empty lines
    lines = [line.strip() for line in content.split('\n') if line.strip()]
    normalized = " ".join(lines)
    return hashlib.sha256(normalized.encode("utf-8")).hexdigest()


def hash_structure(node_type: str, child_hashes: List[str]) -> str:
    """
    Create a structural hash from node type and children.
    
    This allows matching nodes with same structure but different content.
    """
    combined = node_type + ":" + ",".join(sorted(child_hashes))
    return hashlib.sha256(combined.encode("utf-8")).hexdigest()


def compute_groovy_body_hash(node, source_bytes: bytes) -> Optional[str]:
    """
    Compute hash of method/closure body only (excluding name and parameters).
    
    This allows detecting renamed methods with identical bodies.
    """
    body = node.child_by_field_name("body")
    if not body:
        return None
    
    body_code = source_bytes[body.start_byte:body.end_byte].decode("utf-8", errors="replace")
    return hash_content(body_code)


# =============================================================================
# RECURSIVE PARSING
# =============================================================================

def parse_groovy_recursive(
    node,
    source_bytes: bytes,
    depth: int = 0,
    parent_hash: Optional[str] = None,
    path: str = "",
    context: Dict[str, Any] = None,
    max_depth: int = 50
) -> RecursiveNodeSignature:
    """
    Recursively parse a Groovy node and all its children until pure statements.
    
    This is the main recursive parsing function for Groovy with enhanced
    support for closures, collection methods, and Emery DSL.
    
    Args:
        node: tree-sitter Node object
        source_bytes: Original source code as bytes
        depth: Current nesting depth
        parent_hash: Hash of parent node
        path: Current path for context (e.g., "class:Foo/method:bar/if:0")
        context: Additional parsing context
        max_depth: Maximum recursion depth
        
    Returns:
        RecursiveNodeSignature for this node and all descendants
    """
    context = context or {}
    
    # Recursion protection
    if depth > max_depth:
        # Create a simple signature for deeply nested nodes
        code = source_bytes[node.start_byte:node.end_byte].decode("utf-8", errors="replace")
        return RecursiveNodeSignature(
            node_type=node.type,
            content_hash=hash_content(code),
            structure_hash=hash_content(code),
            start_line=node.start_point[0] + 1,
            end_line=node.end_point[0] + 1,
            depth=depth,
            is_pure_statement=True,
            code=code
        )
    
    # 1. Get node content and compute content hash
    code = source_bytes[node.start_byte:node.end_byte].decode("utf-8", errors="replace")
    content_hash = hash_content(code)
    
    # 2. Determine node category and context
    node_type = node.type
    
    # Check for collection method context
    is_collection_method = is_collection_method_call(node, source_bytes)
    context["is_collection_method"] = is_collection_method
    
    # Check for structural closure context
    parent_node = context.get("parent_node")
    is_structural = is_structural_closure(node, parent_node)
    context["is_structural"] = is_structural
    
    # Classify node
    category = classify_groovy_node(node_type, context)
    is_pure = (category == GroovyNodeCategory.PURE_STATEMENT)
    is_cont = (category == GroovyNodeCategory.CONTAINER)
    is_combined = (category == GroovyNodeCategory.CLOSURE_COMBINED)
    
    # 3. Extract identifier
    identifier = extract_groovy_identifier(node, source_bytes)
    
    # 4. Check for Emery DSL
    is_emery, emery_module, emery_method = is_emery_dsl_call(node, source_bytes)
    
    # 5. Build path for context
    current_path = f"{path}/{node_type}"
    if identifier:
        current_path += f":{identifier}"
    
    # 6. Create signature
    signature = RecursiveNodeSignature(
        node_type=node_type,
        identifier=identifier,
        content_hash=content_hash,
        structure_hash="",  # computed after children
        body_hash=None,
        start_line=node.start_point[0] + 1,
        end_line=node.end_point[0] + 1,
        start_byte=node.start_byte,
        end_byte=node.end_byte,
        depth=depth,
        parent_hash=parent_hash,
        path=current_path,
        children=[],
        is_pure_statement=is_pure,
        is_container=is_cont,
        is_closure_combined=is_combined,
        is_emery_dsl=is_emery,
        is_collection_method=is_collection_method,
        closure_context=context.get("closure_context"),
        emery_module=emery_module,
        emery_method=emery_method,
        code=code,
    )
    
    # 7. Extract if branches for if statements
    if node_type == "if_statement":
        signature.if_branches = extract_if_branches(node, source_bytes)
    
    # 8. If pure statement or combined, stop recursion
    if is_pure or is_combined:
        signature.structure_hash = content_hash
        return signature
    
    # 9. Otherwise, recurse into children
    child_context = context.copy()
    child_context["parent_node"] = node
    child_nodes = get_groovy_children_to_parse(node, source_bytes, child_context)
    
    for child in child_nodes:
        child_sig = parse_groovy_recursive(
            child,
            source_bytes,
            depth + 1,
            content_hash,
            current_path,
            child_context,
            max_depth
        )
        signature.children.append(child_sig)
    
    # 10. Compute structure hash from children
    child_hashes = [c.structure_hash for c in signature.children]
    signature.structure_hash = hash_structure(node_type, child_hashes)
    
    # 11. Compute body hash for functions/methods
    if is_groovy_function_type(node_type):
        signature.body_hash = compute_groovy_body_hash(node, source_bytes)
    
    return signature


# =============================================================================
# UTILITY FUNCTIONS
# =============================================================================

def set_recursion_limit(limit: int = 5000):
    """Set Python recursion limit for deep AST parsing."""
    sys.setrecursionlimit(limit)


def print_groovy_signature(sig: RecursiveNodeSignature, indent: int = 0):
    """
    Print a recursive signature tree for debugging.
    
    Args:
        sig: RecursiveNodeSignature to print
        indent: Current indentation level
    """
    prefix = "  " * indent
    name = sig.identifier or "<anonymous>"
    
    print(f"{prefix}{sig.node_type}: {name}")
    print(f"{prefix}  hash: {sig.content_hash[:12]}...")
    print(f"{prefix}  lines: {sig.start_line}-{sig.end_line}")
    print(f"{prefix}  depth: {sig.depth}")
    print(f"{prefix}  pure: {sig.is_pure_statement}")
    print(f"{prefix}  container: {sig.is_container}")
    print(f"{prefix}  combined: {sig.is_closure_combined}")
    
    if sig.is_emery_dsl:
        print(f"{prefix}  emery: {sig.emery_module}.{sig.emery_method}")
    
    if sig.if_branches:
        print(f"{prefix}  if_branches: {len(sig.if_branches)}")
        for i, branch in enumerate(sig.if_branches):
            print(f"{prefix}    {i}: {branch.branch_type} ({branch.start_line}-{branch.end_line})")
    
    for child in sig.children:
        print_groovy_signature(child, indent + 1)


# =============================================================================
# USAGE EXAMPLE
# =============================================================================

def example_groovy_usage():
    """
    Example of how to use the recursive parser for Groovy.
    """
    try:
        import tree_sitter_groovy as tsgroovy
        from tree_sitter import Language, Parser
    except ImportError:
        print("tree_sitter_groovy not available for example")
        return
    
    # Initialize parser
    language = Language(tsgroovy.language())
    parser = Parser(language)
    
    # Sample Groovy code with closures and Emery DSL
    source = b"""
    class DataProcessor {
        def processData(items) {
            // Collection method with closure (combined)
            def filtered = items.findAll { item ->
                return item.isActive && item.value > 0
            }
            
            // Emery DSL call
            USER_PROFILE_FORM formObj = Emery.form.newForm("USER_PROFILE_FORM")
            
            // Control flow with nested statements
            if (filtered.size() > 0) {
                filtered.each { item ->
                    println "Processing: ${item.name}"
                }
            } else {
                println "No items to process"
            }
            
            // Emery form field access
            F.userName = "john.doe"
            F.skillsMultiRow.rows << newRow
            
            return filtered
        }
    }
    """
    
    # Set recursion limit
    set_recursion_limit(5000)
    
    # Parse
    tree = parser.parse(source)
    
    # Recursive parse
    signature = parse_groovy_recursive(tree.root_node, source)
    
    # Print structure
    print("Groovy AST Recursive Structure:")
    print("=" * 50)
    print_groovy_signature(signature)


if __name__ == "__main__":
    example_groovy_usage()