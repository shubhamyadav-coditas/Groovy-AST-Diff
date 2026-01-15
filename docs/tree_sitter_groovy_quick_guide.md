# 🚀 Tree-sitter Groovy Quick Implementation Guide

## 📋 What This Is

A concise guide to our tree-sitter-groovy implementation for the Groovy AST Diff project. This covers the essential setup, customizations, and usage without the lengthy technical details.

---

## 🎯 Why Tree-sitter Groovy?

| ✅ Pros | ❌ Challenges |
|---------|---------------|
| 🔄 Consistent with JavaScript implementation | ⚠️ PyPI package is work-in-progress |
| 🌳 High-quality AST with source locations | 🔧 Requires custom build process |
| ⚡ Fast parsing performance | 📝 Grammar modifications needed |
| 🎨 Extensible for custom syntax (Emery DSL) | 🛠️ Manual setup required |

**Decision**: Best option despite setup complexity - provides superior AST quality and consistency.

---

## 🔧 Quick Setup

### 1️⃣ One-Command Setup
```bash
python setup_parser.py
```

This automatically:
- 📥 Clones tree-sitter-groovy from GitHub
- 🔄 Replaces grammar with our custom version
- 🏗️ Builds the parser library
- ✅ Tests the installation

### 2️⃣ What Gets Built
```
build/groovy.so                    # ← The parser library you need
parsers/tree-sitter-groovy/        # ← Source repository
grammar.js                         # ← Our custom grammar
```

---

## 🎨 Grammar Customizations

### What We Added

| 🏷️ Feature | 📝 Why Needed | ✅ Result |
|------------|---------------|-----------|
| 🏗️ **Constructor Support** | Original grammar couldn't parse constructors | `MyClass(param) { }` now works |
| 📋 **Enhanced Enums** | Limited enum definition support | `enum Status { ACTIVE }` fully supported |
| 🧬 **Trait Definitions** | Traits weren't supported | `trait Auditable { }` now recognized |
| ⚡ **Conflict Resolution** | Constructor vs method ambiguity | Clear disambiguation rules |

### Before vs After

**❌ Before (Original Grammar):**
```groovy
// Constructor parsed as generic expression
MyClass(param) { }  // ← Not recognized as constructor
```

**✅ After (Custom Grammar):**
```groovy
// Constructor properly identified
MyClass(param) { }  // ← Parsed as constructor_definition
```

---

## 🚨 Common Issues & Solutions

### Build Failures
```bash
# Missing compiler
sudo apt-get install build-essential  # Ubuntu
xcode-select --install                 # macOS

# Missing tree-sitter CLI
npm install -g tree-sitter-cli
```

### Parser Not Found
```bash
# Run setup again
python setup_parser.py

# Check if library exists
ls -la build/groovy.so
```

---

## 📊 Performance

| File Size | Parse Time | Memory |
|-----------|------------|--------|
| < 1KB | ~1-5ms | ~2MB |
| 1-10KB | ~5-50ms | ~3MB |
| 10-100KB | ~50-500ms | ~5MB |

**Accuracy**: 95-99% for standard Groovy syntax

---

## 🔄 Usage in Code

```python
# Setup (one-time)
from groovy_ast_diff import GroovyASTDiff

# Use
service = GroovyASTDiff()
result = service.compare_files("old.groovy", "new.groovy")
```

The parser is automatically loaded from `build/groovy.so`.

---

## 🎯 Supported Groovy Features

| ✅ Fully Supported | ⚠️ Limited Support |
|-------------------|-------------------|
| 🏛️ Classes, Interfaces, Traits | 🎭 Dynamic meta-programming |
| ⚙️ Methods, Constructors | 🔗 Cross-file analysis |
| 📋 Enums with methods | 🔄 Runtime-generated code |
| 🔒 Closures & Collections | 🏗️ Some Gradle DSL patterns |
| 🎯 Emery DSL constructs | |

---

## 🔮 Future Plans

### Short-term
- 📊 Monitor PyPI package for stable release
- 🧪 Expand test coverage for edge cases
- ⚡ Performance optimization

### Long-term
- 🔄 Migrate to official PyPI package when stable
- 🤝 Contribute improvements back to community
- 🔍 Evaluate alternatives if maintenance becomes complex

---

## 🆘 Quick Troubleshooting

| 🚨 Problem | 💡 Solution |
|-----------|-------------|
| `groovy.so not found` | Run `python setup_parser.py` |
| `tree-sitter command not found` | Install: `npm install -g tree-sitter-cli` |
| Build fails | Install build tools (see above) |
| Parsing errors | Check if file is valid Groovy syntax |

---

## 📚 Key Files

| 📁 File | 🎯 Purpose |
|---------|------------|
| `setup_parser.py` | Automated setup script |
| `grammar.js` | Our custom grammar rules |
| `build/groovy.so` | Compiled parser library |
| `groovy_ast_diff.py` | Main parser integration |

---

**💡 TL;DR**: Run `python setup_parser.py` once, then use the parser normally. Our custom grammar adds constructor, enum, and trait support that the original lacked.

---

*For detailed technical information, see the full [Tree-sitter Groovy Implementation Guide](tree_sitter_groovy_implementation_guide.md).*
