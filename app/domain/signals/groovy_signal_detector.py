"""
Groovy Signal Detector for Change Complexity Analysis.

Orchestrates signal detection using individual signal detectors.
Detects risky code patterns (change signals) in Groovy code that contribute
to the overall change complexity score.
"""

from dataclasses import dataclass, field
from typing import Dict, List, Optional, Set
from tree_sitter import Node, Language, Parser

from .base_signal_detector import BaseSignalDetector
from .metaclass_signal_detector import MetaClassSignalDetector
from .dynamic_method_signal_detector import DynamicMethodSignalDetector
from .pipeline_step_signal_detector import PipelineStepSignalDetector
from .closure_capture_signal_detector import ClosureCaptureSignalDetector
from .new_dependency_signal_detector import NewDependencySignalDetector


@dataclass
class SignalConfig:
    """Configuration for signal detection with configurable scores."""
    
    # Signal scores (default values from Groovy complexity document)
    metaClass_usage: float = 15.0
    dynamic_method_resolution: float = 10.0
    pipeline_step: float = 12.0
    closure_mutable_capture: float = 8.0
    new_dependency: float = 10.0
    
    def to_dict(self) -> Dict[str, float]:
        """Convert to dictionary format."""
        return {
            'metaClass_usage': self.metaClass_usage,
            'dynamic_method_resolution': self.dynamic_method_resolution,
            'pipeline_step': self.pipeline_step,
            'closure_mutable_capture': self.closure_mutable_capture,
            'new_dependency': self.new_dependency
        }
    
    def get_score(self, signal_name: str) -> float:
        """Get the score for a specific signal type."""
        return self.to_dict().get(signal_name, 0.0)


@dataclass
class SignalResult:
    """Result of signal detection for a code block."""
    
    # Count of each signal type detected
    signal_counts: Dict[str, int] = field(default_factory=lambda: {
        'metaClass_usage': 0,
        'dynamic_method_resolution': 0,
        'pipeline_step': 0,
        'closure_mutable_capture': 0,
        'new_dependency': 0
    })
    
    # Score contribution of each signal type
    signal_scores: Dict[str, float] = field(default_factory=lambda: {
        'metaClass_usage': 0.0,
        'dynamic_method_resolution': 0.0,
        'pipeline_step': 0.0,
        'closure_mutable_capture': 0.0,
        'new_dependency': 0.0
    })
    
    # Total signal score
    total_signal_score: float = 0.0
    
    def calculate_scores(self, config: SignalConfig) -> None:
        """Calculate signal scores based on counts and config."""
        total = 0.0
        
        for signal_name, count in self.signal_counts.items():
            score = count * config.get_score(signal_name)
            self.signal_scores[signal_name] = score
            total += score
        
        self.total_signal_score = total


class GroovySignalDetector:
    """
    Orchestrates signal detection in Groovy code blocks.
    
    Uses individual signal detectors for each signal type:
    - MetaClassSignalDetector: metaClass modifications
    - DynamicMethodSignalDetector: invokeMethod, methodMissing, etc.
    - PipelineStepSignalDetector: sh, parallel, node, stage, etc.
    - ClosureCaptureSignalDetector: closures capturing mutable state
    - NewDependencySignalDetector: new infrastructure/plugin imports
    """
    
    def __init__(self, language: Language, parser: Parser):
        """
        Initialize the signal detector with all individual detectors.
        
        Args:
            language: Tree-sitter Language instance for Groovy
            parser: Tree-sitter Parser instance
        """
        self.language = language
        self.parser = parser
        self.config = SignalConfig()
        
        # Initialize individual signal detectors
        self._detectors: Dict[str, BaseSignalDetector] = {
            'metaClass_usage': MetaClassSignalDetector(),
            'dynamic_method_resolution': DynamicMethodSignalDetector(),
            'pipeline_step': PipelineStepSignalDetector(),
            'closure_mutable_capture': ClosureCaptureSignalDetector(),
            'new_dependency': NewDependencySignalDetector(),
        }
    
    def set_config(self, config: SignalConfig) -> None:
        """Set custom signal configuration."""
        self.config = config
    
    def get_detectors(self) -> List[BaseSignalDetector]:
        """Get list of all signal detectors."""
        return list(self._detectors.values())
    
    def detect_signals(
        self,
        code: str,
        source_imports: Optional[Set[str]] = None
    ) -> SignalResult:
        """
        Detect all change signals in a code block.
        
        Args:
            code: The code block to analyze
            source_imports: Set of imports from source file (for detecting NEW imports)
            
        Returns:
            SignalResult with counts and scores for each signal type
        """
        if not code or not code.strip():
            return SignalResult()
        
        # Parse the code
        try:
            tree = self.parser.parse(code.encode('utf-8'))
            root_node = tree.root_node
        except Exception as e:
            print(f"Warning: Failed to parse code for signal detection: {e}")
            return SignalResult()
        
        result = SignalResult()
        source_bytes = code.encode('utf-8')
        
        # Run each detector
        for signal_name, detector in self._detectors.items():
            try:
                if signal_name == 'new_dependency':
                    # Pass source_imports for new dependency detection
                    count = detector.detect(root_node, source_bytes, source_imports=source_imports)
                else:
                    count = detector.detect(root_node, source_bytes)
                
                result.signal_counts[signal_name] = count
            except Exception as e:
                print(f"Warning: Error in {signal_name} detection: {e}")
                result.signal_counts[signal_name] = 0
        
        # Calculate scores based on counts
        result.calculate_scores(self.config)
        
        return result
    
    def detect_single_signal(
        self,
        signal_name: str,
        code: str,
        source_imports: Optional[Set[str]] = None
    ) -> int:
        """
        Detect a single signal type in a code block.
        
        Args:
            signal_name: The name of the signal to detect
            code: The code block to analyze
            source_imports: Set of imports from source file (for new_dependency)
            
        Returns:
            Count of signal occurrences
        """
        if signal_name not in self._detectors:
            raise ValueError(f"Unknown signal type: {signal_name}")
        
        if not code or not code.strip():
            return 0
        
        try:
            tree = self.parser.parse(code.encode('utf-8'))
            root_node = tree.root_node
            source_bytes = code.encode('utf-8')
            
            detector = self._detectors[signal_name]
            
            if signal_name == 'new_dependency':
                return detector.detect(root_node, source_bytes, source_imports=source_imports)
            else:
                return detector.detect(root_node, source_bytes)
        except Exception as e:
            print(f"Warning: Error detecting {signal_name}: {e}")
            return 0
    
    def extract_imports_from_code(self, code: str) -> Set[str]:
        """
        Extract all import statements from code.
        
        Used to compare source and target imports for new_dependency detection.
        
        Args:
            code: Source code to analyze
            
        Returns:
            Set of import statement strings
        """
        if not code or not code.strip():
            return set()
        
        try:
            tree = self.parser.parse(code.encode('utf-8'))
            root_node = tree.root_node
            source_bytes = code.encode('utf-8')
        except Exception:
            return set()
        
        imports: Set[str] = set()
        
        def traverse(node: Node):
            if node.type in ['groovy_import', 'import_declaration', 'import_statement']:
                try:
                    import_text = source_bytes[node.start_byte:node.end_byte].decode('utf-8')
                    imports.add(import_text)
                except (UnicodeDecodeError, IndexError):
                    pass
            
            for child in node.children:
                traverse(child)
        
        traverse(root_node)
        return imports
