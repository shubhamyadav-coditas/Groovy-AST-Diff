// Test Case 08: Multiple Infrastructure Dependencies Added
// After: Multiple infra imports - Redis, MongoDB, Elasticsearch

import redis.clients.jedis.Jedis
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import org.elasticsearch.client.RestHighLevelClient

class Application {
    
    private Jedis redis
    private MongoClient mongo
    private RestHighLevelClient elasticsearch
    
    Application() {
        redis = new Jedis("localhost", 6379)
        mongo = MongoClients.create("mongodb://localhost:27017")
        // Elasticsearch client setup
    }
    
    def run() {
        println("Application started")
        cacheData()
        storeData()
        indexData()
        println("Application finished")
    }
    
    def cacheData() {
        redis.set("key", "value")
    }
    
    def storeData() {
        def database = mongo.getDatabase("mydb")
        database.getCollection("items").insertOne([name: "test"])
    }
    
    def indexData() {
        // Index to Elasticsearch
    }
}

def app = new Application()
app.run()
