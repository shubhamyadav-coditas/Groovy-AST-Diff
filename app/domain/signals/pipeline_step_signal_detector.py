"""
Pipeline Step Signal Detector for Groovy.

Detects pipeline steps commonly used in Jenkins, Gradle, and other CI/CD tools.
These indicate changes that can have wide blast radius in build/deployment systems.
"""

from typing import Set
from tree_sitter import Node
from .base_signal_detector import BaseSignalDetector


class PipelineStepSignalDetector(BaseSignalDetector):
    """
    Detects pipeline steps in Groovy code.
    
    Looks for:
    - sh 'command', bat 'command', powershell 'command'
    - parallel { ... }, node { ... }, stage('name') { ... }
    - Jenkins pipeline DSL methods
    - Common CI/CD build steps
    
    Excludes:
    - Method calls on objects (e.g., Emery.log.error(), object.build())
    - Any method called with a receiver (something.method()) is NOT a pipeline step
    - Only standalone function calls are considered pipeline steps
    
    Default score: +12 per occurrence
    """
    
    # Pipeline-related method names (Jenkins/Gradle/etc.)
    PIPELINE_METHODS: Set[str] = {
        # Shell execution
        'sh', 'bat', 'powershell',
        # Jenkins pipeline core
        'parallel', 'node', 'stage', 'pipeline',
        # Jenkins steps
        'steps', 'script', 'checkout', 'git',
        # Jenkins wrappers
        'withCredentials', 'withEnv', 'timeout',
        # Jenkins publishers
        'archiveArtifacts', 'publishHTML', 'junit',
        # Jenkins flow control
        'input', 'milestone', 'lock',
        # Jenkins status
        'echo', 'error', 'unstable',
        # Common CI/CD steps
        'build', 'deploy', 'test',
    }
    
    @property
    def signal_name(self) -> str:
        return 'pipeline_step'
    
    @property
    def default_score(self) -> float:
        return 12.0
    
    def detect(self, root_node: Node, source_bytes: bytes, **kwargs) -> int:
        """
        Detect pipeline steps.
        
        Args:
            root_node: The root AST node to analyze
            source_bytes: The source code as bytes
            
        Returns:
            Count of pipeline step occurrences
        """
        count = 0
        # Track counted nodes by (start_byte, end_byte) to avoid double-counting
        counted_nodes: set = set()
        
        def has_receiver_object(node: Node) -> bool:
            """
            Check if the method call has a receiver object (e.g., object.method()).
            
            Pipeline steps are standalone calls like: sh 'command', stage('Build') { }
            NOT method chains like: Emery.log.error(), object.build()
            
            Returns True if this is a method call on an object (should be excluded).
            """
            # Get the full text of the call
            node_text = self._get_node_text(node, source_bytes)
            
            # If the node text contains a dot, it's a method chain
            # e.g., "Emery.log.error(...)" contains dots before the method
            if '.' in node_text:
                # Find where the method name appears and check if there's a dot before it
                method_name = self._extract_method_name(node, source_bytes)
                if method_name:
                    # Check if the pattern is "something.methodName"
                    # The method should be called directly, not on an object
                    method_lower = method_name.lower()
                    for pipeline_method in self.PIPELINE_METHODS:
                        if pipeline_method.lower() == method_lower:
                            # Check if there's a receiver before this method
                            # Pattern: receiver.method or receiver.property.method
                            idx = node_text.rfind(method_name)
                            if idx > 0 and node_text[idx - 1] == '.':
                                return True
                return False
            
            return False
        
        def is_chained_identifier(node: Node) -> bool:
            """
            Check if an identifier is part of a method chain (preceded by a dot).
            """
            # Check previous sibling for dot operator
            if node.prev_sibling:
                prev_text = self._get_node_text(node.prev_sibling, source_bytes)
                if prev_text == '.':
                    return True
            
            # Check parent for property_expression or dotted patterns
            parent = node.parent
            if parent and parent.type in ['property_expression', 'dotted_identifier', 'member_access']:
                # Check if this identifier is after a dot in the parent
                parent_text = self._get_node_text(parent, source_bytes)
                node_text = self._get_node_text(node, source_bytes)
                idx = parent_text.rfind(node_text)
                if idx > 0 and parent_text[idx - 1] == '.':
                    return True
            
            return False
        
        def traverse(node: Node):
            nonlocal count
            
            # Check for method/function calls
            if node.type in ['method_call', 'function_call', 'juxt_function_call']:
                method_name = self._extract_method_name(node, source_bytes)
                if method_name and method_name.lower() in [p.lower() for p in self.PIPELINE_METHODS]:
                    # Only count if this is a standalone call, not a method on an object
                    if not has_receiver_object(node):
                        node_key = (node.start_byte, node.end_byte)
                        if node_key not in counted_nodes:
                            counted_nodes.add(node_key)
                            count += 1
            
            # Also check for identifier followed by block (Groovy DSL pattern)
            # This catches cases like: node('linux') { ... } or pipeline { ... }
            # where the identifier is the pipeline step name
            if node.type == 'identifier':
                text = self._get_node_text(node, source_bytes)
                if text.lower() in [p.lower() for p in self.PIPELINE_METHODS]:
                    # Skip if this is part of a method chain (has a dot before it)
                    if is_chained_identifier(node):
                        for child in node.children:
                            traverse(child)
                        return
                    
                    # Check if followed by a block/closure
                    if node.next_sibling and node.next_sibling.type in ['closure', 'block', 'argument_list']:
                        # Check if parent node was already counted as a function call
                        parent = node.parent
                        if parent:
                            parent_key = (parent.start_byte, parent.end_byte)
                            if parent_key not in counted_nodes:
                                # Use identifier position as key to avoid counting twice
                                node_key = (node.start_byte, node.end_byte)
                                if node_key not in counted_nodes:
                                    counted_nodes.add(node_key)
                                    # Also add parent to prevent double-count from function_call detection
                                    counted_nodes.add(parent_key)
                                    count += 1
            
            for child in node.children:
                traverse(child)
        
        traverse(root_node)
        return count
