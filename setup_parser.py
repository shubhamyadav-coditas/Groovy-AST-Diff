#!/usr/bin/env python3
"""
Script to clone and build the tree-sitter-groovy parser.
"""
import os
import subprocess
import sys
from pathlib import Path

try:
    from tree_sitter import Language
except ImportError:
    print("Error: tree-sitter package not found. Please install it first:")
    print("pip install tree-sitter==0.21.3")
    sys.exit(1)

def run_command(cmd, cwd=None):
    """Run a shell command and handle errors."""
    try:
        result = subprocess.run(cmd, shell=True, cwd=cwd, check=True, 
                              capture_output=True, text=True)
        print(f"✓ {cmd}")
        return result
    except subprocess.CalledProcessError as e:
        print(f"✗ {cmd}")
        print(f"Error: {e.stderr}")
        return None

def main():
    """Set up the tree-sitter-groovy parser."""
    print("Setting up Tree-sitter Groovy parser...")
    
    # Check if parser already exists
    build_dir = Path("build")
    groovy_so = build_dir / "groovy.so"
    
    if groovy_so.exists():
        print("✓ Tree-sitter Groovy parser already exists!")
        print(f"Library location: {groovy_so.absolute()}")
        return
    
    # Create parsers directory
    parsers_dir = Path("parsers")
    parsers_dir.mkdir(exist_ok=True)
    
    groovy_parser_dir = parsers_dir / "tree-sitter-groovy"
    
    # Clone the repository if it doesn't exist
    if not groovy_parser_dir.exists():
        print("Cloning tree-sitter-groovy repository...")
        result = run_command(
            "git clone https://github.com/murtaza64/tree-sitter-groovy.git",
            cwd=parsers_dir
        )
        if result is None:
            print("Failed to clone repository")
            sys.exit(1)
    else:
        print("Tree-sitter-groovy repository already exists, updating...")
        run_command("git pull", cwd=groovy_parser_dir)
    
    # Build the parser using tree-sitter Python library
    print("Building the Groovy parser...")
    
    # Check if we have the necessary files
    src_dir = groovy_parser_dir / "src"
    if not src_dir.exists():
        print("Error: src directory not found in the repository")
        sys.exit(1)
    
    parser_c = src_dir / "parser.c"
    if not parser_c.exists():
        print("Error: parser.c not found. The repository may not be complete.")
        print("Trying to use an alternative repository...")
        
        # Remove the incomplete repository
        import shutil
        shutil.rmtree(groovy_parser_dir)
        
        # Try alternative repository
        result = run_command(
            "git clone https://github.com/Decodetalkers/tree-sitter-groovy.git",
            cwd=parsers_dir
        )
        if result is None:
            print("Failed to clone alternative repository")
            sys.exit(1)
    
    # Build the language library
    try:
        print("Building language library...")
        build_dir = Path("build")
        build_dir.mkdir(exist_ok=True)
        
        Language.build_library(
            str(build_dir / "groovy.so"),
            [str(groovy_parser_dir)]
        )
        print("✓ Tree-sitter Groovy parser built successfully!")
        print(f"Parser location: {groovy_parser_dir.absolute()}")
        print(f"Library location: {(build_dir / 'groovy.so').absolute()}")
        
    except Exception as e:
        print(f"Error building parser: {e}")
        print("This might be due to missing C compiler or incomplete repository.")
        print("\nTrying to copy from original project...")
        
        # Try to copy from the original project if it exists
        original_build = Path("../Groovy-AST-Diff/build/groovy.so")
        if original_build.exists():
            import shutil
            shutil.copy2(original_build, build_dir / "groovy.so")
            print("✓ Copied parser from original project!")
            print(f"Library location: {(build_dir / 'groovy.so').absolute()}")
        else:
            print("Could not build or find Groovy parser.")
            print("Please ensure you have a C compiler installed (gcc/clang).")
            sys.exit(1)

if __name__ == "__main__":
    main()
