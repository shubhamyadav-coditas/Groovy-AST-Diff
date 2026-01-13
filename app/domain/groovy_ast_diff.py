#!/usr/bin/env python3
"""
Groovy AST Diff Tool

A Python application that compares two Groovy source files by analyzing their
Abstract Syntax Trees (ASTs) using Tree-sitter, following the same approach
as the JavaScript AST Diff POC.

This implementation provides:
- Block-level comparison (classes, methods, fields, etc.)
- Hierarchical diff structure with statement-level comparison
- Multi-phase matching strategy
- Rich similarity scoring
- Comprehensive change type detection
"""

import argparse
import json
import os
import sys
import warnings
from pathlib import Path
from typing import Dict, List, Optional, Tuple, Any, Set
import difflib

# Suppress tree-sitter deprecation warnings
warnings.filterwarnings("ignore", category=FutureWarning, module="tree_sitter")

# Increase recursion limit for deep AST structures
sys.setrecursionlimit(5000)

import tree_sitter
from tree_sitter import Language, Parser, Node

from ..types.groovy_types import (
    ChangeType, BlockType, StatementChangeType,
    GROOVY_NODE_TYPE_TO_BLOCK_TYPE,
    GROOVY_FUNCTION_TYPES,
    GROOVY_CLASS_TYPES, 
    GROOVY_FIELD_TYPES,
    GROOVY_PURE_STATEMENT_TYPES,
    GROOVY_CONTAINER_TYPES
)
from .groovy_domain import (
    BlockSignature, StatementSignature, StatementDiff, BlockDiff, 
    ComparisonResult, GroovyASTNode,
    calculate_similarity, normalize_code, hash_content
)
from .groovy_recursive_parser import GroovyRecursiveParser


