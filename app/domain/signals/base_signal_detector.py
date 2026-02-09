"""
Base class for signal detectors.

Provides common functionality for AST-based signal detection.
"""

from abc import ABC, abstractmethod
from typing import Optional
from tree_sitter import Node


class BaseSignalDetector(ABC):
    """
    Base class for all signal detectors.
    
    Provides common AST traversal utilities and defines the interface
    that all signal detectors must implement.
    """
    
    @abstractmethod
    def detect(self, root_node: Node, source_bytes: bytes, **kwargs) -> int:
        """
        Detect the signal in the given AST.
        
        Args:
            root_node: The root AST node to analyze
            source_bytes: The source code as bytes
            **kwargs: Additional arguments specific to the signal type
            
        Returns:
            Count of signal occurrences detected
        """
        pass
    
    @property
    @abstractmethod
    def signal_name(self) -> str:
        """Return the name of this signal type."""
        pass
    
    @property
    @abstractmethod
    def default_score(self) -> float:
        """Return the default score for this signal type."""
        pass
    
    def _get_node_text(self, node: Node, source_bytes: bytes) -> str:
        """Get the text content of an AST node."""
        try:
            return source_bytes[node.start_byte:node.end_byte].decode('utf-8')
        except (UnicodeDecodeError, IndexError):
            return ""
    
    def _traverse_recursive(self, node: Node, visit_func, accumulator=None):
        """
        Recursively traverse an AST node and its children.
        
        Args:
            node: Starting AST node
            visit_func: Function to call for each node (node, accumulator) -> accumulator
            accumulator: Accumulator value passed to visit_func
            
        Returns:
            Final accumulator value after traversing all nodes
        """
        if accumulator is None:
            accumulator = 0
        accumulator = visit_func(node, accumulator)
        
        for child in node.children:
            accumulator = self._traverse_recursive(child, visit_func, accumulator)
        
        return accumulator
    
    def _extract_method_name(self, node: Node, source_bytes: bytes) -> Optional[str]:
        """Extract method name from a method call node."""
        # Try to find identifier child
        for child in node.children:
            if child.type == 'identifier':
                return self._get_node_text(child, source_bytes)
        
        # For chained calls, get the first part
        text = self._get_node_text(node, source_bytes)
        if '(' in text:
            method_part = text.split('(')[0]
            if '.' in method_part:
                return method_part.split('.')[-1]
            return method_part
        
        return None
