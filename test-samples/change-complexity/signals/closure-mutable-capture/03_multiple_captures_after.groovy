// Test Case 03: Multiple Closures Capturing State
// After: Multiple closures capturing different mutable variables

def analyzeData() {
    def numbers = [10, 20, 30, 40, 50]
    def sum = 0           // Mutable outer state 1
    def count = 0         // Mutable outer state 2
    def maxValue = 0      // Mutable outer state 3
    
    // Closure 1: Captures and modifies 'sum'
    numbers.each { num ->
        sum = sum + num
    }
    
    // Closure 2: Captures and modifies 'count'
    numbers.findAll { num ->
        count++
        return num > 20
    }
    
    // Closure 3: Captures and modifies 'maxValue'
    numbers.each { num ->
        if (num > maxValue) {
            maxValue = num
        }
    }
    
    println("Sum: ${sum}, Count: ${count}, Max: ${maxValue}")
}

analyzeData()
