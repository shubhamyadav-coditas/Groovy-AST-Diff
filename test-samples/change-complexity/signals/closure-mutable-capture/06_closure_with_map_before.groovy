// Test Case 06: Closure Modifying Outer Map
// Before: Simple map operations

def categorizeItems() {
    def items = [
        [name: 'apple', type: 'fruit'],
        [name: 'carrot', type: 'vegetable'],
        [name: 'banana', type: 'fruit']
    ]
    
    def fruits = items.findAll { it.type == 'fruit' }
    println(fruits)
}

categorizeItems()