class GroovyASTDiff:
    """
    Groovy AST comparison service following the JavaScript POC approach.
    
    Uses a sophisticated multi-phase matching strategy:
    1. Block-level extraction and classification
    2. Multi-phase matching (identifier, content hash, structural similarity)
    3. Statement-level comparison for modified blocks
    4. Hierarchical diff generation
    """
    
    def __init__(self):
        """Initialize the Groovy AST differ."""
        self.parser = None
        self.language = None
        self.recursive_parser = GroovyRecursiveParser()
        self._setup_parser()
    
    def _setup_parser(self):
        """Set up the Tree-sitter parser for Groovy."""
        try:
            # Path to the already built Groovy parser library
            library_path = Path("build/groovy.so")
            
            if not library_path.exists():
                raise FileNotFoundError(
                    "Groovy parser library not found. Please run 'python setup_parser.py' first."
                )
            
            # Use the already built library (don't rebuild it)
            self.language = Language(str(library_path), 'groovy')
            self.parser = Parser()
            self.parser.set_language(self.language)
            
        except Exception as e:
            print(f"Error setting up parser: {e}")
            print("Make sure to run 'python setup_parser.py' first.")
            sys.exit(1)
    
    def compare_files(self, file_a_path: str, file_b_path: str) -> ComparisonResult:
        """
        Compare two Groovy files and return detailed differences.
        
        Args:
            file_a_path: Path to the first file (original/base)
            file_b_path: Path to the second file (modified/new)
            
        Returns:
            ComparisonResult with all detected changes
        """
        try:
            print("Initializing parser...")
            
            # Read and parse both files
            print(f"Parsing file A: {file_a_path}")
            with open(file_a_path, 'r', encoding='utf-8') as f:
                source_a = f.read()
            
            print(f"Parsing file B: {file_b_path}")
            with open(file_b_path, 'r', encoding='utf-8') as f:
                source_b = f.read()
            
            return self.compare_sources(
                source_a.encode('utf-8'), 
                source_b.encode('utf-8'),
                file_a_path,
                file_b_path
            )
            
        except Exception as e:
            return ComparisonResult(
                is_identical=False,
                structural_similarity=0.0,
                total_blocks_a=0,
                total_blocks_b=0,
                blocks_added=0,
                blocks_deleted=0,
                blocks_modified=0,
                blocks_moved=0,
                blocks_unchanged=0,
                diffs=[],
                error=str(e),
                file_a_path=file_a_path,
                file_b_path=file_b_path
            )
    
    def compare_sources(
        self, 
        source_a: bytes, 
        source_b: bytes,
        file_a_path: str = "",
        file_b_path: str = ""
    ) -> ComparisonResult:
        """
        Compare two Groovy sources using recursive AST parsing and return detailed hierarchical differences.
        
        Args:
            source_a: Source code of first file as bytes
            source_b: Source code of second file as bytes
            file_a_path: Path to first file (for metadata)
            file_b_path: Path to second file (for metadata)
            
        Returns:
            ComparisonResult with hierarchical BlockDiff and nested StatementDiff
        """
        try:
            # Parse both sources
            tree_a = self.parser.parse(source_a)
            tree_b = self.parser.parse(source_b)
            
            # Check for actual ERROR nodes, not just the has_error flag (which can be false positive)
            def has_actual_errors(node):
                """Check for actual ERROR nodes in the AST."""
                if node.type == 'ERROR':
                    return True
                return any(has_actual_errors(child) for child in node.children)
            
            if has_actual_errors(tree_a.root_node) or has_actual_errors(tree_b.root_node):
                error_details = self._get_detailed_parsing_errors(
                    tree_a, tree_b, source_a, source_b, file_a_path, file_b_path
                )
                if error_details:  # Only return error if actual errors were found
                    return self._error_result(f"One or both sources have syntax errors: {error_details}")
            
            print("Extracting hierarchical signatures using recursive parser...")
            # Extract hierarchical signatures using recursive parser
            signatures_a = self._extract_recursive_signatures(tree_a.root_node, source_a)
            signatures_b = self._extract_recursive_signatures(tree_b.root_node, source_b)
            
            print(f"Found {len(signatures_a)} top-level blocks in file A, {len(signatures_b)} in file B")
            
            # Compare using recursive approach
            print("Comparing using recursive approach...")
            return self._compare_recursive_signatures(signatures_a, signatures_b, source_a, source_b, file_a_path, file_b_path)
            
        except Exception as e:
            import traceback
            traceback.print_exc()
            return self._error_result(f"Comparison failed: {e}")
    
    def _extract_recursive_signatures(self, root_node: Node, source: bytes) -> List[Any]:
        """
        Extract top-level blocks using recursive parser to build hierarchical signatures.
        
        This replaces the old flat block extraction with recursive parsing that
        captures the full hierarchy of nested structures.
        """
        signatures = []
        print("####################################################################################################")
        
        # Process each top-level child node
        children = list(root_node.named_children)
        i = 0
        
        while i < len(children):
            child = children[i]
            
            # Handle typed declarations: identifier followed by assignment (emery syntax: USER_PROFILE_FORM formObj = Emery.form.newForm("USER_PROFILE_FORM"))
            if (child.type == 'identifier' and 
                i + 1 < len(children) and 
                children[i + 1].type == 'assignment'):
                
                # Combine the type identifier and assignment into a single typed declaration
                type_node = child
                assignment_node = children[i + 1]
                
                # Create a synthetic node that spans both the type and assignment
                combined_start = type_node.start_byte
                combined_end = assignment_node.end_byte
                combined_code = source[combined_start:combined_end].decode('utf-8', errors='replace')
                
                # Use the assignment node as the base but with extended code
                signature = self.recursive_parser.parse_recursive(assignment_node, source)
                # Override the code to include the type declaration
                signature.code = combined_code
                signatures.append(signature)
                
                # Skip the next node (assignment) since we processed it
                i += 2
                continue
            
            # Handle method calls with closure arguments: declaration/expression followed by closure
            # This fixes Groovy collection methods like: list.findAll { condition }
            if (child.type in {'declaration', 'expression_statement'} and 
                i + 1 < len(children) and 
                children[i + 1].type == 'closure'):
                
                # Check if the declaration/expression ends with a method call (like statusData.findAll)
                method_call_node = child
                closure_node = children[i + 1]
                
                # Combine the method call and closure into a single statement
                combined_start = method_call_node.start_byte
                combined_end = closure_node.end_byte
                combined_code = source[combined_start:combined_end].decode('utf-8', errors='replace')
                
                # Use the method call node as the base but with extended code
                signature = self.recursive_parser.parse_recursive(method_call_node, source)
                # Override the code to include the closure
                signature.code = combined_code
                signatures.append(signature)
                
                # Skip the next node (closure) since we processed it
                i += 2
                continue
            
            # Skip structural nodes that aren't meaningful blocks
            if child.type in {'identifier', 'type', 'parameters', 'formal_parameters'}:
                i += 1
                continue
            
            # Check if this is a recognized top-level block type
            block_type = GROOVY_NODE_TYPE_TO_BLOCK_TYPE.get(child.type)
            if block_type:
                # Use recursive parser to build hierarchical signature
                signature = self.recursive_parser.parse_recursive(child, source)
                signatures.append(signature)
            
            i += 1
        
        return signatures
    
    def _compare_recursive_signatures(
        self, 
        signatures_a: List[Any], 
        signatures_b: List[Any], 
        source_a: bytes, 
        source_b: bytes,
        file_a_path: str = "",
        file_b_path: str = ""
    ) -> ComparisonResult:
        """
        Compare hierarchical signatures using recursive matching and generate nested diffs.
        
        This implements the full recursive comparison approach similar to the JavaScript POC.
        """
        # Convert recursive signatures to BlockSignature format for compatibility
        blocks_a = self._convert_recursive_to_block_signatures(signatures_a)
        blocks_b = self._convert_recursive_to_block_signatures(signatures_b)
        
        # Use the existing comparison logic but with enhanced recursive statement analysis
        return self._compare_blocks(blocks_a, blocks_b, source_a, source_b, file_a_path, file_b_path)
    
    def _convert_recursive_to_block_signatures(self, recursive_signatures: List[Any]) -> List[BlockSignature]:
        """
        Convert recursive signatures to BlockSignature format for compatibility with existing comparison logic.
        
        This allows us to leverage the existing multi-phase matching while using recursive parsing.
        """
        block_signatures = []
        
        for sig in recursive_signatures:
            # Map recursive signature to BlockSignature
            block_type = GROOVY_NODE_TYPE_TO_BLOCK_TYPE.get(sig.node_type, BlockType.UNKNOWN)
            
            block_sig = BlockSignature(
                block_type=block_type,
                identifier=sig.identifier or sig.content_hash[:16],
                content_hash=sig.content_hash,
                start_line=sig.start_line,
                end_line=sig.end_line,
                code=sig.code,
                node_type=sig.node_type,
                children_count=len(sig.children),
                modifiers=[]  # TODO: Extract modifiers from recursive signature
            )
            
            block_signatures.append(block_sig)
        
        return block_signatures
    
    def _extract_top_level_blocks(self, root_node: Node, source: bytes) -> List[BlockSignature]:
        """
        Extract top-level blocks from the AST.
        
        Identifies classes, methods, fields, and other significant constructs.
        """
        blocks = []
        
        def extract_blocks_recursive(node: Node, depth: int = 0, in_class: bool = False):
            """Recursively extract blocks from the AST."""
            node_type = node.type
            
            
            # Skip certain nodes that aren't meaningful blocks
            if node_type in {'program', 'source_file', 'compilation_unit'}:
                # Process children of root-level containers
                for child in node.named_children:
                    extract_blocks_recursive(child, depth)
                return
            
            # Check if this is a block we care about
            block_type = GROOVY_NODE_TYPE_TO_BLOCK_TYPE.get(node_type)
            if block_type:
                block = self._create_block_signature(node, source, block_type)
                if block:
                    blocks.append(block)
                    
                    # Class methods and fields will be analyzed as statements within the class
                    # during statement-level comparison, not as separate top-level blocks
                    return  # Don't recurse further after extracting a block
            
            # For non-block nodes, check children (but only at shallow depth)
            if depth < 3:  # Allow slightly deeper recursion
                for child in node.named_children:
                    extract_blocks_recursive(child, depth + 1, in_class)
        
        extract_blocks_recursive(root_node)
        return blocks
    
    def _extract_class_members(self, class_node: Node, source: bytes, blocks: List[BlockSignature]):
        """Extract methods and fields from within a class."""
        def extract_from_node(node: Node):
            """Extract blocks from a node and its children."""
            node_type = node.type
            
            # Check if this node is a method or field
            block_type = GROOVY_NODE_TYPE_TO_BLOCK_TYPE.get(node_type)
            if block_type and node_type in (GROOVY_FUNCTION_TYPES | GROOVY_FIELD_TYPES):
                block = self._create_block_signature(node, source, block_type)
                if block:
                    # Prefix identifier with class name for uniqueness
                    class_name = self._extract_identifier(class_node, source) or "UnknownClass"
                    block.identifier = f"{class_name}.{block.identifier}"
                    blocks.append(block)
                return
            
            # Recursively check children
            for child in node.named_children:
                extract_from_node(child)
        
        # Extract from class body
        for child in class_node.named_children:
            extract_from_node(child)
    
    def _create_block_signature(self, node: Node, source: bytes, block_type: BlockType) -> Optional[BlockSignature]:
        """Create a block signature from a tree-sitter node."""
        try:
            # Extract code
            code = source[node.start_byte:node.end_byte].decode('utf-8', errors='replace')
            
            # Extract identifier
            identifier = self._extract_identifier(node, source) or hash_content(code)
            
            # Extract modifiers
            modifiers = self._extract_modifiers(node, source)
            
            # Calculate content hash
            content_hash = hash_content(code)
            
            # Count children
            children_count = len(list(node.named_children))
            
            return BlockSignature(
                block_type=block_type,
                identifier=identifier,
                content_hash=content_hash,
                start_line=node.start_point[0] + 1,
                end_line=node.end_point[0] + 1,
                code=code,
                node_type=node.type,
                children_count=children_count,
                modifiers=modifiers
            )
            
        except Exception as e:
            print(f"Error creating block signature: {e}")
            return None
    
    def _extract_identifier(self, node: Node, source: bytes) -> Optional[str]:
        """Extract the identifier (name) from a node."""
        # Try to find name field first
        name_node = node.child_by_field_name("name")
        if name_node:
            return source[name_node.start_byte:name_node.end_byte].decode('utf-8', errors='replace')
        
        # For function definitions, look for the function name more carefully
        if node.type in GROOVY_FUNCTION_TYPES:
            # Skip return type and look for the actual function name
            for i, child in enumerate(node.named_children):
                if child.type == "identifier":
                    # For Groovy functions, the function name is typically the second identifier
                    # (first might be return type like "String", "void", etc.)
                    identifier_text = source[child.start_byte:child.end_byte].decode('utf-8', errors='replace')
                    
                    # Skip common return types and look for the actual function name
                    if identifier_text not in {'void', 'String', 'int', 'double', 'boolean', 'Object', 'def'}:
                        return identifier_text
                    
                    # If this is a return type, look for the next identifier
                    for j in range(i + 1, len(node.named_children)):
                        next_child = node.named_children[j]
                        if next_child.type == "identifier":
                            return source[next_child.start_byte:next_child.end_byte].decode('utf-8', errors='replace')
                    
                    # If no next identifier found, this might be the function name after all
                    return identifier_text
        
        elif node.type in GROOVY_CLASS_TYPES:
            # Look for identifier as direct child
            for child in node.named_children:
                if child.type == "identifier":
                    return source[child.start_byte:child.end_byte].decode('utf-8', errors='replace')
        
        elif node.type in GROOVY_FIELD_TYPES:
            # For fields, look for variable_declarator or identifier
            for child in node.named_children:
                if child.type == "variable_declarator":
                    # Look for identifier within the declarator
                    for grandchild in child.named_children:
                        if grandchild.type == "identifier":
                            return source[grandchild.start_byte:grandchild.end_byte].decode('utf-8', errors='replace')
                elif child.type == "identifier":
                    return source[child.start_byte:child.end_byte].decode('utf-8', errors='replace')

        elif node.type == "function_call":
            # For function calls, extract the function name from the 'function' field
            function_node = node.child_by_field_name("function")
            if function_node:
                if function_node.type == "identifier":
                    # Simple function call: func()
                    return source[function_node.start_byte:function_node.end_byte].decode('utf-8', errors='replace')
                elif function_node.type == "dotted_identifier":
                    # Dotted function call: Emery.mdos.getMdosDisplayValuesClob()
                    # Extract the full dotted path as the identifier
                    return source[function_node.start_byte:function_node.end_byte].decode('utf-8', errors='replace')
                elif function_node.type in {"member_access", "property_access"}:
                    # Member access: obj.method()
                    return source[function_node.start_byte:function_node.end_byte].decode('utf-8', errors='replace')

        # Generic fallback - look for any identifier child
        for child in node.named_children:
            if child.type == "identifier":
                return source[child.start_byte:child.end_byte].decode('utf-8', errors='replace')
        
        return None
    
    def _extract_modifiers(self, node: Node, source: bytes) -> List[str]:
        """Extract modifiers (static, private, etc.) from a node."""
        modifiers = []
        
        # Look for modifier nodes in the tree
        for child in node.children:
            if child.type in {'public', 'private', 'protected', 'static', 'final', 'abstract'}:
                modifier = source[child.start_byte:child.end_byte].decode('utf-8', errors='replace')
                modifiers.append(modifier)
        
        return modifiers
    
    def _compare_blocks(
        self, 
        blocks_a: List[BlockSignature], 
        blocks_b: List[BlockSignature],
        source_a: bytes,
        source_b: bytes,
        file_a_path: str,
        file_b_path: str
    ) -> ComparisonResult:
        """
        Compare two lists of blocks using multi-phase matching strategy.
        
        Phase 1: Match by identifier (exact name match)
        Phase 2: Match by content hash (exact content match → MOVED)
        Phase 3: Match by structural similarity (≥70% similarity → MODIFIED)
        Phase 4: Remaining unmatched → ADDED/DELETED
        """
        diffs = []
        matched_a = set()
        matched_b = set()
        
        # Phase 1: Match by identifier (same type and name)
        print("Phase 1: Matching by identifier...")
        for i, block_a in enumerate(blocks_a):
            for j, block_b in enumerate(blocks_b):
                if (i not in matched_a and j not in matched_b and
                    block_a.block_type == block_b.block_type and
                    block_a.identifier == block_b.identifier):
                    
                    if block_a.content_hash == block_b.content_hash:
                        # Check if position changed (moved) - prioritize line position over array position
                        if block_a.start_line != block_b.start_line:
                            change_type = ChangeType.MOVED
                            similarity = 100.0
                            description = f"Moved {block_a.block_type.value} '{block_a.identifier}' from line {block_a.start_line} to line {block_b.start_line}"
                        else:
                            change_type = ChangeType.UNCHANGED
                            similarity = 100.0
                            description = f"Identical {block_a.block_type.value} '{block_a.identifier}'"
                    else:
                        # Modified (same identifier, different content) - check if also moved
                        similarity = calculate_similarity(block_a.code, block_b.code) * 100
                        if block_a.start_line != block_b.start_line:
                            # Different line position AND different content = MOVED_MODIFIED
                            change_type = ChangeType.MOVED_MODIFIED
                            description = f"Moved and modified {block_a.block_type.value} '{block_a.identifier}' from position {i+1} to {j+1} ({similarity:.1f}% similar)"
                        else:
                            # Same line position, different content = MODIFIED
                            change_type = ChangeType.MODIFIED
                            description = f"Modified {block_a.block_type.value} '{block_a.identifier}' ({similarity:.1f}% similar)"
                    
                    diff = BlockDiff(
                        change_type=change_type,
                        block_type=block_a.block_type,
                        identifier=block_a.identifier,
                        file_a_start_line=block_a.start_line,
                        file_a_end_line=block_a.end_line,
                        file_a_code=block_a.code,
                        file_b_start_line=block_b.start_line,
                        file_b_end_line=block_b.end_line,
                        file_b_code=block_b.code,
                        similarity_score=similarity,
                        description=description,
                        modifiers=block_b.modifiers
                    )
                    
                    # Add statement-level comparison for modified and moved_modified blocks
                    if change_type in [ChangeType.MODIFIED, ChangeType.MOVED_MODIFIED]:
                        diff.statement_diffs = self._compare_statements(block_a, block_b, source_a, source_b)
                    
                    # For modified blocks, add statement-level comparison
                    if change_type == ChangeType.MODIFIED:
                        diff.statement_diffs = self._compare_statements(block_a, block_b, source_a, source_b)
                    
                    diffs.append(diff)
                    matched_a.add(i)
                    matched_b.add(j)
                    break
        
        # Phase 2: Match by content hash (moved blocks)
        print("Phase 2: Matching by content hash...")
        for i, block_a in enumerate(blocks_a):
            if i in matched_a:
                continue
            for j, block_b in enumerate(blocks_b):
                if (j not in matched_b and
                    block_a.block_type == block_b.block_type and
                    block_a.content_hash == block_b.content_hash and
                    block_a.identifier == block_b.identifier):  # Same identifier for MOVED
                    
                    # Check if position actually changed
                    if block_a.start_line != block_b.start_line:
                        change_type = ChangeType.MOVED
                        description = f"Moved {block_a.block_type.value} '{block_a.identifier}' from line {block_a.start_line} to line {block_b.start_line}"
                    else:
                        change_type = ChangeType.UNCHANGED
                        description = f"Identical {block_a.block_type.value} '{block_a.identifier}'"
                    
                    diff = BlockDiff(
                        change_type=change_type,
                        block_type=block_a.block_type,
                        identifier=block_a.identifier,
                        file_a_start_line=block_a.start_line,
                        file_a_end_line=block_a.end_line,
                        file_a_code=block_a.code,
                        file_b_start_line=block_b.start_line,
                        file_b_end_line=block_b.end_line,
                        file_b_code=block_b.code,
                        similarity_score=100.0,
                        description=description,
                        modifiers=block_b.modifiers
                    )
                    
                    diffs.append(diff)
                    matched_a.add(i)
                    matched_b.add(j)
                    break
        
        # Phase 3: Match by structural similarity (hybrid approach)
        print("Phase 3: Matching by structural similarity...")
        
        # First pass: Use original greedy matching for high-confidence matches
        temp_matched_a = set()
        temp_matched_b = set()
        high_confidence_matches = []
        
        for i, block_a in enumerate(blocks_a):
            if i in matched_a:
                continue
            for j, block_b in enumerate(blocks_b):
                if (j not in matched_b and j not in temp_matched_b and
                    block_a.block_type == block_b.block_type):
                    
                    similarity = calculate_similarity(block_a.code, block_b.code)
                    
                    # Use higher threshold for comments (70%) as they need stronger similarity
                    threshold = 0.7 if block_a.block_type == BlockType.COMMENT else 0.3
                    
                    # High confidence threshold - if similarity is very high, match immediately
                    high_confidence_threshold = 0.9
                    
                    if similarity >= high_confidence_threshold:
                        high_confidence_matches.append((similarity, i, j, block_a, block_b))
                        temp_matched_a.add(i)
                        temp_matched_b.add(j)
                        break  # Take first high-confidence match (original behavior)
        
        # Second pass: For remaining blocks, use best-match approach
        similarity_candidates = []
        for i, block_a in enumerate(blocks_a):
            if i in matched_a or i in temp_matched_a:
                continue
            for j, block_b in enumerate(blocks_b):
                if (j not in matched_b and j not in temp_matched_b and
                    block_a.block_type == block_b.block_type):
                    
                    similarity = calculate_similarity(block_a.code, block_b.code)
                    threshold = 0.7 if block_a.block_type == BlockType.COMMENT else 0.3
                    
                    if similarity >= threshold:
                        similarity_candidates.append((similarity, i, j, block_a, block_b))
        
        # Sort by similarity (highest first) for best matches on remaining blocks
        similarity_candidates.sort(key=lambda x: x[0], reverse=True)
        
        # Process all matches (high-confidence first, then best-match)
        all_matches = high_confidence_matches + similarity_candidates
        
        for similarity, i, j, block_a, block_b in all_matches:
            if i in matched_a or j in matched_b:
                continue  # Already matched
                
            # Determine change type based on identifier and position
            if block_a.identifier == block_b.identifier:
                # Same identifier - check if position changed (moved and modified)
                if block_a.start_line != block_b.start_line:  # Different line positions = moved
                    change_type = ChangeType.MOVED_MODIFIED
                    description = f"Moved and modified {block_a.block_type.value} '{block_a.identifier}' from position {i} to {j} ({similarity*100:.1f}% similar)"
                else:
                    # Same position - just modified
                    change_type = ChangeType.MODIFIED
                    description = f"Modified {block_a.block_type.value} '{block_a.identifier}' ({similarity*100:.1f}% similar)"
            else:
                # Different identifiers - check if they're hash-based (comments, etc.)
                # or if they're similar enough to be considered modified
                is_hash_based_a = (len(block_a.identifier) == 16 and 
                                 all(c in '0123456789abcdef' for c in block_a.identifier.lower()))
                is_hash_based_b = (len(block_b.identifier) == 16 and 
                                 all(c in '0123456789abcdef' for c in block_b.identifier.lower()))
                
                if (is_hash_based_a and is_hash_based_b) or \
                   self._are_identifiers_similar(block_a.identifier, block_b.identifier, similarity):
                    # Hash-based identifiers or similar identifiers - treat as modified
                    if block_a.start_line != block_b.start_line:
                        change_type = ChangeType.MOVED_MODIFIED
                        description = f"Moved and modified {block_a.block_type.value} '{block_a.identifier}' → '{block_b.identifier}' from position {i} to {j} ({similarity*100:.1f}% similar)"
                    else:
                        change_type = ChangeType.MODIFIED
                        description = f"Modified {block_a.block_type.value} '{block_a.identifier}' → '{block_b.identifier}' ({similarity*100:.1f}% similar)"
                else:
                    # Very different identifiers - skip this match
                    continue
            
            diff = BlockDiff(
                change_type=change_type,
                block_type=block_a.block_type,
                identifier=block_b.identifier,
                file_a_start_line=block_a.start_line,
                file_a_end_line=block_a.end_line,
                file_a_code=block_a.code,
                file_b_start_line=block_b.start_line,
                file_b_end_line=block_b.end_line,
                file_b_code=block_b.code,
                similarity_score=similarity * 100,
                description=description,
                modifiers=block_b.modifiers
            )
            
            # Add statement-level comparison for modified and moved_modified blocks
            if change_type in [ChangeType.MODIFIED, ChangeType.MOVED_MODIFIED]:
                diff.statement_diffs = self._compare_statements(block_a, block_b, source_a, source_b)
            
            diffs.append(diff)
            matched_a.add(i)
            matched_b.add(j)
        
        # Phase 4: Remaining unmatched blocks
        print("Phase 4: Processing unmatched blocks...")
        
        # Deleted blocks (in A but not matched in B)
        for i, block_a in enumerate(blocks_a):
            if i not in matched_a:
                diff = BlockDiff(
                    change_type=ChangeType.DELETED,
                    block_type=block_a.block_type,
                    identifier=block_a.identifier,
                    file_a_start_line=block_a.start_line,
                    file_a_end_line=block_a.end_line,
                    file_a_code=block_a.code,
                    similarity_score=0.0,
                    description=f"Deleted {block_a.block_type.value} '{block_a.identifier}'",
                    modifiers=block_a.modifiers
                )
                diffs.append(diff)
        
        # Added blocks (in B but not matched in A)
        for j, block_b in enumerate(blocks_b):
            if j not in matched_b:
                diff = BlockDiff(
                    change_type=ChangeType.ADDED,
                    block_type=block_b.block_type,
                    identifier=block_b.identifier,
                    file_b_start_line=block_b.start_line,
                    file_b_end_line=block_b.end_line,
                    file_b_code=block_b.code,
                    similarity_score=0.0,
                    description=f"Added {block_b.block_type.value} '{block_b.identifier}'",
                    modifiers=block_b.modifiers
                )
                diffs.append(diff)
        
        # Calculate summary statistics
        change_counts = {}
        for change_type in ChangeType:
            change_counts[change_type] = sum(1 for diff in diffs if diff.change_type == change_type)
        
        # Calculate structural similarity
        unchanged_count = change_counts[ChangeType.UNCHANGED]
        moved_count = change_counts[ChangeType.MOVED]
        common_elements = unchanged_count + moved_count
        total_elements = len(blocks_a) + len(blocks_b)
        
        # Use Sørensen-Dice coefficient
        structural_similarity = (2 * common_elements / total_elements) if total_elements > 0 else 1.0
        
        is_identical = (len(diffs) == 0 or 
                       (len(diffs) > 0 and all(diff.change_type == ChangeType.UNCHANGED for diff in diffs)))
        
        return ComparisonResult(
            is_identical=is_identical,
            structural_similarity=structural_similarity,
            total_blocks_a=len(blocks_a),
            total_blocks_b=len(blocks_b),
            blocks_added=change_counts[ChangeType.ADDED],
            blocks_deleted=change_counts[ChangeType.DELETED],
            blocks_modified=change_counts[ChangeType.MODIFIED],
            blocks_moved=change_counts[ChangeType.MOVED],
            blocks_moved_modified=change_counts[ChangeType.MOVED_MODIFIED],
            blocks_unchanged=change_counts[ChangeType.UNCHANGED],
            diffs=diffs,
            file_a_path=file_a_path,
            file_b_path=file_b_path,
            file_a_nodes=self._count_nodes_in_blocks(blocks_a),
            file_b_nodes=self._count_nodes_in_blocks(blocks_b)
        )
    
    def _compare_statements(
        self, 
        block_a: BlockSignature, 
        block_b: BlockSignature,
        source_a: bytes,
        source_b: bytes
    ) -> List[StatementDiff]:
        """
        Compare statements within two modified blocks using recursive parser analysis.
        
        This uses the recursive parser to build hierarchical signatures and compare
        nested structures at all levels, following the JavaScript POC approach.
        """
        try:
            # Use recursive parser to analyze both blocks hierarchically
            print(f"Comparing statements in {block_a.block_type.value} '{block_a.identifier}' using recursive parser...")
            
            # Parse the entire files to get proper context
            tree_a = self.parser.parse(source_a)
            tree_b = self.parser.parse(source_b)
            
            # Find the specific nodes for these blocks
            node_a = self._find_node_at_line(tree_a.root_node, block_a.start_line)
            node_b = self._find_node_at_line(tree_b.root_node, block_b.start_line)
            
            if not node_a or not node_b:
                return self._simple_block_comparison(block_a, block_b)
            
            # Extract and compare the CONTENTS of the container, not the container itself
            if node_a.type in GROOVY_CLASS_TYPES:
                # For classes, extract and compare class members (methods, fields)
                return self._compare_class_members_directly(node_a, node_b, source_a, source_b)
            elif node_a.type in GROOVY_FUNCTION_TYPES:
                # For functions, extract and compare function body statements
                return self._compare_function_body_directly(node_a, node_b, source_a, source_b)
            else:
                # Special handling for if_statement nodes - use branch-aware analysis
                if node_a.type == "if_statement" and node_b.type == "if_statement":
                    return self._compare_if_statement_branches(node_a, node_b, source_a, source_b)
                
                # Special handling for try_statement nodes - use branch-aware analysis
                print(f"DEBUG: Checking try_statement: node_a.type={node_a.type}, node_b.type={node_b.type}")
                if node_a.type == "try_statement" and node_b.type == "try_statement":
                    print("DEBUG: Using try_statement branch-aware analysis")
                    return self._compare_try_statement_branches(node_a, node_b, source_a, source_b)
                
                # For other containers, use generic approach
                return self._compare_container_contents_directly(node_a, node_b, source_a, source_b)
            
        except Exception as e:
            print(f"Warning: Statement comparison failed, using simple comparison: {e}")
            return self._simple_block_comparison(block_a, block_b)
    
    def _are_identifiers_similar(self, id1: str, id2: str, threshold: float = 0.5) -> bool:
        """
        Check if two identifiers are similar using Sørensen-Dice coefficient.
        This helps in matching modified functions/variables with slightly different names.
        """
        if not id1 and not id2:
            return True
        if not id1 or not id2:
            return False
        
        # Split by common delimiters to get meaningful parts
        import re
        parts1 = set(re.split(r'[._]', id1.lower()))
        parts2 = set(re.split(r'[._]', id2.lower()))
        
        intersection = len(parts1.intersection(parts2))
        union = len(parts1) + len(parts2)
        
        return (2 * intersection) / union >= threshold if union > 0 else False
    
    def _compare_class_members_directly(self, class_node_a: Node, class_node_b: Node, source_a: bytes, source_b: bytes) -> List[StatementDiff]:
        """
        Compare class members (methods, fields) directly without redundant class wrapper.
        
        This extracts the actual members and compares them with proper identifier matching
        and similarity thresholds.
        """
        # Find class bodies
        body_a = self._find_class_body(class_node_a)
        body_b = self._find_class_body(class_node_b)
        
        if not body_a or not body_b:
            return []
        
        # Extract member signatures
        members_a = []
        members_b = []
        
        for i, child in enumerate(body_a.named_children):
            # Check if this child is ANY recognized BlockType (truly recursive)
            block_type = GROOVY_NODE_TYPE_TO_BLOCK_TYPE.get(child.type)
            if block_type or child.type in {"comment", "line_comment", "block_comment"}:
                code = source_a[child.start_byte:child.end_byte].decode('utf-8', errors='replace')
                identifier = self._extract_identifier(child, source_a) or f"member_{i}"
                
                member_sig = StatementSignature(
                    content_hash=hash_content(code),
                    code=code.strip(),
                    start_line=child.start_point[0] + 1,
                    end_line=child.end_point[0] + 1,
                    index=i,
                    node_type=child.type,
                    identifier=identifier
                )
                members_a.append(member_sig)
        
        for i, child in enumerate(body_b.named_children):
            # Check if this child is ANY recognized BlockType (truly recursive)
            block_type = GROOVY_NODE_TYPE_TO_BLOCK_TYPE.get(child.type)
            if block_type or child.type in {"comment", "line_comment", "block_comment"}:
                code = source_b[child.start_byte:child.end_byte].decode('utf-8', errors='replace')
                identifier = self._extract_identifier(child, source_b) or f"member_{i}"
                
                member_sig = StatementSignature(
                    content_hash=hash_content(code),
                    code=code.strip(),
                    start_line=child.start_point[0] + 1,
                    end_line=child.end_point[0] + 1,
                    index=i,
                    node_type=child.type,
                    identifier=identifier
                )
                members_b.append(member_sig)
        
        # Compare members with proper identifier matching and similarity thresholds
        return self._compare_statement_lists(members_a, members_b, class_node_a, class_node_b, source_a, source_b)
    
    def _compare_function_body_directly(self, func_node_a: Node, func_node_b: Node, source_a: bytes, source_b: bytes) -> List[StatementDiff]:
        """
        Compare function body statements directly without redundant function wrapper.
        """
        # Find function bodies
        body_a = self._find_function_body(func_node_a)
        body_b = self._find_function_body(func_node_b)
        
        if not body_a or not body_b:
            return []
        
        # Extract body statements
        statements_a = []
        statements_b = []
        
        for i, child in enumerate(body_a.named_children):
            code = source_a[child.start_byte:child.end_byte].decode('utf-8', errors='replace')
            identifier = self._extract_statement_identifier(child, source_a) or f"stmt_{i}"
            
            stmt_sig = StatementSignature(
                content_hash=hash_content(code),
                code=code.strip(),
                start_line=child.start_point[0] + 1,
                end_line=child.end_point[0] + 1,
                index=i,
                node_type=child.type,
                identifier=identifier
            )
            statements_a.append(stmt_sig)
        
        for i, child in enumerate(body_b.named_children):
            code = source_b[child.start_byte:child.end_byte].decode('utf-8', errors='replace')
            identifier = self._extract_statement_identifier(child, source_b) or f"stmt_{i}"
            
            stmt_sig = StatementSignature(
                content_hash=hash_content(code),
                code=code.strip(),
                start_line=child.start_point[0] + 1,
                end_line=child.end_point[0] + 1,
                index=i,
                node_type=child.type,
                identifier=identifier
            )
            statements_b.append(stmt_sig)
        
        # Compare statements
        return self._compare_statement_lists(statements_a, statements_b, func_node_a, func_node_b, source_a, source_b)
    
    def _compare_container_contents_directly(self, node_a: Node, node_b: Node, source_a: bytes, source_b: bytes) -> List[StatementDiff]:
        """
        Generic container content comparison for other container types.
        """
        # Extract direct children
        children_a = self._extract_container_children(node_a, source_a)
        children_b = self._extract_container_children(node_b, source_b)
        
        # Compare children
        return self._compare_statement_lists(children_a, children_b, node_a, node_b, source_a, source_b)
    
    def _compare_container_generically(self, node_a, node_b, source_a: bytes, source_b: bytes) -> List[StatementDiff]:
        """
        GENERIC recursive comparison that works for ANY container type.
        
        This replaces all the specialized logic (classes, functions, etc.) with
        a single unified approach that can handle:
        - Classes (methods, fields, comments)
        - Functions (statements, nested blocks)  
        - Control flow (if/else, loops, try/catch)
        - Any other container construct
        """
        try:
            # Step 1: Extract direct children from both containers
            children_a = self._extract_container_children(node_a, source_a)
            children_b = self._extract_container_children(node_b, source_b)
            
            # Step 2: Compare the children using the same logic as statement comparison
            statement_diffs = self._compare_statement_lists(children_a, children_b, node_a, node_b, source_a, source_b)
            
            # Step 3: For any matched container children, recurse deeper
            for diff in statement_diffs:
                if (diff.change_type in [StatementChangeType.MODIFIED, StatementChangeType.MOVED_MODIFIED] and
                    self._is_container_node(diff.node_type)):
                    
                    # Find the original nodes for recursive comparison
                    child_node_a = self._find_child_node_by_line(node_a, diff.file_a_line)
                    child_node_b = self._find_child_node_by_line(node_b, diff.file_b_line)
                    
                    if child_node_a and child_node_b:
                        # Recursively compare the contents of this container
                        child_diffs = self._compare_container_generically(
                            child_node_a, child_node_b, source_a, source_b
                        )
                        diff.child_diffs = child_diffs
            
            return statement_diffs
            
        except Exception as e:
            print(f"Warning: Generic container comparison failed: {e}")
            # Fallback to the old class-specific logic for compatibility
            if node_a.type in GROOVY_CLASS_TYPES:
                return self._extract_and_compare_class_members(node_a, node_b, source_a, source_b)
            else:
                return self._extract_and_compare_function_statements(node_a, node_b, source_a, source_b)
    
    def _compare_switch_statement_cases(self, node_a: Node, node_b: Node, source_a: bytes, source_b: bytes) -> List[StatementDiff]:
        """
        Compare switch statement cases similar to JavaScript POC approach.
        
        Extracts individual case statements and compares them separately.
        """
        try:
            # Find switch_block nodes in both switch statements
            switch_block_a = None
            switch_block_b = None
            
            for child in node_a.named_children:
                if child.type == "switch_block":
                    switch_block_a = child
                    break
                    
            for child in node_b.named_children:
                if child.type == "switch_block":
                    switch_block_b = child
                    break
            
            if not switch_block_a or not switch_block_b:
                return []
            
            # Extract case statements from both switch blocks
            cases_a = []
            cases_b = []
            
            for child in switch_block_a.named_children:
                if child.type in ["case", "switch_default"]:
                    case_code = source_a[child.start_byte:child.end_byte].decode('utf-8', errors='replace')
                    cases_a.append({
                        "node": child,
                        "code": case_code,
                        "type": child.type,
                        "line": child.start_point[0] + 1
                    })
            
            for child in switch_block_b.named_children:
                if child.type in ["case", "switch_default"]:
                    case_code = source_b[child.start_byte:child.end_byte].decode('utf-8', errors='replace')
                    cases_b.append({
                        "node": child,
                        "code": case_code,
                        "type": child.type,
                        "line": child.start_point[0] + 1
                    })
            
            # Compare cases using simple matching (by index for now)
            diffs = []
            max_cases = max(len(cases_a), len(cases_b))
            
            for i in range(max_cases):
                if i < len(cases_a) and i < len(cases_b):
                    case_a = cases_a[i]
                    case_b = cases_b[i]
                    
                    # Calculate similarity
                    similarity = self._calculate_text_similarity(case_a["code"], case_b["code"])
                    
                    if similarity < 1.0:  # Modified case
                        diffs.append(StatementDiff(
                            change_type=StatementChangeType.MODIFIED,
                            code=case_b["code"],
                            node_type=case_b["type"],
                            file_a_line=case_a["line"],
                            file_a_index=i,
                            file_b_line=case_b["line"],
                            file_b_index=i,
                            description=f"Statement modified at index {i} ({similarity*100:.0f}% similar)",
                            old_code=case_a["code"],
                            similarity_score=similarity,
                            child_diffs=[],
                            is_container=False,
                            branch_label=None
                        ))
                elif i < len(cases_a):
                    # Deleted case
                    case_a = cases_a[i]
                    diffs.append(StatementDiff(
                        change_type=StatementChangeType.DELETED,
                        code=case_a["code"],
                        node_type=case_a["type"],
                        file_a_line=case_a["line"],
                        file_a_index=i,
                        description=f"Case deleted at index {i}",
                        is_container=False,
                        child_diffs=[]
                    ))
                else:
                    # Added case
                    case_b = cases_b[i]
                    diffs.append(StatementDiff(
                        change_type=StatementChangeType.ADDED,
                        code=case_b["code"],
                        node_type=case_b["type"],
                        file_b_line=case_b["line"],
                        file_b_index=i,
                        description=f"Case added at index {i}",
                        is_container=False,
                        child_diffs=[]
                    ))
            
            return diffs
            
        except Exception as e:
            print(f"Warning: Failed to compare switch statement cases: {e}")
            return []

    def _compare_switch_block_cases(self, switch_block_a: Node, switch_block_b: Node, source_a: bytes, source_b: bytes) -> List[StatementDiff]:
        """
        Compare switch block cases directly (when comparison happens at switch_block level).
        
        This is similar to _compare_switch_statement_cases but works directly on switch_block nodes.
        """
        try:
            # Extract case statements from both switch blocks
            cases_a = []
            cases_b = []
            
            for child in switch_block_a.named_children:
                if child.type in ["case", "switch_default"]:
                    case_code = source_a[child.start_byte:child.end_byte].decode('utf-8', errors='replace')
                    cases_a.append({
                        "node": child,
                        "code": case_code,
                        "type": child.type,
                        "line": child.start_point[0] + 1
                    })
            
            for child in switch_block_b.named_children:
                if child.type in ["case", "switch_default"]:
                    case_code = source_b[child.start_byte:child.end_byte].decode('utf-8', errors='replace')
                    cases_b.append({
                        "node": child,
                        "code": case_code,
                        "type": child.type,
                        "line": child.start_point[0] + 1
                    })
            
            # Compare cases using simple matching (by index for now)
            diffs = []
            max_cases = max(len(cases_a), len(cases_b))
            
            for i in range(max_cases):
                if i < len(cases_a) and i < len(cases_b):
                    case_a = cases_a[i]
                    case_b = cases_b[i]
                    
                    # Calculate similarity
                    similarity = self._calculate_text_similarity(case_a["code"], case_b["code"])
                    
                    if similarity < 1.0:  # Modified case
                        diffs.append(StatementDiff(
                            change_type=StatementChangeType.MODIFIED,
                            code=case_b["code"],
                            node_type=case_b["type"],
                            file_a_line=case_a["line"],
                            file_a_index=i,
                            file_b_line=case_b["line"],
                            file_b_index=i,
                            description=f"Statement modified at index {i} ({similarity*100:.0f}% similar)",
                            old_code=case_a["code"],
                            similarity_score=similarity,
                            child_diffs=[],
                            is_container=False,
                            branch_label=None
                        ))
                elif i < len(cases_a):
                    # Deleted case
                    case_a = cases_a[i]
                    diffs.append(StatementDiff(
                        change_type=StatementChangeType.DELETED,
                        code=case_a["code"],
                        node_type=case_a["type"],
                        file_a_line=case_a["line"],
                        file_a_index=i,
                        description=f"Case deleted at index {i}",
                        is_container=False,
                        child_diffs=[]
                    ))
                else:
                    # Added case
                    case_b = cases_b[i]
                    diffs.append(StatementDiff(
                        change_type=StatementChangeType.ADDED,
                        code=case_b["code"],
                        node_type=case_b["type"],
                        file_b_line=case_b["line"],
                        file_b_index=i,
                        description=f"Case added at index {i}",
                        is_container=False,
                        child_diffs=[]
                    ))
            
            return diffs
            
        except Exception as e:
            print(f"Warning: Failed to compare switch block cases: {e}")
            return []

    def _fix_malformed_closure_statements(self, closure_node, source: bytes):
        """
        WORKAROUND: Fix grammar parsing bug where 'count++\\nprintln' gets parsed as malformed juxt_function_call.
        
        This detects the specific pattern:
        - juxt_function_call containing 'count++\\n    println'
        - string containing '"While loop: $count"'
        
        And converts it back to the correct statements:
        - increment_op: 'count++'
        - juxt_function_call: 'println "While loop: $count"'
        """
        children = list(closure_node.named_children)
        
        # Check for the specific malformed pattern
        if (len(children) == 2 and 
            children[0].type == 'juxt_function_call' and 
            children[1].type == 'string'):
            
            malformed_call = children[0]
            string_node = children[1]
            
            # Get the text of the malformed call
            call_text = source[malformed_call.start_byte:malformed_call.end_byte].decode('utf-8', errors='replace')
            string_text = source[string_node.start_byte:string_node.end_byte].decode('utf-8', errors='replace')
            
            # Check if this matches the malformed pattern: variable++\n    println
            # This is a generic fix for the grammar bug where increment + println gets malformed
            if ('++' in call_text and 'println' in call_text):
                
                print(f"WORKAROUND: Detected malformed parsing pattern, fixing...")
                
                # Create fixed statements
                fixed_children = []
                
                # Extract the increment operation (e.g., "x++", "count++", etc.)
                import re
                increment_match = re.search(r'(\w+\+\+)', call_text)
                if increment_match:
                    increment_code = increment_match.group(1)
                else:
                    increment_code = "variable++"  # fallback
                
                # Statement 1: variable++ (increment_op)
                fixed_children.append((malformed_call, increment_code, 'increment_op'))
                
                # Statement 2: println "message" (juxt_function_call)  
                println_code = f'println {string_text}'
                fixed_children.append((string_node, println_code, 'juxt_function_call'))
                
                return fixed_children
        
        # No malformed pattern detected, return original children
        return children

    def _calculate_text_similarity(self, text_a: str, text_b: str) -> float:
        """Calculate similarity between two text strings using simple ratio."""
        try:
            from difflib import SequenceMatcher
            return SequenceMatcher(None, text_a, text_b).ratio()
        except Exception:
            # Fallback to simple equality check
            return 1.0 if text_a == text_b else 0.0

    def _compare_if_statement_branches(
        self,
        node_a,
        node_b,
        source_a: bytes,
        source_b: bytes
    ) -> List[StatementDiff]:
        """
        Compare if_statement nodes using branch-aware analysis similar to JavaScript POC.
        
        This method extracts and compares individual branches (if, else if, else)
        and detects changes at the branch level.
        """
        diffs = []
        
        # Extract branches from both nodes
        branches_a = self._extract_if_branches(node_a, source_a)
        branches_b = self._extract_if_branches(node_b, source_b)
        
        # Track which branches have been matched
        matched_a = set()
        matched_b = set()
        
        # Phase 1: Match by condition (for if/else if branches)
        for i, branch_a in enumerate(branches_a):
            if i in matched_a:
                continue
                
            for j, branch_b in enumerate(branches_b):
                if j in matched_b:
                    continue
                
                # Match by condition for if/else_if branches
                if (branch_a["branch_type"] in ["if", "else_if"] and 
                    branch_b["branch_type"] in ["if", "else_if"] and
                    branch_a["condition"] == branch_b["condition"]):
                    
                    # Same condition - compare body statements
                    body_diffs = self._compare_statement_lists_generic(
                        branch_a["statements"], branch_b["statements"], source_a, source_b
                    )
                    
                    if not body_diffs:
                        # Unchanged branch
                        change_type = StatementChangeType.UNCHANGED
                        desc = f"{branch_a['branch_type']} ({branch_a['condition']}) unchanged"
                    else:
                        # Modified branch body
                        change_type = StatementChangeType.MODIFIED
                        desc = f"{branch_a['branch_type']} ({branch_a['condition']}) body modified"
                    
                    branch_label = f"{branch_a['branch_type']}({branch_a['condition']})"
                    
                    diffs.append(StatementDiff(
                        change_type=change_type,
                        code=branch_b["code"],
                        node_type="if_branch",
                        file_a_line=branch_a["start_line"],
                        file_a_index=i,
                        file_b_line=branch_b["start_line"],
                        file_b_index=j,
                        description=desc,
                        old_code=branch_a["code"],
                        branch_label=branch_label,
                        is_container=True,
                        child_diffs=body_diffs
                    ))
                    
                    matched_a.add(i)
                    matched_b.add(j)
                    break
                
                # Match else branches
                elif (branch_a["branch_type"] == "else" and 
                      branch_b["branch_type"] == "else"):
                    
                    # Compare else body statements
                    body_diffs = self._compare_statement_lists_generic(
                        branch_a["statements"], branch_b["statements"], source_a, source_b
                    )
                    
                    if not body_diffs:
                        change_type = StatementChangeType.UNCHANGED
                        desc = "else block unchanged"
                    else:
                        change_type = StatementChangeType.MODIFIED
                        desc = "else block modified"
                    
                    diffs.append(StatementDiff(
                        change_type=change_type,
                        code=branch_b["code"],
                        node_type="else_branch",
                        file_a_line=branch_a["start_line"],
                        file_a_index=i,
                        file_b_line=branch_b["start_line"],
                        file_b_index=j,
                        description=desc,
                        old_code=branch_a["code"],
                        branch_label="else",
                        is_container=True,
                        child_diffs=body_diffs
                    ))
                    
                    matched_a.add(i)
                    matched_b.add(j)
                    break
        
        # Phase 2: Handle unmatched branches (added/deleted)
        for i, branch_a in enumerate(branches_a):
            if i not in matched_a:
                # Deleted branch
                branch_label = f"{branch_a['branch_type']}({branch_a['condition']})" if branch_a['condition'] else branch_a['branch_type']
                diffs.append(StatementDiff(
                    change_type=StatementChangeType.DELETED,
                    code=branch_a["code"],
                    node_type="if_branch" if branch_a["branch_type"] != "else" else "else_branch",
                    file_a_line=branch_a["start_line"],
                    file_a_index=i,
                    description=f"{branch_a['branch_type']} ({branch_a['condition']}) deleted from position {i}" if branch_a['condition'] else f"{branch_a['branch_type']} deleted from position {i}",
                    branch_label=branch_label,
                    is_container=True
                ))
        
        for j, branch_b in enumerate(branches_b):
            if j not in matched_b:
                # Added branch
                branch_label = f"{branch_b['branch_type']}({branch_b['condition']})" if branch_b['condition'] else branch_b['branch_type']
                diffs.append(StatementDiff(
                    change_type=StatementChangeType.ADDED,
                    code=branch_b["code"],
                    node_type="if_branch" if branch_b["branch_type"] != "else" else "else_branch",
                    file_b_line=branch_b["start_line"],
                    file_b_index=j,
                    description=f"{branch_b['branch_type']} ({branch_b['condition']}) added at position {j}" if branch_b['condition'] else f"{branch_b['branch_type']} added at position {j}",
                    branch_label=branch_label,
                    is_container=True
                ))
        
        return diffs
    
    def _extract_if_branches(self, if_node, source: bytes) -> List[dict]:
        """
        Extract branches from an if_statement node.
        
        Returns a list of branch dictionaries with:
        - branch_type: "if", "else_if", or "else"
        - condition: the condition code (None for else)
        - statements: list of statement nodes in the branch body
        - start_line: starting line number
        - code: full branch code
        """
        branches = []
        
        # Main if branch
        condition_node = if_node.child_by_field_name("condition")
        body_node = if_node.child_by_field_name("body")
        
        if condition_node and body_node:
            condition_code = source[condition_node.start_byte:condition_node.end_byte].decode('utf-8', errors='replace')
            body_statements = self._get_block_statements_from_node(body_node)
            
            branches.append({
                "branch_type": "if",
                "condition": condition_code,
                "statements": body_statements,
                "start_line": if_node.start_point[0] + 1,
                "code": source[if_node.start_byte:body_node.end_byte].decode('utf-8', errors='replace')
            })
        
        # Handle else/else if
        else_body_node = if_node.child_by_field_name("else_body")
        if else_body_node:
            if else_body_node.type == "if_statement":
                # This is an "else if" - recursively extract its branches
                nested_branches = self._extract_if_branches(else_body_node, source)
                for branch in nested_branches:
                    if branch["branch_type"] == "if":
                        branch["branch_type"] = "else_if"
                    branches.extend([branch])
            else:
                # This is a plain "else"
                else_statements = self._get_block_statements_from_node(else_body_node)
                branches.append({
                    "branch_type": "else",
                    "condition": None,
                    "statements": else_statements,
                    "start_line": else_body_node.start_point[0] + 1,
                    "code": source[else_body_node.start_byte:else_body_node.end_byte].decode('utf-8', errors='replace')
                })
        
        return branches

    def _compare_try_statement_branches(
        self,
        node_a,
        node_b,
        source_a: bytes,
        source_b: bytes
    ) -> List[StatementDiff]:
        """
        Compare try_statement nodes using branch-aware analysis similar to JavaScript POC.
        
        This method extracts and compares individual branches (try, catch, finally)
        and detects changes at the branch level.
        """
        diffs = []
        
        # Extract branches from both nodes
        branches_a = self._extract_try_branches(node_a, source_a)
        branches_b = self._extract_try_branches(node_b, source_b)
        
        # Track which branches have been matched
        matched_a = set()
        matched_b = set()
        
        # Phase 1: Match by branch type (try, catch, finally)
        for i, branch_a in enumerate(branches_a):
            if i in matched_a:
                continue
                
            for j, branch_b in enumerate(branches_b):
                if j in matched_b:
                    continue
                
                # Match by branch type
                if branch_a["branch_type"] == branch_b["branch_type"]:
                    
                    # Compare body statements
                    body_diffs = self._compare_statement_lists_generic(
                        branch_a["statements"], branch_b["statements"], source_a, source_b
                    )
                    
                    if not body_diffs:
                        # Unchanged branch
                        change_type = StatementChangeType.UNCHANGED
                        desc = f"{branch_a['branch_type']} block unchanged"
                    else:
                        # Modified branch body
                        change_type = StatementChangeType.MODIFIED
                        desc = f"{branch_a['branch_type']} block modified"
                    
                    branch_label = branch_a['branch_type']
                    if branch_a['branch_type'] == 'catch' and branch_a.get('exception_type'):
                        branch_label += f"({branch_a['exception_type']})"
                    
                    diffs.append(StatementDiff(
                        change_type=change_type,
                        code=branch_b["code"],
                        node_type=f"{branch_a['branch_type']}_block",
                        file_a_line=branch_a["start_line"],
                        file_a_index=i,
                        file_b_line=branch_b["start_line"],
                        file_b_index=j,
                        description=desc,
                        old_code=branch_a["code"],
                        branch_label=branch_label,
                        is_container=True,
                        child_diffs=body_diffs
                    ))
                    
                    matched_a.add(i)
                    matched_b.add(j)
                    break
        
        # Phase 2: Handle unmatched branches (added/deleted)
        for i, branch_a in enumerate(branches_a):
            if i not in matched_a:
                diffs.append(StatementDiff(
                    change_type=StatementChangeType.DELETED,
                    code="",
                    node_type=f"{branch_a['branch_type']}_block",
                    file_a_line=branch_a["start_line"],
                    file_a_index=i,
                    description=f"Deleted {branch_a['branch_type']} block",
                    old_code=branch_a["code"],
                    branch_label=branch_a['branch_type'],
                    is_container=True
                ))
        
        for j, branch_b in enumerate(branches_b):
            if j not in matched_b:
                diffs.append(StatementDiff(
                    change_type=StatementChangeType.ADDED,
                    code=branch_b["code"],
                    node_type=f"{branch_b['branch_type']}_block",
                    file_b_line=branch_b["start_line"],
                    file_b_index=j,
                    description=f"Added {branch_b['branch_type']} block",
                    branch_label=branch_b['branch_type'],
                    is_container=True
                ))
        
        return diffs

    def _extract_try_branches(self, try_node, source: bytes) -> List[Dict]:
        """
        Extract try, catch, and finally branches from a try_statement node.
        """
        branches = []
        
        # Extract try body
        try_body = try_node.child_by_field_name("body")
        if try_body:
            try_statements = self._get_block_statements_from_node(try_body)
            branches.append({
                "branch_type": "try",
                "code": source[try_body.start_byte:try_body.end_byte].decode('utf-8'),
                "start_line": try_body.start_point[0] + 1,
                "end_line": try_body.end_point[0] + 1,
                "statements": try_statements
            })
        
        # Extract catch body
        catch_body = try_node.child_by_field_name("catch_body")
        if catch_body:
            catch_statements = self._get_block_statements_from_node(catch_body)
            
            # Try to extract exception type from catch clause
            exception_type = None
            catch_exception = try_node.child_by_field_name("catch_exception")
            if catch_exception:
                exception_type = source[catch_exception.start_byte:catch_exception.end_byte].decode('utf-8')
            
            branches.append({
                "branch_type": "catch",
                "code": source[catch_body.start_byte:catch_body.end_byte].decode('utf-8'),
                "start_line": catch_body.start_point[0] + 1,
                "end_line": catch_body.end_point[0] + 1,
                "statements": catch_statements,
                "exception_type": exception_type
            })
        
        # Extract finally body
        finally_body = try_node.child_by_field_name("finally_body")
        if finally_body:
            finally_statements = self._get_block_statements_from_node(finally_body)
            branches.append({
                "branch_type": "finally",
                "code": source[finally_body.start_byte:finally_body.end_byte].decode('utf-8'),
                "start_line": finally_body.start_point[0] + 1,
                "end_line": finally_body.end_point[0] + 1,
                "statements": finally_statements
            })
        
        return branches
    
    def _compare_statement_lists_generic(
        self,
        statements_a: List,
        statements_b: List,
        source_a: bytes,
        source_b: bytes
    ) -> List[StatementDiff]:
        """
        Generic method to compare two lists of statement nodes.
        
        This is used by the branch comparison to compare statements within
        individual if/else if/else branches.
        """
        # Convert nodes to StatementSignature objects
        sigs_a = []
        for i, stmt in enumerate(statements_a):
            print(f"DEBUG: Processing statement {i}: type={type(stmt)}, hasattr start_byte={hasattr(stmt, 'start_byte')}")
            if hasattr(stmt, 'start_byte'):
                code = source_a[stmt.start_byte:stmt.end_byte].decode('utf-8', errors='replace')
                identifier = self._extract_statement_identifier(stmt, source_a) or self._extract_identifier(stmt, source_a) or f"anonymous_{stmt.type}"
            else:
                # This is likely a StatementSignature object, not a tree-sitter node
                print(f"DEBUG: Statement is not a tree-sitter node: {stmt}")
                continue
            
            sigs_a.append(StatementSignature(
                content_hash=hash_content(code),
                code=code.strip(),
                start_line=stmt.start_point[0] + 1,
                end_line=stmt.end_point[0] + 1,
                index=i,
                node_type=stmt.type,
                identifier=identifier
            ))
        
        sigs_b = []
        for i, stmt in enumerate(statements_b):
            code = source_b[stmt.start_byte:stmt.end_byte].decode('utf-8', errors='replace')
            identifier = self._extract_statement_identifier(stmt, source_b) or self._extract_identifier(stmt, source_b) or f"anonymous_{stmt.type}"
            
            sigs_b.append(StatementSignature(
                content_hash=hash_content(code),
                code=code.strip(),
                start_line=stmt.start_point[0] + 1,
                end_line=stmt.end_point[0] + 1,
                index=i,
                node_type=stmt.type,
                identifier=identifier
            ))
        
        # Use existing statement comparison logic
        return self._compare_statement_lists(sigs_a, sigs_b, None, None, source_a, source_b)
    
    def _get_block_statements_from_node(self, block_node) -> List:
        """Get statements from a block node."""
        if block_node.type in {"block", "statement_block", "closure"}:
            return list(block_node.named_children)
        else:
            # Single statement
            return [block_node]
    
    def _extract_container_children(self, container_node, source: bytes) -> List[StatementSignature]:
        """
        GENERIC child extraction that works for ANY container type.
        
        This extracts direct children from:
        - Classes → methods, fields, comments
        - Functions → statements, nested blocks
        - If/else → condition, then/else blocks  
        - Loops → condition, body
        - Try/catch → try block, catch blocks, finally
        - Any other container
        """
        children = []
        
        # For classes, look inside the class body
        if container_node.type in GROOVY_CLASS_TYPES:
            body_node = self._find_class_body(container_node)
            if body_node:
                container_node = body_node
        
        # For functions, extract both signature components AND body statements
        elif container_node.type in GROOVY_FUNCTION_TYPES:
            # First, extract function signature components (return type, parameters)
            for child in container_node.named_children:
                if child.type in {'builtintype', 'parameter_list'}:
                    code = source[child.start_byte:child.end_byte].decode('utf-8', errors='replace')
                    
                    # Create meaningful identifiers for signature components
                    if child.type == 'builtintype':
                        identifier = f"return_type_{code.strip()}"
                    elif child.type == 'parameter_list':
                        identifier = f"parameters_{hash_content(code)[:8]}"
                    else:
                        identifier = f"signature_{child.type}"
                    
                    child_sig = StatementSignature(
                        content_hash=hash_content(code),
                        code=code.strip(),
                        start_line=child.start_point[0] + 1,
                        end_line=child.end_point[0] + 1,
                        index=len(children),
                        node_type=child.type,
                        identifier=identifier
                    )
                    children.append(child_sig)
            
            # Then, extract function body statements
            body_node = self._find_function_body(container_node)
            if body_node:
                container_node = body_node
            else:
                # If no body found, we've already extracted signature components, so return
                return children
        
        # Special handling for control flow containers (for, while, do-while, if, etc.)
        if container_node.type in {"for_loop", "for_in_loop", "while_loop", "do_while_loop", "if_statement"}:
            # For control flow, extract the body statements, not the structural wrapper
            body_node = None
            
            # Find the body node
            if container_node.type in {"for_loop", "for_in_loop", "while_loop", "do_while_loop"}:
                body_node = container_node.child_by_field_name("body")
            elif container_node.type == "if_statement":
                # For if statements, get the body clause
                body_node = container_node.child_by_field_name("body")
            
            if body_node and body_node.type == "closure":
                # Extract statements from within the closure
                # Use improved statement extraction for control flow containers
                # This ensures proper detection of added/modified/deleted statements within loops/conditionals
                
                # Check if this closure has multiple distinct statements that should be analyzed separately
                has_multiple_statements = len(body_node.named_children) > 1
                has_control_flow_children = any(child.type in {"if_statement", "for_loop", "while_loop", "do_while_loop", "try_statement"} 
                                              for child in body_node.named_children)
                
                # Use individual statement extraction if we have multiple statements or nested control flow
                if has_multiple_statements or has_control_flow_children:
                    # WORKAROUND: Check for grammar parsing bug where count++\nprintln gets parsed as malformed juxt_function_call
                    fixed_children = self._fix_malformed_closure_statements(body_node, source)
                    
                    for i, child in enumerate(fixed_children):
                        if isinstance(child, tuple):  # Fixed statement tuple (node, code, node_type)
                            node, code, node_type = child
                            identifier = self._extract_statement_identifier(node, source) or self._extract_identifier(node, source) or f"anonymous_{node_type}"
                            
                            child_sig = StatementSignature(
                                content_hash=hash_content(code),
                                code=code.strip(),
                                start_line=node.start_point[0] + 1,
                                end_line=node.end_point[0] + 1,
                                index=len(children),
                                node_type=node_type,
                                identifier=identifier
                            )
                        else:  # Regular node
                            code = source[child.start_byte:child.end_byte].decode('utf-8', errors='replace')
                            identifier = self._extract_statement_identifier(child, source) or self._extract_identifier(child, source) or f"anonymous_{child.type}"
                            
                            child_sig = StatementSignature(
                                content_hash=hash_content(code),
                                code=code.strip(),
                                start_line=child.start_point[0] + 1,
                                end_line=child.end_point[0] + 1,
                                index=len(children),
                                node_type=child.type,
                                identifier=identifier
                            )
                        children.append(child_sig)
                else:
                    # For simple single-statement closures, use the original grouping logic
                    # This preserves backward compatibility for simple cases
                    child = body_node.named_children[0]
                    code = source[child.start_byte:child.end_byte].decode('utf-8', errors='replace')
                    identifier = self._extract_statement_identifier(child, source) or self._extract_identifier(child, source) or f"anonymous_{child.type}"
                    
                    child_sig = StatementSignature(
                        content_hash=hash_content(code),
                        code=code.strip(),
                        start_line=child.start_point[0] + 1,
                        end_line=child.end_point[0] + 1,
                        index=len(children),
                        node_type=child.type,
                        identifier=identifier
                    )
                    children.append(child_sig)
                        
                return children
        
        # Extract all direct children (statements, comments, nested containers)
        for i, child in enumerate(container_node.named_children):
            # Skip identifier nodes (function names are already captured at the function level)
            if child.type in {'identifier'}:
                continue
                
            # Skip structural closures - they should be handled by the special logic above
            if child.type == "closure" and container_node.type in {"for_loop", "for_in_loop", "while_loop", "do_while_loop", "if_statement"}:
                continue
            
            # WORKAROUND: Apply malformed statement fix for closures processed directly
            if child.type == "closure":
                # Check if this closure has the malformed parsing pattern
                fixed_closure_children = self._fix_malformed_closure_statements(child, source)
                
                # If the workaround was applied, process the fixed children
                if len(fixed_closure_children) > 0 and isinstance(fixed_closure_children[0], tuple):
                    for j, fixed_child in enumerate(fixed_closure_children):
                        node, code, node_type = fixed_child
                        identifier = self._extract_statement_identifier(node, source) or self._extract_identifier(node, source) or f"anonymous_{node_type}"
                        
                        child_sig = StatementSignature(
                            content_hash=hash_content(code),
                            code=code.strip(),
                            start_line=node.start_point[0] + 1,
                            end_line=node.end_point[0] + 1,
                            index=len(children),
                            node_type=node_type,
                            identifier=identifier
                        )
                        children.append(child_sig)
                    continue  # Skip the normal processing for this closure
                
            code = source[child.start_byte:child.end_byte].decode('utf-8', errors='replace')
            
            # For comments, use content hash as identifier
            if child.type in {"comment", "line_comment", "block_comment"}:
                identifier = hash_content(code)
            else:
                identifier = self._extract_identifier(child, source) or f"anonymous_{child.type}"
            
            child_sig = StatementSignature(
                content_hash=hash_content(code),
                code=code.strip(),
                start_line=child.start_point[0] + 1,
                end_line=child.end_point[0] + 1,
                index=len(children),
                node_type=child.type,
                identifier=identifier
            )
            children.append(child_sig)
        
        return children
    
    def _is_container_node(self, node_type: str) -> bool:
        """Check if a node type represents a container that can have children."""
        # Never treat closures as containers in the main AST diff logic
        # They are handled contextually in the recursive parser
        if node_type in {"closure", "closure_expression"}:
            return False
        return node_type in GROOVY_CONTAINER_TYPES or node_type in GROOVY_FUNCTION_TYPES or node_type in GROOVY_CLASS_TYPES
    
    def _find_class_body(self, class_node):
        """Find the body of a class (usually a closure or block)."""
        for child in class_node.named_children:
            if child.type in {'class_body', 'block', 'closure'}:
                return child
        return None
    
    def _find_function_body(self, function_node):
        """Find the body of a function (usually a closure or block)."""
        for child in function_node.named_children:
            if child.type in {'closure', 'block', 'statement_block'}:
                return child
        return None
    
    def _find_child_node_by_line(self, parent_node, target_line: int):
        """Find a child node that starts at the target line."""
        for child in parent_node.named_children:
            if child.start_point[0] + 1 == target_line:
                return child
        return None
    
    def _extract_and_compare_class_members(self, class_node_a, class_node_b, source_a: bytes, source_b: bytes) -> List[StatementDiff]:
        """Fallback: Extract and compare class members using the old logic."""
        try:
            members_a = self._extract_class_member_signatures(class_node_a, source_a)
            members_b = self._extract_class_member_signatures(class_node_b, source_b)
            return self._compare_member_lists(members_a, members_b)
        except Exception as e:
            print(f"Warning: Class member comparison fallback failed: {e}")
            return []
    
    def _extract_and_compare_function_statements(self, func_node_a, func_node_b, source_a: bytes, source_b: bytes) -> List[StatementDiff]:
        """Fallback: Extract and compare function statements using the old logic."""
        try:
            return self._compare_function_statements_direct(func_node_a, func_node_b, source_a, source_b)
        except Exception as e:
            print(f"Warning: Function statement comparison fallback failed: {e}")
            return []
    
    def _find_node_at_line(self, root_node, target_line: int):
        """Find the AST node that starts at the target line."""
        def search_node(node):
            # Check if this node starts at the target line
            if node.start_point[0] + 1 == target_line:
                # Prefer more specific nodes over generic ones
                if node.type in {'source_file', 'program'}:
                    # Look for a more specific child at the same line
                    for child in node.named_children:
                        if child.start_point[0] + 1 == target_line:
                            return child
                return node
            
            # Recursively search children
            for child in node.named_children:
                result = search_node(child)
                if result:
                    return result
            return None
        
        return search_node(root_node)
    
    def _compare_class_members(
        self,
        class_a: BlockSignature,
        class_b: BlockSignature,
        source_a: bytes,
        source_b: bytes
    ) -> List[StatementDiff]:
        """
        Compare members (methods, fields) within two class blocks.
        
        This provides detailed analysis of what changed inside a class.
        """
        try:
            # Parse both files to get AST
            tree_a = self.parser.parse(source_a)
            tree_b = self.parser.parse(source_b)
            
            # Find the class nodes
            class_node_a = self._find_node_at_line(tree_a.root_node, class_a.start_line)
            class_node_b = self._find_node_at_line(tree_b.root_node, class_b.start_line)
            
            if not class_node_a or not class_node_b:
                return self._simple_block_comparison(class_a, class_b)
            
            # Extract members from both classes
            members_a = self._extract_class_member_signatures(class_node_a, source_a)
            members_b = self._extract_class_member_signatures(class_node_b, source_b)
            
            # Compare members using similar logic to block comparison
            return self._compare_member_lists(members_a, members_b)
            
        except Exception as e:
            print(f"Warning: Class member comparison failed for {class_a.identifier}: {e}")
            return self._simple_block_comparison(class_a, class_b)
    
    def _extract_class_member_signatures(self, class_node, source: bytes) -> List[StatementSignature]:
        """Extract method and field signatures from a class node."""
        members = []
        
        for child in class_node.named_children:
            # Skip non-member nodes (like class name, extends clause, etc.)
            if child.type not in GROOVY_FUNCTION_TYPES and child.type not in GROOVY_FIELD_TYPES:
                # Check if it's a class body that contains the actual members
                if child.type in {'class_body', 'block', 'closure'}:
                    for member_child in child.named_children:
                        # Extract members (functions, fields) and comments
                        if (member_child.type in GROOVY_FUNCTION_TYPES or 
                            member_child.type in GROOVY_FIELD_TYPES or
                            member_child.type in {"comment", "line_comment", "block_comment"}):
                            
                            # Extract member info
                            code = source[member_child.start_byte:member_child.end_byte].decode('utf-8', errors='replace')
                            
                            # For comments, use content hash as identifier (like top-level comments)
                            if member_child.type in {"comment", "line_comment", "block_comment"}:
                                identifier = hash_content(code)
                            else:
                                identifier = self._extract_identifier(member_child, source) or f"anonymous_{member_child.type}"
                            
                            content_hash = hash_content(code)
                            
                            member = StatementSignature(
                                content_hash=content_hash,
                                code=code.strip(),
                                start_line=member_child.start_point[0] + 1,
                                end_line=member_child.end_point[0] + 1,
                                index=len(members),
                                node_type=member_child.type
                            )
                            members.append(member)
                continue
            
            # Extract member info for direct children
            code = source[child.start_byte:child.end_byte].decode('utf-8', errors='replace')
            identifier = self._extract_identifier(child, source) or f"anonymous_{child.type}"
            content_hash = hash_content(code)
            
            member = StatementSignature(
                content_hash=content_hash,
                code=code.strip(),
                start_line=child.start_point[0] + 1,
                end_line=child.end_point[0] + 1,
                index=len(members),
                node_type=child.type
            )
            members.append(member)
        
        return members
    
    def _compare_member_lists(self, members_a: List[StatementSignature], members_b: List[StatementSignature]) -> List[StatementDiff]:
        """Compare two lists of class members and generate statement diffs."""
        diffs = []
        matched_a = set()
        matched_b = set()
        
        # Phase 1: Match by content hash (unchanged/moved)
        for i, member_a in enumerate(members_a):
            for j, member_b in enumerate(members_b):
                if (i not in matched_a and j not in matched_b and 
                    member_a.content_hash == member_b.content_hash):
                    
                    change_type = StatementChangeType.UNCHANGED if i == j else StatementChangeType.MOVED
                    
                    diff = StatementDiff(
                        change_type=change_type,
                        code=member_b.code,
                        node_type=member_b.node_type,
                        file_a_line=member_a.start_line,
                        file_a_index=i,
                        file_b_line=member_b.start_line,
                        file_b_index=j,
                        description=f"Statement {change_type.value}: {member_b.node_type}",
                        is_container=member_b.node_type in GROOVY_FUNCTION_TYPES
                    )
                    diffs.append(diff)
                    matched_a.add(i)
                    matched_b.add(j)
                    break
        
        # Phase 2: Match by similarity (modified)
        for i, member_a in enumerate(members_a):
            if i in matched_a:
                continue
            
            best_match = None
            best_similarity = 0.0
            best_j = -1
            
            for j, member_b in enumerate(members_b):
                if j in matched_b or member_a.node_type != member_b.node_type:
                    continue
                
                similarity = calculate_similarity(member_a.code, member_b.code)
                # Use higher threshold for comments (70%) as they need stronger similarity
                threshold = 0.7 if member_a.node_type in {"comment", "line_comment", "block_comment"} else 0.6
                if similarity > best_similarity and similarity >= threshold:
                    best_similarity = similarity
                    best_match = member_b
                    best_j = j
            
            if best_match:
                is_container = best_match.node_type in GROOVY_FUNCTION_TYPES
                
                # Determine if it's moved and modified or just modified
                if i != best_j:
                    change_type = StatementChangeType.MOVED_MODIFIED
                    description = f"Statement moved and modified: {best_match.node_type} from position {i+1} to {best_j+1} ({best_similarity:.1%} similar)"
                else:
                    change_type = StatementChangeType.MODIFIED
                    description = f"Statement modified: {best_match.node_type} ({best_similarity:.1%} similar)"
                
                diff = StatementDiff(
                    change_type=change_type,
                    code=best_match.code,
                    node_type=best_match.node_type,
                    file_a_line=member_a.start_line,
                    file_a_index=i,
                    file_b_line=best_match.start_line,
                    file_b_index=best_j,
                    old_code=member_a.code,
                    similarity_score=best_similarity,
                    description=description,
                    is_container=is_container
                )
                
                # For container statements, populate child_diffs using recursive parser
                # Don't generate child diffs for closures at all - they cause infinite recursion
                # The for loop, if, while etc. analysis should happen at the statement level
                if is_container and best_match.node_type not in {"closure", "closure_expression"}:
                    try:
                        child_diffs = self._generate_child_diffs_for_members(member_a, best_match)
                        diff.child_diffs = child_diffs
                    except Exception as e:
                        print(f"Warning: Failed to generate child diffs for {best_match.node_type}: {e}")
                        diff.child_diffs = []
                else:
                    diff.child_diffs = []
                
                diffs.append(diff)
                matched_a.add(i)
                matched_b.add(best_j)
        
        # Phase 3: Remaining unmatched (added/deleted)
        for i, member_a in enumerate(members_a):
            if i not in matched_a:
                diff = StatementDiff(
                    change_type=StatementChangeType.DELETED,
                    code=member_a.code,
                    node_type=member_a.node_type,
                    file_a_line=member_a.start_line,
                    file_a_index=i,
                    description=f"Statement deleted: {member_a.node_type}",
                    is_container=member_a.node_type in GROOVY_FUNCTION_TYPES
                )
                diffs.append(diff)
        
        for j, member_b in enumerate(members_b):
            if j not in matched_b:
                diff = StatementDiff(
                    change_type=StatementChangeType.ADDED,
                    code=member_b.code,
                    node_type=member_b.node_type,
                    file_b_line=member_b.start_line,
                    file_b_index=j,
                    description=f"Statement added: {member_b.node_type}",
                    is_container=member_b.node_type in GROOVY_FUNCTION_TYPES
                )
                diffs.append(diff)
        
        return diffs
    
    def _generate_child_diffs_for_members(self, member_a: StatementSignature, member_b: StatementSignature) -> List[StatementDiff]:
        """
        Generate child diffs for container statements (like function_definition) using recursive parser.
        
        This analyzes what changed inside the container (method parameters, body statements, etc.)
        """
        try:
            # Special handling for switch_block nodes - they can't be parsed in isolation
            if member_a.node_type == "switch_block" or member_b.node_type == "switch_block":
                # Switch blocks contain case statements that should be compared as pure statements
                # Don't try to parse them in isolation as they're not valid standalone Groovy
                return []
            
            # Parse both member codes as individual units
            member_a_code = member_a.code.encode('utf-8')
            member_b_code = member_b.code.encode('utf-8')
            
            tree_a = self.parser.parse(member_a_code)
            tree_b = self.parser.parse(member_b_code)
            
            if not tree_a.root_node.children or not tree_b.root_node.children:
                return []
            
            # Get the function/container nodes
            node_a = tree_a.root_node.children[0]
            node_b = tree_b.root_node.children[0]
            
            # For function definitions, we want to compare the children (body contents), not the function itself
            if node_a.type in GROOVY_FUNCTION_TYPES and node_b.type in GROOVY_FUNCTION_TYPES:
                return self._compare_function_body_contents(node_a, node_b, member_a_code, member_b_code, member_a, member_b)
            
            # For other containers, use the recursive parser
            sig_a = self.recursive_parser.parse_recursive(node_a, member_a_code)
            sig_b = self.recursive_parser.parse_recursive(node_b, member_b_code)
            
            # Get the children and compare them directly
            if sig_a.children and sig_b.children:
                return self.recursive_parser._compare_container_children(sig_a, sig_b)
            
            return []
            
        except Exception as e:
            print(f"Warning: Failed to generate child diffs: {e}")
            return []
    
    def _compare_function_body_contents(self, node_a, node_b, source_a: bytes, source_b: bytes, member_a: StatementSignature, member_b: StatementSignature) -> List[StatementDiff]:
        """
        Compare the contents of two function bodies to generate child diffs.
        
        This extracts the body statements and compares them directly.
        """
        try:
            # Extract function body children
            children_a = self._extract_function_body_children(node_a)
            children_b = self._extract_function_body_children(node_b)
            
            if not children_a and not children_b:
                return []
            
            # Create signatures for the body children
            sigs_a = []
            for child in children_a:
                sig = self.recursive_parser.parse_recursive(child, source_a)
                # Adjust line numbers to be relative to the original file
                sig.start_line = member_a.start_line + (sig.start_line - 1)
                sig.end_line = member_a.start_line + (sig.end_line - 1)
                sigs_a.append(sig)
            
            sigs_b = []
            for child in children_b:
                sig = self.recursive_parser.parse_recursive(child, source_b)
                # Adjust line numbers to be relative to the original file
                sig.start_line = member_b.start_line + (sig.start_line - 1)
                sig.end_line = member_b.start_line + (sig.end_line - 1)
                sigs_b.append(sig)
            
            # Compare the body children using the container comparison logic
            return self._compare_function_body_signatures(sigs_a, sigs_b)
            
        except Exception as e:
            print(f"Warning: Failed to compare function body contents: {e}")
            return []
    
    def _extract_function_body_children(self, function_node):
        """Extract the children from a function's body."""
        children = []
        
        # Look for the function body (usually a closure or block)
        for child in function_node.named_children:
            if child.type in {'closure', 'block', 'statement_block'}:
                # Return the children of the body
                return child.named_children
        
        return children
    
    def _compare_function_body_signatures(self, sigs_a: List, sigs_b: List) -> List[StatementDiff]:
        """Compare function body signatures using multi-phase matching."""
        diffs = []
        matched_a = set()
        matched_b = set()
        
        # Phase 1: Match by content hash (exact matches)
        for i, sig_a in enumerate(sigs_a):
            for j, sig_b in enumerate(sigs_b):
                if (i not in matched_a and j not in matched_b and
                    sig_a.content_hash == sig_b.content_hash):
                    
                    # Recursively compare matched children
                    child_diffs = self.recursive_parser.compare_recursive_statements(sig_a, sig_b)
                    diffs.extend(child_diffs)
                    
                    matched_a.add(i)
                    matched_b.add(j)
                    break
        
        # Phase 2: Match by similarity (for modified statements)
        for i, sig_a in enumerate(sigs_a):
            if i in matched_a:
                continue
            
            for j, sig_b in enumerate(sigs_b):
                if j in matched_b:
                    continue
                
                if (sig_a.node_type == sig_b.node_type and
                    self.recursive_parser._calculate_similarity(sig_a.code, sig_b.code) >= 0.6):
                    
                    # Recursively compare similar children
                    child_diffs = self.recursive_parser.compare_recursive_statements(sig_a, sig_b)
                    diffs.extend(child_diffs)
                    
                    matched_a.add(i)
                    matched_b.add(j)
                    break
        
        # Phase 3: Remaining unmatched children (added/deleted)
        for i, sig_a in enumerate(sigs_a):
            if i not in matched_a:
                diff = StatementDiff(
                    change_type=StatementChangeType.DELETED,
                    code=sig_a.code,
                    node_type=sig_a.node_type,
                    file_a_line=sig_a.start_line,
                    description=f"Deleted {sig_a.node_type}",
                    is_container=sig_a.is_container
                )
                diffs.append(diff)
        
        for j, sig_b in enumerate(sigs_b):
            if j not in matched_b:
                diff = StatementDiff(
                    change_type=StatementChangeType.ADDED,
                    code=sig_b.code,
                    node_type=sig_b.node_type,
                    file_b_line=sig_b.start_line,
                    description=f"Added {sig_b.node_type}",
                    is_container=sig_b.is_container
                )
                diffs.append(diff)
        
        return diffs
    
    def _compare_function_statements_direct(self, node_a: Node, node_b: Node, source_a: bytes, source_b: bytes) -> List[StatementDiff]:
        """Compare individual statements within function bodies directly."""
        # Extract function body statements
        statements_a = self._extract_function_body_statements(node_a, source_a)
        statements_b = self._extract_function_body_statements(node_b, source_b)
        
        return self._compare_statement_lists(statements_a, statements_b, node_a, node_b, source_a, source_b)
    
    def _extract_function_body_statements(self, function_node: Node, source: bytes) -> List[StatementSignature]:
        """Extract individual statements from a function body."""
        statements = []
        
        # Find the function body (closure)
        body_node = None
        for child in function_node.named_children:
            if child.type == "closure":
                body_node = child
                break
        
        if not body_node:
            return statements
        
        # Extract individual statements from the body
        for i, child in enumerate(body_node.named_children):
            if child.type in {"comment", "line_comment", "block_comment"}:
                # Handle comments
                code = source[child.start_byte:child.end_byte].decode('utf-8', errors='replace')
                statements.append(StatementSignature(
                    node_type="comment",
                    code=code,
                    start_line=child.start_point[0] + 1,
                    end_line=child.end_point[0] + 1,
                    content_hash=hash_content(code),
                    index=i
                ))
            else:
                # Handle all other statement types
                code = source[child.start_byte:child.end_byte].decode('utf-8', errors='replace')
                statements.append(StatementSignature(
                    node_type=child.type,
                    code=code,
                    start_line=child.start_point[0] + 1,
                    end_line=child.end_point[0] + 1,
                    content_hash=hash_content(code),
                    index=i
                ))
        
        return statements
    
    def _extract_statement_identifier(self, node: Node, source: bytes) -> Optional[str]:
        """Extract identifier from a statement node."""
        if node.type == "return_statement":
            return "return"
        elif node.type in {"comment", "line_comment", "block_comment"}:
            # Use first few words of comment as identifier
            code = source[node.start_byte:node.end_byte].decode('utf-8', errors='replace')
            words = code.replace("//", "").replace("/*", "").replace("*/", "").strip().split()[:3]
            return "_".join(words) if words else "comment"
        elif node.type == "if_statement":
            return "if"
        elif node.type == "expression_statement":
            # Try to extract variable name or method call
            for child in node.named_children:
                if child.type == "assignment":
                    # Look for left side of assignment
                    for grandchild in child.named_children:
                        if grandchild.type == "identifier":
                            return source[grandchild.start_byte:grandchild.end_byte].decode('utf-8', errors='replace')
                elif child.type in {"method_invocation", "juxt_function_call"}:
                    # Look for method name (handle both method_invocation and juxt_function_call like println)
                    for grandchild in child.named_children:
                        if grandchild.type == "identifier":
                            return source[grandchild.start_byte:grandchild.end_byte].decode('utf-8', errors='replace')
                    # For juxt_function_call, the first child might be the function name directly
                    if child.type == "juxt_function_call" and child.named_children:
                        first_child = child.named_children[0]
                        if first_child.type == "identifier":
                            return source[first_child.start_byte:first_child.end_byte].decode('utf-8', errors='replace')
                # Handle direct function calls (like println) that might not be wrapped in method_invocation
                elif child.type == "identifier":
                    return source[child.start_byte:child.end_byte].decode('utf-8', errors='replace')
        
        return None
    
    def _are_statements_potentially_similar(self, stmt_a: StatementSignature, stmt_b: StatementSignature) -> bool:
        """
        Check if two statements are potentially similar enough to warrant similarity calculation.
        
        This prevents calculating similarity for completely different statements like:
        - println "[ERROR]" vs System.out.println "[WARNING]"
        - Different method calls, different purposes
        """
        # Must be same node type
        if stmt_a.node_type != stmt_b.node_type:
            return False
        
        # For function calls, check if they use similar method names
        if stmt_a.node_type in {"juxt_function_call", "method_invocation", "function_call"}:
            # Extract method names for comparison
            method_a = self._extract_method_name(stmt_a.code)
            method_b = self._extract_method_name(stmt_b.code)
            
            if method_a and method_b:
                # Different method families are not similar
                # println vs System.out.println = different families
                # println vs print = similar family
                if not self._are_method_names_similar(method_a, method_b):
                    return False
        
        # For return statements, they're usually similar
        if stmt_a.node_type == "return":
            return True
            
        # For assignments, check if they assign to similar variables
        if stmt_a.node_type == "assignment":
            return True  # Most assignments are worth comparing
            
        # For comments, they're usually different
        if stmt_a.node_type == "comment":
            return False  # Comments are usually completely different
            
        # Default: allow similarity calculation
        return True
    
    def _extract_method_name(self, code: str) -> str:
        """Extract the main method name from a function call."""
        code = code.strip()
        
        # Handle System.out.println, System.err.println
        if "System.out." in code:
            return "System.out"
        if "System.err." in code:
            return "System.err"
        
        # Handle simple method calls like println, print
        if code.startswith("println"):
            return "println"
        if code.startswith("print"):
            return "print"
            
        # Extract first word as method name
        words = code.split()
        return words[0] if words else ""
    
    def _are_method_names_similar(self, method_a: str, method_b: str) -> bool:
        """Check if two method names are from similar families."""
        # Same method = similar
        if method_a == method_b:
            return True
            
        # println family
        println_family = {"println", "print"}
        if method_a in println_family and method_b in println_family:
            return True
            
        # System output family  
        system_family = {"System.out", "System.err"}
        if method_a in system_family and method_b in system_family:
            return True
            
        # Different families
        return False
    
    def _compare_statement_lists(self, statements_a: List[StatementSignature], statements_b: List[StatementSignature], 
                                container_node_a: Node = None, container_node_b: Node = None, 
                                source_a: bytes = None, source_b: bytes = None) -> List[StatementDiff]:
        """Compare two lists of statements and return diffs with relative positioning analysis."""
        diffs = []
        matched_a = set()
        matched_b = set()
        
        # Phase 1: Exact matches by content hash (unchanged statements)
        for i, stmt_a in enumerate(statements_a):
            for j, stmt_b in enumerate(statements_b):
                if (i not in matched_a and j not in matched_b and
                    stmt_a.content_hash == stmt_b.content_hash and
                    stmt_a.node_type == stmt_b.node_type):
                    
                    # Check if position changed within the block (moved)
                    if i != j:
                        diffs.append(StatementDiff(
                            change_type=StatementChangeType.MOVED,
                            code=stmt_b.code,
                            node_type=stmt_b.node_type,
                            file_a_line=stmt_a.start_line,
                            file_a_index=i,
                            file_b_line=stmt_b.start_line,
                            file_b_index=j,
                            description=f"Statement moved: {stmt_b.node_type} from position {i+1} to {j+1}",
                            is_container=False
                        ))
                    # If i == j, it's unchanged, so we don't add it to diffs
                    
                    matched_a.add(i)
                    matched_b.add(j)
                    break
        
        # Phase 1.5: Match by identifier OR by semantic role (for function signatures)
        for i, stmt_a in enumerate(statements_a):
            if i in matched_a:
                continue
            for j, stmt_b in enumerate(statements_b):
                if j not in matched_b and stmt_a.node_type == stmt_b.node_type:
                    
                    # Check for identifier match (normal case)
                    identifier_match = (stmt_a.identifier and stmt_b.identifier and 
                                      stmt_a.identifier == stmt_b.identifier)
                    
                    # Check for semantic role match (function signature components)
                    semantic_match = (stmt_a.node_type in {'builtintype', 'parameter_list'} and
                                    stmt_b.node_type in {'builtintype', 'parameter_list'} and
                                    stmt_a.node_type == stmt_b.node_type)
                    
                    if identifier_match or semantic_match:
                        
                        similarity = calculate_similarity(stmt_a.code, stmt_b.code)
                        
                        # Determine if it's moved and modified or just modified
                        if i != j:
                            change_type = StatementChangeType.MOVED_MODIFIED
                            description = f"Statement moved and modified: {stmt_b.node_type} '{stmt_b.identifier}' from position {i+1} to {j+1} ({similarity:.1%} similar)"
                        else:
                            change_type = StatementChangeType.MODIFIED
                            description = f"Statement modified: {stmt_b.node_type} '{stmt_b.identifier}' ({similarity:.1%} similar)"
                        
                        is_container = stmt_b.node_type in GROOVY_CONTAINER_TYPES
                        
                        diff = StatementDiff(
                            change_type=change_type,
                            code=stmt_b.code,
                            node_type=stmt_b.node_type,
                            file_a_line=stmt_a.start_line,
                            file_a_index=i,
                            file_b_line=stmt_b.start_line,
                            file_b_index=j,
                            old_code=stmt_a.code,
                            similarity_score=similarity,
                            description=description,
                            is_container=is_container
                        )
                        
                        # For container statements, generate child diffs recursively
                        # Skip closures to avoid infinite recursion
                        if is_container and stmt_b.node_type not in {"closure", "closure_expression"} and container_node_a and container_node_b and source_a and source_b:
                            try:
                                # Find the actual AST nodes for these statements
                                node_a = self._find_node_at_line(container_node_a, stmt_a.start_line)
                                node_b = self._find_node_at_line(container_node_b, stmt_b.start_line)
                                
                                if node_a and node_b:
                                    # Special handling for if_statement nodes - use branch-aware analysis
                                    if node_a.type == "if_statement" and node_b.type == "if_statement":
                                        diff.child_diffs = self._compare_if_statement_branches(node_a, node_b, source_a, source_b)
                                    # Special handling for switch_statement nodes - extract individual case statements
                                    elif node_a.type == "switch_statement" and node_b.type == "switch_statement":
                                        diff.child_diffs = self._compare_switch_statement_cases(node_a, node_b, source_a, source_b)
                                    # Special handling for switch_block nodes - extract individual case statements
                                    elif node_a.type == "switch_block" and node_b.type == "switch_block":
                                        diff.child_diffs = self._compare_switch_block_cases(node_a, node_b, source_a, source_b)
                                    else:
                                        # Extract children from both nodes and compare recursively
                                        children_a = self._extract_container_children(node_a, source_a)
                                        children_b = self._extract_container_children(node_b, source_b)
                                        
                                        # Recursively compare the children
                                        diff.child_diffs = self._compare_statement_lists(
                                            children_a, children_b, node_a, node_b, source_a, source_b
                                        )
                                else:
                                    diff.child_diffs = []
                            except Exception as e:
                                print(f"Warning: Failed to generate child diffs for {stmt_b.node_type}: {e}")
                                diff.child_diffs = []
                        else:
                            diff.child_diffs = []
                        
                        diffs.append(diff)
                        matched_a.add(i)
                        matched_b.add(j)
                        break
        
        # Phase 2: Similar statements (modified or moved_modified)
        for i, stmt_a in enumerate(statements_a):
            if i in matched_a:
                continue
            
            best_match = None
            best_similarity = 0.0
            best_j = -1
            
            for j, stmt_b in enumerate(statements_b):
                if j in matched_b or stmt_a.node_type != stmt_b.node_type:
                    continue
                
                # Only calculate similarity for potentially similar statements
                # Quick pre-filter: check if they have similar structure
                if self._are_statements_potentially_similar(stmt_a, stmt_b):
                    similarity = calculate_similarity(stmt_a.code, stmt_b.code)
                    if similarity > best_similarity and similarity >= 0.5:  # 50% threshold as requested
                        best_similarity = similarity
                        best_match = stmt_b
                        best_j = j
            
            if best_match:
                # Determine if it's moved and modified or just modified
                if i != best_j:
                    change_type = StatementChangeType.MOVED_MODIFIED
                    description = f"Statement moved and modified: {best_match.node_type} from position {i+1} to {best_j+1} ({best_similarity:.1%} similar)"
                else:
                    change_type = StatementChangeType.MODIFIED
                    description = f"Statement modified: {best_match.node_type} ({best_similarity:.1%} similar)"
                
                diffs.append(StatementDiff(
                    change_type=change_type,
                    code=best_match.code,
                    node_type=best_match.node_type,
                    file_a_line=stmt_a.start_line,
                    file_a_index=i,
                    file_b_line=best_match.start_line,
                    file_b_index=best_j,
                    old_code=stmt_a.code,
                    similarity_score=best_similarity,
                    description=description,
                    is_container=False
                ))
                matched_a.add(i)
                matched_b.add(best_j)
        
        # Phase 3: Deleted statements (with specific line ranges)
        for i, stmt_a in enumerate(statements_a):
            if i not in matched_a:
                diffs.append(StatementDiff(
                    change_type=StatementChangeType.DELETED,
                    code=stmt_a.code,
                    node_type=stmt_a.node_type,
                    file_a_line=stmt_a.start_line,
                    file_a_index=i,
                    description=f"Statement deleted: {stmt_a.node_type} (lines {stmt_a.start_line}-{stmt_a.end_line})",
                    is_container=False
                ))
        
        # Phase 4: Added statements (with specific line ranges)
        for j, stmt_b in enumerate(statements_b):
            if j not in matched_b:
                diffs.append(StatementDiff(
                    change_type=StatementChangeType.ADDED,
                    code=stmt_b.code,
                    node_type=stmt_b.node_type,
                    file_b_line=stmt_b.start_line,
                    file_b_index=j,
                    description=f"Statement added: {stmt_b.node_type} (lines {stmt_b.start_line}-{stmt_b.end_line})",
                    is_container=False
                ))
        
        return diffs
    
    def _simple_block_comparison(
        self, 
        block_a: BlockSignature, 
        block_b: BlockSignature
    ) -> List[StatementDiff]:
        """Fallback simple comparison when recursive parsing fails."""
        return [
            StatementDiff(
                change_type=StatementChangeType.MODIFIED,
                code=block_b.code[:100] + "..." if len(block_b.code) > 100 else block_b.code,
                node_type="block_content",
                file_a_line=block_a.start_line,
                file_b_line=block_b.start_line,
                description=f"Block content modified",
                old_code=block_a.code[:100] + "..." if len(block_a.code) > 100 else block_a.code,
                similarity_score=calculate_similarity(block_a.code, block_b.code)
            )
        ]
    
    def _count_nodes_in_blocks(self, blocks: List[BlockSignature]) -> int:
        """Count total nodes across all blocks."""
        return sum(block.children_count for block in blocks)
    
    def _get_detailed_parsing_errors(self, tree_a, tree_b, source_a: bytes, source_b: bytes, 
                                   file_a_path: str, file_b_path: str) -> str:
        """Extract detailed parsing error information from both trees."""
        errors = []
        
        def extract_errors_from_tree(tree, source: bytes, file_path: str, file_label: str):
            """Extract error details from a single tree."""
            file_errors = []
            source_lines = source.decode('utf-8').splitlines()
            
            def find_errors(node, depth=0):
                if node.type == 'ERROR':
                    start_line = node.start_point[0] + 1
                    start_col = node.start_point[1] + 1
                    end_line = node.end_point[0] + 1
                    end_col = node.end_point[1] + 1
                    
                    # Get the problematic text
                    error_text = node.text.decode('utf-8', errors='replace')[:100]
                    if len(error_text) > 100:
                        error_text += "..."
                    
                    # Get context (line where error occurs)
                    context_line = ""
                    if start_line <= len(source_lines):
                        context_line = source_lines[start_line - 1].strip()
                    
                    file_errors.append({
                        'line': start_line,
                        'column': start_col,
                        'error_text': error_text.replace('\n', '\\n'),
                        'context': context_line[:100] + ("..." if len(context_line) > 100 else "")
                    })
                
                for child in node.children:
                    find_errors(child, depth + 1)
            
            find_errors(tree.root_node)
            
            if file_errors:
                error_details = []
                for i, error in enumerate(file_errors[:5]):  # Limit to first 5 errors
                    error_details.append(
                        f"Line {error['line']}:{error['column']} - '{error['error_text']}' "
                        f"(context: '{error['context']}')"
                    )
                
                total_errors = len(file_errors)
                if total_errors > 5:
                    error_details.append(f"... and {total_errors - 5} more errors")
                
                errors.append(f"{file_label} ({file_path}): {'; '.join(error_details)}")
        
        # Check both trees for actual ERROR nodes
        def has_actual_errors(node):
            """Check for actual ERROR nodes in the AST."""
            if node.type == 'ERROR':
                return True
            return any(has_actual_errors(child) for child in node.children)
        
        if has_actual_errors(tree_a.root_node):
            extract_errors_from_tree(tree_a, source_a, file_a_path, "File A")
        
        if has_actual_errors(tree_b.root_node):
            extract_errors_from_tree(tree_b, source_b, file_b_path, "File B")
        
        return " | ".join(errors) if errors else None
    
    def _error_result(self, error_message: str) -> ComparisonResult:
        """Create an error result."""
        return ComparisonResult(
            is_identical=False,
            structural_similarity=0.0,
            total_blocks_a=0,
            total_blocks_b=0,
            blocks_added=0,
            blocks_deleted=0,
            blocks_modified=0,
            blocks_moved=0,
            blocks_moved_modified=0,
            blocks_unchanged=0,
            diffs=[],
            error=error_message
        )


