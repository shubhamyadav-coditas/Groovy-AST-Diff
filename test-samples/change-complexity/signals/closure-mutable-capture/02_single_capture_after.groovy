// Test Case 02: Single Closure Mutable Capture
// After: One closure captures mutable outer 'counter' variable

def processData() {
    def items = [1, 2, 3, 4, 5]
    def counter = 0  // Mutable outer state
    
    items.each { item ->
        counter = counter + item  // Closure captures and modifies 'counter'
        println("Running total: ${counter}")
    }
    
    println("Final count: ${counter}")
}

processData()
