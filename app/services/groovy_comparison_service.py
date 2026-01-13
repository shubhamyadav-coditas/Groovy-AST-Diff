"""Groovy AST comparison service containing business logic for AST operations."""

from pathlib import Path

from app.domain.groovy_ast_diff import GroovyASTDiff
from app.domain.groovy_domain import ComparisonResult


class GroovyComparisonService:
    """
    Service layer for Groovy AST comparison operations.

    Contains business logic for comparing Groovy files using AST analysis.
    """

    def __init__(self) -> None:
        """Initialize the Groovy comparison service."""
        self.groovy_differ = GroovyASTDiff()

    def compare_files(self, file_a_path: str | Path, file_b_path: str | Path) -> ComparisonResult:
        """
        Compare two Groovy files and return detailed comparison results.

        Args:
            file_a_path: Path to the first Groovy file (original/base)
            file_b_path: Path to the second Groovy file (modified/new)

        Returns:
            ComparisonResult with detailed differences and statistics

        Raises:
            Exception: If files cannot be read or parsed
        """
        return self.groovy_differ.compare_files(str(file_a_path), str(file_b_path))

    def analyze_file(self, file_path: str | Path) -> dict:
        """
        Analyze a single Groovy file and return basic statistics.

        Args:
            file_path: Path to the Groovy file

        Returns:
            Dictionary with file analysis information

        Raises:
            Exception: If file cannot be read or parsed
        """
        # Read file content
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()

        # Count total lines
        total_lines = len(content.splitlines())
        
        try:
            # Parse the file using the groovy differ's parser
            tree = self.groovy_differ.parser.parse(bytes(content, "utf8"))
            root_node = tree.root_node
            
            # Extract blocks using the private method (we need to access it)
            blocks = self.groovy_differ._extract_top_level_blocks(root_node, bytes(content, "utf8"))
            
            # Count different types of blocks
            return {
                "total_lines": total_lines,
                "total_blocks": len(blocks),
            }
            
        except Exception as e:
            # If parsing fails, return basic info
            return {
                "total_lines": total_lines,
                "total_blocks": 0,
            }