def format_output(result: ComparisonResult) -> str:
    """Format the comparison result."""
    output = []
    
    # Header
    output.append("Groovy AST Diff Tool")
    output.append("=" * 60)
    output.append("")
    
    if result.error:
        output.append(f"ERROR: {result.error}")
        return "\n".join(output)
    
    # File info
    output.append("COMPARISON SUMMARY:")
    output.append("-" * 40)
    output.append(f"File A: {os.path.basename(result.file_a_path)} ({result.total_blocks_a} blocks)")
    output.append(f"File B: {os.path.basename(result.file_b_path)} ({result.total_blocks_b} blocks)")
    output.append(f"Structural Similarity: {result.structural_similarity:.1%}")
    output.append(f"Identical: {'Yes' if result.is_identical else 'No'}")
    output.append("")
    
    # Change statistics
    output.append("CHANGE STATISTICS:")
    output.append("-" * 40)
    output.append(f"  Added:         {result.blocks_added}")
    output.append(f"  Deleted:       {result.blocks_deleted}")
    output.append(f"  Modified:      {result.blocks_modified}")
    output.append(f"  Moved:         {result.blocks_moved}")
    output.append(f"  Moved-Modified: {result.blocks_moved_modified}")
    output.append(f"  Unchanged:     {result.blocks_unchanged}")
    output.append("")
    
    # Detailed changes
    if result.diffs:
        output.append("DETAILED CHANGES:")
        output.append("-" * 40)
        
        # Group by change type
        by_type = {}
        for diff in result.diffs:
            if diff.change_type not in by_type:
                by_type[diff.change_type] = []
            by_type[diff.change_type].append(diff)
        
        for change_type in [ChangeType.ADDED, ChangeType.DELETED, ChangeType.MODIFIED, ChangeType.MOVED, ChangeType.MOVED_MODIFIED]:
            if change_type in by_type:
                output.append(f"\n{change_type.value.upper()} ({len(by_type[change_type])}):")
                for diff in by_type[change_type][:10]:  # Show first 10
                    line_info = ""
                    if diff.file_a_start_line and diff.file_b_start_line:
                        line_info = f" (line {diff.file_a_start_line} → {diff.file_b_start_line})"
                    elif diff.file_a_start_line:
                        line_info = f" (line {diff.file_a_start_line})"
                    elif diff.file_b_start_line:
                        line_info = f" (line {diff.file_b_start_line})"
                    
                    modifiers_str = " ".join(diff.modifiers) + " " if diff.modifiers else ""
                    output.append(f"  • {modifiers_str}{diff.block_type.value} '{diff.identifier}'{line_info}")
                    if diff.similarity_score > 0:
                        output.append(f"    Similarity: {diff.similarity_score:.1f}%")
                    
                    # Show detailed statement diffs for modified blocks
                    if diff.statement_diffs:
                        output.append(f"    Statement changes: {len(diff.statement_diffs)}")
                        _format_statement_diffs(diff.statement_diffs, output, indent=6)
                
                if len(by_type[change_type]) > 10:
                    output.append(f"  ... and {len(by_type[change_type]) - 10} more")
    
    output.append("")
    output.append("=" * 60)
    
    return "\n".join(output)


