#!/usr/bin/env python3
"""
Groovy AST Diff Tool - Main Entry Point

A Python application that compares two Groovy source files by analyzing their
Abstract Syntax Trees (ASTs) using Tree-sitter.
"""

import sys
from pathlib import Path

# Add src directory to path
sys.path.insert(0, str(Path(__file__).parent / "src"))

from src.groovy_ast_diff import main

if __name__ == "__main__":
    main()