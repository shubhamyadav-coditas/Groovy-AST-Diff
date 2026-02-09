// Test: metaClass modifications - after
// Uses metaClass to add methods dynamically

class StringHelper {
    
    void setupEnhancements() {
        // Add method to String class at runtime
        String.metaClass.shout = { -> delegate.toUpperCase() + "!" }
        
        // Add another method
        String.metaClass.whisper = { -> delegate.toLowerCase() }
        
        // Add static method
        String.metaClass.static.createGreeting = { name -> "Hello, $name!" }
    }
    
    void setupWithExpandoMetaClass() {
        ExpandoMetaClass emc = new ExpandoMetaClass(Integer)
        emc.triple = { -> delegate * 3 }
        emc.initialize()
    }
    
    String process(String input) {
        // Use the dynamically added method
        return input.metaClass.invokeMethod(input, "shout", null)
    }
}
