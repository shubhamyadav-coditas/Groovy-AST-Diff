try {
    def result = 10 / 0
    println "result"
} catch (ArithmeticException e) {
    println "Division by zero"
    println "catch"
} finally {
    println "Finally executed"
    println "finally"
}
