"""
Recursive AST Parser for Groovy - Following JavaScript AST Diff POC Approach

This module implements deep recursive parsing of Groovy ASTs, drilling down into
nested code blocks until reaching pure statements, similar to the JavaScript implementation.
"""

from typing import List, Optional, Dict, Set, Tuple
from dataclasses import dataclass, field
import hashlib

from groovy_types import (
    BlockType, ChangeType, StatementChangeType,
    GROOVY_CONTAINER_TYPES, GROOVY_PURE_STATEMENT_TYPES,
    GROOVY_FUNCTION_TYPES, GROOVY_CLASS_TYPES
)
from groovy_domain import StatementSignature, StatementDiff


# Groovy container types that need recursive parsing
GROOVY_RECURSIVE_CONTAINERS = {
    # Control flow containers
    "if_statement",
    "else_clause", 
    "switch_statement",
    "switch_case",
    "switch_default",
    "try_statement",
    "catch_clause",
    "finally_clause",
    
    # Loop containers
    "for_statement",
    "for_in_statement", 
    "while_statement",
    "do_while_statement",
    
    # Block containers
    "block",
    "statement_block",
    
    # Function/Class containers
    "function_definition",
    "method_definition",
    "constructor_definition",
    "class_definition",
    "interface_definition",
    
    # Groovy-specific containers
    "closure",
    "closure_expression",
}

# Pure statement types (leaf nodes) - stop recursion here
GROOVY_LEAF_STATEMENTS = {
    "expression_statement",
    "return_statement", 
    "throw_statement",
    "break_statement",
    "continue_statement",
    "assert_statement",
    "import_statement",
    "package_statement",
    "variable_declaration",
    "field_declaration",
    "empty_statement",
}


@dataclass
class RecursiveNodeSignature:
    """Signature for a node at any depth in the Groovy AST."""
    
    # Identity
    node_type: str
    identifier: Optional[str] = None
    
    # Hashes for comparison
    content_hash: str = ""
    structure_hash: str = ""
    body_hash: Optional[str] = None
    
    # Position info
    start_line: int = 0
    end_line: int = 0
    depth: int = 0
    
    # Parent reference
    parent_hash: Optional[str] = None
    path: str = ""
    
    # Children (recursive)
    children: List['RecursiveNodeSignature'] = field(default_factory=list)
    
    # Flags
    is_pure_statement: bool = False
    is_container: bool = False
    
    # Original code
    code: str = ""


