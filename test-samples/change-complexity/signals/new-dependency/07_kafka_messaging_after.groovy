// Test Case 07: Kafka Messaging Dependencies Added
// After: Apache Kafka for distributed messaging

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer

class MessageQueue {
    
    private KafkaProducer<String, String> producer
    private KafkaConsumer<String, String> consumer
    private String topic = "my-topic"
    
    MessageQueue() {
        def props = [
            'bootstrap.servers': 'localhost:9092',
            'key.serializer': 'org.apache.kafka.common.serialization.StringSerializer',
            'value.serializer': 'org.apache.kafka.common.serialization.StringSerializer'
        ]
        producer = new KafkaProducer<>(props)
    }
    
    def publish(String message) {
        def record = new ProducerRecord<>(topic, message)
        producer.send(record)
        println("Published to Kafka: ${message}")
    }
    
    def consume() {
        def records = consumer.poll(1000)
        return records.isEmpty() ? null : records.first().value()
    }
}

def queue = new MessageQueue()
queue.publish("Hello")
