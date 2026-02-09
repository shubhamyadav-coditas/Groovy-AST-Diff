"""Pydantic schemas for Groovy AST comparison."""

from typing import List, Optional

from pydantic import BaseModel


class StatementDiffSchema(BaseModel):
    """Schema for statement-level differences."""

    change_type: str
    code: str
    node_type: str
    file_a_line: Optional[int] = None
    file_a_index: Optional[int] = None
    file_b_line: Optional[int] = None
    file_b_index: Optional[int] = None
    description: str = ""
    old_code: Optional[str] = None
    similarity_score: Optional[float] = None
    is_container: bool = False
    branch_label: Optional[str] = None
    child_diffs: List["StatementDiffSchema"] = []



class BlockDiffSchema(BaseModel):
    """Schema for block-level differences."""

    change_type: str
    description: str = ""
    block_type: str
    identifier: str
    file_a_start_line: Optional[int] = None
    file_a_end_line: Optional[int] = None
    file_a_code: Optional[str] = None
    file_b_start_line: Optional[int] = None
    file_b_end_line: Optional[int] = None
    file_b_code: Optional[str] = None
    similarity_score: float = 0.0
    statement_diffs: List[StatementDiffSchema] = []



class ComparisonSummary(BaseModel):
    """Schema for comparison summary statistics."""

    is_identical: bool
    structural_similarity: float
    total_blocks_a: int
    total_blocks_b: int
    blocks_added: int
    blocks_deleted: int
    blocks_modified: int
    blocks_moved: int
    blocks_moved_modified: int = 0
    blocks_unchanged: int


class CompareFilesResponse(BaseModel):
    """Schema for file comparison response."""

    message: str
    file_a: "FileInfoSimple"
    file_b: "FileInfoSimple"
    summary: ComparisonSummary
    differences: List[BlockDiffSchema]
    warnings: Optional[str] = None


# Import here to avoid circular imports
from app.schemas.file import FileInfoSimple

# Update forward references
CompareFilesResponse.model_rebuild()
StatementDiffSchema.model_rebuild()