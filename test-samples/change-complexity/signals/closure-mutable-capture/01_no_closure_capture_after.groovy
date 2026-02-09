// Test Case 01: Control - No Closure Mutable Capture
// After: Still no closures capturing mutable state (only local closure vars)

class DataProcessor {
    
    void processItems(List items) {
        items.each { item ->
            def localVar = item * 2
            println(localVar)
        }
    }
    
    int calculateSum(List<Integer> numbers) {
        return numbers.sum()
    }
    
    void printFormatted(List items) {
        items.collect { it.toString() }.each { println(it) }
    }
}

def processor = new DataProcessor()
processor.processItems([1, 2, 3])
