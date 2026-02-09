# Imports/Dependencies Metric in Groovy

## Overview

The imports/dependencies metric measures the number of import statements in Groovy code. Import statements declare explicit dependencies on external packages and classes. A higher import count indicates more external coupling, which can affect maintainability, testability, and portability.

## What We Measure

This metric counts import statements with weighted scoring:

| Import Type | Weight | Example |
|-------------|--------|---------|
| Regular Import | 1 | `import java.util.ArrayList` |
| Static Import | 1 | `import static java.lang.Math.PI` |
| Star Import (Wildcard) | 2 | `import java.util.*` |

### Formula

```
Import Score = Regular Imports + Static Imports + (Star Imports × 2)
```

## Import Types

### 1. Regular Imports

Standard imports that bring in a specific class.

```groovy
import java.util.ArrayList           // +1
import org.apache.commons.lang3.StringUtils  // +1
import com.company.service.UserService       // +1
```

### 2. Static Imports

Imports that bring in static members (methods or fields) from a class.

```groovy
import static java.lang.Math.PI              // +1
import static java.lang.Math.abs             // +1
import static java.util.Collections.sort     // +1
```

### 3. Star Imports (Wildcard)

Star imports bring in all classes from a package. They are weighted as **+2** because:
- They import potentially many classes
- Dependencies are less explicit
- Can cause naming conflicts
- Harder to track actual dependencies

```groovy
import java.util.*                           // +2
import org.springframework.beans.factory.annotation.*  // +2
import com.company.model.*                   // +2
```

## Examples with Calculations

### Example 1: No Imports

```groovy
class Calculator {
    int add(int a, int b) {
        return a + b
    }
}
```

**Calculation:**
- Regular imports: 0
- Static imports: 0
- Star imports: 0
- **Total: 0**

### Example 2: Regular Imports Only

```groovy
import java.util.ArrayList
import java.util.HashMap
import org.apache.commons.lang3.StringUtils

class DataProcessor {
    def process(String input) {
        return StringUtils.trim(input)
    }
}
```

**Calculation:**
- Regular imports: 3
- Static imports: 0
- Star imports: 0
- **Total: 3**

### Example 3: Mixed Import Types

```groovy
import java.util.ArrayList
import java.util.HashMap
import static java.lang.Math.PI
import static java.lang.Math.abs
import org.springframework.beans.factory.annotation.*

class MathService {
    double calculate(double value) {
        return abs(value) * PI
    }
}
```

**Calculation:**
- Regular imports: 2 (ArrayList, HashMap)
- Static imports: 2 (PI, abs)
- Star imports: 1 × 2 = 2 (Spring annotations)
- **Total: 2 + 2 + 2 = 6**

### Example 4: Heavy Star Imports

```groovy
import java.util.*
import org.springframework.*
import com.company.model.*
import com.company.service.*
import com.company.repository.*

class HeavilyDependentService {
    // ...
}
```

**Calculation:**
- Regular imports: 0
- Static imports: 0
- Star imports: 5 × 2 = 10
- **Total: 10**

## Why This Metric Matters

### 1. **Coupling Awareness**
- High import counts indicate tight coupling to external systems
- Changes in dependencies may require code changes

### 2. **Testability**
- More imports often means more dependencies to mock
- Increases test complexity and setup

### 3. **Portability**
- Code with many imports is harder to move between projects
- May require dependency resolution in new environments

### 4. **Build Time**
- More dependencies = longer compile times
- Larger deployment artifacts

### 5. **Security Considerations**
- Each external dependency is a potential security risk
- Dependency vulnerabilities affect your code

### 6. **Maintenance Burden**
- External APIs can change
- Version conflicts between dependencies
- Deprecated APIs need attention

## Best Practices

### 1. **Avoid Star Imports**
```groovy
// Avoid - unclear what's being used
import org.springframework.*

// Better - explicit dependencies
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
```

### 2. **Remove Unused Imports**
- Clean up imports regularly
- Use IDE tools to organize imports

### 3. **Group Related Imports**
```groovy
// Java standard library
import java.util.List
import java.util.Map

// Third-party libraries
import org.apache.commons.lang3.StringUtils

// Project imports
import com.company.model.User
```

### 4. **Prefer Specific Static Imports**
```groovy
// Avoid
import static java.lang.Math.*

// Better
import static java.lang.Math.PI
import static java.lang.Math.abs
```

## Impact on Change Complexity

When analyzing code changes:

1. **Adding imports** increases dependency complexity
2. **Adding star imports** has double impact (+2 vs +1)
3. **Removing imports** decreases complexity (negative delta)
4. **Converting star imports to specific imports** may increase count but improves clarity

## Scoring Guidelines

| Score Range | Dependency Level | Description |
|-------------|-----------------|-------------|
| 0-2 | Low | Minimal external dependencies |
| 3-5 | Medium | Moderate number of imports |
| 6-10 | High | Significant external coupling |
| 11+ | Very High | Heavy dependency on externals |

## Summary

The imports metric provides insight into:
- How many external packages the code depends on
- The explicitness of dependencies (star vs specific)
- Potential maintenance and testing overhead

Lower scores indicate more self-contained code with fewer external dependencies.