"""
MetaClass Signal Detector for Groovy.

Detects metaClass modifications which indicate runtime behavior changes.
These are high-risk patterns that can affect program behavior unpredictably.
"""

from tree_sitter import Node
from .base_signal_detector import BaseSignalDetector


class MetaClassSignalDetector(BaseSignalDetector):
    """
    Detects metaClass usage in Groovy code.
    
    Looks for:
    - obj.metaClass = ...
    - obj.metaClass.methodName = ...
    - SomeClass.metaClass { ... }
    
    Default score: +15 per occurrence
    """
    
    @property
    def signal_name(self) -> str:
        return 'metaClass_usage'
    
    @property
    def default_score(self) -> float:
        return 15.0
    
    def detect(self, root_node: Node, source_bytes: bytes, **kwargs) -> int:
        """
        Detect metaClass modifications.
        
        Args:
            root_node: The root AST node to analyze
            source_bytes: The source code as bytes
            
        Returns:
            Count of metaClass usage occurrences
        """
        count = 0
        counted_nodes = set()  # Track counted nodes to avoid double-counting
        
        def traverse(node: Node):
            nonlocal count
            
            # Check for dotted_identifier containing 'metaClass'
            # e.g., String.metaClass, String.metaClass.reverse
            if node.type == 'dotted_identifier':
                text = self._get_node_text(node, source_bytes)
                # Check if this dotted identifier contains .metaClass
                if '.metaClass' in text:
                    # Only count the innermost dotted_identifier that contains metaClass
                    # to avoid counting both "String.metaClass" and "String.metaClass.reverse"
                    node_id = (node.start_byte, node.end_byte)
                    if node_id not in counted_nodes:
                        # Check if parent is also a dotted_identifier with metaClass
                        # If so, skip this one (we'll count the parent)
                        parent_has_metaclass = False
                        if node.parent and node.parent.type == 'dotted_identifier':
                            parent_text = self._get_node_text(node.parent, source_bytes)
                            if '.metaClass' in parent_text:
                                parent_has_metaclass = True
                        
                        if not parent_has_metaclass:
                            count += 1
                            counted_nodes.add(node_id)
            
            # Check for property access to 'metaClass' (older patterns)
            if node.type in ['property_access', 'field_access', 'access_op']:
                text = self._get_node_text(node, source_bytes)
                if '.metaClass' in text or text == 'metaClass':
                    node_id = (node.start_byte, node.end_byte)
                    if node_id not in counted_nodes:
                        count += 1
                        counted_nodes.add(node_id)
            
            # Check for identifier 'metaClass' in assignments (e.g., obj.metaClass = emc)
            if node.type == 'identifier':
                text = self._get_node_text(node, source_bytes)
                if text == 'metaClass':
                    # Check if parent is an assignment or property access
                    if node.parent and node.parent.type in [
                        'assignment', 'property_access', 'field_access', 
                        'access_op', 'dotted_identifier'
                    ]:
                        # This is already counted via dotted_identifier, so skip
                        pass
            
            # Check for getMetaClass() method calls
            if node.type in ['function_call', 'method_call']:
                text = self._get_node_text(node, source_bytes)
                if 'getMetaClass()' in text or 'getMetaClass(' in text:
                    node_id = (node.start_byte, node.end_byte)
                    if node_id not in counted_nodes:
                        count += 1
                        counted_nodes.add(node_id)
            
            for child in node.children:
                traverse(child)
        
        traverse(root_node)
        return count
