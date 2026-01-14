# Four-Phase Matching Strategy

## Overview

The Four-Phase Matching Strategy is a sophisticated algorithm used in the Groovy AST Diff implementation to accurately compare and match code blocks between two versions of a file. This multi-phase approach ensures high accuracy in identifying changes while minimizing false positives and negatives.

## The Four Phases Explained

### Phase 1: Identifier Matching (Exact Name Match)

**Purpose**: Match blocks that have the same type and identifier (name).

**Logic**: 
- Compare blocks with identical `node_type` AND `identifier`
- This catches renamed methods, moved functions, and unchanged elements

**Example**:
```groovy
// File A
class Calculator {
    def add(a, b) { return a + b }
    def subtract(a, b) { return a - b }
}

// File B  
class Calculator {
    def add(a, b) { return a + b + 1 }  // Modified implementation
    def multiply(a, b) { return a * b }  // New method
}
```

**Phase 1 Results**:
- ✅ `add` method matches by identifier → Will be compared for content changes
- ❌ `subtract` method has no match in File B
- ❌ `multiply` method has no match in File A

---

### Phase 2: Content Hash Matching (MOVED Detection)

**Purpose**: Identify blocks that have identical content but may be in different positions.

**Logic**:
- Compare `content_hash` of unmatched blocks from Phase 1
- Identical hashes indicate the block was moved without modification

**Example**:
```groovy
// File A
class DataProcessor {
    def validateInput(data) {
        if (data == null) throw new IllegalArgumentException("Data cannot be null")
        return true
    }
    
    def processData(data) {
        validateInput(data)
        return data.transform()
    }
}

// File B
class DataProcessor {
    def processData(data) {
        validateInput(data)
        return data.transform()
    }
    
    def validateInput(data) {  // Same content, different position
        if (data == null) throw new IllegalArgumentException("Data cannot be null")
        return true
    }
}
```

**Phase 2 Results**:
- ✅ `validateInput` method matches by content hash → Classified as **MOVED**
- ✅ `processData` method matches by content hash → Classified as **UNCHANGED**

---

### Phase 3: Structural Similarity Matching (≥70% Similarity → MODIFIED)

**Purpose**: Identify blocks that have been modified but retain structural similarity.

**Logic**:
- Calculate similarity percentage between unmatched blocks
- Threshold: ≥70% similarity indicates modification
- Uses custom similarity algorithm (not Zhang-Shasha)

**Example**:
```groovy
// File A
def calculateTotal(items) {
    def total = 0
    for (item in items) {
        total += item.price
    }
    return total
}

// File B
def calculateTotal(items) {
    def total = 0
    def tax = 0.1
    for (item in items) {
        total += item.price * (1 + tax)  // Added tax calculation
    }
    return total
}
```

**Similarity Analysis**:
- Method signature: 100% match
- Variable declarations: 75% match (added `tax` variable)
- Loop structure: 90% match
- Return statement: 100% match
- **Overall similarity: ~85%** → Classified as **MODIFIED**

---

### Phase 4: Unmatched Processing (ADDED/DELETED)

**Purpose**: Handle blocks that couldn't be matched in previous phases.

**Logic**:
- Blocks remaining in File A → **DELETED**
- Blocks remaining in File B → **ADDED**

**Example**:
```groovy
// File A
class UserService {
    def createUser(userData) { /* implementation */ }
    def deleteUser(userId) { /* implementation */ }  // Will be DELETED
}

// File B
class UserService {
    def createUser(userData) { /* implementation */ }
    def updateUser(userId, userData) { /* implementation */ }  // Will be ADDED
    def getUserById(userId) { /* implementation */ }  // Will be ADDED
}
```

**Phase 4 Results**:
- 🗑️ `deleteUser` method → Classified as **DELETED**
- ➕ `updateUser` method → Classified as **ADDED**
- ➕ `getUserById` method → Classified as **ADDED**

## Complete Example Walkthrough

### Input Files