class GroovyRecursiveParser:
    """
    Recursive parser for Groovy ASTs following the JavaScript POC approach.
    
    This parser drills down into nested structures until reaching pure statements,
    enabling fine-grained comparison at the statement level.
    """
    
    def __init__(self):
        """Initialize the recursive parser."""
        pass
    
    def parse_recursive(
        self,
        node,
        source: bytes,
        depth: int = 0,
        parent_hash: Optional[str] = None,
        path: str = ""
    ) -> RecursiveNodeSignature:
        """
        Recursively parse a node and all its children until pure statements.
        
        Args:
            node: tree-sitter Node object
            source: Original source code as bytes
            depth: Current nesting depth
            parent_hash: Hash of parent node
            path: Current path for context
            
        Returns:
            RecursiveNodeSignature for this node and all descendants
        """
        # 1. Get node content and compute content hash
        code = source[node.start_byte:node.end_byte].decode('utf-8', errors='replace')
        content_hash = self._hash_content(code)
        
        # 2. Determine node category
        node_type = node.type
        is_pure = node_type in GROOVY_LEAF_STATEMENTS
        is_container = node_type in GROOVY_RECURSIVE_CONTAINERS
        
        # 3. Extract identifier if named
        identifier = self._extract_identifier(node, source)
        
        # 4. Build path for context
        current_path = f"{path}/{node_type}"
        if identifier:
            current_path += f":{identifier}"
        
        # 5. Create signature
        signature = RecursiveNodeSignature(
            node_type=node_type,
            identifier=identifier,
            content_hash=content_hash,
            structure_hash="",  # computed after children
            body_hash=None,
            start_line=node.start_point[0] + 1,
            end_line=node.end_point[0] + 1,
            depth=depth,
            parent_hash=parent_hash,
            path=current_path,
            children=[],
            is_pure_statement=is_pure,
            is_container=is_container,
            code=code,
        )
        
        # 6. If pure statement, stop recursion
        if is_pure:
            signature.structure_hash = content_hash
            return signature
        
        # 7. Otherwise, recurse into children
        child_nodes = self._get_parseable_children(node)
        
        for child in child_nodes:
            child_sig = self.parse_recursive(
                child,
                source,
                depth + 1,
                content_hash,
                current_path
            )
            signature.children.append(child_sig)
        
        # 8. Compute structure hash from children
        child_hashes = [c.structure_hash for c in signature.children]
        signature.structure_hash = self._hash_structure(node_type, child_hashes)
        
        # 9. Compute body hash for functions
        if node_type in GROOVY_FUNCTION_TYPES:
            signature.body_hash = self._compute_body_hash(node, source)
        
        return signature
    
    def compare_recursive_statements(
        self,
        sig_a: RecursiveNodeSignature,
        sig_b: RecursiveNodeSignature
    ) -> List[StatementDiff]:
        """
        Compare two recursive signatures and generate statement-level diffs.
        
        This is the core recursive comparison that goes deep into nested structures.
        """
        diffs = []
        
        # If both are pure statements, compare directly
        if sig_a.is_pure_statement and sig_b.is_pure_statement:
            if sig_a.content_hash == sig_b.content_hash:
                change_type = StatementChangeType.UNCHANGED
            else:
                change_type = StatementChangeType.MODIFIED
            
            diff = StatementDiff(
                change_type=change_type,
                code=sig_b.code,
                node_type=sig_b.node_type,
                file_a_line=sig_a.start_line,
                file_b_line=sig_b.start_line,
                old_code=sig_a.code if change_type == StatementChangeType.MODIFIED else None,
                similarity_score=self._calculate_similarity(sig_a.code, sig_b.code) if change_type == StatementChangeType.MODIFIED else None
            )
            diffs.append(diff)
            return diffs
        
        # If both are containers, compare their children recursively
        if sig_a.is_container and sig_b.is_container:
            child_diffs = self._compare_container_children(sig_a, sig_b)
            
            # Create container diff
            container_diff = StatementDiff(
                change_type=StatementChangeType.MODIFIED if child_diffs else StatementChangeType.UNCHANGED,
                code=sig_b.code[:100] + "..." if len(sig_b.code) > 100 else sig_b.code,
                node_type=sig_b.node_type,
                file_a_line=sig_a.start_line,
                file_b_line=sig_b.start_line,
                child_diffs=child_diffs,
                is_container=True,
                description=f"Container {sig_b.node_type} with {len(child_diffs)} changes"
            )
            diffs.append(container_diff)
            return diffs
        
        # Mixed case - one is container, one is pure
        diff = StatementDiff(
            change_type=StatementChangeType.MODIFIED,
            code=sig_b.code,
            node_type=sig_b.node_type,
            file_a_line=sig_a.start_line,
            file_b_line=sig_b.start_line,
            old_code=sig_a.code,
            similarity_score=self._calculate_similarity(sig_a.code, sig_b.code),
            description="Structure changed from container to statement or vice versa"
        )
        diffs.append(diff)
        return diffs
    
    def _compare_container_children(
        self,
        sig_a: RecursiveNodeSignature,
        sig_b: RecursiveNodeSignature
    ) -> List[StatementDiff]:
        """
        Compare children of two container nodes using multi-phase matching.
        """
        diffs = []
        children_a = sig_a.children
        children_b = sig_b.children
        
        matched_a = set()
        matched_b = set()
        
        # Phase 1: Match by content hash (exact matches)
        for i, child_a in enumerate(children_a):
            for j, child_b in enumerate(children_b):
                if (i not in matched_a and j not in matched_b and
                    child_a.content_hash == child_b.content_hash):
                    
                    # Recursively compare matched children
                    child_diffs = self.compare_recursive_statements(child_a, child_b)
                    diffs.extend(child_diffs)
                    
                    matched_a.add(i)
                    matched_b.add(j)
                    break
        
        # Phase 2: Match by similarity (for modified statements)
        for i, child_a in enumerate(children_a):
            if i in matched_a:
                continue
            
            for j, child_b in enumerate(children_b):
                if j in matched_b:
                    continue
                
                if (child_a.node_type == child_b.node_type and
                    self._calculate_similarity(child_a.code, child_b.code) >= 0.7):
                    
                    # Recursively compare similar children
                    child_diffs = self.compare_recursive_statements(child_a, child_b)
                    diffs.extend(child_diffs)
                    
                    matched_a.add(i)
                    matched_b.add(j)
                    break
        
        # Phase 3: Remaining unmatched children
        for i, child_a in enumerate(children_a):
            if i not in matched_a:
                diff = StatementDiff(
                    change_type=StatementChangeType.DELETED,
                    code=child_a.code,
                    node_type=child_a.node_type,
                    file_a_line=child_a.start_line,
                    description=f"Deleted {child_a.node_type}"
                )
                diffs.append(diff)
        
        for j, child_b in enumerate(children_b):
            if j not in matched_b:
                diff = StatementDiff(
                    change_type=StatementChangeType.ADDED,
                    code=child_b.code,
                    node_type=child_b.node_type,
                    file_b_line=child_b.start_line,
                    description=f"Added {child_b.node_type}"
                )
                diffs.append(diff)
        
        return diffs
    
    def _get_parseable_children(self, node) -> List:
        """
        Get children that should be parsed recursively for Groovy.
        
        This determines what to recurse into for each Groovy container node type.
        """
        children = []
        node_type = node.type
        
        # Function/Method containers
        if node_type in GROOVY_FUNCTION_TYPES:
            body = node.child_by_field_name("body")
            if body:
                if body.type in {"block", "statement_block"}:
                    # Parse all statements in the body
                    for child in body.named_children:
                        children.append(child)
                else:
                    # Single statement body
                    children.append(body)
        
        # Class containers
        elif node_type in GROOVY_CLASS_TYPES:
            body = node.child_by_field_name("body")
            if body:
                for child in body.named_children:
                    children.append(child)
        
        # If statement
        elif node_type == "if_statement":
            consequence = node.child_by_field_name("consequence")
            alternative = node.child_by_field_name("alternative")
            
            if consequence:
                children.extend(self._get_block_statements(consequence))
            if alternative:
                children.extend(self._get_block_statements(alternative))
        
        # Switch statement
        elif node_type == "switch_statement":
            body = node.child_by_field_name("body")
            if body:
                for case in body.named_children:
                    children.append(case)
        
        elif node_type in {"switch_case", "switch_default"}:
            # Get statements after the case label
            for child in node.named_children:
                if child.type not in {"identifier", "number", "string"}:
                    children.append(child)
        
        # Loops
        elif node_type in {"for_statement", "for_in_statement", "while_statement", "do_while_statement"}:
            body = node.child_by_field_name("body")
            if body:
                children.extend(self._get_block_statements(body))
        
        # Try-catch-finally
        elif node_type == "try_statement":
            body = node.child_by_field_name("body")
            handler = node.child_by_field_name("handler")
            finalizer = node.child_by_field_name("finalizer")
            
            if body:
                children.extend(self._get_block_statements(body))
            if handler:
                children.append(handler)
            if finalizer:
                children.append(finalizer)
        
        elif node_type in {"catch_clause", "finally_clause"}:
            body = node.child_by_field_name("body")
            if body:
                children.extend(self._get_block_statements(body))
        
        # Block containers
        elif node_type in {"block", "statement_block"}:
            for child in node.named_children:
                children.append(child)
        
        # Closure
        elif node_type in {"closure", "closure_expression"}:
            body = node.child_by_field_name("body")
            if body:
                children.extend(self._get_block_statements(body))
        
        return children
    
    def _get_block_statements(self, block_node) -> List:
        """Get statements from a block node."""
        if block_node.type in {"block", "statement_block"}:
            return list(block_node.named_children)
        else:
            # Single statement
            return [block_node]
    
    def _extract_identifier(self, node, source: bytes) -> Optional[str]:
        """Extract identifier from a node."""
        # Try name field first
        name_node = node.child_by_field_name("name")
        if name_node:
            return source[name_node.start_byte:name_node.end_byte].decode('utf-8', errors='replace')
        
        # Look for identifier children
        for child in node.named_children:
            if child.type == "identifier":
                return source[child.start_byte:child.end_byte].decode('utf-8', errors='replace')
        
        return None
    
    def _hash_content(self, content: str) -> str:
        """Create a normalized hash of content."""
        normalized = " ".join(content.split())
        return hashlib.sha256(normalized.encode("utf-8")).hexdigest()[:16]
    
    def _hash_structure(self, node_type: str, child_hashes: List[str]) -> str:
        """Create a structural hash from node type and children."""
        combined = node_type + ":" + ",".join(sorted(child_hashes))
        return hashlib.sha256(combined.encode("utf-8")).hexdigest()[:16]
    
    def _compute_body_hash(self, node, source: bytes) -> Optional[str]:
        """Compute hash of function body only."""
        body = node.child_by_field_name("body")
        if not body:
            return None
        
        body_code = source[body.start_byte:body.end_byte].decode("utf-8", errors='replace')
        return self._hash_content(body_code)
    
    def _calculate_similarity(self, text1: str, text2: str) -> float:
        """Calculate similarity between two text strings."""
        if not text1 and not text2:
            return 1.0
        if not text1 or not text2:
            return 0.0
        
        # Simple character-based similarity
        common_chars = sum(1 for a, b in zip(text1, text2) if a == b)
        max_length = max(len(text1), len(text2))
        
        return common_chars / max_length if max_length > 0 else 0.0
