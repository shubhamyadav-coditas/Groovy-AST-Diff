// Test Case 08: Callback Closure Pattern
// Before: Simple callback without mutable capture (no def variables captured)

def executeWithCallback() {
    // Direct callback without capturing outer state
    performOperation { result ->
        println("Callback received: ${result}")
    }
}

def performOperation(Closure callback) {
    String result = "operation complete"  // Typed, not def
    callback(result)
}

executeWithCallback()
