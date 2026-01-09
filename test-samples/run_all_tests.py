#!/usr/bin/env python3
"""
Test runner for all Groovy AST Diff test samples.

This script runs the AST diff tool on all test file pairs and generates
a summary report of the results.
"""

import os
import subprocess
import json
from pathlib import Path

def run_ast_diff(before_file, after_file):
    """Run AST diff on a pair of files and return the results."""
    try:
        result = subprocess.run([
            'python3', '../groovy_ast_diff.py', 
            before_file, after_file, 
            '--json'
        ], capture_output=True, text=True, cwd=Path(__file__).parent)
        
        if result.returncode == 0:
            return json.loads(result.stdout)
        else:
            return {"error": result.stderr}
    except Exception as e:
        return {"error": str(e)}

def main():
    """Run all test pairs and generate summary."""
    test_pairs = [
        ("class_before.groovy", "class_after.groovy", "Class Constructs"),
        ("method_before.groovy", "method_after.groovy", "Method Constructs"),
        ("field_before.groovy", "field_after.groovy", "Field/Property Constructs"),
        ("closure_before.groovy", "closure_after.groovy", "Closure Constructs"),
        ("script_before.groovy", "script_after.groovy", "Script Constructs"),
        ("import_before.groovy", "import_after.groovy", "Import/Package Constructs"),
        ("expression_before.groovy", "expression_after.groovy", "Expression Constructs"),
        ("comment_before.groovy", "comment_after.groovy", "Comment Constructs"),
        ("comprehensive_before.groovy", "comprehensive_after.groovy", "Comprehensive Test"),
    ]
    
    print("🧪 Running Groovy AST Diff Test Suite")
    print("=" * 50)
    
    for before, after, description in test_pairs:
        print(f"\n📋 Testing: {description}")
        print(f"   Files: {before} ↔ {after}")
        
        result = run_ast_diff(before, after)
        
        if "error" in result:
            print(f"   ❌ ERROR: {result['error']}")
            continue
            
        # Extract summary statistics
        summary = {
            "identical": result.get("is_identical", False),
            "similarity": result.get("structural_similarity", 0.0),
            "total_changes": len(result.get("differences", [])),
            "added": result.get("blocks_added", 0),
            "deleted": result.get("blocks_deleted", 0),
            "modified": result.get("blocks_modified", 0),
            "moved": result.get("blocks_moved", 0),
        }
        
        print(f"   ✅ Results:")
        print(f"      • Identical: {summary['identical']}")
        print(f"      • Similarity: {summary['similarity']:.1%}")
        print(f"      • Changes: {summary['total_changes']} total")
        print(f"      • Added: {summary['added']}, Deleted: {summary['deleted']}")
        print(f"      • Modified: {summary['modified']}, Moved: {summary['moved']}")
        
        # Check for statement-level diffs
        has_statement_diffs = any(
            len(diff.get("statement_diffs", [])) > 0 
            for diff in result.get("differences", [])
        )
        if has_statement_diffs:
            print(f"      • Statement-level analysis: ✅ Present")
        
    print(f"\n🎉 Test suite completed!")
    print(f"💡 Run individual tests with:")
    print(f"   python3 ../groovy_ast_diff.py <before> <after> --output results.json")

if __name__ == "__main__":
    main()
