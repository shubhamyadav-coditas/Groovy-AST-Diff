try {
    def result = 20 / 5
    println "result2"
} catch (ArithmeticException e) {
    println "Math error"
    println "catch2"
} finally {
    println "Finally executed (modified)"
    println "finally2"
}
