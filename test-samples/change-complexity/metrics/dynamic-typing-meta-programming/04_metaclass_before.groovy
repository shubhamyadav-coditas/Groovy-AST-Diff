// Test: metaClass modifications - before
// No metaClass usage

class StringHelper {
    
    String shout(String input) {
        return input.toUpperCase() + "!"
    }
    
    String whisper(String input) {
        return input.toLowerCase()
    }
}
