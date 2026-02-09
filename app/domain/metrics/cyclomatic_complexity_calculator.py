"""
Cyclomatic Complexity Calculator for Groovy AST nodes.

Calculates cyclomatic complexity by counting decision points in the AST.
Uses recursive traversal to handle nested structures properly.
"""

from tree_sitter import Node
from .base_metric_calculator import BaseMetricCalculator


class CyclomaticComplexityCalculator(BaseMetricCalculator):
    """
    Calculates cyclomatic complexity for Groovy code blocks.
    
    Cyclomatic complexity measures the number of linearly independent paths
    through a program's source code. It counts decision points like if statements,
    loops, switch cases, try-catch blocks, and ternary operators.
    """
    
    # Groovy decision point node types
    DECISION_NODE_TYPES = {
        'if_statement',
        'while_loop',
        'do_while_loop',
        'for_loop',
        'for_in_loop',
        'enhanced_for_statement',
        'switch_statement',
        'try_statement',
        'catch_clause',
        'catch',  # Groovy uses 'catch' as node type for catch blocks
        'conditional_expression',  # ternary operator (?:)
        'binary_expression',  # for && and || operators
        'binary_op',  # Groovy binary operations (including && and ||)
        'case_statement',
        'ternary_expression',  # alternative ternary node type
        'elvis_expression',  # elvis operator (?:)
        'safe_navigation_expression',  # safe navigation (?.)
        'spread_safe_navigation_expression',  # spread safe navigation (*?.)
        'assert_statement',  # assert statements
        'assertion'  # Groovy assertion nodes
    }
    
    # Binary operators that create decision points
    DECISION_OPERATORS = {'&&', '||'}
    
    # Groovy-specific operators that create decision points
    GROOVY_DECISION_OPERATORS = {
        '?:', '?.', '*?.', '?'  # elvis, safe navigation, spread safe navigation, ternary
    }
    
    def calculate(self, ast_node: Node, source_bytes: bytes) -> int:
        """
        Calculate cyclomatic complexity for the given AST node.
        
        The base complexity is 1, and we add 1 for each decision point.
        
        Args:
            ast_node: Root AST node of the code block to analyze
            source_bytes: Complete source code as bytes
            
        Returns:
            Cyclomatic complexity value (minimum 1)
        """
        # Base complexity is 1
        complexity = 1
        
        # Count decision points recursively
        complexity += self._count_decision_points(ast_node, source_bytes)
        
        return complexity
    
    def _count_decision_points(self, node: Node, source_bytes: bytes) -> int:
        """
        Recursively count decision points in the AST subtree.
        
        Args:
            node: Current AST node
            source_bytes: Source code as bytes
            
        Returns:
            Number of decision points found
        """
        count = 0
        
        # Check if current node is a decision point
        if node.type in self.DECISION_NODE_TYPES:
            if node.type in ['binary_expression', 'binary_op']:
                # Only count binary expressions with decision operators
                count += self._count_decision_binary_operators(node, source_bytes)
            elif node.type == 'switch_statement':
                # For switch statements, count each case as a decision point
                count += self._count_switch_cases(node)
            elif node.type == 'try_statement':
                # Don't count try_statement itself - we count 'catch' nodes directly
                # This avoids double-counting since parser puts 'catch' as separate nodes
                pass
            elif node.type in ['catch', 'catch_clause']:
                # Each catch block adds a decision point
                count += 1
            else:
                # Regular decision points (if, while, for, etc.)
                count += 1
        
        # Handle parser limitation: multiple catch blocks are incorrectly parsed
        # as identifier nodes with text 'catch' instead of proper 'catch' nodes
        if node.type == 'identifier':
            node_text = self._get_node_text(node, source_bytes)
            if node_text == 'catch':
                # This is a catch block that was incorrectly parsed as identifier
                count += 1
        
        # Check for Groovy-specific operator tokens directly
        # These are the actual operator tokens in the AST (not the parent nodes)
        if node.type == '?.':  # Safe navigation operator token
            count += 1
        elif node.type == '?:':  # Elvis operator token
            count += 1
        elif node.type == '*?.':  # Spread safe navigation operator token
            count += 1
        elif node.type == '?':  # Ternary operator token (part of ternary_op)
            # Only count if parent is ternary_op (to distinguish from other uses of ?)
            if node.parent and node.parent.type == 'ternary_op':
                count += 1
        
        # Recursively count in all children
        for child in node.children:
            count += self._count_decision_points(child, source_bytes)
        
        return count
    
    def _count_decision_binary_operators(self, node: Node, source_bytes: bytes) -> int:
        """
        Count decision points in binary expressions (&&, ||).
        
        Only counts the operator token that is a DIRECT child of this binary_op node,
        not operators in nested child binary_op nodes (those will be counted separately
        when we recurse into them).
        
        Args:
            node: Binary expression AST node
            source_bytes: Source code as bytes
            
        Returns:
            Number of decision points (1 for && or ||, 0 otherwise)
        """
        count = 0
        
        # Look for operator tokens that are direct children of this node
        # The AST structure is: binary_op -> [left_operand, operator_token, right_operand]
        for child in node.children:
            if child.type in self.DECISION_OPERATORS:
                count += 1
        
        return count
    
    def _count_switch_cases(self, switch_node: Node) -> int:
        """
        Count case statements in a switch statement.
        
        Each case (including default) adds to complexity.
        
        Args:
            switch_node: Switch statement AST node
            
        Returns:
            Number of case statements found
        """
        case_count = 0
        
        # Find the switch_block node first
        switch_block = None
        for child in switch_node.children:
            if child.type == 'switch_block':
                switch_block = child
                break
        
        if switch_block:
            # Count direct 'case' children of switch_block (not nested case keywords)
            for child in switch_block.children:
                if child.type == 'case':
                    case_count += 1
        
        return case_count
    
    def _count_catch_clauses(self, try_node: Node) -> int:
        """
        Count catch clauses in a try statement.
        
        Each catch clause adds to complexity.
        
        Args:
            try_node: Try statement AST node
            
        Returns:
            Number of catch clauses found
        """
        catch_count = 0
        
        # Count 'catch' keywords directly as children of try_statement
        for child in try_node.children:
            if child.type == 'catch':
                catch_count += 1
        
        return catch_count
    
    def _count_groovy_operators(self, node: Node, source_bytes: bytes) -> int:
        """
        Count Groovy-specific operators that create decision points.
        
        Args:
            node: Current AST node
            source_bytes: Source code as bytes
            
        Returns:
            Number of Groovy decision operators found in this node
        """
        count = 0
        node_text = self._get_node_text(node, source_bytes)
        
        # Count ternary operators (? :) - but avoid counting safe navigation
        # Look for pattern: condition ? value1 : value2
        ternary_count = 0
        if '?' in node_text and ':' in node_text:
            # Simple heuristic: count standalone ? that are not part of ?. or ?:
            for i, char in enumerate(node_text):
                if char == '?' and i + 1 < len(node_text):
                    next_char = node_text[i + 1]
                    if next_char not in ['.', ':']:  # Not safe navigation or elvis
                        ternary_count += 1
        count += ternary_count
        
        # Count elvis operators (?:)
        elvis_count = node_text.count('?:')
        count += elvis_count
        
        # Count spread safe navigation operators (*?.) first
        spread_count = node_text.count('*?.')
        count += spread_count
        
        # Count safe navigation operators (?.) but exclude those that are part of *?.
        total_safe_nav = node_text.count('?.')
        safe_nav_count = total_safe_nav - spread_count  # Subtract *?. occurrences
        count += safe_nav_count
        
        
        return count
    
    def get_complexity_breakdown(self, ast_node: Node, source_bytes: bytes) -> dict:
        """
        Get a detailed breakdown of complexity contributors.
        
        Useful for debugging and understanding complexity sources.
        
        Args:
            ast_node: Root AST node to analyze
            source_bytes: Source code as bytes
            
        Returns:
            Dictionary with breakdown of complexity sources
        """
        breakdown = {
            'base_complexity': 1,
            'if_statements': 0,
            'loops': 0,
            'switch_cases': 0,
            'try_catch': 0,
            'ternary_operators': 0,
            'logical_operators': 0,
            'groovy_operators': 0,
            'total': 1
        }
        
        def analyze_node(node):
            if node.type == 'if_statement':
                breakdown['if_statements'] += 1
            elif node.type in ['while_loop', 'do_while_loop', 'for_loop', 'for_in_loop', 'while_statement', 'for_statement', 'enhanced_for_statement']:
                breakdown['loops'] += 1
            elif node.type == 'switch_statement':
                cases = self._count_switch_cases(node)
                breakdown['switch_cases'] += cases
            elif node.type in ['catch', 'catch_clause']:
                # Count each catch block (don't count try_statement itself)
                breakdown['try_catch'] += 1
            elif node.type == 'identifier':
                # Handle parser limitation: multiple catch blocks parsed as identifier 'catch'
                node_text = self._get_node_text(node, source_bytes)
                if node_text == 'catch':
                    breakdown['try_catch'] += 1
            elif node.type == 'conditional_expression':
                breakdown['ternary_operators'] += 1
            elif node.type in ['binary_expression', 'binary_op']:
                decisions = self._count_decision_binary_operators(node, source_bytes)
                breakdown['logical_operators'] += decisions
            elif node.type in ['assertion', 'assert_statement']:
                breakdown['groovy_operators'] += 1
            # Count Groovy operator tokens directly
            elif node.type == '?.':  # Safe navigation
                breakdown['groovy_operators'] += 1
            elif node.type == '?:':  # Elvis operator
                breakdown['groovy_operators'] += 1
            elif node.type == '*?.':  # Spread safe navigation
                breakdown['groovy_operators'] += 1
            elif node.type == '?':  # Ternary operator (part of ternary_op)
                if node.parent and node.parent.type == 'ternary_op':
                    breakdown['ternary_operators'] += 1
            
            for child in node.children:
                analyze_node(child)
        
        analyze_node(ast_node)
        
        # Calculate total
        breakdown['total'] = (
            breakdown['base_complexity'] +
            breakdown['if_statements'] +
            breakdown['loops'] +
            breakdown['switch_cases'] +
            breakdown['try_catch'] +
            breakdown['ternary_operators'] +
            breakdown['logical_operators'] +
            breakdown['groovy_operators']
        )
        
        return breakdown