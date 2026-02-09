// Test Case 04: Nested Closures
// After: Nested closures capturing mutable outer state

def processMatrix() {
    def matrix = [[1, 2], [3, 4], [5, 6]]
    def total = 0       // Mutable outer state
    def rowSums = []    // Mutable outer state (list)
    
    // Outer closure captures 'total' and 'rowSums'
    matrix.each { row ->
        def rowSum = 0
        
        // Inner closure captures 'total' from outermost scope
        row.each { cell ->
            total = total + cell
            rowSum = rowSum + cell
        }
        
        rowSums.add(rowSum)
    }
    
    println("Total: ${total}, Row sums: ${rowSums}")
}

processMatrix()
