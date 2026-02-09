// Test Case 03: Multiple Closures Capturing State
// Before: Simple processing without mutable captures

def analyzeData() {
    def numbers = [10, 20, 30, 40, 50]
    
    def doubled = numbers.collect { it * 2 }
    def filtered = numbers.findAll { it > 20 }
    
    println("Doubled: ${doubled}")
    println("Filtered: ${filtered}")
}

analyzeData()
