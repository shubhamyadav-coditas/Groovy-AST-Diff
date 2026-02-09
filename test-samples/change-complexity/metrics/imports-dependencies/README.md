# Import/Dependencies Test Samples

This folder contains test samples for the imports metric calculation.

## Test Cases

### 1. `no_imports` - No imports to adding imports
- **Before**: No imports (score: 0)
- **After**: 2 regular imports (score: 2)
- **Delta**: +2

### 2. `star_imports` - Regular imports to star imports
- **Before**: 2 regular imports (score: 2)
- **After**: 3 star imports (score: 6) - star imports count as 2 each
- **Delta**: +4

### 3. `static_imports` - No imports to static imports
- **Before**: No imports (score: 0)
- **After**: 4 static imports (score: 4)
- **Delta**: +4

### 4. `heavy_dependencies` - Light to heavy import usage
- **Before**: 1 regular import (score: 1)
- **After**: 9 regular imports + 1 star import (score: 11)
- **Delta**: +10

## Scoring Formula

```
Import Score = Regular Imports + Static Imports + (Star Imports × 2)
```

Star imports (wildcard) are weighted at 2x because they:
- Import potentially many classes
- Make dependencies less explicit
- Can cause naming conflicts
