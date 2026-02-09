// Test Case 05: Closure Modifying Outer Collection
// After: Closure captures and modifies outer collection

def buildReport() {
    def items = ['apple', 'banana', 'cherry']
    def results = []     // Mutable outer collection
    def errors = []      // Mutable outer collection
    
    // Closure captures 'results' and 'errors' collections
    items.each { item ->
        if (item.length() > 5) {
            results.add("Valid: ${item}")
        } else {
            errors.add("Too short: ${item}")
        }
    }
    
    println("Results: ${results}")
    println("Errors: ${errors}")
}

buildReport()
