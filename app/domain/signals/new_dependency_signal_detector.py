"""
New Dependency Signal Detector for Groovy.

Detects new infrastructure/plugin dependencies that were not present
in the source code. These indicate increased coupling to external systems.
"""

from typing import Optional, Set
from tree_sitter import Node
from .base_signal_detector import BaseSignalDetector


class NewDependencySignalDetector(BaseSignalDetector):
    """
    Detects new infrastructure/plugin dependencies.
    
    Looks for:
    - New imports that match known infrastructure patterns
    - Jenkins, Kubernetes, Docker, cloud provider imports
    - Build tool and framework imports
    
    Default score: +10 per occurrence
    """
    
    # Known infrastructure/plugin import patterns
    INFRA_IMPORT_PATTERNS: Set[str] = {
        # CI/CD tools
        'jenkins', 'hudson', 'gradle', 'maven',
        # Container/orchestration
        'kubernetes', 'docker',
        # Cloud providers
        'aws', 'azure', 'gcp', 'google.cloud',
        # Infrastructure as code
        'terraform', 'ansible', 'puppet', 'chef',
        # Frameworks
        'spring', 'hibernate', 'grails',
        # Messaging
        'kafka', 'rabbitmq', 'activemq',
        # Databases
        'mongodb', 'redis', 'elasticsearch',
    }
    
    @property
    def signal_name(self) -> str:
        return 'new_dependency'
    
    @property
    def default_score(self) -> float:
        return 10.0
    
    def detect(
        self,
        root_node: Node,
        source_bytes: bytes,
        source_imports: Optional[Set[str]] = None,
        **kwargs
    ) -> int:
        """
        Detect new infrastructure/plugin dependencies.
        
        Args:
            root_node: The root AST node to analyze
            source_bytes: The source code as bytes
            source_imports: Set of imports from source file (for detecting NEW imports)
            
        Returns:
            Count of new infrastructure dependencies
        """
        count = 0
        source_imports = source_imports or set()
        
        def traverse(node: Node):
            nonlocal count
            
            # Check for import declarations
            if node.type in ['groovy_import', 'import_declaration', 'import_statement']:
                import_text = self._get_node_text(node, source_bytes)
                
                # Check if this is a new import (not in source)
                is_new = import_text not in source_imports
                
                if is_new:
                    # Check if it matches infrastructure patterns
                    import_lower = import_text.lower()
                    for pattern in self.INFRA_IMPORT_PATTERNS:
                        if pattern in import_lower:
                            count += 1
                            break
            
            for child in node.children:
                traverse(child)
        
        traverse(root_node)
        return count
