class NestedExample {
    
    void process(List data) {
        data.each { row ->
            row.each { cell ->
                println cell
            }
        }
    }
}