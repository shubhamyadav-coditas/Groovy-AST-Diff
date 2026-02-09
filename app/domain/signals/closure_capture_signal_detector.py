"""
Closure Mutable Capture Signal Detector for Groovy.

Detects closures that capture mutable outer state, which can lead to
unexpected behavior and race conditions.
"""

from typing import Set
from tree_sitter import Node
from .base_signal_detector import BaseSignalDetector


class ClosureCaptureSignalDetector(BaseSignalDetector):
    """
    Detects closures that capture mutable outer state.
    
    Looks for:
    - Closures that reference variables defined with 'def' outside the closure
    - Closures that potentially modify outer variables
    
    Default score: +8 per occurrence
    """
    
    # Parent types that indicate a closure is NOT a lambda/callback
    # These are structural body blocks, not actual closures
    # Reference: app/types/groovy_types.py for correct AST node type names
    NON_LAMBDA_PARENTS: set = {
        # Class/Interface definitions
        'class_definition',
        'interface_definition',
        'trait_definition',
        'enum_definition',
        'annotation_definition',
        
        # Function/Method definitions
        'function_definition',
        'method_definition',
        'constructor_definition',
        
        # Control flow statements (using correct Groovy AST node types)
        'if_statement',
        'else_clause',
        'for_loop',              # NOT for_statement
        'for_in_loop',           # NOT for_in_statement
        'while_loop',            # NOT while_statement
        'do_while_loop',
        'try_statement',
        'catch_clause',
        'finally_clause',
        'switch_statement',
        'switch_block',          # Container for switch cases
        'case',                  # Switch case container
        
        # Block containers
        'block',
        'statement_block',
        'closure_expression',
        
        # Synchronized block
        'synchronized_statement',
    }
    
    @property
    def signal_name(self) -> str:
        return 'closure_mutable_capture'
    
    @property
    def default_score(self) -> float:
        return 8.0
    
    def detect(self, root_node: Node, source_bytes: bytes, **kwargs) -> int:
        """
        Detect closures that capture mutable outer state.
        
        Args:
            root_node: The root AST node to analyze
            source_bytes: The source code as bytes
            
        Returns:
            Count of closures capturing mutable state
        """
        # First, collect all def variable declarations at outer scope
        outer_def_vars: Set[str] = set()
        
        def collect_outer_defs(node: Node, in_lambda: bool = False):
            """
            Collect def variables from outer scope.
            
            We distinguish between:
            - Function body closures (direct child of function_definition) - continue collecting
            - Class body closures (direct child of class_definition) - continue collecting
            - Lambda/callback closures (child of function_call, etc.) - stop collecting
            """
            # Check if this is a lambda closure (not a function/class/control-flow body)
            if node.type == 'closure':
                parent = node.parent
                # If parent is a structural body (function, class, if, while, etc.) - continue
                # If parent is function_call, juxt_function_call, etc. - this is a lambda, stop
                if parent and parent.type not in self.NON_LAMBDA_PARENTS:
                    return  # Don't recurse into lambda closures
            
            # Handle 'declaration' node type: def counter = 0
            # Structure: declaration -> def, variable_declarator -> identifier
            if node.type == 'declaration':
                has_def = False
                for child in node.children:
                    if child.type == 'def':
                        has_def = True
                        break
                
                if has_def:
                    # Find identifier in variable_declarator children
                    for child in node.children:
                        if child.type == 'variable_declarator':
                            for subchild in child.children:
                                if subchild.type == 'identifier':
                                    var_name = self._get_node_text(subchild, source_bytes)
                                    outer_def_vars.add(var_name)
                                    break
            
            # Handle assignment with 'def': def x = value (alternative structure)
            if node.type == 'assignment':
                for child in node.children:
                    if child.type == 'def':
                        for sibling in node.children:
                            if sibling.type == 'identifier':
                                var_name = self._get_node_text(sibling, source_bytes)
                                outer_def_vars.add(var_name)
                                break
                        break
            
            for child in node.children:
                collect_outer_defs(child, in_lambda)
        
        collect_outer_defs(root_node)
        
        # Now check closures for references to these variables
        count = 0
        
        def check_closures(node: Node):
            nonlocal count
            
            if node.type == 'closure':
                # Only count lambda closures, not function/class/control-flow bodies
                parent = node.parent
                if parent and parent.type not in self.NON_LAMBDA_PARENTS:
                    # This is a lambda/callback closure
                    closure_text = self._get_node_text(node, source_bytes)
                    for var_name in outer_def_vars:
                        # Simple check: if variable name appears in closure
                        if var_name in closure_text:
                            count += 1
                            break
            
            for child in node.children:
                check_closures(child)
        
        check_closures(root_node)
        return count
