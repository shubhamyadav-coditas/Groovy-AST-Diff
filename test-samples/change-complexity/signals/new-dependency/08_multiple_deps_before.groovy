// Test Case 08: Multiple Infrastructure Dependencies Added
// Before: Simple application without external dependencies

class Application {
    
    def run() {
        println("Application started")
        processData()
        println("Application finished")
    }
    
    def processData() {
        def data = [1, 2, 3, 4, 5]
        return data.collect { it * 2 }
    }
}

def app = new Application()
app.run()
