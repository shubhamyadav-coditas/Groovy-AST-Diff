"""
Dynamic Method Resolution Signal Detector for Groovy.

Detects dynamic method resolution patterns which indicate runtime
method dispatch that cannot be statically analyzed.
"""

from typing import Set
from tree_sitter import Node
from .base_signal_detector import BaseSignalDetector


class DynamicMethodSignalDetector(BaseSignalDetector):
    """
    Detects dynamic method resolution patterns in Groovy code.
    
    Looks for:
    - invokeMethod, methodMissing, propertyMissing definitions
    - getProperty, setProperty overrides
    - Other dynamic method patterns like getAt, putAt
    
    Default score: +10 per occurrence
    """
    
    # Dynamic method resolution patterns
    DYNAMIC_METHOD_NAMES: Set[str] = {
        'invokeMethod', 'methodMissing', 'propertyMissing',
        'getProperty', 'setProperty', 'getAt', 'putAt',
        'respondsTo', 'hasProperty', 'getMetaClass',
    }
    
    @property
    def signal_name(self) -> str:
        return 'dynamic_method_resolution'
    
    @property
    def default_score(self) -> float:
        return 10.0
    
    def detect(self, root_node: Node, source_bytes: bytes, **kwargs) -> int:
        """
        Detect dynamic method resolution patterns.
        
        Args:
            root_node: The root AST node to analyze
            source_bytes: The source code as bytes
            
        Returns:
            Count of dynamic method resolution occurrences
        """
        count = 0
        
        def traverse(node: Node):
            nonlocal count
            
            # Check for method definitions with dynamic names
            if node.type in ['method_definition', 'function_definition', 'method_declaration']:
                # Look for identifier child
                for child in node.children:
                    if child.type == 'identifier':
                        method_name = self._get_node_text(child, source_bytes)
                        if method_name in self.DYNAMIC_METHOD_NAMES:
                            count += 1
                        break
            
            # Check for method calls to dynamic methods
            if node.type in ['method_call', 'function_call', 'juxt_function_call']:
                text = self._get_node_text(node, source_bytes)
                for dynamic_method in self.DYNAMIC_METHOD_NAMES:
                    if dynamic_method in text:
                        count += 1
                        break
            
            for child in node.children:
                traverse(child)
        
        traverse(root_node)
        return count
