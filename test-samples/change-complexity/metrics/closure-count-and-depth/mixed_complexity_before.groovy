class MixedExample {
    
    def greet = { name ->
        println "Hello, $name"
    }
    
    void process(List items) {
        items.each { println it }
    }
}
