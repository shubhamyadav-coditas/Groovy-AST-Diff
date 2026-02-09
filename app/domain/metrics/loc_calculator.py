"""
Lines of Code (LOC) Calculator for Groovy AST nodes.

Calculates lines of code within AST node boundaries, counting only
non-blank, non-comment lines for accurate complexity measurement.
"""

from typing import List, Tuple
from tree_sitter import Node
from .base_metric_calculator import BaseMetricCalculator


class LinesOfCodeCalculator(BaseMetricCalculator):
    """
    Calculates lines of code for Groovy code blocks.
    
    This metric counts:
    - Non-blank lines
    - Non-comment lines (excludes // and /* */ comments)
    - Lines within the specific AST node boundaries
    
    Uses AST node start/end points for precise line counting.
    """
    
    def calculate(self, ast_node: Node, source_bytes: bytes) -> int:
        """
        Calculate lines of code for the given AST node.
        
        Uses the AST node's start_point and end_point to determine
        the exact line range to count.
        
        Args:
            ast_node: Root AST node of the code block to analyze
            source_bytes: Complete source code as bytes
            
        Returns:
            Number of significant lines of code
        """
        start_line = ast_node.start_point[0]  # 0-based line number
        end_line = ast_node.end_point[0]      # 0-based line number
        
        return self._count_significant_lines(source_bytes, start_line, end_line)
    
    def _count_significant_lines(self, source_bytes: bytes, start_line: int, end_line: int) -> int:
        """
        Count non-blank, non-comment lines in the specified range.
        
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
            in_block_comment = False
            
            for i in range(start_line, min(end_line + 1, len(lines))):
                line = lines[i].strip()
                
                # Skip blank lines
                if not line:
                    continue
                
                # Handle block comments
                if self._is_block_comment_start(line):
                    in_block_comment = True
                    # Check if the comment also ends on the same line
                    if self._is_block_comment_end(line):
                        in_block_comment = False
                    # If there's code before OR after the comment, count the line
                    if self._has_code_before_comment(line) or self._has_code_after_comment_end(line):
                        count += 1
                    continue
                
                if in_block_comment:
                    if self._is_block_comment_end(line):
                        in_block_comment = False
                        # Check if there's code after the comment end
                        if self._has_code_after_comment_end(line):
                            count += 1
                    continue
                
                # Skip single-line comments
                if line.startswith('//'):
                    continue
                
                # Check for inline comments and count if there's code before them
                if '//' in line:
                    code_part = line.split('//')[0].strip()
                    if code_part:
                        count += 1
                    continue
                
                # This is a significant line of code
                count += 1
            
            return count
            
        except (UnicodeDecodeError, IndexError):
            return 0
    
    def _is_block_comment_start(self, line: str) -> bool:
        """Check if line starts a block comment."""
        return '/*' in line
    
    def _is_block_comment_end(self, line: str) -> bool:
        """Check if line ends a block comment."""
        return '*/' in line
    
    def _has_code_before_comment(self, line: str) -> bool:
        """Check if there's code before a comment starts."""
        comment_start = line.find('/*')
        if comment_start == -1:
            return False
        
        code_before = line[:comment_start].strip()
        return len(code_before) > 0
    
    def _has_code_after_comment_end(self, line: str) -> bool:
        """Check if there's code after a block comment ends."""
        comment_end = line.find('*/')
        if comment_end == -1:
            return False
        
        code_after = line[comment_end + 2:].strip()
        return len(code_after) > 0
    
    def get_line_breakdown(self, ast_node: Node, source_bytes: bytes) -> dict:
        """
        Get a detailed breakdown of line counting.
        
        Args:
            ast_node: Root AST node to analyze
            source_bytes: Source code as bytes
            
        Returns:
            Dictionary with detailed line analysis
        """
        start_line = ast_node.start_point[0]
        end_line = ast_node.end_point[0]
        
        try:
            source_text = source_bytes.decode('utf-8')
            lines = source_text.split('\n')
            
            total_lines = end_line - start_line + 1
            blank_lines = 0
            comment_lines = 0
            code_lines = 0
            mixed_lines = 0  # Lines with both code and comments
            
            in_block_comment = False
            
            for i in range(start_line, min(end_line + 1, len(lines))):
                line = lines[i].strip()
                
                if not line:
                    blank_lines += 1
                    continue
                
                # Handle block comments
                if self._is_block_comment_start(line):
                    in_block_comment = True
                    if self._has_code_before_comment(line):
                        mixed_lines += 1
                    else:
                        comment_lines += 1
                    
                    if self._is_block_comment_end(line):
                        in_block_comment = False
                        if self._has_code_after_comment_end(line):
                            if not self._has_code_before_comment(line):
                                # Reclassify as mixed if we only counted it as comment
                                comment_lines -= 1
                                mixed_lines += 1
                    continue
                
                if in_block_comment:
                    if self._is_block_comment_end(line):
                        in_block_comment = False
                        if self._has_code_after_comment_end(line):
                            mixed_lines += 1
                        else:
                            comment_lines += 1
                    else:
                        comment_lines += 1
                    continue
                
                # Single-line comments
                if line.startswith('//'):
                    comment_lines += 1
                    continue
                
                # Inline comments
                if '//' in line:
                    code_part = line.split('//')[0].strip()
                    if code_part:
                        mixed_lines += 1
                    else:
                        comment_lines += 1
                    continue
                
                # Pure code line
                code_lines += 1
            
            significant_lines = code_lines + mixed_lines
            
            return {
                'total_lines': total_lines,
                'blank_lines': blank_lines,
                'comment_lines': comment_lines,
                'code_lines': code_lines,
                'mixed_lines': mixed_lines,
                'significant_lines': significant_lines,
                'line_range': f"{start_line + 1}-{end_line + 1}",  # Convert to 1-based for display
                'breakdown_percentage': {
                    'blank': round((blank_lines / total_lines) * 100, 1) if total_lines > 0 else 0,
                    'comments': round((comment_lines / total_lines) * 100, 1) if total_lines > 0 else 0,
                    'code': round((code_lines / total_lines) * 100, 1) if total_lines > 0 else 0,
                    'mixed': round((mixed_lines / total_lines) * 100, 1) if total_lines > 0 else 0
                }
            }
            
        except (UnicodeDecodeError, IndexError):
            return {
                'total_lines': 0,
                'blank_lines': 0,
                'comment_lines': 0,
                'code_lines': 0,
                'mixed_lines': 0,
                'significant_lines': 0,
                'line_range': 'unknown',
                'breakdown_percentage': {'blank': 0, 'comments': 0, 'code': 0, 'mixed': 0}
            }
    
    def calculate_with_details(self, ast_node: Node, source_bytes: bytes) -> dict:
        """
        Calculate LOC with detailed breakdown in a single call.
        
        Args:
            ast_node: Root AST node to analyze
            source_bytes: Source code as bytes
            
        Returns:
            Dictionary with LOC count and detailed breakdown
        """
        breakdown = self.get_line_breakdown(ast_node, source_bytes)
        
        return {
            'lines_of_code': breakdown['significant_lines'],
            'breakdown': breakdown
        }
