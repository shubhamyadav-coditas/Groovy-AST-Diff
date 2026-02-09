// Comprehensive Test 02: Medium Metrics - After
// Tests all 5 metrics with moderate values

import java.util.List
import java.util.Map
import groovy.json.JsonSlurper

@Service
class DataProcessor {
    
    @Autowired
    def repository
    
    def items = []
    def cache = [:]
    
    // Cyclomatic: if, for, &&
    def process(def input) {
        if (input == null || input.isEmpty()) {
            return "Invalid"
        }
        
        def result = []
        for (item in input) {
            if (item.active && item.valid) {
                result.add(item.name)
            }
        }
        return result
    }
    
    // Closures
    def transform(def data) {
        def filtered = data.findAll { it.enabled }
        def mapped = filtered.collect { it.value * 2 }
        return mapped
    }
    
    def calculate(def value) {
        def multiplier = 2
        def result = value * multiplier
        return result
    }
}
