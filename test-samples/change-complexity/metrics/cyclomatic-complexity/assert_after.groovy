// Test: Assert statements - after (with assertions)
// Expected CC: 4 (base + 3 assertions)

class Calculator {
    int divide(int a, int b) {
        assert b != 0 : "Divisor cannot be zero"
        assert a >= 0 : "Dividend must be non-negative"
        assert b > 0 : "Divisor must be positive"
        
        return a / b
    }
}
