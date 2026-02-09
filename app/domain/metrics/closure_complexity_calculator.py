"""
Closure Complexity Calculator for Groovy AST nodes.

Calculates complexity related to closures, including count and nesting depth.
Closures add significant complexity due to scope capture, deferred execution,
and potential for deep nesting.
"""

from typing import List, Tuple
from tree_sitter import Node
from .base_metric_calculator import BaseMetricCalculator


class ClosureComplexityCalculator(BaseMetricCalculator):
    """
    Calculates closure complexity for Groovy code blocks.
    
    Closure complexity considers:
    - Number of closures in the code
    - Nesting depth of closures
    - Types of closure usage (method calls vs standalone)
    
    Formula: closure_count + (0.5 * max_nesting_depth)
    """
    
    # Node types that represent closures
    CLOSURE_NODE_TYPES = {
        'closure',
        'closure_expression',
        'lambda_expression'
    }
    
    # Method names commonly used with closures
    CLOSURE_METHOD_PATTERNS = {
        'each', 'find', 'findAll', 'collect', 'select', 'reject',
        'sort', 'groupBy', 'any', 'every', 'inject', 'fold',
        'filter', 'map', 'reduce', 'forEach', 'withDefault'
    }

    # Parent types that indicate this is NOT a true closure (it's a structural block)
    NON_CLOSURE_PARENT_TYPES = {
        'class_definition',      # Class body
        'function_definition',   # Method body
        'for_loop',              # For loop body
        'for_in_loop',           # For-in loop body
        'while_loop',            # While loop body
        'do_while_loop',         # Do-while loop body
        'if_statement',          # If/else body
        'try_statement',         # Try/catch/finally body
        'switch_statement',      # Switch body
        'switch_block',          # Switch block
    }
    
    # Parent types that indicate this IS a true closure
    TRUE_CLOSURE_PARENT_TYPES = {
        'variable_declarator',   # Closure assigned to variable
        'assignment',            # Closure in assignment
        'declaration',           # Closure in declaration
        'argument_list',         # Closure passed as argument
        'juxt_function_call',    # Closure passed to method (Groovy style)
        'method_call',           # Closure passed to method
        'function_call',         # Closure passed to function
        'return',                # Closure returned from method
        'binary_op',             # Closure in binary operation
    }
    
    def calculate(self, ast_node: Node, source_bytes: bytes) -> float:
        """
        Calculate closure complexity for the given AST node.
        
        Args:
            ast_node: Root AST node of the code block to analyze
            source_bytes: Complete source code as bytes
            
        Returns:
            Closure complexity score (float)
        """
        # Find all closures in the subtree
        closures = self._find_all_closures(ast_node, source_bytes)
        
        if not closures:
            return 0.0
        
        closure_count = len(closures)
        max_depth = self._calculate_max_nesting_depth(closures)
        
        # Formula: count + (0.5 * max_depth)
        complexity = closure_count + (0.5 * max_depth)
        
        return complexity
    
    def _find_all_closures(self, node: Node, source_bytes: bytes) -> list:
        """
        Find all TRUE closure expressions in the AST subtree.
        
        Only counts closures that are:
        - Assigned to variables (def fn = { })
        - Passed as arguments to methods (list.each { })
        - Returned from methods
        
        Does NOT count structural blocks like:
        - Class bodies
        - Method bodies
        - Loop bodies
        - If/else bodies
        
        Args:
            node: Root AST node
            source_bytes: Source code as bytes
            
        Returns:
            List of true closure nodes found
        """
        closures = []
        
        def find_closures_recursive(current_node):
            # Check if this is a closure node
            if current_node.type in self.CLOSURE_NODE_TYPES:
                # Check if it's a TRUE closure by examining its parent
                if self._is_true_closure(current_node):
                    closures.append(current_node)
            
            # Recursively search children
            for child in current_node.children:
                find_closures_recursive(child)
        
        find_closures_recursive(node)
        return closures
    
    def _is_true_closure(self, closure_node: Node) -> bool:
        """
        Determine if a closure node represents a TRUE closure (not a structural block).
        
        Args:
            closure_node: The closure node to check
            
        Returns:
            True if this is a true closure, False if it's a structural block
        """
        parent = closure_node.parent
        if not parent:
            return False
        
        parent_type = parent.type
        
        # Definitely NOT a closure if parent is a structural construct
        if parent_type in self.NON_CLOSURE_PARENT_TYPES:
            return False
        
        # Definitely IS a closure if parent indicates closure usage
        if parent_type in self.TRUE_CLOSURE_PARENT_TYPES:
            return True
        
        # For other cases, check grandparent
        grandparent = parent.parent
        if grandparent:
            grandparent_type = grandparent.type
            
            # If grandparent is a structural construct, this is likely a nested block
            if grandparent_type in self.NON_CLOSURE_PARENT_TYPES:
                return False
            
            # If grandparent indicates closure usage
            if grandparent_type in self.TRUE_CLOSURE_PARENT_TYPES:
                return True

        return False
    
    def _is_closure_method_call(self, method_call_node: Node, source_bytes: bytes) -> bool:
        """
        Check if a method call is likely to use closures.
        
        Args:
            method_call_node: Method call AST node
            source_bytes: Source code as bytes
            
        Returns:
            True if the method call typically uses closures
        """
        method_text = self._get_node_text(method_call_node, source_bytes)
        
        for pattern in self.CLOSURE_METHOD_PATTERNS:
            if pattern in method_text:
                return True
        
        return False
    
    def _find_closure_arguments(self, method_call_node: Node) -> list:
        """
        Find closure arguments in a method call.
        
        Args:
            method_call_node: Method call AST node
            
        Returns:
            List of closure nodes that are arguments to the method
        """
        closure_args = []
        
        def find_closure_args_recursive(node):
            if node.type in self.CLOSURE_NODE_TYPES:
                closure_args.append(node)
            
            for child in node.children:
                find_closure_args_recursive(child)
        
        find_closure_args_recursive(method_call_node)
        return closure_args
    
    def _is_block_used_as_closure(self, block_node: Node) -> bool:
        """
        Check if a block is being used as a closure.
        
        Args:
            block_node: Block AST node
            
        Returns:
            True if the block appears to be used as a closure
        """
        # Check parent context to see if this block is a closure
        parent = block_node.parent
        if not parent:
            return False
        
        # If parent is a method call, this might be a closure argument
        if parent.type == 'method_call':
            return True
        
        # If parent is an assignment where the block is on the right side
        if parent.type in ['assignment', 'variable_declaration']:
            return True
        
        return False
    
    def _calculate_max_nesting_depth(self, closures: list) -> int:
        """
        Calculate the maximum nesting depth among all closures.
        
        Args:
            closures: List of closure nodes
            
        Returns:
            Maximum nesting depth found
        """
        max_depth = 0
        
        for closure in closures:
            depth = self._calculate_closure_nesting_depth(closure)
            max_depth = max(max_depth, depth)
        
        return max_depth
    
    def _calculate_closure_nesting_depth(self, closure_node: Node) -> int:
        """
        Calculate the nesting depth of a specific closure.
        
        Counts how many parent TRUE closures this closure is nested within.
        Only counts parent closures that are themselves true closures
        (not class bodies, method bodies, etc.).
        
        Args:
            closure_node: The closure node to analyze
            
        Returns:
            Nesting depth (0 for top-level closures)
        """
        depth = 0
        parent = closure_node.parent
        
        while parent:
            if parent.type in self.CLOSURE_NODE_TYPES:
                # Only count if this parent closure is a TRUE closure
                if self._is_true_closure(parent):
                    depth += 1
            parent = parent.parent
        
        return depth
    
    def get_closure_breakdown(self, ast_node: Node, source_bytes: bytes) -> dict:
        """
        Get a detailed breakdown of closure complexity.
        
        Args:
            ast_node: Root AST node to analyze
            source_bytes: Source code as bytes
            
        Returns:
            Dictionary with detailed closure analysis
        """
        closures = self._find_all_closures(ast_node, source_bytes)
        
        if not closures:
            return {
                'closure_count': 0,
                'max_nesting_depth': 0,
                'total_complexity': 0.0,
                'closures_by_depth': {}
            }
        
        # Group closures by nesting depth
        closures_by_depth = {}
        for closure in closures:
            depth = self._calculate_closure_nesting_depth(closure)
            if depth not in closures_by_depth:
                closures_by_depth[depth] = []
            closures_by_depth[depth].append(closure)
        
        max_depth = max(closures_by_depth.keys()) if closures_by_depth else 0
        total_complexity = len(closures) + (0.5 * max_depth)
        
        return {
            'closure_count': len(closures),
            'max_nesting_depth': max_depth,
            'total_complexity': total_complexity,
            'closures_by_depth': {
                depth: len(nodes) for depth, nodes in closures_by_depth.items()
            },
            'complexity_formula': f"{len(closures)} + (0.5 * {max_depth}) = {total_complexity}"
        }
    
    def _analyze_closure_patterns(self, ast_node: Node, source_bytes: bytes) -> dict:
        """
        Analyze different patterns of closure usage.
        
        Args:
            ast_node: Root AST node
            source_bytes: Source code as bytes
            
        Returns:
            Dictionary with closure pattern analysis
        """
        closures = self._find_all_closures(ast_node, source_bytes)
        
        patterns = {
            'method_closures': 0,      # Closures passed to methods
            'standalone_closures': 0,   # Standalone closure definitions
            'nested_closures': 0,       # Closures inside other closures
            'simple_closures': 0        # Non-nested closures
        }
        
        for closure in closures:
            depth = self._calculate_closure_nesting_depth(closure)
            
            if depth > 0:
                patterns['nested_closures'] += 1
            else:
                patterns['simple_closures'] += 1
            
            # Check if closure is an argument to a method
            parent = closure.parent
            if parent and parent.type == 'method_call':
                patterns['method_closures'] += 1
            else:
                patterns['standalone_closures'] += 1
        
        return patterns
