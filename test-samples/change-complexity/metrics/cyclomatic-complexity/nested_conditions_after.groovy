// Test: Nested conditions - after (deeply nested)
// Expected CC: 7 (base + 6 if statements)

class OrderProcessor {
    String processOrder(Order order) {
        if (order != null) {
            if (order.isValid()) {
                if (order.hasItems()) {
                    if (order.paymentReceived) {
                        if (order.shippingAddress != null) {
                            if (order.shippingAddress.isDeliverable()) {
                                return "Ready to ship"
                            }
                            return "Invalid shipping address"
                        }
                        return "Missing shipping address"
                    }
                    return "Awaiting payment"
                }
                return "Empty order"
            }
            return "Invalid order"
        }
        return "Null order"
    }
}
