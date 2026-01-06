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

import tree_sitter
from tree_sitter import Language, Parser, Node

from .groovy_types import (
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
            # Path to the built Groovy parser
            parser_path = Path("parsers/tree-sitter-groovy")
            
            if not parser_path.exists():
                raise FileNotFoundError(
                    "Groovy parser not found. Please run 'python setup_parser.py' first."
                )
            
            # Build the language from the parser
            Language.build_library(
                'build/groovy.so',
                [str(parser_path)]
            )
            
            self.language = Language('build/groovy.so', 'groovy')
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
        Compare two Groovy sources and return detailed differences.
        
        Args:
            source_a: Source code of first file as bytes
            source_b: Source code of second file as bytes
            file_a_path: Path to first file (for metadata)
            file_b_path: Path to second file (for metadata)
            
        Returns:
            ComparisonResult with BlockDiff and nested StatementDiff
        """
        try:
            # Parse both sources
            tree_a = self.parser.parse(source_a)
            tree_b = self.parser.parse(source_b)
            
            if tree_a.root_node.has_error or tree_b.root_node.has_error:
                return self._error_result("One or both sources have syntax errors")
            
            print("Extracting top-level blocks...")
            # Extract top-level blocks
            blocks_a = self._extract_top_level_blocks(tree_a.root_node, source_a)
            blocks_b = self._extract_top_level_blocks(tree_b.root_node, source_b)
            
            print(f"Found {len(blocks_a)} blocks in file A, {len(blocks_b)} blocks in file B")
            
            # Compare blocks and generate diffs
            print("Comparing blocks...")
            return self._compare_blocks(blocks_a, blocks_b, source_a, source_b, file_a_path, file_b_path)
            
        except Exception as e:
            import traceback
            traceback.print_exc()
            return self._error_result(f"Comparison failed: {e}")
    
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
                    
                    # If this is a class, also extract its methods and fields
                    if node_type in GROOVY_CLASS_TYPES:
                        self._extract_class_members(node, source, blocks)
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
                        # Check if position changed (moved)
                        if i != j or block_a.start_line != block_b.start_line:
                            change_type = ChangeType.MOVED
                            similarity = 100.0
                            description = f"Moved {block_a.block_type.value} '{block_a.identifier}' from position {i+1} to {j+1}"
                        else:
                            change_type = ChangeType.UNCHANGED
                            similarity = 100.0
                            description = f"Identical {block_a.block_type.value} '{block_a.identifier}'"
                    else:
                        # Modified (same identifier, different content) - check if also moved
                        similarity = calculate_similarity(block_a.code, block_b.code) * 100
                        if i != j:
                            # Different position AND different content = MOVED_MODIFIED
                            change_type = ChangeType.MOVED_MODIFIED
                            description = f"Moved and modified {block_a.block_type.value} '{block_a.identifier}' from position {i+1} to {j+1} ({similarity:.1f}% similar)"
                        else:
                            # Same position, different content = MODIFIED
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
        
        # Phase 3: Match by structural similarity
        print("Phase 3: Matching by structural similarity...")
        for i, block_a in enumerate(blocks_a):
            if i in matched_a:
                continue
            for j, block_b in enumerate(blocks_b):
                if (j not in matched_b and
                    block_a.block_type == block_b.block_type):
                    
                    similarity = calculate_similarity(block_a.code, block_b.code)
                    
                    if similarity >= 0.3:  # Lower threshold to catch more changes
                        # Determine change type based on identifier and position
                        if block_a.identifier == block_b.identifier:
                            # Same identifier - check if position changed (moved and modified)
                            if i != j:  # Different array positions = moved
                                change_type = ChangeType.MOVED_MODIFIED
                                description = f"Moved and modified {block_a.block_type.value} '{block_a.identifier}' from position {i+1} to {j+1} ({similarity*100:.1f}% similar)"
                            else:
                                # Same position - just modified
                                change_type = ChangeType.MODIFIED
                                description = f"Modified {block_a.block_type.value} '{block_a.identifier}' ({similarity*100:.1f}% similar)"
                        else:
                            # Different identifiers - this is likely a different function altogether
                            # Skip this match and let it be handled as separate add/delete
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
                        break
        
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
            blocks_moved=change_counts[ChangeType.MOVED] + change_counts[ChangeType.MOVED_MODIFIED],
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
        Compare statements within two modified blocks using improved analysis.
        
        For classes, this compares individual methods and fields.
        For methods, this compares individual statements within the method body.
        """
        try:
            # Special handling for class blocks - compare their members
            if block_a.block_type == BlockType.CLASS:
                return self._compare_class_members(block_a, block_b, source_a, source_b)
            
            # For other blocks, use recursive parsing
            tree_a = self.parser.parse(source_a)
            tree_b = self.parser.parse(source_b)
            
            # Find the specific nodes for these blocks
            node_a = self._find_node_at_line(tree_a.root_node, block_a.start_line)
            node_b = self._find_node_at_line(tree_b.root_node, block_b.start_line)
            
            if not node_a or not node_b:
                return self._simple_block_comparison(block_a, block_b)
            
            # Extract and compare individual statements directly
            return self._compare_function_statements_direct(node_a, node_b, source_a, source_b)
            
        except Exception as e:
            print(f"Warning: Statement comparison failed, using simple comparison: {e}")
            return self._simple_block_comparison(block_a, block_b)
    
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
            print(f"Warning: Class member comparison failed: {e}")
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
                        if member_child.type in GROOVY_FUNCTION_TYPES or member_child.type in GROOVY_FIELD_TYPES:
                            # Extract member info
                            code = source[member_child.start_byte:member_child.end_byte].decode('utf-8', errors='replace')
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
                if similarity > best_similarity and similarity >= 0.3:  # 30% threshold
                    best_similarity = similarity
                    best_match = member_b
                    best_j = j
            
            if best_match:
                diff = StatementDiff(
                    change_type=StatementChangeType.MODIFIED,
                    code=best_match.code,
                    node_type=best_match.node_type,
                    file_a_line=member_a.start_line,
                    file_a_index=i,
                    file_b_line=best_match.start_line,
                    file_b_index=best_j,
                    old_code=member_a.code,
                    similarity_score=best_similarity,
                    description=f"Statement modified: {best_match.node_type} ({best_similarity:.1%} similar)",
                    is_container=best_match.node_type in GROOVY_FUNCTION_TYPES
                )
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
    
    def _compare_function_statements_direct(self, node_a: Node, node_b: Node, source_a: bytes, source_b: bytes) -> List[StatementDiff]:
        """Compare individual statements within function bodies directly."""
        # Extract function body statements
        statements_a = self._extract_function_body_statements(node_a, source_a)
        statements_b = self._extract_function_body_statements(node_b, source_b)
        
        return self._compare_statement_lists(statements_a, statements_b)
    
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
                elif child.type == "method_invocation":
                    # Look for method name
                    for grandchild in child.named_children:
                        if grandchild.type == "identifier":
                            return source[grandchild.start_byte:grandchild.end_byte].decode('utf-8', errors='replace')
        
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
    
    def _compare_statement_lists(self, statements_a: List[StatementSignature], statements_b: List[StatementSignature]) -> List[StatementDiff]:
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
    output.append(f"  Added:     {result.blocks_added}")
    output.append(f"  Deleted:   {result.blocks_deleted}")
    output.append(f"  Modified:  {result.blocks_modified}")
    output.append(f"  Moved:     {result.blocks_moved}")
    output.append(f"  Unchanged: {result.blocks_unchanged}")
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
            diff_obj['statement_diffs'] = _convert_statement_diffs_to_json(diff.statement_diffs)
        
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
        'blocks_unchanged': result.blocks_unchanged,
        'file_a_path': result.file_a_path,
        'file_b_path': result.file_b_path,
        'differences': differences,
        'message': message
    }


def _convert_statement_diffs_to_json(statement_diffs: List[StatementDiff]) -> List[dict]:
    """Convert statement diffs to JSON format matching JavaScript POC."""
    json_diffs = []
    
    for i, stmt_diff in enumerate(statement_diffs):
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
        
        # Add child diffs if available
        if stmt_diff.child_diffs:
            stmt_obj['child_diffs'] = _convert_statement_diffs_to_json(stmt_diff.child_diffs)
        else:
            stmt_obj['child_diffs'] = []
        
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
        print(f"  Unchanged: {result.blocks_unchanged}")
        print(f"  Similarity: {result.structural_similarity:.1%}")
        
    else:
        # Default: detailed formatted output
        print(format_output(result))


if __name__ == "__main__":
    main()
