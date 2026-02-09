"""
Change complexity aggregator for combining block-level scores.

Aggregates individual block complexity scores into a final file-level
change complexity score with summary statistics.

Formula: FileScore = max(normalized_block_score for all blocks)
Where: normalized_block_score = block_change_complexity / sqrt(block_loc)

Thresholds from Groovy complexity document:
- < 25: Auto-merge (Low risk)
- 25-40: Review required (Medium risk)
- > 40: Block/Redesign (High risk)
"""

from collections import defaultdict
from typing import Dict, List

from .change_complexity_calculator import BlockComplexityResult, BlockComplexityCalculator
from ..schemas.change_complexity import ChangeComplexitySummary


class ChangeComplexityAggregator:
    """
    Aggregates block-level complexity scores into file-level metrics.
    
    Aggregation strategy:
    - Take max of all normalized_block_score values
    - normalized_block_score = block_change_complexity / sqrt(block_loc)
    - Apply thresholds: <25 (Low), 25-40 (Medium), >40 (High)
    - Provide detailed summary statistics
    """
    
    def __init__(self):
        """Initialize the aggregator."""
        pass
    
    def aggregate_complexity(
        self,
        block_results: List[BlockComplexityResult],
        weights_used: Dict[str, float]
    ) -> tuple[float, str, ChangeComplexitySummary]:
        """
        Aggregate block complexity results into overall file complexity.
        
        Args:
            block_results: List of block complexity calculation results
            weights_used: The weights that were used in calculations
            
        Returns:
            Tuple of (overall_score, risk_level, summary_stats)
        """
        if not block_results:
            # No changes detected
            return 0.0, "Low", self._create_empty_summary(weights_used)
        
        # Calculate overall complexity score
        # Formula: sum of all block_change_complexity values
        overall_score = self._calculate_overall_score(block_results)
        
        # Determine risk level using thresholds from Groovy document
        risk_level = BlockComplexityCalculator.get_risk_level(overall_score)
        
        # Generate summary statistics
        summary = self._generate_summary(block_results, weights_used)
        
        return overall_score, risk_level, summary
    
    def _calculate_overall_score(
        self,
        block_results: List[BlockComplexityResult]
    ) -> float:
        """
        Calculate the overall change complexity score.
        
        Formula: max(block.normalized_block_score for all blocks)
        Where: normalized_block_score = block_change_complexity / sqrt(block_loc)
        
        The score is capped at 100.
        """
        if not block_results:
            return 0.0
        score = max(result.normalized_block_score for result in block_results)
        # Cap the score at 100
        if score > 100:
            score = 100.0
        return score
    
    def _generate_summary(
        self,
        block_results: List[BlockComplexityResult],
        weights_used: Dict[str, float]
    ) -> ChangeComplexitySummary:
        """Generate comprehensive summary statistics."""
        
        # Count blocks by change type
        change_type_counts = defaultdict(int)
        for result in block_results:
            change_type_counts[result.change_type] += 1
        
        # Count blocks by risk level
        risk_level_counts = defaultdict(int)
        for result in block_results:
            risk_level = BlockComplexityCalculator.get_risk_level(result.block_change_complexity)
            risk_level_counts[risk_level] += 1
        
        # Calculate complexity statistics
        complexity_scores = [result.block_change_complexity for result in block_results]
        normalized_scores = [result.normalized_block_score for result in block_results]
        average_complexity = sum(complexity_scores) / len(complexity_scores) if complexity_scores else 0.0
        max_complexity = max(complexity_scores) if complexity_scores else 0.0
        max_normalized = max(normalized_scores) if normalized_scores else 0.0
        
        # Calculate totals for each component
        total_target = sum(result.target_complexity for result in block_results)
        total_delta = sum(result.delta_score for result in block_results)
        total_signal = sum(result.total_signal_score for result in block_results)
        
        return ChangeComplexitySummary(
            total_changed_blocks=len(block_results),
            blocks_by_change_type=dict(change_type_counts),
            blocks_by_risk_level=dict(risk_level_counts),
            average_block_complexity=round(average_complexity, 2),
            max_block_complexity=round(max_complexity, 2),
            max_normalized_block_score=round(max_normalized, 2),
            total_target_complexity=round(total_target, 2),
            total_delta_score=round(total_delta, 2),
            total_signal_score=round(total_signal, 2),
            weights_used=weights_used
        )
    
    def _create_empty_summary(
        self,
        weights_used: Dict[str, float]
    ) -> ChangeComplexitySummary:
        """Create summary for when no changes are detected."""
        
        return ChangeComplexitySummary(
            total_changed_blocks=0,
            blocks_by_change_type={},
            blocks_by_risk_level={"Low": 0},
            average_block_complexity=0.0,
            max_block_complexity=0.0,
            max_normalized_block_score=0.0,
            total_target_complexity=0.0,
            total_delta_score=0.0,
            total_signal_score=0.0,
            weights_used=weights_used
        )
    
    def get_top_complex_blocks(
        self,
        block_results: List[BlockComplexityResult],
        limit: int = 5
    ) -> List[BlockComplexityResult]:
        """
        Get the most complex blocks for highlighting in results.
        
        Args:
            block_results: List of block complexity results
            limit: Maximum number of blocks to return
            
        Returns:
            List of top complex blocks, sorted by block_change_complexity
        """
        if not block_results:
            return []
        
        # Sort by block change complexity (descending)
        sorted_blocks = sorted(
            block_results,
            key=lambda x: x.block_change_complexity,
            reverse=True
        )
        
        return sorted_blocks[:limit]
    
    def get_blocks_by_risk_level(
        self,
        block_results: List[BlockComplexityResult],
        risk_level: str
    ) -> List[BlockComplexityResult]:
        """
        Get all blocks at a specific risk level.
        
        Args:
            block_results: List of block complexity results
            risk_level: Target risk level ("Low", "Medium", "High")
            
        Returns:
            List of blocks at the specified risk level
        """
        matching_blocks = []
        
        for result in block_results:
            block_risk = BlockComplexityCalculator.get_risk_level(result.block_change_complexity)
            if block_risk == risk_level:
                matching_blocks.append(result)
        
        return matching_blocks
    
    def calculate_change_impact_distribution(
        self,
        block_results: List[BlockComplexityResult]
    ) -> Dict[str, Dict[str, float]]:
        """
        Calculate distribution of complexity by change type and block type.
        
        Returns:
            Nested dictionary with change_type -> block_type -> total_complexity
        """
        distribution = defaultdict(lambda: defaultdict(float))
        
        for result in block_results:
            change_type = result.change_type
            block_type = result.block_type
            complexity = result.block_change_complexity
            
            distribution[change_type][block_type] += complexity
        
        # Convert defaultdict to regular dict for JSON serialization
        return {
            change_type: dict(block_types)
            for change_type, block_types in distribution.items()
        }
    
    @staticmethod
    def format_complexity_message(
        overall_score: float,
        risk_level: str,
        summary: ChangeComplexitySummary,
        file_a_name: str,
        file_b_name: str
    ) -> str:
        """
        Generate a human-readable message describing the complexity analysis.
        
        Args:
            overall_score: Overall complexity score
            risk_level: Risk level classification
            summary: Summary statistics
            file_a_name: Name of the original file
            file_b_name: Name of the modified file
            
        Returns:
            Formatted message string
        """
        if summary.total_changed_blocks == 0:
            return f"No changes detected between {file_a_name} and {file_b_name}"
        
        # Build change summary
        change_parts = []
        for change_type, count in summary.blocks_by_change_type.items():
            if count > 0:
                change_parts.append(f"{count} {change_type}")
        
        change_summary = ", ".join(change_parts)
        
        # Build risk recommendation based on thresholds
        if risk_level == "Low":
            recommendation = "Auto-merge eligible"
        elif risk_level == "Medium":
            recommendation = "Review required"
        else:
            recommendation = "Consider redesign"
        
        # Build score breakdown
        score_breakdown = (
            f"Target: {summary.total_target_complexity:.1f}, "
            f"Delta: {summary.total_delta_score:.1f}, "
            f"Signals: {summary.total_signal_score:.1f}"
        )
        
        return (
            f"Change complexity analysis complete: {change_summary}. "
            f"Overall score: {overall_score:.1f} ({risk_level} risk - {recommendation}). "
            f"[{score_breakdown}]"
        )
