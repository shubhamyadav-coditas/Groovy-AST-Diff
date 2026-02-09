// Comprehensive Test 04: High Complexity - Before
// Starting point with some existing complexity

import java.util.List
import java.util.Map

@Component
class OrderProcessor {
    
    @Autowired
    def orderRepository
    
    def processOrder(def order) {
        if (order == null) {
            return null
        }
        return order.id
    }
}
