"""Pydantic schemas for Change Complexity Score API."""

from typing import Dict, List, Optional

from pydantic import BaseModel, Field


class ComplexityWeights(BaseModel):
    """Custom weights for complexity metrics."""
    
    weight_cyclomatic: float = Field(
        default=0.35,
        ge=0.0,
        le=1.0,
        description="Weight for cyclomatic complexity (0.0-1.0)"
    )
    weight_dynamic_typing: float = Field(
        default=0.25,
        ge=0.0,
        le=1.0,
        description="Weight for dynamic typing & meta-programming (0.0-1.0)"
    )
    weight_closures: float = Field(
        default=0.20,
        ge=0.0,
        le=1.0,
        description="Weight for closure complexity (0.0-1.0)"
    )
    weight_imports: float = Field(
        default=0.15,
        ge=0.0,
        le=1.0,
        description="Weight for imports/dependencies (0.0-1.0)"
    )
    weight_loc: float = Field(
        default=0.05,
        ge=0.0,
        le=1.0,
        description="Weight for lines of code (0.0-1.0)"
    )

    def to_dict(self) -> Dict[str, float]:
        """Convert to dictionary format expected by calculator."""
        return {
            'cyclomatic_complexity': self.weight_cyclomatic,
            'dynamic_typing_score': self.weight_dynamic_typing,
            'closure_complexity': self.weight_closures,
            'imports_count': self.weight_imports,
            'lines_of_code': self.weight_loc
        }


class SignalScoreConfig(BaseModel):
    """Custom scores for change signals."""
    
    metaClass_usage: float = Field(
        default=15.0,
        ge=0.0,
        description="Score for metaClass modifications"
    )
    dynamic_method_resolution: float = Field(
        default=10.0,
        ge=0.0,
        description="Score for dynamic method resolution (invokeMethod, etc.)"
    )
    pipeline_step: float = Field(
        default=12.0,
        ge=0.0,
        description="Score for pipeline steps (sh, parallel, etc.)"
    )
    closure_mutable_capture: float = Field(
        default=8.0,
        ge=0.0,
        description="Score for closures capturing mutable state"
    )
    new_dependency: float = Field(
        default=10.0,
        ge=0.0,
        description="Score for new infrastructure/plugin dependencies"
    )


class BlockComplexityDetail(BaseModel):
    """Detailed complexity information for a single changed block with granular breakdown."""
    
    # Block identification
    block_identifier: str = Field(description="Block identifier (method name, class name, etc.)")
    block_type: str = Field(description="Type of block (method, class, field, etc.)")
    change_type: str = Field(description="Type of change (added, deleted, modified, moved, moved_modified)")
    file_a_lines: Optional[str] = Field(
        default=None,
        description="Line range in file A (e.g., '10-25' or '15')"
    )
    file_b_lines: Optional[str] = Field(
        default=None,
        description="Line range in file B (e.g., '10-25' or '15')"
    )
    
    # 1. Raw metric values (per metric)
    source_metrics: Dict[str, float] = Field(
        description="Raw metric values from source block (file A)"
    )
    target_metrics: Dict[str, float] = Field(
        description="Raw metric values from target block (file B)"
    )
    
    # 2. Weighted metric values (per metric)
    weighted_source_metrics: Dict[str, float] = Field(
        description="Weighted metric values from source block (raw × weight)"
    )
    weighted_target_metrics: Dict[str, float] = Field(
        description="Weighted metric values from target block (raw × weight)"
    )
    
    # 3. Weighted complexity scores (sum of weighted metrics)
    source_complexity: float = Field(
        description="Sum of weighted source metrics"
    )
    target_complexity: float = Field(
        description="Sum of weighted target metrics"
    )
    
    # 4. Delta calculation
    raw_delta: float = Field(
        description="target_complexity - source_complexity"
    )
    effective_delta: float = Field(
        description="max(0, raw_delta) - negative deltas are ignored"
    )
    delta_multiplier: float = Field(
        description="Multiplier applied to effective delta (default 1.6)"
    )
    delta_score: float = Field(
        description="effective_delta × delta_multiplier"
    )
    
    # 5. Signal detection (per signal type)
    signal_counts: Dict[str, int] = Field(
        description="Count of each signal type detected"
    )
    signal_scores: Dict[str, float] = Field(
        description="Score contribution of each signal type"
    )
    total_signal_score: float = Field(
        description="Sum of all signal scores"
    )
    
    # 6. Final block score
    block_change_complexity: float = Field(
        description="Final score: target_complexity + delta_score + total_signal_score"
    )
    
    # 7. Normalized block score (for file-level aggregation)
    normalized_block_score: float = Field(
        description="Normalized score: block_change_complexity / sqrt(block_loc). Uses source LOC for deleted blocks, target LOC for others."
    )


class ChangeComplexitySummary(BaseModel):
    """Summary statistics for change complexity analysis."""
    
    # Change counts
    total_changed_blocks: int = Field(description="Total number of changed blocks analyzed")
    blocks_by_change_type: Dict[str, int] = Field(
        description="Count of blocks by change type"
    )
    blocks_by_risk_level: Dict[str, int] = Field(
        description="Count of blocks by risk level (Low/Medium/High)"
    )
    
    # Complexity statistics
    average_block_complexity: float = Field(
        description="Average complexity across all changed blocks"
    )
    max_block_complexity: float = Field(
        description="Highest block_change_complexity among changed blocks"
    )
    max_normalized_block_score: float = Field(
        description="Highest normalized_block_score (used as overall file score)"
    )
    total_target_complexity: float = Field(
        description="Sum of target complexity across all blocks"
    )
    total_delta_score: float = Field(
        description="Sum of delta scores across all blocks"
    )
    total_signal_score: float = Field(
        description="Sum of signal scores across all blocks"
    )
    
    # Weights used
    weights_used: Dict[str, float] = Field(
        description="The actual weights used in calculation"
    )


class ChangeComplexityResponse(BaseModel):
    """Complete response for change complexity analysis."""
    
    message: str = Field(description="Human-readable summary message")
    
    # File information
    file_a: "FileInfoSimple" = Field(description="Information about the original file")
    file_b: "FileInfoSimple" = Field(description="Information about the modified file")
    
    # Overall complexity
    overall_change_complexity_score: float = Field(
        description="Overall change complexity score: max(normalized_block_score) across all blocks"
    )
    risk_level: str = Field(
        description="Overall risk level: Low (<25), Medium (25-40), High (>40)"
    )
    
    # Detailed breakdown
    changed_blocks: List[BlockComplexityDetail] = Field(
        description="Detailed complexity analysis for each changed block"
    )
    summary: ChangeComplexitySummary = Field(
        description="Summary statistics and metadata"
    )
    
    # Optional warnings
    warnings: Optional[str] = Field(
        default=None,
        description="Any warnings or issues encountered during analysis"
    )


class ChangeComplexityRequest(BaseModel):
    """Request parameters for change complexity analysis."""
    
    # Custom weights (optional)
    weights: Optional[ComplexityWeights] = Field(
        default=None,
        description="Custom weights for complexity metrics (optional)"
    )
    
    # Custom signal scores (optional)
    signal_scores: Optional[SignalScoreConfig] = Field(
        default=None,
        description="Custom scores for change signals (optional)"
    )
    
    # Delta multiplier
    delta_multiplier: float = Field(
        default=1.6,
        ge=0.0,
        description="Multiplier for delta score (default 1.6)"
    )


# Import here to avoid circular imports
from app.schemas.file import FileInfoSimple

# Update forward references
ChangeComplexityResponse.model_rebuild()
