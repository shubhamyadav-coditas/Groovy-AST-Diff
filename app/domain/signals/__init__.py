"""
Signal detection module for Groovy change complexity analysis.

This module contains detectors for risky code patterns (change signals)
that contribute to the overall change complexity score.

Each signal type has its own detector class:
- MetaClassSignalDetector: detects metaClass modifications
- DynamicMethodSignalDetector: detects dynamic method resolution patterns
- PipelineStepSignalDetector: detects CI/CD pipeline steps
- ClosureCaptureSignalDetector: detects closures capturing mutable state
- NewDependencySignalDetector: detects new infrastructure dependencies
"""

from .base_signal_detector import BaseSignalDetector
from .metaclass_signal_detector import MetaClassSignalDetector
from .dynamic_method_signal_detector import DynamicMethodSignalDetector
from .pipeline_step_signal_detector import PipelineStepSignalDetector
from .closure_capture_signal_detector import ClosureCaptureSignalDetector
from .new_dependency_signal_detector import NewDependencySignalDetector
from .groovy_signal_detector import GroovySignalDetector, SignalConfig, SignalResult

__all__ = [
    # Base class
    'BaseSignalDetector',
    # Individual detectors
    'MetaClassSignalDetector',
    'DynamicMethodSignalDetector',
    'PipelineStepSignalDetector',
    'ClosureCaptureSignalDetector',
    'NewDependencySignalDetector',
    # Orchestrator
    'GroovySignalDetector',
    # Configuration and result
    'SignalConfig',
    'SignalResult',
]
