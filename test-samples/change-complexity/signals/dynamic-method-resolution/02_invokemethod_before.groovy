class DynamicHandler {
    String name = "Handler"
    
    void process(String data) {
        println "Processing: ${data}"
    }
    
    void validate(String data) {
        println "Validating: ${data}"
    }
}

def handler = new DynamicHandler()
handler.process("test data")