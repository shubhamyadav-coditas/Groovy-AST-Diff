"""
Change Complexity Calculator for Groovy AST blocks.

Calculates block-level complexity scores using the formula:
    BlockScore = TargetComplexity + DeltaScore + SignalScore
    NormalizedBlockScore = BlockScore / sqrt(BlockLOC)

Where:
- TargetComplexity = weighted sum of 5 metrics on TARGET block
- DeltaScore = max(0, Target - Source) × DeltaMultiplier
- SignalScore = sum of triggered change signals
- BlockLOC = lines of code from source (for deleted) or target (for others)

File-level score = max(NormalizedBlockScore) across all blocks
"""

import math
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Set

from tree_sitter import Parser

from ..types.groovy_types import ChangeType
from .groovy_domain import BlockDiff
from .signals.groovy_signal_detector import GroovySignalDetector, SignalConfig, SignalResult
from .metrics.cyclomatic_complexity_calculator import CyclomaticComplexityCalculator
from .metrics.dynamic_typing_calculator import DynamicTypingCalculator
from .metrics.closure_complexity_calculator import ClosureComplexityCalculator
from .metrics.import_dependency_calculator import ImportDependencyCalculator
from .metrics.loc_calculator import LinesOfCodeCalculator
from ..types.groovy_types import BlockType


# Default delta multiplier from Groovy complexity document
DEFAULT_DELTA_MULTIPLIER = 1.6


@dataclass
class BlockComplexityResult:
    """Result of block complexity calculation with granular breakdown."""
    
    # Block identification
    block_identifier: str
    block_type: str
    change_type: str
    file_a_lines: Optional[str] = None  # e.g., "10-25"
    file_b_lines: Optional[str] = None  # e.g., "15-30"
    
    # 1. Raw metric values (per metric)
    source_metrics: Dict[str, float] = field(default_factory=lambda: {
        'cyclomatic_complexity': 0.0,
        'dynamic_typing_score': 0.0,
        'closure_complexity': 0.0,
        'imports_count': 0.0,
        'lines_of_code': 0.0
    })
    target_metrics: Dict[str, float] = field(default_factory=lambda: {
        'cyclomatic_complexity': 0.0,
        'dynamic_typing_score': 0.0,
        'closure_complexity': 0.0,
        'imports_count': 0.0,
        'lines_of_code': 0.0
    })
    
    # 2. Weighted metric values (per metric)
    weighted_source_metrics: Dict[str, float] = field(default_factory=lambda: {
        'cyclomatic_complexity': 0.0,
        'dynamic_typing_score': 0.0,
        'closure_complexity': 0.0,
        'imports_count': 0.0,
        'lines_of_code': 0.0
    })
    weighted_target_metrics: Dict[str, float] = field(default_factory=lambda: {
        'cyclomatic_complexity': 0.0,
        'dynamic_typing_score': 0.0,
        'closure_complexity': 0.0,
        'imports_count': 0.0,
        'lines_of_code': 0.0
    })
    
    # 3. Weighted complexity scores (sum of weighted metrics)
    source_complexity: float = 0.0  # Sum of weighted source metrics
    target_complexity: float = 0.0  # Sum of weighted target metrics
    
    # 4. Delta calculation
    raw_delta: float = 0.0           # target_complexity - source_complexity
    effective_delta: float = 0.0     # max(0, raw_delta)
    delta_multiplier: float = DEFAULT_DELTA_MULTIPLIER
    delta_score: float = 0.0         # effective_delta × delta_multiplier
    
    # 5. Signal detection (per signal type)
    signal_counts: Dict[str, int] = field(default_factory=lambda: {
        'metaClass_usage': 0,
        'dynamic_method_resolution': 0,
        'pipeline_step': 0,
        'closure_mutable_capture': 0,
        'new_dependency': 0
    })
    signal_scores: Dict[str, float] = field(default_factory=lambda: {
        'metaClass_usage': 0.0,
        'dynamic_method_resolution': 0.0,
        'pipeline_step': 0.0,
        'closure_mutable_capture': 0.0,
        'new_dependency': 0.0
    })
    total_signal_score: float = 0.0
    
    # 6. Final block score
    block_change_complexity: float = 0.0  # target + delta_score + signal_score
    
    # 7. Normalized block score (for file-level aggregation)
    # Formula: block_change_complexity / sqrt(block_loc)
    # block_loc = source LOC for deleted blocks, target LOC for others
    normalized_block_score: float = 0.0


