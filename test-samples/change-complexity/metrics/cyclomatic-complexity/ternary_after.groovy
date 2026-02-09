// Test: Ternary operators - after (ternary and elvis)
// Expected CC: 5 (base + 3 ternary + 1 elvis)
// Note: The nested ternary on line 13 counts as 2 ternary operators (outer + inner)

class Formatter {
    String format(String value, String defaultValue) {
        // Ternary operator
        def result = value != null ? value : "N/A"
        
        // Elvis operator
        def display = value ?: defaultValue
        
        // Nested ternary
        def status = value == null ? "null" : (value.isEmpty() ? "empty" : "valid")
        
        return status
    }
}
