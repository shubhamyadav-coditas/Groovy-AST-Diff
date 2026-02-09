class Calculator {
    int add(int a, int b) {
        return a + b
    }
    
    int subtract(int a, int b) {
        return a - b
    }
    
    int multiply(int a, int b) {
        return a * b
    }
}

def calc = new Calculator()
println calc.add(5, 3)
println calc.subtract(10, 4)