class BlockComplexityCalculator:
    """
    Calculates complexity scores for individual changed blocks.
    
    Uses the formula:
        BlockScore = TargetComplexity + DeltaScore + SignalScore
    
    Parses block code directly using Tree-sitter for AST analysis.
    """
    
    # Default weights for complexity metrics
    DEFAULT_WEIGHTS: Dict[str, float] = {
        'cyclomatic_complexity': 0.35,
        'dynamic_typing_score': 0.25,
        'closure_complexity': 0.20,
        'imports_count': 0.15,
        'lines_of_code': 0.05
    }
    
    def __init__(self, language, parser: Parser):
        """
        Initialize the complexity calculator.
        
        Args:
            language: Tree-sitter Language instance for Groovy
            parser: Tree-sitter Parser instance for parsing block code
        """
        self.language = language
        self.parser = parser
        
        # Initialize metric calculators
        self.cyclomatic_calc = CyclomaticComplexityCalculator(language)
        self.dynamic_typing_calc = DynamicTypingCalculator(language)
        self.closure_calc = ClosureComplexityCalculator(language)
        self.import_calc = ImportDependencyCalculator(language)
        self.loc_calc = LinesOfCodeCalculator(language)
        
        # Initialize signal detector
        self.signal_detector = GroovySignalDetector(language, parser)
        
        # Configuration
        self.delta_multiplier = DEFAULT_DELTA_MULTIPLIER
        self.signal_config = SignalConfig()
    
    def set_delta_multiplier(self, multiplier: float) -> None:
        """Set custom delta multiplier."""
        self.delta_multiplier = multiplier
    
    def set_signal_config(self, config: SignalConfig) -> None:
        """Set custom signal configuration."""
        self.signal_config = config
        self.signal_detector.set_config(config)
    
    def calculate_block_complexity(
        self,
        block_diff: BlockDiff,
        custom_weights: Optional[Dict[str, float]] = None
    ) -> BlockComplexityResult:
        """
        Calculate complexity for a single changed block.
        
        Args:
            block_diff: The block difference from AST comparison
            custom_weights: Optional custom weights for metrics
            
        Returns:
            BlockComplexityResult with all complexity calculations
        """
        weights = custom_weights or self.DEFAULT_WEIGHTS
        change_type = block_diff.change_type
        
        # Initialize result
        result = BlockComplexityResult(
            block_identifier=block_diff.identifier,
            block_type=block_diff.block_type.value,
            change_type=change_type.value,
            file_a_lines=self._format_line_range(
                block_diff.file_a_start_line,
                block_diff.file_a_end_line
            ),
            file_b_lines=self._format_line_range(
                block_diff.file_b_start_line,
                block_diff.file_b_end_line
            ),
            delta_multiplier=self.delta_multiplier
        )
        
        # Extract source imports for new_dependency detection
        source_imports: Set[str] = set()
        if block_diff.file_a_code:
            source_imports = self.signal_detector.extract_imports_from_code(block_diff.file_a_code)
        
        # Calculate metrics based on change type
        if change_type == ChangeType.DELETED:
            # For DELETED: analyze source only, no target
            result.source_metrics = self._calculate_metrics(block_diff.file_a_code, weights)
            result.weighted_source_metrics, result.source_complexity = self._calculate_weighted_complexity(
                result.source_metrics, weights
            )
            # Target is zero for deleted blocks
            result.target_complexity = 0.0
            
            # Detect signals in SOURCE for deleted blocks
            if block_diff.file_a_code:
                signal_result = self.signal_detector.detect_signals(
                    block_diff.file_a_code, None
                )
                result.signal_counts = signal_result.signal_counts
                result.signal_scores = signal_result.signal_scores
                result.total_signal_score = signal_result.total_signal_score
        
        elif change_type == ChangeType.ADDED:
            # For ADDED: analyze target only, no source
            result.target_metrics = self._calculate_metrics(block_diff.file_b_code, weights)
            result.weighted_target_metrics, result.target_complexity = self._calculate_weighted_complexity(
                result.target_metrics, weights
            )
            # Source is zero for added blocks
            result.source_complexity = 0.0
            
            # Detect signals in TARGET for added blocks
            if block_diff.file_b_code:
                signal_result = self.signal_detector.detect_signals(
                    block_diff.file_b_code, source_imports
                )
                result.signal_counts = signal_result.signal_counts
                result.signal_scores = signal_result.signal_scores
                result.total_signal_score = signal_result.total_signal_score
        
        else:
            # For MODIFIED, MOVED, MOVED_MODIFIED: analyze both
            result.source_metrics = self._calculate_metrics(block_diff.file_a_code, weights)
            result.target_metrics = self._calculate_metrics(block_diff.file_b_code, weights)
            
            result.weighted_source_metrics, result.source_complexity = self._calculate_weighted_complexity(
                result.source_metrics, weights
            )
            result.weighted_target_metrics, result.target_complexity = self._calculate_weighted_complexity(
                result.target_metrics, weights
            )
            
            # Detect signals in TARGET for modified/moved blocks
            if block_diff.file_b_code:
                signal_result = self.signal_detector.detect_signals(
                    block_diff.file_b_code, source_imports
                )
                result.signal_counts = signal_result.signal_counts
                result.signal_scores = signal_result.signal_scores
                result.total_signal_score = signal_result.total_signal_score
        
        # Calculate delta
        result.raw_delta = result.target_complexity - result.source_complexity
        result.effective_delta = max(0.0, result.raw_delta)
        result.delta_score = result.effective_delta * self.delta_multiplier
        
        # Calculate final block score
        # Formula: TargetComplexity + DeltaScore + SignalScore
        result.block_change_complexity = (
            result.target_complexity +
            result.delta_score +
            result.total_signal_score
        )
        
        # Calculate normalized block score
        # Formula: block_change_complexity / sqrt(block_loc)
        # For deleted blocks, use source LOC; for others, use target LOC
        if change_type == ChangeType.DELETED:
            block_loc = result.source_metrics.get('lines_of_code', 0.0)
        else:
            block_loc = result.target_metrics.get('lines_of_code', 0.0)
        
        # Avoid division by zero - use sqrt(1) = 1 as minimum
        if block_loc > 0:
            result.normalized_block_score = result.block_change_complexity / math.sqrt(block_loc)
        else:
            # If LOC is 0, normalized score equals block score
            result.normalized_block_score = result.block_change_complexity
        
        return result
    
    def calculate_multiple_blocks(
        self,
        block_diffs: List[BlockDiff],
        custom_weights: Optional[Dict[str, float]] = None
    ) -> List[BlockComplexityResult]:
        """
        Calculate complexity for multiple changed blocks.
        
        Args:
            block_diffs: List of block differences
            custom_weights: Optional custom weights for metrics
            
        Returns:
            List of BlockComplexityResult objects
        """
        results = []
        
        for block_diff in block_diffs:
            # Skip unchanged blocks
            if block_diff.change_type == ChangeType.UNCHANGED or block_diff.block_type == BlockType.COMMENT:
                continue
            
            try:
                result = self.calculate_block_complexity(block_diff, custom_weights)
                results.append(result)
            except Exception as e:
                print(f"Error calculating complexity for block {block_diff.identifier}: {e}")
                # Create error result with zero values
                results.append(self._create_error_result(block_diff, str(e)))
        
        return results
    
    def _calculate_metrics(
        self,
        code: Optional[str],
        weights: Dict[str, float]
    ) -> Dict[str, float]:
        """
        Calculate all 5 metrics for a code block.
        
        Args:
            code: The code block to analyze
            weights: Metric weights (not used for raw calculation, but passed for consistency)
            
        Returns:
            Dictionary of metric name to raw value
        """
        if not code or not code.strip():
            return {
                'cyclomatic_complexity': 0.0,
                'dynamic_typing_score': 0.0,
                'closure_complexity': 0.0,
                'imports_count': 0.0,
                'lines_of_code': 0.0
            }
        
        try:
            # Parse the code
            tree = self.parser.parse(code.encode('utf-8'))
            root_node = tree.root_node
            source_bytes = code.encode('utf-8')
            
            # Calculate each metric
            cyclomatic = float(self.cyclomatic_calc.calculate(root_node, source_bytes))
            dynamic_typing = float(self.dynamic_typing_calc.calculate(root_node, source_bytes))
            closure = float(self.closure_calc.calculate(root_node, source_bytes))
            imports = float(self.import_calc.calculate(root_node, source_bytes))
            loc = float(self.loc_calc.calculate(root_node, source_bytes))
            
            return {
                'cyclomatic_complexity': cyclomatic,
                'dynamic_typing_score': dynamic_typing,
                'closure_complexity': closure,
                'imports_count': imports,
                'lines_of_code': loc
            }
        
        except Exception as e:
            print(f"Warning: Failed to calculate metrics: {e}")
            return {
                'cyclomatic_complexity': 0.0,
                'dynamic_typing_score': 0.0,
                'closure_complexity': 0.0,
                'imports_count': 0.0,
                'lines_of_code': 0.0
            }
    
    def _calculate_weighted_complexity(
        self,
        metrics: Dict[str, float],
        weights: Dict[str, float]
    ) -> tuple[Dict[str, float], float]:
        """
        Calculate weighted metrics and their sum.
        
        Formula:
            (CC × 0.35) + (DT × 0.25) + (CL × 0.20) + (IMP × 0.15) + (LOC × 0.05)
        
        Args:
            metrics: Raw metric values
            weights: Metric weights
            
        Returns:
            Tuple of (weighted_metrics dict, total weighted complexity score)
        """
        weighted_metrics = {}
        total = 0.0
        for metric_name, value in metrics.items():
            weight = weights.get(metric_name, 0.0)
            weighted_value = value * weight
            weighted_metrics[metric_name] = weighted_value
            total += weighted_value
        return weighted_metrics, total
    
    def _format_line_range(
        self,
        start_line: Optional[int],
        end_line: Optional[int]
    ) -> Optional[str]:
        """Format line range as string."""
        if start_line is None or end_line is None:
            return None
        
        if start_line == end_line:
            return str(start_line)
        else:
            return f"{start_line}-{end_line}"
    
    def _create_error_result(
        self,
        block_diff: BlockDiff,
        error_msg: str
    ) -> BlockComplexityResult:
        """Create a minimal result for blocks that failed to process."""
        return BlockComplexityResult(
            block_identifier=block_diff.identifier,
            block_type=block_diff.block_type.value,
            change_type=block_diff.change_type.value,
            file_a_lines=self._format_line_range(
                block_diff.file_a_start_line,
                block_diff.file_a_end_line
            ),
            file_b_lines=self._format_line_range(
                block_diff.file_b_start_line,
                block_diff.file_b_end_line
            ),
            delta_multiplier=self.delta_multiplier
        )
    
    @staticmethod
    def get_risk_level(complexity_score: float) -> str:
        """
        Determine risk level based on complexity score.
        
        Thresholds from Groovy complexity document:
        - < 20: Auto-merge (Low)
        - 21-40: Normal Review (Medium)
        - 41-60: Senior Review (High)
        - > 60: Block/Redesign (Critical)
        """

        if complexity_score < 20:
            return "Low"
        elif complexity_score <= 40:
            return "Medium"
        elif complexity_score <= 60:
            return "High"
        else:
            return "Critical"