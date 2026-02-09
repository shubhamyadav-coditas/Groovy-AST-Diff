"""
Import Dependency Calculator for Groovy AST nodes.

Calculates complexity related to import statements (direct dependencies).
"""

from typing import List, Tuple
from tree_sitter import Node
from .base_metric_calculator import BaseMetricCalculator


class ImportDependencyCalculator(BaseMetricCalculator):
    """
    Calculates import complexity for Groovy code blocks.
    
    This metric captures:
    - Regular import statements
    - Static imports
    - Star imports (wildcard)
    
    All import types have equal weight of 1.0.
    Returns the count of imports as a single metric.
    """
    
    # Node types for import statements (updated for Groovy Tree-sitter grammar)
    IMPORT_NODE_TYPES = {
        'import_declaration',
        'static_import_declaration',
        'groovy_import',  # Groovy-specific import node type
        'import'          # Import keyword node type
    }
    
    def calculate(self, ast_node: Node, source_bytes: bytes) -> int:
        """
        Calculate import count for the given AST node.
        
        Args:
            ast_node: Root AST node of the code block to analyze
            source_bytes: Complete source code as bytes
            
        Returns:
            Count of imports (all imports count as 1)
        """
        return self._count_imports(ast_node, source_bytes)
    
    def _count_imports(self, node: Node, source_bytes: bytes) -> int:
        """
        Count import statements in the AST subtree.
        
        All import types (regular, static, star) count as 1.
        
        Args:
            node: Current AST node
            source_bytes: Source code as bytes
            
        Returns:
            Number of import statements found
        """
        count = 0
        
        def count_imports_recursive(current_node):
            nonlocal count
            
            if current_node.type in self.IMPORT_NODE_TYPES:
                # Only count the main import nodes, not nested keywords
                if current_node.type in ['groovy_import', 'import_declaration', 'static_import_declaration']:
                    count += 1
            
            # Recursively check children
            for child in current_node.children:
                count_imports_recursive(child)
        
        count_imports_recursive(node)
        return count
    
    def get_import_breakdown(self, ast_node: Node, source_bytes: bytes) -> dict:
        """
        Get a detailed breakdown of import complexity.
        
        Args:
            ast_node: Root AST node to analyze
            source_bytes: Source code as bytes
            
        Returns:
            Dictionary with detailed import analysis
        """
        regular_imports = 0
        static_imports = 0
        star_imports = 0
        
        def analyze_imports_recursive(current_node):
            nonlocal regular_imports, static_imports, star_imports
            
            if current_node.type in ['groovy_import', 'import_declaration', 'static_import_declaration']:
                node_text = self._get_node_text(current_node, source_bytes)
                
                is_star = '*' in node_text
                is_static = 'static ' in node_text or 'static\t' in node_text
                
                if is_star:
                    star_imports += 1
                elif is_static:
                    static_imports += 1
                else:
                    regular_imports += 1
            
            for child in current_node.children:
                analyze_imports_recursive(child)
        
        analyze_imports_recursive(ast_node)
        
        # All imports count as 1
        total_count = regular_imports + static_imports + star_imports
        
        return {
            'regular_imports': regular_imports,
            'static_imports': static_imports,
            'star_imports': star_imports,
            'total_count': total_count,
            'formula': f'{regular_imports} regular + {static_imports} static + {star_imports} star = {total_count}'
        }