def _format_statement_diffs(statement_diffs: List[StatementDiff], output: List[str], indent: int = 0):
    """Format statement-level diffs in hierarchical structure like JavaScript POC."""
    prefix = " " * indent
    
    for stmt_diff in statement_diffs[:5]:  # Show first 5 to avoid overwhelming output
        change_symbol = {
            StatementChangeType.ADDED: "➕",
            StatementChangeType.DELETED: "➖", 
            StatementChangeType.MODIFIED: "✏️",
            StatementChangeType.MOVED: "↔️",
            StatementChangeType.MOVED_MODIFIED: "↔️✏️",
            StatementChangeType.UNCHANGED: "✅"
        }.get(stmt_diff.change_type, "•")
        
        # Format the statement description
        code_preview = stmt_diff.code[:50] + "..." if len(stmt_diff.code) > 50 else stmt_diff.code
        code_preview = code_preview.replace('\n', ' ').strip()
        
        line_info = ""
        if stmt_diff.file_a_line and stmt_diff.file_b_line:
            line_info = f" (line {stmt_diff.file_a_line} → {stmt_diff.file_b_line})"
        elif stmt_diff.file_a_line:
            line_info = f" (line {stmt_diff.file_a_line})"
        elif stmt_diff.file_b_line:
            line_info = f" (line {stmt_diff.file_b_line})"
        
        output.append(f"{prefix}{change_symbol} {stmt_diff.change_type.value.upper()}: {stmt_diff.node_type}")
        output.append(f"{prefix}    └─ {code_preview}{line_info}")
        
        if stmt_diff.similarity_score is not None:
            output.append(f"{prefix}    └─ Similarity: {stmt_diff.similarity_score:.1%}")
        
        # Recursively show child diffs for containers
        if stmt_diff.child_diffs:
            output.append(f"{prefix}    └─ Nested changes:")
            _format_statement_diffs(stmt_diff.child_diffs, output, indent + 8)
    
    if len(statement_diffs) > 5:
        output.append(f"{prefix}... and {len(statement_diffs) - 5} more statement changes")


