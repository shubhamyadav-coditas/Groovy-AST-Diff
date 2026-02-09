// Test Case 09: Closure in Class Context
// After: Class with closures capturing mutable method-local state

class EventProcessor {
    List<String> events = []
    
    void addEvent(String event) {
        events.add(event)
    }
    
    void processEvents() {
        events.each { event ->
            println("Processing: ${event}")
        }
    }
    
    List<String> getFilteredEvents(String prefix) {
        return events.findAll { it.startsWith(prefix) }
    }
    
    Map<String, Integer> countByType() {
        def counts = [:]  // Mutable local variable
        
        // Closure captures 'counts'
        events.each { event ->
            def type = event.split(':')[0]
            counts[type] = (counts[type] ?: 0) + 1
        }
        
        return counts
    }
    
    void processWithStats() {
        def successCount = 0   // Mutable local variable
        def failCount = 0      // Mutable local variable
        
        // Closure captures both counters
        events.each { event ->
            if (event.contains('success')) {
                successCount++
            } else if (event.contains('fail')) {
                failCount++
            }
        }
        
        println("Success: ${successCount}, Fail: ${failCount}")
    }
}

def processor = new EventProcessor()
processor.addEvent("user:login:success")
processor.addEvent("user:logout:success")
processor.addEvent("auth:fail")
processor.processWithStats()
