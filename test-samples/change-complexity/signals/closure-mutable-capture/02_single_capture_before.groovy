// Test Case 02: Single Closure Mutable Capture
// Before: No closure capturing mutable state

def processData() {
    def items = [1, 2, 3, 4, 5]
    def result = items.collect { it * 2 }
    println(result)
}

processData()
