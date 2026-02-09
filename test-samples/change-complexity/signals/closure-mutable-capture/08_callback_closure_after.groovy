// Test Case 08: Callback Closure Pattern
// After: Callback closure captures mutable outer state

def executeWithCallback() {
    def state = 'initial'     // Mutable outer state
    def callCount = 0         // Mutable outer state
    
    // Callback closure captures and modifies outer state
    def callback = { result ->
        callCount++
        state = result
        println("Callback #${callCount}: state changed to ${state}")
    }
    
    performOperation(callback)
    performOperation(callback)
    
    println("Final state: ${state}, Call count: ${callCount}")
}

def performOperation(Closure callback) {
    def result = "operation-${System.currentTimeMillis()}"
    callback(result)
}

executeWithCallback()
