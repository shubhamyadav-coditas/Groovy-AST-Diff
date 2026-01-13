#!/usr/bin/env python3
"""
Modified script to clone, replace grammar.js with custom version, and build the tree-sitter-groovy parser.
This script uses the custom grammar.js from the project root to support Java-style for loops.
"""
import os
import subprocess
import sys
import shutil
from pathlib import Path

try:
    from tree_sitter import Language
except ImportError:
    print("Error: tree-sitter package not found. Please install it first:")
    print("pip install tree-sitter==0.21.3")
    sys.exit(1)

def run_command(cmd, cwd=None, check=True):
    """Run a shell command and handle errors."""
    try:
        result = subprocess.run(cmd, shell=True, cwd=cwd, check=check, 
                              capture_output=True, text=True)
        if result.returncode == 0:
            print(f"✓ {cmd}")
        else:
            print(f"⚠ {cmd} (exit code: {result.returncode})")
        return result
    except subprocess.CalledProcessError as e:
        print(f"✗ {cmd}")
        print(f"Error: {e.stderr}")
        return None

def install_tree_sitter_cli():
    """Install tree-sitter CLI if not available."""
    print("Checking for tree-sitter CLI...")
    
    # Check if already installed
    result = run_command("tree-sitter --version", check=False)
    if result and result.returncode == 0:
        print(f"✓ tree-sitter CLI found: {result.stdout.strip()}")
        return True
    
    print("tree-sitter CLI not found. Installing...")
    
    # Try npm first
    npm_check = run_command("npm --version", check=False)
    if npm_check and npm_check.returncode == 0:
        print("Installing tree-sitter CLI via npm...")
        result = run_command("npm install -g tree-sitter-cli", check=False)
        if result and result.returncode == 0:
            print("✓ tree-sitter CLI installed via npm")
            return True
    
    # Try cargo if npm fails
    cargo_check = run_command("cargo --version", check=False)
    if cargo_check and cargo_check.returncode == 0:
        print("Installing tree-sitter CLI via cargo...")
        result = run_command("cargo install tree-sitter-cli", check=False)
        if result and result.returncode == 0:
            print("✓ tree-sitter CLI installed via cargo")
            return True
    
    print("❌ Could not install tree-sitter CLI")
    print("Please install Node.js/npm or Rust/cargo first, then run:")
    print("  npm install -g tree-sitter-cli")
    print("  OR")
    print("  cargo install tree-sitter-cli")
    return False

def replace_grammar_with_custom(groovy_parser_dir):
    """Replace the default grammar.js with our custom grammar.js."""
    print("=== Replacing Grammar with Custom Version ===")
    
    # Path to our custom grammar.js in project root
    custom_grammar_file = Path("grammar.js")
    
    if not custom_grammar_file.exists():
        print(f"❌ Custom grammar file not found: {custom_grammar_file.absolute()}")
        print("Please ensure grammar.js exists in the project root directory.")
        return False
    
    # Path to the grammar.js in the cloned repository
    repo_grammar_file = groovy_parser_dir / "grammar.js"
    repo_grammar_backup = groovy_parser_dir / "grammar.js.original"
    
    # Backup original grammar if not already backed up
    if not repo_grammar_backup.exists():
        shutil.copy2(repo_grammar_file, repo_grammar_backup)
        print("✓ Backed up original grammar.js")
    
    # Replace with our custom grammar
    shutil.copy2(custom_grammar_file, repo_grammar_file)
    print(f"✓ Replaced grammar.js with custom version from {custom_grammar_file.absolute()}")
    
    return True

def generate_parser_from_grammar(groovy_parser_dir):
    """Generate parser.c from the custom grammar.js using tree-sitter CLI."""
    print("=== Generating Parser from Custom Grammar ===")
    
    # Install tree-sitter CLI if needed
    if not install_tree_sitter_cli():
        print("❌ Cannot generate parser without tree-sitter CLI")
        return False
    
    # Generate parser
    print("Generating parser.c from custom grammar.js...")
    result = run_command("tree-sitter generate --no-bindings", cwd=groovy_parser_dir)
    
    if not result or result.returncode != 0:
        print("❌ Failed to generate parser")
        print("Error output:")
        if result and result.stderr:
            print(result.stderr)
        return False
    
    # Verify parser.c was generated
    parser_c = groovy_parser_dir / "src" / "parser.c"
    if not parser_c.exists():
        print("❌ parser.c was not generated")
        return False
    
    print("✓ parser.c generated successfully from custom grammar")
    return True

