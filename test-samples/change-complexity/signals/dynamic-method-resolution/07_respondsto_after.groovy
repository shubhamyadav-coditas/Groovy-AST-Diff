class MessageHandler {
    void handleText(String text) {
        println "Text: ${text}"
    }
    
    void handleNumber(int num) {
        println "Number: ${num}"
    }
    
    void dispatch(String methodName, Object value) {
        if (this.respondsTo(methodName, value.getClass())) {
            this."${methodName}"(value)
        } else {
            println "No handler for: ${methodName}"
        }
    }
}

def handler = new MessageHandler()
handler.handleText("Hello")
handler.handleNumber(42)
handler.dispatch("handleText", "Dynamic call")
handler.dispatch("handleUnknown", "Will fail gracefully")