def _create_json_result(result: ComparisonResult) -> dict:
    """Create comprehensive JSON result matching JavaScript POC structure."""
    
    # Filter out unchanged blocks unless files are identical
    if result.is_identical:
        # For identical files, return minimal structure
        return {
            'is_identical': True,
            'structural_similarity': 1.0,
            'total_blocks_a': result.total_blocks_a,
            'total_blocks_b': result.total_blocks_b,
            'blocks_added': 0,
            'blocks_deleted': 0,
            'blocks_modified': 0,
            'blocks_moved': 0,
            'blocks_moved_modified': 0,
            'blocks_unchanged': result.blocks_unchanged,
            'file_a_path': result.file_a_path,
            'file_b_path': result.file_b_path,
            'diffs': [],
            'message': 'Files are identical - no differences found'
        }
    
    # Filter diffs to exclude unchanged blocks
    filtered_diffs = [diff for diff in result.diffs if diff.change_type != ChangeType.UNCHANGED]
    
    # Create comprehensive diff objects
    differences = []
    for diff in filtered_diffs:
        diff_obj = {
            'change_type': diff.change_type.value,
            'block_type': diff.block_type.value,
            'identifier': diff.identifier,
            'file_a_start_line': diff.file_a_start_line,
            'file_a_end_line': diff.file_a_end_line,
            'file_a_code': diff.file_a_code,
            'file_b_start_line': diff.file_b_start_line,
            'file_b_end_line': diff.file_b_end_line,
            'file_b_code': diff.file_b_code,
            'similarity_score': diff.similarity_score,
            'description': diff.description,
            'modifiers': diff.modifiers
        }
        
        # Add statement diffs if available
        if diff.statement_diffs:
            diff_obj['statement_diffs'] = _convert_statement_diffs_to_json(diff.statement_diffs, depth=0, visited=set())
        
        differences.append(diff_obj)
    
    # Calculate message
    change_count = len(filtered_diffs)
    if change_count == 0:
        message = "No significant changes detected"
    else:
        message = f"Detected {change_count} change(s). Similarity: {result.structural_similarity:.1%}"
    
    return {
        'is_identical': result.is_identical,
        'structural_similarity': result.structural_similarity,
        'total_blocks_a': result.total_blocks_a,
        'total_blocks_b': result.total_blocks_b,
        'blocks_added': result.blocks_added,
        'blocks_deleted': result.blocks_deleted,
        'blocks_modified': result.blocks_modified,
        'blocks_moved': result.blocks_moved,
        'blocks_moved_modified': result.blocks_moved_modified,
        'blocks_unchanged': result.blocks_unchanged,
        'file_a_path': result.file_a_path,
        'file_b_path': result.file_b_path,
        'differences': differences,
        'message': message
    }


