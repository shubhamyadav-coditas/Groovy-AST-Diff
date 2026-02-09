// Test: If statements - after (multiple if/else if)
// Expected CC: 5 (base + 4 if statements)

class Validator {
    String validate(String input) {
        if (input == null) {
            return "Input is null"
        }
        if (input.isEmpty()) {
            return "Input is empty"
        }
        if (input.length() < 3) {
            return "Input too short"
        }
        if (input.length() > 100) {
            return "Input too long"
        }
        return "Valid"
    }
}
