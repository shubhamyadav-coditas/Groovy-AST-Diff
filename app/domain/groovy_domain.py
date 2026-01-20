"""
Domain models for Groovy AST comparison.

This module contains all dataclass definitions used in Groovy AST comparison,
following the same structure as the JavaScript AST Diff POC.
"""

from dataclasses import dataclass, field
from typing import List, Optional, Tuple, Any

from ..types.groovy_types import BlockType, ChangeType, StatementChangeType


@dataclass
class BlockSignature:
    """
    Signature for a Groovy code block used in matching.

    Contains all information needed to identify and compare blocks.
    """

    block_type: BlockType
    identifier: str                    # Name for named blocks, content hash for unnamed
    content_hash: str                  # Hash of the block's text content
    start_line: int
    end_line: int
    code: str                          # The actual source code
    node_type: str                     # tree-sitter node type
    children_count: int = 0            # Number of child nodes

    def __hash__(self) -> int:
        """Hash based on type and identifier for set operations."""
        return hash((self.block_type, self.identifier))

    def __eq__(self, other: object) -> bool:
        """Equality based on type and identifier."""
        if not isinstance(other, BlockSignature):
            return False
        return self.block_type == other.block_type and self.identifier == other.identifier


@dataclass
class StatementSignature:
    """Signature for a single statement used in matching."""

    content_hash: str                  # Hash of normalized statement code
    code: str                          # Original code text
    start_line: int                    # Line number in original file
    end_line: int
    index: int                         # Position index within the block
    node_type: str                     # AST node type
    identifier: Optional[str] = None   # Statement identifier (function name, variable name, etc.)


@dataclass
class StatementDiff:
    """
    Represents a statement-level difference within a modified block.
    
    For container statements (if, for, while, switch, try), this includes
    nested child_diffs showing changes within the container's body.
    """

    change_type: StatementChangeType
    code: str                          # The statement code (new version for MODIFIED)
    node_type: str                     # AST node type
    # Position in file A (None if added)
    file_a_line: Optional[int] = None
    file_a_index: Optional[int] = None
    # Position in file B (None if deleted)
    file_b_line: Optional[int] = None
    file_b_index: Optional[int] = None
    # Human-readable description
    description: str = ""
    # For MODIFIED/MOVED_MODIFIED: the original code before change
    old_code: Optional[str] = None
    # For MODIFIED/MOVED_MODIFIED: similarity score (0.0 to 1.0)
    similarity_score: Optional[float] = None
    # For container statements: nested diffs showing changes within the container's body
    child_diffs: List["StatementDiff"] = field(default_factory=list)
    # Flag indicating if this is a container statement
    is_container: bool = False
    # For if/switch: which branch (consequence, alternative, case_0, etc.)
    branch_label: Optional[str] = None


@dataclass
class BlockDiff:
    """Detailed diff information for a single block."""

    change_type: ChangeType
    block_type: BlockType
    identifier: str
    # File A info
    file_a_start_line: Optional[int] = None
    file_a_end_line: Optional[int] = None
    file_a_code: Optional[str] = None
    # File B info
    file_b_start_line: Optional[int] = None
    file_b_end_line: Optional[int] = None
    file_b_code: Optional[str] = None
    # Similarity score (0-100)
    similarity_score: float = 0.0
    # Statement-level diffs for MODIFIED blocks
    statement_diffs: List[StatementDiff] = field(default_factory=list)
    # Human-readable description
    description: str = ""
    # Modifiers (static, private, etc.)
    modifiers: List[str] = field(default_factory=list)


@dataclass
class ComparisonResult:
    """Complete result of comparing two Groovy ASTs."""

    # Summary statistics
    is_identical: bool
    structural_similarity: float       # 0.0 to 1.0
    total_blocks_a: int
    total_blocks_b: int
    # Change counts
    blocks_added: int
    blocks_deleted: int
    blocks_modified: int
    blocks_moved: int
    blocks_moved_modified: int
    blocks_unchanged: int
    # Detailed diffs
    diffs: List[BlockDiff]
    # Error info
    error: Optional[str] = None
    # Warning info (non-blocking issues like parsing errors)
    warnings: Optional[str] = None
    # Additional metadata
    file_a_path: str = ""
    file_b_path: str = ""
    file_a_nodes: int = 0
    file_b_nodes: int = 0


@dataclass
class GroovyASTNode:
    """Represents a node in the Groovy AST with enhanced properties."""
    
    type: str
    text: str
    start_point: Tuple[int, int]
    end_point: Tuple[int, int]
    children: List['GroovyASTNode']
    # Enhanced properties
    identifier: Optional[str] = None   # Name if this is a named entity
    content_hash: str = ""             # Hash of the content
    is_container: bool = False         # Whether this node contains other blocks
    
    def __post_init__(self):
        """Ensure children is always a list."""
        if self.children is None:
            self.children = []


def calculate_similarity(text1: str, text2: str) -> float:
    """
    Calculate similarity between two text strings using Sørensen-Dice coefficient.
    
    The Sørensen-Dice coefficient is calculated as:
    2 * |intersection| / (|set1| + |set2|)
    
    Returns a float between 0.0 and 1.0, where 1.0 means identical.
    """
    if not text1 and not text2:
        return 1.0
    if not text1 or not text2:
        return 0.0
    
    # Normalize text for comparison
    text1_normalized = normalize_code(text1)
    text2_normalized = normalize_code(text2)
    
    if text1_normalized == text2_normalized:
        return 1.0
    
    # Create bigrams (2-character sequences) for Sørensen-Dice
    def get_bigrams(text: str) -> set:
        """Get set of character bigrams from text."""
        if len(text) < 2:
            return {text}
        return {text[i:i+2] for i in range(len(text) - 1)}
    
    bigrams1 = get_bigrams(text1_normalized)
    bigrams2 = get_bigrams(text2_normalized)
    
    if not bigrams1 and not bigrams2:
        return 1.0
    if not bigrams1 or not bigrams2:
        return 0.0
    
    # Sørensen-Dice coefficient: 2 * |intersection| / (|set1| + |set2|)
    intersection = len(bigrams1.intersection(bigrams2))
    total_bigrams = len(bigrams1) + len(bigrams2)
    
    return (2.0 * intersection) / total_bigrams if total_bigrams > 0 else 0.0


def normalize_code(code: str) -> str:
    """
    Normalize code for comparison by removing extra whitespace and standardizing formatting.
    """
    import re
    
    # First, join all whitespace into single spaces
    normalized = " ".join(code.split())
    
    # Remove spaces around common punctuation to handle formatting differences
    # Remove spaces before/after: ( ) [ ] { } , ; . 
    normalized = re.sub(r'\s*([()[\]{},;.])\s*', r'\1', normalized)
    
    # Add back necessary spaces after commas and semicolons for readability
    normalized = re.sub(r'([,;])', r'\1 ', normalized)
    
    # Remove any double spaces that might have been created
    normalized = re.sub(r'\s+', ' ', normalized)
    
    return normalized.strip()


def hash_content(content: str) -> str:
    """
    Create a normalized hash of content for comparison.
    """
    import hashlib
    normalized = normalize_code(content)
    return hashlib.sha256(normalized.encode("utf-8")).hexdigest()[:16]