**File A (before.groovy)**:
```groovy
class OrderProcessor {
    def validateOrder(order) {
        return order != null && order.items.size() > 0
    }
    
    def calculateTotal(order) {
        def total = 0
        for (item in order.items) {
            total += item.price
        }
        return total
    }
    
    def processPayment(order, amount) {
        // Payment processing logic
        return paymentGateway.charge(amount)
    }
}
```

**File B (after.groovy)**:
```groovy
class OrderProcessor {
    def calculateTotal(order) {
        def total = 0
        def discount = 0.05
        for (item in order.items) {
            total += item.price * (1 - discount)  // Added discount
        }
        return total
    }
    
    def validateOrder(order) {  // Moved position
        return order != null && order.items.size() > 0
    }
    
    def sendConfirmation(order) {  // New method
        emailService.send(order.customerEmail, "Order confirmed")
    }
}
```

### Phase-by-Phase Analysis

#### Phase 1: Identifier Matching
- ✅ `validateOrder`: Found in both files → Match by identifier
- ✅ `calculateTotal`: Found in both files → Match by identifier
- ❌ `processPayment`: Only in File A
- ❌ `sendConfirmation`: Only in File B

#### Phase 2: Content Hash Matching
- ✅ `validateOrder`: Identical content → **MOVED** (different position)
- ❌ `calculateTotal`: Different content (discount added)
- ❌ `processPayment`: No match in File B
- ❌ `sendConfirmation`: No match in File A

#### Phase 3: Structural Similarity
- ✅ `calculateTotal`: 
  - Method signature: 100% match
  - Variable declarations: 66% match (added discount)
  - Loop structure: 85% match (modified calculation)
  - Return statement: 100% match
  - **Overall: ~78% similarity** → **MODIFIED**

#### Phase 4: Unmatched Processing
- 🗑️ `processPayment`: **DELETED**
- ➕ `sendConfirmation`: **ADDED**

### Final Results

```json
{
  "change_type": "modified",
  "block_type": "class",
  "identifier": "OrderProcessor",
  "statement_diffs": [
    {
      "change_type": "moved",
      "identifier": "validateOrder",
      "old_position": 1,
      "new_position": 2
    },
    {
      "change_type": "modified", 
      "identifier": "calculateTotal",
      "similarity_percentage": 78.5
    },
    {
      "change_type": "deleted",
      "identifier": "processPayment"
    },
    {
      "change_type": "added",
      "identifier": "sendConfirmation"
    }
  ]
}
```

## Benefits of the Four-Phase Strategy

### 1. **High Accuracy**
- Reduces false positives by using multiple matching criteria
- Handles complex scenarios like method reordering and refactoring

### 2. **Comprehensive Coverage**
- **Phase 1**: Catches straightforward modifications
- **Phase 2**: Detects code movement without changes
- **Phase 3**: Identifies substantial modifications
- **Phase 4**: Ensures no changes are missed

### 3. **Intelligent Classification**
- Distinguishes between MOVED, MODIFIED, ADDED, and DELETED
- Provides similarity percentages for modified blocks
- Maintains context about the nature of changes

### 4. **Performance Optimization**
- Early matching in Phase 1 reduces computation for later phases
- Hash-based matching in Phase 2 is very fast
- Similarity calculation only for remaining candidates

## Implementation Notes

### Similarity Calculation
The similarity algorithm considers:
- **Structural similarity**: AST node types and hierarchy
- **Content similarity**: Variable names, method calls, literals
- **Positional similarity**: Relative positions of elements

### Thresholds
- **Phase 3 Similarity Threshold**: 70% (configurable)
- **Hash Matching**: Exact match required
- **Identifier Matching**: Exact string match required

### Edge Cases Handled
- **Empty methods**: Matched by signature even with empty bodies
- **Comment changes**: Ignored in content hash calculation
- **Whitespace differences**: Normalized during comparison
- **Parameter reordering**: Detected through structural analysis

This four-phase approach ensures that the AST diff tool can accurately identify and classify all types of code changes, providing developers with precise and actionable information about modifications between code versions.