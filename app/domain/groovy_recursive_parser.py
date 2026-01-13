"""
Recursive AST Parser for Groovy - Following JavaScript AST Diff POC Approach

This module implements deep recursive parsing of Groovy ASTs, drilling down into
nested code blocks until reaching pure statements, similar to the JavaScript implementation.
"""

from typing import List, Optional, Dict, Set, Tuple
from dataclasses import dataclass, field
import hashlib

from ..types.groovy_types import (
    BlockType, ChangeType, StatementChangeType,
    GROOVY_CONTAINER_TYPES, GROOVY_PURE_STATEMENT_TYPES,
    GROOVY_FUNCTION_TYPES, GROOVY_CLASS_TYPES
)
from .groovy_domain import StatementSignature, StatementDiff


# Groovy container types that need recursive parsing
GROOVY_RECURSIVE_CONTAINERS = {
    # Control flow containers (CORRECTED node types)
    "if_statement",
    "else_clause", 
    "switch_statement",
    "switch_block",      # FIXED: back to container, but with special handling
    # NOTE: "case" remains in GROOVY_LEAF_STATEMENTS to prevent infinite recursion
    "switch_default",
    "try_statement",
    
    # Loop containers (CORRECTED node types)
    "for_loop",           # FIXED: was for_statement
    "for_in_loop",       # FIXED: was for_in_statement
    "while_loop",        # FIXED: was while_statement
    "do_while_loop",     # FIXED: actual node type is do_while_loop
    "do_while_statement", # Keep this for completeness
    
    # Block containers
    "block",
    "statement_block",
    
    # Function/Class containers
    "function_definition",
    "method_definition",
    "constructor_definition",
    "class_definition",
    "interface_definition",
    "trait_definition",
    "enum_definition",
    "annotation_definition",
    
    # Groovy-specific containers (context-aware handling)
    "closure",           # Now handled contextually - structural blocks are containers
    "closure_expression", # Real closures are treated as pure statements
    
    # Additional Groovy containers
    "synchronized_statement",
    "labeled_statement",
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
    "declaration", # declaration is a pure statement in groovy grammar
    "case",        # FIXED: case statements are pure (leaf nodes) to prevent infinite recursion
    # NOTE: switch_block moved back to containers with special handling
    # NOTE: closures are now handled contextually in parse_recursive method
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
        path: str = "",
        max_depth: int = 10  # Add recursion depth protection
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
        # 0. Check recursion depth to prevent infinite loops
        if depth > max_depth:
            # Create a simple signature for deeply nested nodes
            code = source[node.start_byte:node.end_byte].decode('utf-8', errors='replace')
            content_hash = self._hash_content(code)
            return RecursiveNodeSignature(
                node_type=node.type,
                identifier=None,
                content_hash=content_hash,
                structure_hash=content_hash,
                body_hash=None,
                start_line=node.start_point[0] + 1,
                end_line=node.end_point[0] + 1,
                depth=depth,
                parent_hash=parent_hash,
                path=f"{path}/{node.type}:MAX_DEPTH",
                children=[],
                is_pure_statement=True,  # Treat as pure to stop recursion
                is_container=False,
                code=code[:100] + "..." if len(code) > 100 else code,
            )

        # 1. Get node content and compute content hash
        code = source[node.start_byte:node.end_byte].decode('utf-8', errors='replace')
        content_hash = self._hash_content(code)
        
        # 2. Determine node category (with special handling for closures)
        node_type = node.type
        
        # Special handling for closures - check if it's a real closure or structural block
        if node_type == "closure":
            closure_context = self._get_closure_context(node)
            if closure_context in {"CLASS_BODY", "FUNCTION_BODY", "FOR_LOOP_BODY", "WHILE_LOOP_BODY", 
                                 "IF_STATEMENT_BODY", "SWITCH_BODY", "TRY_BODY", "CATCH_BODY", "FINALLY_BODY"}:
                # This is a structural block - don't treat as closure, parse children directly
                # Skip creating a signature for this closure and parse its children instead
                child_signatures = []
                for child in node.named_children:
                    child_sig = self.parse_recursive(child, source, depth, parent_hash, path, max_depth)
                    child_signatures.append(child_sig)
                
                # For structural blocks, return the children directly without wrapping in a closure
                # This prevents infinite recursion by not creating a closure signature
                if len(child_signatures) == 1:
                    # If there's only one child, return it directly
                    return child_signatures[0]
                elif len(child_signatures) > 1:
                    # If there are multiple children, create a transparent container
                    return RecursiveNodeSignature(
                        node_type="structural_block",  # Don't use "closure" 
                        identifier=None,
                        content_hash=content_hash,
                        structure_hash=self._hash_structure("structural_block", [c.structure_hash for c in child_signatures]),
                        body_hash=None,
                        start_line=node.start_point[0] + 1,
                        end_line=node.end_point[0] + 1,
                        depth=depth,
                        parent_hash=parent_hash,
                        path=f"{path}/structural_block",
                        children=child_signatures,
                        is_pure_statement=False,
                        is_container=True,
                        code=code,
                    )
                else:
                    # No children, treat as pure statement
                    return RecursiveNodeSignature(
                        node_type="empty_block",
                        identifier=None,
                        content_hash=content_hash,
                        structure_hash=content_hash,
                        body_hash=None,
                        start_line=node.start_point[0] + 1,
                        end_line=node.end_point[0] + 1,
                        depth=depth,
                        parent_hash=parent_hash,
                        path=f"{path}/empty_block",
                        children=[],
                        is_pure_statement=True,
                        is_container=False,
                        code=code,
                    )
            else:
                # This is a real closure, treat as pure statement to avoid recursion
                is_pure = True
                is_container = False
        else:
            # Special handling for switch_block to prevent infinite recursion
            if node_type == "switch_block":
                # switch_block is a container, but we need to be careful about recursion
                is_pure = False
                is_container = True
            else:
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
                current_path,
                max_depth
            )
            signature.children.append(child_sig)
        
        # 8. Compute structure hash from children
        child_hashes = [c.structure_hash for c in signature.children]
        signature.structure_hash = self._hash_structure(node_type, child_hashes)
        
        # 9. Compute body hash for functions
        if node_type in GROOVY_FUNCTION_TYPES:
            signature.body_hash = self._compute_body_hash(node, source)
        
        return signature
    
    def _get_closure_context(self, closure_node):
        """Determine if a closure is a real closure or just a structural block."""
        if not closure_node.parent:
            return "UNKNOWN"
        
        parent = closure_node.parent
        parent_type = parent.type
        
        # Check different contexts
        if parent_type == "class_definition":
            return "CLASS_BODY"
        elif parent_type == "function_definition":
            return "FUNCTION_BODY"
        elif parent_type == "for_loop":
            return "FOR_LOOP_BODY"
        elif parent_type == "while_loop":
            return "WHILE_LOOP_BODY"
        elif parent_type == "if_statement":
            return "IF_STATEMENT_BODY"
        elif parent_type == "switch_statement":
            return "SWITCH_BODY"
        elif parent_type == "try_statement":
            # Determine which part of the try-catch-finally this closure belongs to
            try_body = parent.child_by_field_name("body")
            catch_body = parent.child_by_field_name("catch_body")
            finally_body = parent.child_by_field_name("finally_body")
            
            if closure_node == try_body:
                return "TRY_BODY"
            elif closure_node == catch_body:
                return "CATCH_BODY"
            elif closure_node == finally_body:
                return "FINALLY_BODY"
            else:
                return "TRY_BODY"  # fallback
        elif parent_type in ["catch", "finally"]:
            return f"{parent_type.upper()}_BODY"
        elif parent_type in ["function_call", "juxt_function_call"]:
            return "REAL_CLOSURE"  # This is likely a real closure passed to a function
        else:
            return f"OTHER_CONTEXT_{parent_type}"
    
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
            
            # Determine if the container itself changed
            container_changed = sig_a.content_hash != sig_b.content_hash
            
            if container_changed or child_diffs:
                # Create container diff showing the change
                change_type = StatementChangeType.MODIFIED if container_changed else StatementChangeType.UNCHANGED
                
                container_diff = StatementDiff(
                    change_type=change_type,
                    code=sig_b.code[:100] + "..." if len(sig_b.code) > 100 else sig_b.code,
                    node_type=sig_b.node_type,
                    file_a_line=sig_a.start_line,
                    file_b_line=sig_b.start_line,
                    old_code=sig_a.code[:100] + "..." if len(sig_a.code) > 100 else sig_a.code if container_changed else None,
                    child_diffs=child_diffs,
                    is_container=True,
                    similarity_score=self._calculate_similarity(sig_a.code, sig_b.code) if container_changed else 1.0,
                    description=f"Container {sig_b.node_type} with {len(child_diffs)} nested changes"
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
        
        # Phase 1: Match by identifier (same type and name) - CRITICAL for containers like classes/functions
        for i, child_a in enumerate(children_a):
            for j, child_b in enumerate(children_b):
                if (i not in matched_a and j not in matched_b and
                    child_a.node_type == child_b.node_type and
                    child_a.identifier and child_b.identifier and
                    child_a.identifier == child_b.identifier):
                    
                    # Found matching identifier - recursively compare
                    child_diffs = self.compare_recursive_statements(child_a, child_b)
                    diffs.extend(child_diffs)
                    
                    matched_a.add(i)
                    matched_b.add(j)
                    break
        
        # Phase 2: Match by content hash (exact matches)
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
        
        # Phase 3: Match by similarity (for modified statements)
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
        
        # Phase 4: Remaining unmatched children (deleted/added)
        for i, child_a in enumerate(children_a):
            if i not in matched_a:
                # For deleted containers, recursively show what was inside
                child_diffs = []
                if child_a.is_container and child_a.children:
                    child_diffs = self._generate_deleted_child_diffs(child_a)
                
                diff = StatementDiff(
                    change_type=StatementChangeType.DELETED,
                    code=child_a.code,
                    node_type=child_a.node_type,
                    file_a_line=child_a.start_line,
                    description=f"Deleted {child_a.node_type}",
                    is_container=child_a.is_container,
                    child_diffs=child_diffs
                )
                diffs.append(diff)
        
        for j, child_b in enumerate(children_b):
            if j not in matched_b:
                # For added containers, recursively show what was inside
                child_diffs = []
                if child_b.is_container and child_b.children:
                    child_diffs = self._generate_added_child_diffs(child_b)
                
                diff = StatementDiff(
                    change_type=StatementChangeType.ADDED,
                    code=child_b.code,
                    node_type=child_b.node_type,
                    file_b_line=child_b.start_line,
                    description=f"Added {child_b.node_type}",
                    is_container=child_b.is_container,
                    child_diffs=child_diffs
                )
                diffs.append(diff)
        
        return diffs
    
    def _compare_if_statement_branches(
        self,
        sig_a: RecursiveNodeSignature,
        sig_b: RecursiveNodeSignature
    ) -> List[StatementDiff]:
        """
        Compare if_statement nodes using branch-aware analysis.
        
        This method extracts and compares individual branches (if, else if, else)
        similar to the JavaScript POC approach.
        """
        diffs = []
        
        # Extract branches from both nodes (we need to get source from somewhere)
        # For now, let's use a simpler approach and fall back to regular container comparison
        # TODO: Implement proper branch comparison with source access
        return self._compare_container_children(sig_a, sig_b)
        
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
                    body_diffs = self._compare_statement_lists(
                        branch_a["statements"], branch_b["statements"]
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
                    body_diffs = self._compare_statement_lists(
                        branch_a["statements"], branch_b["statements"]
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
    
    def _generate_deleted_child_diffs(self, sig: RecursiveNodeSignature) -> List[StatementDiff]:
        """Generate child diffs for a deleted container, showing all nested deletions."""
        child_diffs = []
        for child in sig.children:
            nested_diffs = []
            if child.is_container and child.children:
                nested_diffs = self._generate_deleted_child_diffs(child)
            
            diff = StatementDiff(
                change_type=StatementChangeType.DELETED,
                code=child.code,
                node_type=child.node_type,
                file_a_line=child.start_line,
                description=f"Deleted {child.node_type}",
                is_container=child.is_container,
                child_diffs=nested_diffs
            )
            child_diffs.append(diff)
        return child_diffs
    
    def _generate_added_child_diffs(self, sig: RecursiveNodeSignature) -> List[StatementDiff]:
        """Generate child diffs for an added container, showing all nested additions."""
        child_diffs = []
        for child in sig.children:
            nested_diffs = []
            if child.is_container and child.children:
                nested_diffs = self._generate_added_child_diffs(child)
            
            diff = StatementDiff(
                change_type=StatementChangeType.ADDED,
                code=child.code,
                node_type=child.node_type,
                file_b_line=child.start_line,
                description=f"Added {child.node_type}",
                is_container=child.is_container,
                child_diffs=nested_diffs
            )
            child_diffs.append(diff)
        return child_diffs
    
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
        
        # Class containers (including new types)
        elif node_type in {"class_definition", "interface_definition", "trait_definition", "enum_definition", "annotation_definition"}:
            body = node.child_by_field_name("body")
            if body:
                for child in body.named_children:
                    children.append(child)
        
        # If statement
        elif node_type == "if_statement":
            body = node.child_by_field_name("body")
            else_body = node.child_by_field_name("else_body")
            
            if body:
                children.extend(self._get_block_statements(body))
            if else_body:
                children.extend(self._get_block_statements(else_body))
        
        # Switch statement
        elif node_type == "switch_statement":
            body = node.child_by_field_name("body")
            if body:
                for case in body.named_children:
                    children.append(case)
        
        # Switch cases (CORRECTED node types)
        elif node_type == "switch_block":
            # Switch block contains case statements
            for child in node.named_children:
                if child.type == "case":
                    children.append(child)
        
        elif node_type in {"case", "switch_default"}:
            # Get statements after the case label
            for child in node.named_children:
                if child.type not in {"identifier", "number", "string"}:
                    children.append(child)
        
        # Loops (CORRECTED node types)
        elif node_type in {"for_loop", "for_in_loop", "while_loop", "do_while_statement"}:
            body = node.child_by_field_name("body")
            if body:
                children.extend(self._get_block_statements(body))
        
        # Try-catch-finally
        elif node_type == "try_statement":
            body = node.child_by_field_name("body")
            catch_body = node.child_by_field_name("catch_body")
            finally_body = node.child_by_field_name("finally_body")
            
            if body:
                children.extend(self._get_block_statements(body))
            if catch_body:
                children.extend(self._get_block_statements(catch_body))
            if finally_body:
                children.extend(self._get_block_statements(finally_body))
        
        # Note: catch_clause and finally_clause are now handled directly in try_statement
        # to avoid double processing since they're accessed via field names
        
        # Block containers
        elif node_type in {"block", "statement_block"}:
            for child in node.named_children:
                children.append(child)
        
        # Closure (context-aware handling)
        elif node_type == "closure":
            closure_context = self._get_closure_context(node)
            if closure_context in {"CLASS_BODY", "FUNCTION_BODY", "FOR_LOOP_BODY", "WHILE_LOOP_BODY", 
                                 "IF_STATEMENT_BODY", "SWITCH_BODY", "TRY_BODY", "CATCH_BODY", "FINALLY_BODY"}:
                # This is a structural block - parse all children normally
                for child in node.named_children:
                    children.append(child)
            # For real closures, don't recurse to avoid complexity
        
        elif node_type == "closure_expression":
            # Handle closure expressions similarly
            for child in node.named_children:
                children.append(child)
        
        # Additional Groovy containers
        elif node_type == "synchronized_statement":
            body = node.child_by_field_name("body")
            if body:
                children.extend(self._get_block_statements(body))
        
        elif node_type == "labeled_statement":
            statement = node.child_by_field_name("statement")
            if statement:
                children.append(statement)
        
        return children
    
    def _extract_if_branches(self, if_node, source: bytes = None) -> List[dict]:
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
            body_statements = self._get_block_statements(body_node)
            
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
                else_statements = self._get_block_statements(else_body_node)
                branches.append({
                    "branch_type": "else",
                    "condition": None,
                    "statements": else_statements,
                    "start_line": else_body_node.start_point[0] + 1,
                    "code": source[else_body_node.start_byte:else_body_node.end_byte].decode('utf-8', errors='replace')
                })
        
        return branches
    
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
