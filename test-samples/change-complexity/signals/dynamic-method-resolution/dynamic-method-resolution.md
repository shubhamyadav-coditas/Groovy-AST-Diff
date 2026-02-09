# Dynamic Method Resolution Signal

## Overview

The **Dynamic Method Resolution** signal detects patterns in Groovy code where method calls or property accesses are resolved at runtime rather than at compile time. These patterns indicate code that cannot be fully analyzed statically, making it harder to predict behavior, trace execution, and safely review changes.

## Why It Matters for Change Complexity

Dynamic method resolution is a powerful Groovy feature, but it introduces significant complexity:

1. **Unpredictable Behavior**: Methods resolved at runtime can behave differently based on the state of the program, making it difficult to reason about what code will execute.

2. **Hidden Dependencies**: Dynamic method calls can invoke methods that aren't visible in the source code, creating implicit dependencies.

3. **Testing Challenges**: Code paths involving dynamic resolution are harder to test exhaustively because the set of possible method calls isn't known at compile time.

4. **Debugging Difficulty**: When issues occur in dynamically-resolved code, stack traces may not clearly show the method invocation chain.

5. **Refactoring Risk**: Renaming or removing methods may silently break dynamic invocations that aren't caught by the compiler.

## Signal Score

- **Default Score**: +10 points per occurrence
- This is an additive score applied to the block's overall change complexity

## Detection Patterns

The detector looks for the following dynamic method resolution patterns:

### 1. Method Override Patterns

These are method definitions that intercept or override normal method dispatch:

| Method Name | Purpose |
|-------------|---------|
| `invokeMethod` | Intercepts all method calls on an object |
| `methodMissing` | Handles calls to undefined methods |
| `propertyMissing` | Handles access to undefined properties |
| `getProperty` | Intercepts all property reads |
| `setProperty` | Intercepts all property writes |

### 2. Operator Overloading

| Method Name | Purpose |
|-------------|---------|
| `getAt` | Overloads the `[]` subscript operator for reading |
| `putAt` | Overloads the `[]` subscript operator for writing |

### 3. Dynamic Dispatch Helpers

| Method Name | Purpose |
|-------------|---------|
| `respondsTo` | Checks if an object responds to a method |
| `hasProperty` | Checks if an object has a property |
| `getMetaClass` | Accesses the runtime metaClass |

## AST Detection Logic

The detector traverses the AST looking for two types of patterns:

### Pattern 1: Method Definitions

```groovy
// AST node types: method_definition, function_definition, method_declaration
// Looks for identifier child matching DYNAMIC_METHOD_NAMES
def invokeMethod(String name, Object args) {
    // This triggers the signal
}
```

### Pattern 2: Method Calls

```groovy
// AST node types: method_call, function_call, juxt_function_call
// Checks if node text contains any DYNAMIC_METHOD_NAMES
if (obj.respondsTo("methodName")) {
    // This triggers the signal
}
```

## Implementation

The detector is implemented in `DynamicMethodSignalDetector` class:

```python
class DynamicMethodSignalDetector(BaseSignalDetector):
    DYNAMIC_METHOD_NAMES = {
        'invokeMethod', 'methodMissing', 'propertyMissing',
        'getProperty', 'setProperty', 'getAt', 'putAt',
        'respondsTo', 'hasProperty', 'getMetaClass',
    }
    
    def detect(self, root_node, source_bytes, **kwargs):
        count = 0
        
        def traverse(node):
            nonlocal count
            
            # Check for method definitions
            if node.type in ['method_definition', 'function_definition', 'method_declaration']:
                for child in node.children:
                    if child.type == 'identifier':
                        method_name = self._get_node_text(child, source_bytes)
                        if method_name in self.DYNAMIC_METHOD_NAMES:
                            count += 1
                        break
            
            # Check for method calls
            if node.type in ['method_call', 'function_call', 'juxt_function_call']:
                text = self._get_node_text(node, source_bytes)
                for dynamic_method in self.DYNAMIC_METHOD_NAMES:
                    if dynamic_method in text:
                        count += 1
                        break
            
            for child in node.children:
                traverse(child)
        
        traverse(root_node)
        return count
```

## Code Examples

### Example 1: invokeMethod Override

```groovy
// BEFORE: No interception
class Handler {
    void process(String data) {
        println data
    }
}

// AFTER: Added invokeMethod (Signal Count: 1)
class Handler {
    void process(String data) {
        println data
    }
    
    def invokeMethod(String name, Object args) {
        println "Intercepted: ${name}"
        return this.metaClass.invokeMethod(this, name, args)
    }
}
```

### Example 2: methodMissing for Builder Pattern

```groovy
// BEFORE: Explicit setters only
class Builder {
    void setName(String n) { name = n }
    void setAge(int a) { age = a }
}

// AFTER: Dynamic builder (Signal Count: 1)
class Builder {
    Map props = [:]
    
    def methodMissing(String name, Object args) {
        if (name.startsWith('set')) {
            props[name[3..-1].toLowerCase()] = args[0]
            return this
        }
        throw new MissingMethodException(name, this.class, args)
    }
}
```

### Example 3: getAt/putAt for Custom Collections

```groovy
// AFTER: Custom subscript operators (Signal Count: 2)
class MyList {
    private List items = []
    
    def getAt(int index) {
        return items[index < 0 ? items.size() + index : index]
    }
    
    void putAt(int index, Object value) {
        items[index] = value
    }
}
```

### Example 4: respondsTo for Dynamic Dispatch

```groovy
// AFTER: Dynamic method dispatch (Signal Count: 1)
class Dispatcher {
    void dispatch(String methodName, Object arg) {
        if (this.respondsTo(methodName)) {
            this."${methodName}"(arg)
        }
    }
}
```

## Impact on Change Complexity

When dynamic method resolution is introduced or modified in a changed block:

1. **Added Blocks**: The signal count from the target code is added to the block's signal score
2. **Modified Blocks**: The signal count from the target code is analyzed (not the source)
3. **Deleted Blocks**: The signal count from the source code is analyzed to understand what was removed
4. **Moved Blocks**: Signal count from target is analyzed (position change doesn't affect signals)

### Score Calculation

```
Signal Score = Count × Default Score
             = Count × 10.0
```

For example, if a block adds 3 dynamic method resolution patterns:
- Signal Count: 3
- Signal Score: 3 × 10.0 = 30.0 points added to complexity

## Best Practices

1. **Prefer Static Methods**: When possible, use explicit method definitions instead of dynamic resolution
2. **Document Dynamic Behavior**: Clearly document what methods/properties are handled dynamically
3. **Limit Scope**: Restrict dynamic method handling to specific, well-defined use cases
4. **Use Type Hints**: Where possible, use `@TypeChecked` or `@CompileStatic` annotations to catch issues early
5. **Test Thoroughly**: Ensure comprehensive test coverage for all dynamic method paths

## Related Signals

- **MetaClass Usage**: Often used together with dynamic method resolution
- **Closure Mutable Capture**: Can be combined with dynamic methods for complex patterns
- **Pipeline Steps**: Jenkins pipeline code often uses dynamic method resolution

## See Also

- [Groovy MetaProgramming Guide](https://groovy-lang.org/metaprogramming.html)
- [methodMissing and propertyMissing](https://groovy-lang.org/metaprogramming.html#_methodmissing)
- [Operator Overloading](https://groovy-lang.org/operators.html#Operator-Overloading)