def _convert_statement_diffs_to_json(statement_diffs: List[StatementDiff], depth: int = 0, max_depth: int = 15, visited: Optional[Set] = None) -> List[dict]:
    """Convert statement diffs to JSON format matching JavaScript POC with recursion protection."""
    if visited is None:
        visited = set()
    
    json_diffs = []
    
    # Prevent infinite recursion in JSON serialization
    if depth > max_depth:
        return [{"error": f"Maximum nesting depth ({max_depth}) exceeded", "truncated": True}]
    
    for i, stmt_diff in enumerate(statement_diffs):
        # Check for circular references
        stmt_id = id(stmt_diff)
        if stmt_id in visited:
            json_diffs.append({"error": "Circular reference detected", "node_type": stmt_diff.node_type})
            continue
        visited.add(stmt_id)
        
        stmt_obj = {
            'change_type': stmt_diff.change_type.value,
            'code': stmt_diff.code,
            'node_type': stmt_diff.node_type,
            'file_a_line': stmt_diff.file_a_line,
            'file_a_index': stmt_diff.file_a_index,
            'file_b_line': stmt_diff.file_b_line,
            'file_b_index': stmt_diff.file_b_index if stmt_diff.file_b_index is not None else i,
            'description': stmt_diff.description or f"Statement {stmt_diff.change_type.value}: {stmt_diff.node_type}",
            'old_code': stmt_diff.old_code,
            'similarity_score': stmt_diff.similarity_score,
            'is_container': stmt_diff.is_container,
            'branch_label': stmt_diff.branch_label
        }
        
        # Add child diffs if available (with recursion protection)
        if stmt_diff.child_diffs and depth < max_depth:
            stmt_obj['child_diffs'] = _convert_statement_diffs_to_json(stmt_diff.child_diffs, depth + 1, max_depth, visited.copy())
        else:
            stmt_obj['child_diffs'] = []
        
        # Remove from visited set after processing
        visited.discard(stmt_id)
        
        json_diffs.append(stmt_obj)
    
    return json_diffs


