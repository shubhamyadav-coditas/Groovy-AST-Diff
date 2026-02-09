class Inspector {
    void inspect(Object obj) {
        println "Class: ${obj.getClass().name}"
        println "Methods: ${obj.getClass().methods*.name.unique().sort()}"
    }
}

def inspector = new Inspector()
inspector.inspect("Hello")
inspector.inspect(42)
