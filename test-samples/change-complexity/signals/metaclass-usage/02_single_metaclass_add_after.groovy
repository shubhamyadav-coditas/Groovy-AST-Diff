class Calculator {
    int add(int a, int b) {
        return a + b
    }
    
    int subtract(int a, int b) {
        return a - b
    }
}

Calculator.metaClass.multiply = { int a, int b ->
    a * b
}

def calc = new Calculator()
println calc.add(5, 3)
println calc.multiply(4, 5)
