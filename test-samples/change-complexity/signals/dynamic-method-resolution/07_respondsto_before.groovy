class MessageHandler {
    void handleText(String text) {
        println "Text: ${text}"
    }
    
    void handleNumber(int num) {
        println "Number: ${num}"
    }
}

def handler = new MessageHandler()
handler.handleText("Hello")
handler.handleNumber(42)
