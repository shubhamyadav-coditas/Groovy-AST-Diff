// Comprehensive Test 05: Extreme Complexity - After
// Maximum complexity: all metrics maximized, all signals triggered multiple times

import java.util.List
import java.util.Map
import java.util.Set
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.transaction.annotation.Transactional
import org.springframework.cache.annotation.Cacheable
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.consumer.KafkaConsumer
import com.mongodb.client.MongoClient
import redis.clients.jedis.Jedis
import org.elasticsearch.client.RestHighLevelClient

@Component
@Transactional
@Service
class BasicService {
    
    @Autowired
    def kafkaProducer
    
    @Autowired
    def mongoClient
    
    @Autowired
    def redisClient
    
    @Value('${app.config}')
    def appConfig
    
    def cache = [:]
    def pendingItems = []
    def processedResults = []
    def errorLog = []
    
    // METRIC 1: High Cyclomatic Complexity
    @Override
    @Cacheable("results")
    @Transactional(rollbackFor = Exception.class)
    def getName(def input) {
        try {
            // Multiple if statements
            if (input == null) {
                return "null input"
            }
            if (input.isEmpty()) {
                return "empty input"
            }
            if (input.length() < 3) {
                return "too short"
            }
            if (input.length() > 100) {
                return "too long"
            }
            
            // Nested conditions with && and ||
            if ((input.startsWith("A") || input.startsWith("B")) && input.endsWith("Z")) {
                if (input.contains("X") && (input.contains("Y") || input.contains("W"))) {
                    return "special pattern"
                }
            }
            
            // Switch statement
            switch (input.charAt(0)) {
                case 'A':
                    return handleTypeA(input)
                case 'B':
                    return handleTypeB(input)
                case 'C':
                    return handleTypeC(input)
                case 'D':
                    return handleTypeD(input)
                default:
                    return handleDefault(input)
            }
            
        } catch (NullPointerException e) {
            errorLog.add("NPE: ${e.message}")
            return "error"
        } catch (IllegalArgumentException e) {
            errorLog.add("IAE: ${e.message}")
            return "error"
        } catch (Exception e) {
            errorLog.add("GEN: ${e.message}")
            return "error"
        }
    }
    
    // More cyclomatic with loops
    def processAll(def items) {
        for (item in items) {
            if (item.active) {
                for (subItem in item.children) {
                    if (subItem.valid && subItem.enabled) {
                        def result = item.process(subItem)
                        while (result == null && item.retryCount < 3) {
                            result = item.process(subItem)
                        }
                    }
                }
            }
        }
    }
    
    // SIGNAL 1: metaClass_usage (multiple)
    void setupMetaClasses() {
        String.metaClass.shout = { -> delegate.toUpperCase() + "!" }
        String.metaClass.whisper = { -> delegate.toLowerCase() }
        String.metaClass.reverse = { -> new StringBuilder(delegate).reverse().toString() }
        
        Integer.metaClass.double = { -> delegate * 2 }
        Integer.metaClass.triple = { -> delegate * 3 }
        
        def emc = new ExpandoMetaClass(List)
        emc.first = { -> delegate[0] }
        emc.last = { -> delegate[-1] }
        emc.initialize()
    }
    
    // SIGNAL 2: dynamic_method_resolution (multiple)
    def invokeMethod(String name, def args) {
        if (name.startsWith("find")) {
            return cache[name]
        }
        if (name.startsWith("get")) {
            return cache[name.substring(3)]
        }
        return super.invokeMethod(name, args)
    }
    
    def methodMissing(String name, def args) {
        if (name.startsWith("compute")) {
            return "Computed: $name"
        }
        return "Missing: $name with $args"
    }
    
    def propertyMissing(String name) {
        if (name.startsWith("config")) {
            return appConfig[name]
        }
        return "Property $name not found"
    }
    
    def getProperty(String name) {
        if (name.startsWith("cached")) {
            return cache[name]
        }
        if (name.startsWith("computed")) {
            return computeValue(name)
        }
        return super.getProperty(name)
    }
    
    def setProperty(String name, def value) {
        if (name.startsWith("validated")) {
            cache[name] = validate(value)
        } else {
            super.setProperty(name, value)
        }
    }
    
    // SIGNAL 3: pipeline_step (multiple)
    def runFullPipeline() {
        pipeline {
            agent any
            
            stages {
                stage('Build') {
                    steps {
                        sh 'mvn clean compile'
                        sh 'mvn package'
                    }
                }
                stage('Test') {
                    parallel {
                        stage('Unit Tests') {
                            steps {
                                sh 'mvn test'
                                junit 'target/surefire-reports/*.xml'
                            }
                        }
                        stage('Integration Tests') {
                            steps {
                                sh 'mvn verify'
                            }
                        }
                    }
                }
                stage('Deploy') {
                    steps {
                        node('deploy-agent') {
                            checkout scm
                            withCredentials([string(credentialsId: 'deploy-key', variable: 'KEY')]) {
                                sh 'deploy.sh'
                            }
                            withEnv(['DEPLOY_ENV=prod']) {
                                timeout(time: 10, unit: 'MINUTES') {
                                    sh 'verify-deploy.sh'
                                }
                            }
                        }
                    }
                }
                stage('Archive') {
                    steps {
                        archiveArtifacts artifacts: 'target/*.jar'
                        build job: 'downstream-job'
                    }
                }
            }
        }
    }
    
    // SIGNAL 4: closure_mutable_capture (multiple)
    def aggregateData() {
        def totalCount = 0
        def totalSum = 0
        def errors = []
        def processed = []
        def skipped = []
        
        pendingItems.each { item ->
            totalCount++
            totalSum += item.value
            processed.add(item.id)
        }
        
        processedResults.findAll { it.valid }.each { result ->
            totalSum += result.amount
            if (result.hasWarning) {
                errors.add(result.warning)
            }
        }
        
        cache.each { key, value ->
            if (value == null) {
                skipped.add(key)
            }
        }
        
        return [
            count: totalCount,
            sum: totalSum,
            errors: errors,
            processed: processed,
            skipped: skipped
        ]
    }
    
    // METRIC 3: High Closure Complexity (nested closures)
    def transformNestedData(def data) {
        def level1 = data.findAll { it.enabled }
            .collect { item ->
                item.children.findAll { child ->
                    child.active && child.valid
                }.collect { child ->
                    child.values.findAll { v -> v > 0 }
                        .collect { v ->
                            v * 2
                        }
                }
            }
        
        def chained = data
            .findAll { it != null }
            .collect { it.name }
            .findAll { it.length() > 3 }
            .collect { it.toUpperCase() }
            .sort()
            .unique()
        
        return [nested: level1, chained: chained]
    }
    
    // More closures
    def withClosures() {
        def multiplier = 3
        def offset = 10
        
        def compute = { value ->
            def step1 = value * multiplier
            def step2 = step1 + offset
            return step2
        }
        
        def validate = { input ->
            input != null && input > 0
        }
        
        return [compute: compute, validate: validate]
    }
    
    // Helper methods
    def handleTypeA(def input) { return "A: $input" }
    def handleTypeB(def input) { return "B: $input" }
    def handleTypeC(def input) { return "C: $input" }
    def handleTypeD(def input) { return "D: $input" }
    def handleDefault(def input) { return "Default: $input" }
    def computeValue(def name) { return "computed_$name" }
    def validate(def value) { return value?.toString()?.trim() }
}
