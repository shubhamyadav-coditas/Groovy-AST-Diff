// Test Case 07: Async/Threaded Closure Capture
// Before: Simple sequential processing

def processAsync() {
    def tasks = ['task1', 'task2', 'task3']
    
    tasks.each { task ->
        println("Processing: ${task}")
    }
    
    println("All tasks listed")
}

processAsync()
