// Test: Nested conditions - before
// Expected CC: 2 (base + 1 if)

class OrderProcessor {
    String processOrder(Order order) {
        if (order != null) {
            return "Processing"
        }
        return "Invalid"
    }
}