def test_custom_parser():
    """Test the custom parser with Java-style syntax."""
    print("\n=== Testing Custom Parser ===")
    
    try:
        # Load the parser
        build_dir = Path("build")
        groovy_so = build_dir / "groovy.so"
        
        if not groovy_so.exists():
            print("❌ groovy.so not found")
            return False
        
        GROOVY_LANGUAGE = Language(str(groovy_so), 'groovy')
        from tree_sitter import Parser
        parser = Parser()
        parser.set_language(GROOVY_LANGUAGE)
        
        # Test cases
        test_cases = [
            {
                "name": "Java-style for loop (DataObject : collection)",
                "code": "for(DataObject obj : objects) { println(obj) }"
            },
            {
                "name": "Groovy-style for loop (in keyword)",
                "code": "for(obj in objects) { println(obj) }"
            },
            {
                "name": "Original problematic syntax",
                "code": "for(DataObject dataObject : dataObjects) { temp += dataObject.resourcename }"
            },
            {
                "name": "Complex Java-style for loop",
                "code": """for(DataObject dataObject : dataObjects) {
    temp += dataObject.resourcename + delimiter
    temp += dataObject.resourceband + delimiter
}"""
            }
        ]
        
        all_passed = True
        for test_case in test_cases:
            print(f"\nTesting: {test_case['name']}")
            
            tree = parser.parse(bytes(test_case['code'], 'utf8'))
            
            if tree.root_node.has_error:
                print("❌ Parse error")
                all_passed = False
                def find_errors(node):
                    if node.type == 'ERROR':
                        print(f"  ERROR: {node.text}")
                    for child in node.children:
                        find_errors(child)
                find_errors(tree.root_node)
            else:
                print("✅ Parsed successfully")
        
        if all_passed:
            print("\n🎉 All tests passed! Java-style for loops are now supported.")
            return True
        else:
            print("\n⚠️  Some tests failed. Check the grammar modifications.")
            return False
        
    except Exception as e:
        print(f"❌ Error testing parser: {e}")
        return False

def main():
    """Set up the tree-sitter-groovy parser with custom grammar."""
    print("=== Tree-sitter Groovy Parser Setup with Custom Grammar ===")
    
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
        if not result or result.returncode != 0:
            print("Failed to clone repository")
            sys.exit(1)
    else:
        print("Tree-sitter-groovy repository already exists")
        print("Updating repository...")
        run_command("git pull", cwd=groovy_parser_dir)
    
    # Replace grammar.js with our custom version
    if not replace_grammar_with_custom(groovy_parser_dir):
        print("❌ Failed to replace grammar with custom version")
        sys.exit(1)
    
    # Generate parser.c from the custom grammar
    if not generate_parser_from_grammar(groovy_parser_dir):
        print("❌ Failed to generate parser from custom grammar")
        sys.exit(1)
    
    # Check if we have the necessary files
    src_dir = groovy_parser_dir / "src"
    if not src_dir.exists():
        print("Error: src directory not found in the repository")
        sys.exit(1)
    
    parser_c = src_dir / "parser.c"
    if not parser_c.exists():
        print("Error: parser.c not found after generation")
        sys.exit(1)
    
    # Build the language library
    print("=== Building Language Library ===")
    try:
        build_dir = Path("build")
        build_dir.mkdir(exist_ok=True)
        
        # Remove old library if it exists
        groovy_so = build_dir / "groovy.so"
        if groovy_so.exists():
            groovy_so.unlink()
            print("✓ Removed old groovy.so")
        
        Language.build_library(
            str(build_dir / "groovy.so"),
            [str(groovy_parser_dir)]
        )
        print("✓ Tree-sitter Groovy parser built successfully!")
        print(f"Parser location: {groovy_parser_dir.absolute()}")
        print(f"Library location: {(build_dir / 'groovy.so').absolute()}")
        
        # Test the parser
        if test_custom_parser():
            print("\n✅ Setup completed successfully! Your custom grammar is working.")
        else:
            print("\n⚠️  Setup completed but some tests failed.")
        
    except Exception as e:
        print(f"Error building parser: {e}")
        print("This might be due to missing C compiler or incomplete repository.")
        
        # Try to copy from original project if it exists
        original_build = Path("../Groovy-AST-Diff/build/groovy.so")
        if original_build.exists():
            shutil.copy2(original_build, build_dir / "groovy.so")
            print("✓ Copied parser from original project!")
            print(f"Library location: {(build_dir / 'groovy.so').absolute()}")
            print("⚠️  Note: This uses the original grammar, not your custom modifications.")
        else:
            print("Could not build or find Groovy parser.")
            print("Please ensure you have a C compiler installed (gcc/clang).")
            sys.exit(1)

if __name__ == "__main__":
    main()