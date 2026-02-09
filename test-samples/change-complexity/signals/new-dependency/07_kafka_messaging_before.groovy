// Test Case 07: Kafka Messaging Dependencies Added
// Before: Simple in-memory queue

class MessageQueue {
    
    private List<String> messages = []
    
    def publish(String message) {
        messages.add(message)
        println("Published: ${message}")
    }
    
    def consume() {
        if (messages.isEmpty()) return null
        return messages.remove(0)
    }
}

def queue = new MessageQueue()
queue.publish("Hello")
println(queue.consume())
