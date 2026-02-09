"""
Base class for metric calculators.

Provides common functionality for AST-based metric extraction.
"""

from abc import ABC, abstractmethod
from typing import Any, Union
from tree_sitter import Language, Node, Query


class BaseMetricCalculator(ABC):
    """
    Base class for all metric calculators.
    
    Provides common AST traversal utilities and defines the interface
    that all metric calculators must implement.
    """
    
    def __init__(self, language: Language):
        """
        Initialize the metric calculator.
        
        Args:
            language: Tree-sitter Language instance for Groovy
        """
        self.language = language
        self._queries = {}
    
    @abstractmethod
    def calculate(self, ast_node: Node, source_bytes: bytes) -> Union[int, float]:
        """
        Calculate the metric value for the given AST node.
        
        Args:
            ast_node: The AST node to analyze (root of changed block)
            source_bytes: The complete source code as bytes
            
        Returns:
            The calculated metric value
        """
        pass
    
    def _create_query(self, query_string: str, query_name: str) -> Query:
        """
        Create and cache a Tree-sitter query.
        
        Args:
            query_string: The Tree-sitter query string
            query_name: Name for caching the query
            
        Returns:
            Compiled Tree-sitter Query object
        """
        if query_name not in self._queries:
            try:
                self._queries[query_name] = self.language.query(query_string)
            except Exception as e:
                # If query creation fails, return a dummy query that matches nothing
                print(f"Warning: Failed to create query '{query_name}': {e}")
                # Create a query that matches nothing as fallback
                self._queries[query_name] = self.language.query("(ERROR) @error")
        
        return self._queries[query_name]
    
    def _traverse_recursively(self, node: Node, visit_func, accumulator=None):
        """
        Recursively traverse an AST node and its children.
        
        Args:
            node: Starting AST node
            visit_func: Function to call for each node (node, accumulator) -> accumulator
            accumulator: Accumulator value passed to visit_func
            
        Returns:
            Final accumulator value after traversing all nodes
        """
        # Visit current node
        if accumulator is None:
            accumulator = 0
        accumulator = visit_func(node, accumulator)
        
        # Recursively visit all children
        for child in node.children:
            accumulator = self._traverse_recursively(child, visit_func, accumulator)
            
        return accumulator
    
    def _get_node_text(self, node: Node, source_bytes: bytes) -> str:
        """
        Get the text content of an AST node.
        
        Args:
            node: AST node
            source_bytes: Source code as bytes
            
        Returns:
            Text content of the node
        """
        try:
            return source_bytes[node.start_byte:node.end_byte].decode('utf-8')
        except (UnicodeDecodeError, IndexError):
            return ""
    
    def _count_nodes_by_type(self, root_node: Node, node_types: list) -> int:
        """
        Count nodes of specific types within a subtree.
        
        Args:
            root_node: Root of the subtree to search
            node_types: List of node type strings to count
            
        Returns:
            Total count of matching nodes
        """
        def count_visitor(node, count):
            if node.type in node_types:
                return count + 1
            return count
        
        return self._traverse_recursively(root_node, count_visitor, 0)
    
    def _find_nodes_by_type(self, root_node: Node, node_types: list) -> list:
        """
        Find all nodes of specific types within a subtree.
        
        Args:
            root_node: Root of the subtree to search
            node_types: List of node type strings to find
            
        Returns:
            List of matching nodes
        """
        found_nodes = []
        
        def collect_visitor(node, nodes_list):
            if node.type in node_types:
                nodes_list.append(node)
            return nodes_list
        
        return self._traverse_recursively(root_node, collect_visitor, found_nodes)
    
    def _get_line_count_in_range(self, source_bytes: bytes, start_line: int, end_line: int) -> int:
        """
        Count non-blank, non-comment lines in a specific range.
        
        Args:
            source_bytes: Source code as bytes
            start_line: Starting line number (0-based)
            end_line: Ending line number (0-based)
            
        Returns:
            Count of significant lines
        """
        try:
            source_text = source_bytes.decode('utf-8')
            lines = source_text.split('\n')
            
            count = 0
            for i in range(start_line, min(end_line + 1, len(lines))):
                line = lines[i].strip()
                # Skip blank lines and comments
                if line and not line.startswith('//') and not line.startswith('/*'):
                    count += 1
                    
            return count
        except (UnicodeDecodeError, IndexError):
            return 0