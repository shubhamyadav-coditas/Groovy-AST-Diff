// Comprehensive Test 01: Low Complexity - After
// Small additions with minimal dynamic features

class SimpleCalculator {
    
    int add(int a, int b) {
        if (a < 0) {
            a = 0
        }
        return a + b
    }
    
    int subtract(int a, int b) {
        return a - b
    }
    
    int multiply(int a, int b) {
        return a * b
    }
}