def main():
    """Main entry point for the CLI application."""
    parser = argparse.ArgumentParser(
        description="Compare two Groovy files using AST analysis",
        epilog="""
            This tool follows the JavaScript AST Diff POC approach:
            - Block-level classification (classes, methods, fields, etc.)
            - Multi-phase matching strategy (identifier, content hash, structural similarity)
            - Hierarchical diff structure with statement-level comparison
            - Rich similarity scoring and change type detection

            Structural Similarity:
            Calculated using Sørensen-Dice coefficient: (2 * common_elements) / (total_elements)
            Common elements include unchanged blocks and moved blocks (same content, different position)
        """,
        formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument('file1', help='First Groovy file to compare')
    parser.add_argument('file2', help='Second Groovy file to compare')
    parser.add_argument('--output', '-o', help='Save results to JSON file')
    parser.add_argument('--json', action='store_true', 
                       help='Output results in JSON format to console')
    parser.add_argument('--simple', '-s', action='store_true', 
                       help='Show simple summary output')
    
    args = parser.parse_args()
    
    # Check if files exist
    if not os.path.exists(args.file1):
        print(f"Error: File '{args.file1}' not found.")
        sys.exit(1)
    
    if not os.path.exists(args.file2):
        print(f"Error: File '{args.file2}' not found.")
        sys.exit(1)
    
    # Create the differ and compare files
    differ = GroovyASTDiff()
    result = differ.compare_files(args.file1, args.file2)
    
    # Handle errors
    if result.error:
        print(f"Error: {result.error}")
        sys.exit(1)
    
    # Output results
    if args.output:
        # Convert result to dict for JSON serialization
        result_dict = _create_json_result(result)
        
        with open(args.output, 'w') as f:
            json.dump(result_dict, f, indent=2)
        print(f"Results saved to {args.output}")
        
    elif args.json:
        # Use same comprehensive JSON structure for console output
        result_dict = _create_json_result(result)
        print(json.dumps(result_dict, indent=2))
        
    elif args.simple:
        # Simple summary output
        print("Groovy AST Diff - Summary:")
        print(f"  Added: {result.blocks_added}")
        print(f"  Deleted: {result.blocks_deleted}")
        print(f"  Modified: {result.blocks_modified}")
        print(f"  Moved: {result.blocks_moved}")
        print(f"  Moved-Modified: {result.blocks_moved_modified}")
        print(f"  Unchanged: {result.blocks_unchanged}")
        print(f"  Similarity: {result.structural_similarity:.1%}")
        
    else:
        # Default: detailed formatted output
        print(format_output(result))


if __name__ == "__main__":
    main()
