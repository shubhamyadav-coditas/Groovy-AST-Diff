// Test Case 05: Closure Modifying Outer Collection
// Before: Simple collection operations

def buildReport() {
    def items = ['apple', 'banana', 'cherry']
    def report = items.collect { "Item: ${it}" }
    println(report.join('\n'))
}

buildReport()
