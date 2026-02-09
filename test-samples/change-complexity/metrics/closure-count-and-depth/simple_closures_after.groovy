class DataProcessor {
    
    void processItems(List items) {
        items.each { item ->
            println item
        }
    }
    
    def calculate = { a, b ->
        return a + b
    }
    
    List filterPositive(List numbers) {
        return numbers.findAll { it > 0 }
    }
}