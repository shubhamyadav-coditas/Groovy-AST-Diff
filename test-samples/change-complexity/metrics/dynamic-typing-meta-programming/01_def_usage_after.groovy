// Test: def keyword usage - after
// Uses def keyword for variables and method returns

class DataProcessor {
    
    def processItem(def item) {
        def result = item.toUpperCase()
        return result
    }
    
    def calculate(def a, def b) {
        def sum = a + b
        return sum
    }
    
    def createMap() {
        def data = [:]
        def list = []
        def count = 0
        return [data: data, list: list, count: count]
    }
}
