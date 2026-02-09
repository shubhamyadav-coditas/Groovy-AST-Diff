class DynamicHandler {
    String name = "Handler"
    
    void process(String data) {
        println "Processing: ${data}"
    }
    
    void validate(String data) {
        println "Validating: ${data}"
    }

    def invokeMethod(String methodName, Object args) {
        println "Intercepted call to: ${methodName}"
        if (this.metaClass.respondsTo(this, methodName)) {
            return this.metaClass.invokeMethod(this, methodName, args)
        }
        return "Method ${methodName} not found"
    }
}

def handler = new DynamicHandler()
handler.process("test data")
handler.unknownMethod("will be intercepted")