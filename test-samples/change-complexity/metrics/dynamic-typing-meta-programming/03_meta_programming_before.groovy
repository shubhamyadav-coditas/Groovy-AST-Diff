// Test: Meta-programming methods - before
// Standard class without meta-programming

class Calculator {
    
    int add(int a, int b) {
        return a + b
    }
    
    int multiply(int a, int b) {
        return a * b
    }
    
    String getOperation(String name) {
        return "operation: " + name
    }
}
