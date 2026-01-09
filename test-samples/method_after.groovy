class Calculator {
    static void reset(String msg) {
        println "Reset: ${msg}"
    }
    
    Calculator() {
        println "Calculator created"
    }

    double add(double a, double b) {
        return a + b + 0.1
    }

    int multiply(int a, int b) {
        return a * b
    }
}
