// Test Case 01: Control - No Closure Mutable Capture
// Before: Simple code without closures capturing mutable state

class DataProcessor {
    
    void processItems(List items) {
        items.each { item ->
            println(item)
        }
    }
    
    int calculateSum(List<Integer> numbers) {
        return numbers.sum()
    }
}

def processor = new DataProcessor()
processor.processItems([1, 2, 3])
