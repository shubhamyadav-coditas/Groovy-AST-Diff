// Test Case 06: Closure Modifying Outer Map
// After: Closure captures and modifies outer map

def categorizeItems() {
    def items = [
        [name: 'apple', type: 'fruit'],
        [name: 'carrot', type: 'vegetable'],
        [name: 'banana', type: 'fruit']
    ]
    
    def categories = [:]  // Mutable outer map
    
    // Closure captures 'categories' map
    items.each { item ->
        def type = item.type
        if (!categories.containsKey(type)) {
            categories[type] = []
        }
        categories[type].add(item.name)
    }
    
    println("Categories: ${categories}")
}

categorizeItems()
