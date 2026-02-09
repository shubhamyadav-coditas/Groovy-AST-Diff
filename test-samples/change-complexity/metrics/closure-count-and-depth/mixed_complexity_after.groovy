class MixedExample {
    
    def greet = { name ->
        println "Hello, $name"
    }
    
    def transform = { input ->
        return input.toUpperCase()
    }
    
    void process(List items) {
        items.each { item ->
            item.properties.each { prop ->
                println "$prop.key: $prop.value"
            }
        }
    }
    
    List chainedProcessing(List data) {
        return data
            .findAll { it.active }
            .collect { it.name }
            .sort { a, b -> a <=> b }
    }
}
