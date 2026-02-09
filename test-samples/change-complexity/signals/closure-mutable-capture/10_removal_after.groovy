// Test Case 10: Removal of Closure Mutable Capture
// After: Refactored to use functional approach without mutable capture

def aggregateData() {
    def items = [1, 2, 3, 4, 5]
    
    // Functional approach - no mutable capture
    def sum = items.sum()
    def max = items.max()
    
    println("Sum: ${sum}, Max: ${max}")
}

aggregateData()
