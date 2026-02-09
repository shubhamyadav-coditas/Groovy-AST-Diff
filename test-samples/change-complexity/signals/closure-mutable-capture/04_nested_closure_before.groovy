// Test Case 04: Nested Closures
// Before: Simple nested iteration without mutable capture

def processMatrix() {
    def matrix = [[1, 2], [3, 4], [5, 6]]
    
    matrix.each { row ->
        row.each { cell ->
            println(cell)
        }
    }
}

processMatrix()
