// Comprehensive Test 03: All Signals - After
// Triggers all 5 signals

import java.util.List
import org.springframework.beans.factory.annotation.Autowired
import org.apache.kafka.clients.producer.KafkaProducer

class ServiceHandler {
    
    def data = []
    
    // Signal 1: metaClass_usage
    void setupMetaClass() {
        String.metaClass.shout = { -> delegate.toUpperCase() + "!" }
    }
    
    // Signal 2: dynamic_method_resolution
    def invokeMethod(String name, def args) {
        return "Dynamic: $name"
    }
    
    def methodMissing(String name, def args) {
        return "Missing: $name"
    }
    
    // Signal 3: pipeline_step
    def runPipeline() {
        sh 'echo "Building..."'
        node('worker') {
            stage('Test') {
                sh 'npm test'
            }
        }
    }
    
    // Signal 4: closure_mutable_capture
    def processWithClosure() {
        def counter = 0
        def items = []
        
        data.each { item ->
            counter++
            items.add(item)
        }
        
        return [count: counter, items: items]
    }
    
    // Signal 5: new_dependency (Kafka import above)
    def sendToKafka(def message) {
        // Uses the new Kafka dependency
        return message
    }
}
