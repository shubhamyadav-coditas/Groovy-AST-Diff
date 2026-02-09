// Test: Ternary operators - before
// Expected CC: 2 (base + 1 if)

class Formatter {
    String format(String value) {
        if (value == null) {
            return "N/A"
        }
        return value
    }
}
