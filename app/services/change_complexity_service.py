"""
Change complexity service for orchestrating complexity analysis workflow.

Business logic layer that coordinates AST diff analysis with complexity
calculation to produce change complexity scores.
"""

from pathlib import Path
from typing import Dict, List, Optional

from ..domain.groovy_ast_diff import GroovyASTDiff
from ..domain.change_complexity_calculator import BlockComplexityCalculator, BlockComplexityResult
from ..domain.change_complexity_aggregator import ChangeComplexityAggregator
from ..domain.groovy_domain import ComparisonResult
from ..domain.signals.groovy_signal_detector import SignalConfig
from ..schemas.change_complexity import (
    ChangeComplexityResponse,
    ChangeComplexitySummary,
    BlockComplexityDetail,
    ComplexityWeights
)
from ..schemas.file import FileInfoSimple


class ChangeComplexityService:
    """
    Service layer for change complexity analysis operations.
    
    Orchestrates the complete workflow:
    1. AST diff analysis (using GroovyASTDiff)
    2. Block-level complexity calculation with direct parsing
    3. Signal detection for risky patterns
    4. Aggregation to file-level complexity score
    """
    
    def __init__(self):
        """Initialize the change complexity service."""
        self.groovy_differ = GroovyASTDiff()
        
        # Initialize block calculator with language and parser from differ
        self.block_calculator = BlockComplexityCalculator(
            self.groovy_differ.language,
            self.groovy_differ.parser
        )
        self.aggregator = ChangeComplexityAggregator()
    
    def analyze_change_complexity(
        self,
        file_a_path: str | Path,
        file_b_path: str | Path,
        custom_weights: Optional[ComplexityWeights] = None,
        custom_signal_config: Optional[SignalConfig] = None,
        delta_multiplier: float = 1.6
    ) -> tuple[ComparisonResult, List[BlockComplexityResult], float, str, ChangeComplexitySummary]:
        """
        Perform complete change complexity analysis on two Groovy files.
        
        Args:
            file_a_path: Path to the original file (source)
            file_b_path: Path to the modified file (target)
            custom_weights: Optional custom weights for metrics
            custom_signal_config: Optional custom signal score configuration
            delta_multiplier: Multiplier for delta score (default 1.6)
            
        Returns:
            Tuple of (comparison_result, block_results, overall_score, risk_level, summary)
            
        Raises:
            Exception: If files cannot be read or parsed
        """
        # Step 1: Perform AST diff analysis
        # This extracts blocks and populates file_a_code/file_b_code for each block
        comparison_result = self.groovy_differ.compare_files(str(file_a_path), str(file_b_path))
        
        if comparison_result.error:
            raise Exception(f"AST comparison failed: {comparison_result.error}")
        
        # Step 2: Filter blocks to analyze (exclude unchanged)
        blocks_to_analyze = self._filter_blocks_for_analysis(comparison_result.diffs)
        
        if not blocks_to_analyze:
            # No changes to analyze
            weights_dict = self._convert_weights_to_dict(custom_weights) if custom_weights else {}
            return comparison_result, [], 0.0, "Low", self.aggregator._create_empty_summary(weights_dict)
        
        # Step 3: Configure calculator
        if delta_multiplier != 1.6:
            self.block_calculator.set_delta_multiplier(delta_multiplier)
        
        if custom_signal_config:
            self.block_calculator.set_signal_config(custom_signal_config)
        
        # Step 4: Calculate complexity for each block
        # The calculator parses block code directly from diff.file_a_code/file_b_code
        weights_dict = self._convert_weights_to_dict(custom_weights) if custom_weights else None
        
        block_results = self.block_calculator.calculate_multiple_blocks(
            blocks_to_analyze,
            weights_dict
        )
        
        # Step 5: Aggregate to overall complexity score
        final_weights = weights_dict or self.block_calculator.DEFAULT_WEIGHTS
        overall_score, risk_level, summary = self.aggregator.aggregate_complexity(
            block_results,
            final_weights
        )
        
        return comparison_result, block_results, overall_score, risk_level, summary
    
    def create_complexity_response(
        self,
        comparison_result: ComparisonResult,
        block_results: List[BlockComplexityResult],
        overall_score: float,
        risk_level: str,
        summary: ChangeComplexitySummary,
        file_a_info: FileInfoSimple,
        file_b_info: FileInfoSimple
    ) -> ChangeComplexityResponse:
        """
        Create the final API response from analysis results.
        
        Args:
            comparison_result: AST diff comparison result
            block_results: Block-level complexity results
            overall_score: Overall complexity score
            risk_level: Risk level classification
            summary: Summary statistics
            file_a_info: File A information
            file_b_info: File B information
            
        Returns:
            Complete ChangeComplexityResponse
        """
        # Convert block results to schema format
        changed_blocks = [
            self._convert_block_result_to_schema(result)
            for result in block_results
        ]
        
        # Generate human-readable message
        message = self.aggregator.format_complexity_message(
            overall_score,
            risk_level,
            summary,
            file_a_info.original_filename,
            file_b_info.original_filename
        )
        
        # Include any warnings from the comparison
        warnings = comparison_result.warnings
        
        return ChangeComplexityResponse(
            message=message,
            file_a=file_a_info,
            file_b=file_b_info,
            overall_change_complexity_score=round(overall_score, 2),
            risk_level=risk_level,
            changed_blocks=changed_blocks,
            summary=summary,
            warnings=warnings
        )
    
    def get_complexity_insights(
        self,
        block_results: List[BlockComplexityResult],
        limit: int = 5
    ) -> Dict[str, List[BlockComplexityResult]]:
        """
        Get insights about the most complex changes for reporting.
        
        Args:
            block_results: Block complexity results
            limit: Maximum number of blocks per category
            
        Returns:
            Dictionary with categorized complex blocks
        """
        insights = {
            'most_complex': self.aggregator.get_top_complex_blocks(block_results, limit),
            'high_risk': self.aggregator.get_blocks_by_risk_level(block_results, "High"),
            'medium_risk': self.aggregator.get_blocks_by_risk_level(block_results, "Medium")
        }
        
        return insights
    
    def _filter_blocks_for_analysis(self, all_diffs):
        """Filter block diffs to only those that should be analyzed."""
        from ..types.groovy_types import ChangeType
        
        # Only analyze changed blocks (exclude unchanged)
        return [
            diff for diff in all_diffs
            if diff.change_type != ChangeType.UNCHANGED
        ]
    
    def _convert_weights_to_dict(self, weights: Optional[ComplexityWeights]) -> Optional[Dict[str, float]]:
        """Convert ComplexityWeights to dictionary format."""
        if weights is None:
            return None
        
        return {
            'cyclomatic_complexity': weights.weight_cyclomatic,
            'dynamic_typing_score': weights.weight_dynamic_typing,
            'closure_complexity': weights.weight_closures,
            'imports_count': weights.weight_imports,
            'lines_of_code': weights.weight_loc
        }
    
    def _convert_block_result_to_schema(self, result: BlockComplexityResult) -> BlockComplexityDetail:
        """Convert BlockComplexityResult to API schema format."""
        return BlockComplexityDetail(
            block_identifier=result.block_identifier,
            block_type=result.block_type,
            change_type=result.change_type,
            file_a_lines=result.file_a_lines,
            file_b_lines=result.file_b_lines,
            
            # Raw metrics
            source_metrics={k: round(v, 2) for k, v in result.source_metrics.items()},
            target_metrics={k: round(v, 2) for k, v in result.target_metrics.items()},
            
            # Weighted metrics
            weighted_source_metrics={k: round(v, 4) for k, v in result.weighted_source_metrics.items()},
            weighted_target_metrics={k: round(v, 4) for k, v in result.weighted_target_metrics.items()},
            
            # Weighted complexity (sum of weighted metrics)
            source_complexity=round(result.source_complexity, 2),
            target_complexity=round(result.target_complexity, 2),
            
            # Delta calculation
            raw_delta=round(result.raw_delta, 2),
            effective_delta=round(result.effective_delta, 2),
            delta_multiplier=result.delta_multiplier,
            delta_score=round(result.delta_score, 2),
            
            # Signal detection
            signal_counts=result.signal_counts,
            signal_scores={k: round(v, 2) for k, v in result.signal_scores.items()},
            total_signal_score=round(result.total_signal_score, 2),
            
            # Final score
            block_change_complexity=round(result.block_change_complexity, 2),
            
            # Normalized score (for file-level aggregation)
            normalized_block_score=round(result.normalized_block_score, 2)
        )
    
    def validate_complexity_request(
        self,
        weights: Optional[ComplexityWeights]
    ) -> Optional[str]:
        """
        Validate complexity analysis request parameters.
        
        Args:
            weights: Custom weights to validate
            
        Returns:
            Error message if validation fails, None if valid
        """
        if weights is None:
            return None
        
        try:
            total = (
                weights.weight_cyclomatic +
                weights.weight_dynamic_typing +
                weights.weight_closures +
                weights.weight_imports +
                weights.weight_loc
            )
            if not (0.95 <= total <= 1.05):
                return f"Weights must sum to approximately 1.0, got {total:.3f}"
            return None
        except Exception as e:
            return f"Invalid weights: {str(e)}"
    
    def estimate_analysis_time(
        self,
        file_a_size: int,
        file_b_size: int
    ) -> float:
        """
        Estimate analysis time based on file sizes.
        
        Args:
            file_a_size: Size of file A in bytes
            file_b_size: Size of file B in bytes
            
        Returns:
            Estimated time in seconds
        """
        # Simple heuristic: ~1 second per 10KB of code
        total_size = file_a_size + file_b_size
        estimated_seconds = (total_size / 10240) + 0.5  # Base 0.5s overhead
        
        return min(estimated_seconds, 30.0)  # Cap at 30 seconds
    
    def check_file_size_limits(
        self,
        file_a_path: str | Path,
        file_b_path: str | Path,
        max_file_size: int = 5 * 1024 * 1024  # 5MB default
    ) -> Optional[str]:
        """
        Check if files exceed size limits for performance.
        
        Args:
            file_a_path: Path to file A
            file_b_path: Path to file B
            max_file_size: Maximum allowed file size in bytes
            
        Returns:
            Warning message if files are too large, None otherwise
        """
        try:
            size_a = Path(file_a_path).stat().st_size
            size_b = Path(file_b_path).stat().st_size
            
            if size_a > max_file_size or size_b > max_file_size:
                return (
                    f"Large files detected (A: {size_a//1024}KB, B: {size_b//1024}KB). "
                    f"Analysis may be slower but will process all changed blocks."
                )
        except Exception:
            pass  # Ignore file size check errors
        
        return None
