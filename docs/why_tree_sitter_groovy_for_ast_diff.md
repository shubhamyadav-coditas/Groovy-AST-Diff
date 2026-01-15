# 🌳 Why Tree-sitter-Groovy for AST Diff?

## 📋 Overview

This document explains **why tree-sitter-groovy was chosen** over other Groovy parsing solutions for converting Groovy source code into an AST and performing **structural diff analysis**.

The primary goal is **accurate, fine-grained, and performant structural comparison**, not compilation or execution.

---

## 🔍 Key Requirements for Groovy AST Diff

To reliably diff Groovy source code, the parser must:

| 🎯 Requirement | 📝 Description |
|---------------|----------------|
| 🏗️ **Preserve exact source structure** | Not just semantics, but original formatting |
| 📍 **Retain source locations** | Lines, columns for precise change tracking |
| 🎨 **Handle Groovy-specific syntax** | Closures, DSLs, traits, meta-programming |
| ⚡ **Support incremental and fast parsing** | Real-time analysis capabilities |
| 🔧 **Be extensible** | Handle grammar gaps and custom DSLs |
| 🐍 **Integrate cleanly with Python-based tooling** | Seamless Python integration |

---

## 🔍 Parser Comparison (Groovy Ecosystem)

| 🏷️ Parser / Library | 🧩 Type | ✅ Pros | ❌ Cons | 🎯 AST Diff Suitability |
|---------------------|---------|---------|---------|-------------------------|
| **🌳 tree-sitter-groovy** | CST | • Concrete Syntax Tree (CST)<br>• Precise source locations<br>• Incremental parsing<br>• Grammar is extensible<br>• Matches JS tree-sitter architecture | • PyPI package is WIP<br>• Requires custom build | ⭐⭐⭐⭐⭐ **Best choice** |
| **🧩 ANTLR4 Groovy Grammar** | AST | • Official Groovy grammar<br>• Very accurate parsing<br>• Complete language coverage | • Heavy runtime<br>• No CST (loses formatting)<br>• Hard Python integration | ⭐⭐⭐ |
| **⚙️ Groovy Compiler AST** | AST | • 100% language correctness<br>• Semantic-rich AST | • Requires JVM<br>• Loses original formatting<br>• Not diff-friendly | ⭐⭐ |
| **🎨 Pygments (Lexer)** | Tokens | • Easy to use<br>• Fast | • No AST<br>• No structure<br>• Not usable for diffs | ⭐ |
| **🧪 Regex-based Parsing** | Custom | • Full control | • Extremely brittle<br>• Impossible for Groovy grammar | ⭐ |

---

## 🏆 Why Tree-sitter-Groovy Wins

### 1️⃣ Concrete Syntax Tree (CST) Advantage 🧠

Tree-sitter produces a **Concrete Syntax Tree**, not just an AST:

| 🏗️ Preserves | 🎯 Enables |
|-------------|------------|
| 🔤 Whitespace | ✅ Accurate `added / deleted / moved / modified` detection |
| 💬 Comments | 📊 Line-level and block-level diffing |
| 📍 Exact token boundaries | 🔍 Precise change location tracking |

> 💡 **Key Insight**: ASTs from compilers often discard information that is **critical for diff tools**.

---

### 2️⃣ Designed for Structural Diffing 🔀

| 🎯 Tree-sitter is optimized for | 🔧 This aligns perfectly with |
|--------------------------------|------------------------------|
| 🏗️ Structural analysis | 📊 Code comparison |
| ⚡ Incremental parsing | 🔄 Refactoring detection |
| 🎯 Editor-grade precision | 🏷️ Change classification (`moved`, `moved_modified`, etc.) |

---

### 3️⃣ Grammar Extensibility 🔧

Groovy is **not a simple language**:

| 🎨 Groovy Complexity | 🔧 Tree-sitter Solution |
|---------------------|------------------------|
| 🔒 DSL-style closures | ➕ Adding missing constructs (constructors, traits, enums) |
| 🔗 Method chaining | ⚖️ Resolving grammar conflicts explicitly |
| 🎭 Meta-programming | 🎯 Supporting **custom DSLs** (e.g. Emery DSL) |
| 🎪 Optional typing | 🛠️ Grammar modification capabilities |

This is **not realistically achievable** with compiled ASTs or lexers.

---

### 4️⃣ Consistency with JavaScript AST Diff ♻️

Our JavaScript AST Diff already uses:

```javascript
tree-sitter-javascript
```

Using tree-sitter-groovy provides:

| 🎯 Benefit | 📝 Description |
|-----------|----------------|
| 🏗️ **Identical parsing architecture** | Same underlying technology |
| 🔄 **Reusable diff logic** | Shared algorithms and patterns |
| 🧩 **Unified block classification model** | Consistent change detection |
| 💰 **Lower long-term maintenance cost** | Single technology stack |

---

### 5️⃣ Performance & Scalability ⚡

| 📊 Aspect | 🌳 tree-sitter-groovy | 🧩 ANTLR4 | ⚙️ Groovy Compiler |
|-----------|----------------------|-----------|-------------------|
| ⚡ **Parsing speed** | Very fast | Moderate | Slow |
| 🔄 **Incremental updates** | ✅ Supported | ❌ Not supported | ❌ Not supported |
| 💾 **Memory footprint** | Low | High | Very high |
| 📁 **Large file handling** | ✅ Stable | ⚠️ Variable | ❌ Resource intensive |

ANTLR and JVM-based solutions are overkill for diffing and slow in comparison.

---

## ❌ Why Not the Groovy Compiler AST?

Although powerful, it is not suitable for diff tools:

| 🚫 Problem | 📝 Impact on Diff Analysis |
|-----------|---------------------------|
| 🎨 **Loses formatting & comments** | Cannot detect formatting-only changes |
| 🧠 **AST is semantic, not structural** | Misses structural reorganization |
| ☕ **Requires JVM + Groovy runtime** | Heavy dependency and setup complexity |
| 🔍 **Cannot accurately detect** | Reordering, formatting changes, DSL structure |

---

## ✅ Final Decision Summary

| 🎯 Criteria | 🏆 Best Option |
|------------|---------------|
| 📊 **Structural diff accuracy** | 🌳 tree-sitter-groovy |
| 🏗️ **CST support** | 🌳 tree-sitter-groovy |
| ⚡ **Performance** | 🌳 tree-sitter-groovy |
| 🔧 **Grammar extensibility** | 🌳 tree-sitter-groovy |
| ♻️ **JS/Groovy parity** | 🌳 tree-sitter-groovy |
| 🐍 **Python integration** | 🌳 tree-sitter-groovy (custom build) |

---

## 🎯 Conclusion

**tree-sitter-groovy** is the most practical and technically sound choice for parsing Groovy source code into a structure suitable for AST/CST-based diffing.

While it requires custom setup today, it uniquely provides:

| ✨ Key Advantage | 💡 Benefit |
|-----------------|-----------|
| 🎯 **Structural precision** | Accurate change detection |
| ⚡ **Performance** | Fast, real-time analysis |
| 🔧 **Extensibility** | Custom DSL support |
| 🏗️ **Architectural consistency** | Unified with JavaScript implementation |

—all of which are **non-negotiable** for a high-quality Groovy AST Diff system.
