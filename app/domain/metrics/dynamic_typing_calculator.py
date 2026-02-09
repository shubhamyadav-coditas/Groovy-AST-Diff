"""
Dynamic Typing & Meta-Programming Calculator for Groovy AST nodes.

Calculates complexity related to dynamic typing and meta-programming features
specific to Groovy, such as 'def' keyword usage, annotations, meta-methods,
and metaClass modifications.
"""

from typing import List, Tuple, Set
from tree_sitter import Node
from .base_metric_calculator import BaseMetricCalculator


class DynamicTypingCalculator(BaseMetricCalculator):
    """
    Calculates dynamic typing and meta-programming complexity for Groovy code.
    
    This metric captures the complexity introduced by:
    - Dynamic typing (def keyword)
    - Annotations (@)
    - Meta-programming methods (invokeMethod, propertyMissing, methodMissing, etc.)
    - metaClass modifications
    
    All constructs have equal weight of 1.0 point each.
    """
    
    # Node types that indicate dynamic typing
    DYNAMIC_TYPING_NODES = {
        'def_declaration',
        'variable_declaration',  # when using 'def'
        'method_declaration'     # when return type is 'def'
    }
    
    # Annotation node types
    ANNOTATION_NODES = {
        'annotation',
        'annotation_list'
    }
    
    # Meta-programming method names
    META_PROGRAMMING_METHODS: Set[str] = {
        'invokeMethod',
        'propertyMissing', 
        'methodMissing',
        'getProperty',
        'setProperty',
        'getMetaClass',
        'setMetaClass',
    }
    
    # MetaClass-related patterns
    METACLASS_PATTERNS: Set[str] = {
        'metaClass',
        'ExpandoMetaClass',
    }
    
    def calculate(self, ast_node: Node, source_bytes: bytes) -> float:
        """
        Calculate dynamic typing complexity for the given AST node.
        
        Args:
            ast_node: Root AST node of the code block to analyze
            source_bytes: Complete source code as bytes
            
        Returns:
            Dynamic typing complexity score (float)
        """
        score = 0.0
        
        # Count def keyword usage (weight: 1.0)
        score += self._count_def_usage(ast_node, source_bytes) * 1.0
        
        # Count annotations (weight: 1.0)
        score += self._count_annotations(ast_node) * 1.0
        
        # Count meta-programming methods (weight: 1.0)
        score += self._count_meta_programming(ast_node, source_bytes) * 1.0
        
        # Count metaClass usage (weight: 1.0)
        score += self._count_metaclass_usage(ast_node, source_bytes) * 1.0
        
        return score
    
    def _count_def_usage(self, node: Node, source_bytes: bytes) -> int:
        """
        Count usage of the 'def' keyword in the AST subtree.
        
        Args:
            node: Current AST node
            source_bytes: Source code as bytes
            
        Returns:
            Number of 'def' usages found
        """
        count = 0
        
        def count_def(current_node):
            nonlocal count
            
            # Look for 'def' keyword nodes directly
            if current_node.type == 'def':
                count += 1
            
            # Recursively check children
            for child in current_node.children:
                count_def(child)
        
        count_def(node)
        return count
    
    def _count_annotations(self, node: Node) -> int:
        """
        Count annotation usage in the AST subtree.
        
        Args:
            node: Current AST node
            
        Returns:
            Number of annotations found
        """
        count = 0
        
        def count_annotations_recursive(current_node):
            nonlocal count
            
            if current_node.type in self.ANNOTATION_NODES:
                count += 1
            
            # Recursively check children
            for child in current_node.children:
                count_annotations_recursive(child)
        
        count_annotations_recursive(node)
        return count
    
    def _count_meta_programming(self, node: Node, source_bytes: bytes) -> int:
        """
        Count meta-programming method usage in the AST subtree.
        
        Counts occurrences of: invokeMethod, propertyMissing, methodMissing,
        getProperty, setProperty, getMetaClass, setMetaClass
        
        Args:
            node: Current AST node
            source_bytes: Source code as bytes
            
        Returns:
            Number of meta-programming method occurrences found
        """
        count = 0
        counted_positions = set()  # Avoid double-counting
        
        def count_meta_methods(current_node):
            nonlocal count
            
            # Check identifier nodes for meta-programming method names
            if current_node.type == 'identifier':
                node_text = self._get_node_text(current_node, source_bytes)
                pos = (current_node.start_byte, current_node.end_byte)
                
                if node_text in self.META_PROGRAMMING_METHODS and pos not in counted_positions:
                    count += 1
                    counted_positions.add(pos)
            
            # Recursively check children
            for child in current_node.children:
                count_meta_methods(child)
        
        count_meta_methods(node)
        return count
    
    def _count_metaclass_usage(self, node: Node, source_bytes: bytes) -> int:
        """
        Count metaClass usage in the AST subtree.
        
        Looks for:
        - .metaClass access/modifications
        - ExpandoMetaClass usage
        
        Args:
            node: Current AST node
            source_bytes: Source code as bytes
            
        Returns:
            Number of metaClass usages found
        """
        count = 0
        counted_positions = set()  # Avoid double-counting same occurrence
        
        def count_metaclass(current_node):
            nonlocal count
            
            # Check for identifier nodes
            if current_node.type == 'identifier':
                node_text = self._get_node_text(current_node, source_bytes)
                pos = (current_node.start_byte, current_node.end_byte)
                
                # Check for metaClass patterns
                if node_text in self.METACLASS_PATTERNS and pos not in counted_positions:
                    count += 1
                    counted_positions.add(pos)
            
            for child in current_node.children:
                count_metaclass(child)
        
        count_metaclass(node)
        return count
    
    def get_dynamic_typing_breakdown(self, ast_node: Node, source_bytes: bytes) -> dict:
        """
        Get a detailed breakdown of dynamic typing complexity contributors.
        
        Args:
            ast_node: Root AST node to analyze
            source_bytes: Source code as bytes
            
        Returns:
            Dictionary with breakdown of dynamic typing sources
        """
        def_count = self._count_def_usage(ast_node, source_bytes)
        annotation_count = self._count_annotations(ast_node)
        meta_count = self._count_meta_programming(ast_node, source_bytes)
        metaclass_count = self._count_metaclass_usage(ast_node, source_bytes)
        
        total_score = def_count + annotation_count + meta_count + metaclass_count
        
        return {
            'def_usage': {
                'count': def_count,
                'weight': 1.0,
                'score': def_count * 1.0
            },
            'annotations': {
                'count': annotation_count,
                'weight': 1.0,
                'score': annotation_count * 1.0
            },
            'meta_programming': {
                'count': meta_count,
                'weight': 1.0,
                'score': meta_count * 1.0
            },
            'metaclass_usage': {
                'count': metaclass_count,
                'weight': 1.0,
                'score': metaclass_count * 1.0
            },
            'total_score': total_score
        }
    
    def _find_def_declarations(self, node: Node, source_bytes: bytes) -> list:
        """
        Find all 'def' declarations in the AST subtree.
        
        Useful for detailed analysis and debugging.
        
        Args:
            node: Root AST node
            source_bytes: Source code as bytes
            
        Returns:
            List of nodes that contain 'def' declarations
        """
        def_nodes = []
        
        def find_def_recursive(current_node):
            if current_node.type in self.DYNAMIC_TYPING_NODES:
                node_text = self._get_node_text(current_node, source_bytes)
                if 'def ' in node_text or node_text.startswith('def'):
                    def_nodes.append(current_node)
            
            for child in current_node.children:
                find_def_recursive(child)
        
        find_def_recursive(node)
        return def_nodes
