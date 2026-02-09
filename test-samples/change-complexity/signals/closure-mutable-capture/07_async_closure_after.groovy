// Test Case 07: Async/Threaded Closure Capture
// After: Closure captures mutable state in potentially async context

def processAsync() {
    def tasks = ['task1', 'task2', 'task3']
    def completedCount = 0   // Mutable - race condition risk!
    def results = [:]        // Mutable - race condition risk!
    
    // Closure captures mutable state - dangerous in async context
    tasks.each { task ->
        Thread.start {
            // Simulated async work
            sleep(100)
            results[task] = "completed"
            completedCount++  // Race condition!
        }
    }
    
    // Wait for completion
    sleep(500)
    println("Completed: ${completedCount}, Results: ${results}")
}

processAsync()
