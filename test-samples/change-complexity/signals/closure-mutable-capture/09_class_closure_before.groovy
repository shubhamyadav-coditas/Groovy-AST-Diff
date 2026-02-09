// Test Case 09: Closure in Class Context
// Before: Class without closures capturing mutable method-local state

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
}

def processor = new EventProcessor()
processor.addEvent("user:login")
processor.addEvent("user:logout")
processor.processEvents()
