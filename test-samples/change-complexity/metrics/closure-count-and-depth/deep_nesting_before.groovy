class DeepNestingExample {
    
    void processData(List data) {
        data.each { item ->
            item.values.each { value ->
                println value
            }
        }
    }
}