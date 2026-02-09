class StandaloneExample {
    
    def add = { a, b -> a + b }
    
    def multiply = { a, b -> a * b }
    
    def greet = { name -> println "Hello, $name" }
    
    def format = { text ->
        def upper = text.toUpperCase()
        def trimmed = upper.trim()
        return trimmed
    }
    
    def validator = { input ->
        if (input == null) {
            return false
        }
        return input.length() > 0
    }
}
