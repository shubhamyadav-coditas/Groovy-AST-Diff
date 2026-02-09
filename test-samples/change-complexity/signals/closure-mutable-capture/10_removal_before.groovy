// Test Case 10: Removal of Closure Mutable Capture
// Before: Code with closures capturing mutable state

def aggregateData() {
    def items = [1, 2, 3, 4, 5]
    def runningSum = 0    // Mutable outer state
    def runningMax = 0    // Mutable outer state
    
    // Closure captures mutable state
    items.each { item ->
        runningSum += item
        if (item > runningMax) {
            runningMax = item
        }
    }
    
    println("Sum: ${runningSum}, Max: ${runningMax}")
}

aggregateData()
