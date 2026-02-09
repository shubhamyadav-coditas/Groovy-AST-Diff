// Comprehensive Test 04: High Complexity - After
// High complexity with all metrics and multiple signals

import java.util.List
import java.util.Map
import java.util.concurrent.ConcurrentHashMap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import redis.clients.jedis.Jedis

@Component
@Transactional
class OrderProcessor {
    
    @Autowired
    def orderRepository
    
    @Autowired
    def paymentService
    
    @Autowired
    def notificationService
    
    def cache = [:]
    def pendingOrders = []
    
    // High cyclomatic complexity: nested if, switch, loops, try-catch
    @Cacheable("orders")
    def processOrder(def order) {
        try {
            if (order == null || !order.valid) {
                return [status: "error", message: "Invalid order"]
            }
            
            if (order.priority == "high" && order.amount > 1000) {
                switch (order.type) {
                    case "express":
                        return handleExpressOrder(order)
                    case "standard":
                        return handleStandardOrder(order)
                    case "bulk":
                        return handleBulkOrder(order)
                    default:
                        return handleDefaultOrder(order)
                }
            }
            
            for (item in order.items) {
                if (item.quantity > 0 && item.inStock) {
                    def total = item.price * item.quantity
                    if (total > order.budget || total < 0) {
                        throw new Exception("Budget exceeded")
                    }
                }
            }
            
            return order
        } catch (NullPointerException e) {
            return [status: "error", message: "Null error"]
        } catch (Exception e) {
            return [status: "error", message: e.message]
        }
    }
    
    // metaClass usage
    void enhanceOrder() {
        Order.metaClass.validate = { -> delegate.items?.size() > 0 }
        Order.metaClass.calculate = { -> delegate.items.sum { it.price } }
    }
    
    // Dynamic method resolution
    def invokeMethod(String name, def args) {
        if (name.startsWith("find")) {
            return cache[name]
        }
        return super.invokeMethod(name, args)
    }
    
    def methodMissing(String name, def args) {
        return "Method $name not implemented"
    }
    
    def getProperty(String name) {
        if (name.startsWith("cached")) {
            return cache[name]
        }
        return super.getProperty(name)
    }
    
    // Pipeline steps
    def deployOrder() {
        node('deploy-agent') {
            stage('Prepare') {
                sh 'echo "Preparing deployment"'
                checkout scm
            }
            stage('Deploy') {
                withCredentials([string(credentialsId: 'api-key', variable: 'KEY')]) {
                    sh 'deploy.sh'
                }
            }
            stage('Verify') {
                timeout(time: 5, unit: 'MINUTES') {
                    sh 'verify.sh'
                }
            }
        }
    }
    
    // Closure capturing mutable state
    def aggregateOrders() {
        def totalAmount = 0
        def processedCount = 0
        def errors = []
        
        pendingOrders.each { order ->
            totalAmount += order.amount
            processedCount++
            if (order.hasError) {
                errors.add(order.id)
            }
        }
        
        def summary = [:]
        cache.each { key, value ->
            summary[key] = value
        }
        
        return [
            total: totalAmount,
            count: processedCount,
            errors: errors,
            summary: summary
        ]
    }
    
    // More closures with nesting
    def transformData(def data) {
        def result = data.findAll { it.active }
            .collect { item ->
                item.values.findAll { v -> v > 0 }
                    .collect { v -> v * 2 }
            }
        return result
    }
    
    def handleExpressOrder(def order) { return order }
    def handleStandardOrder(def order) { return order }
    def handleBulkOrder(def order) { return order }
    def handleDefaultOrder(def order) { return order }
}
