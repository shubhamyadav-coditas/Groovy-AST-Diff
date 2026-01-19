try {
    final int MAX_RETRIES = 3

    def counter = 0

    def logMessage(msg) {
        println "LOG: ${msg}"
    }

    for (int i = 0; i < MAX_RETRIES; i++) {
        counter += i
        logMessage("Iteration ${i}")
    }

    def unusedMethod() {
        "Not used"
    }

    def calculateSum(a, b) {
        a + b
    }

    def result = calculateSum(2, 3)
    println "Result: ${result}"

} catch (IllegalArgumentException e) {
    println "Illegal argument: ${e.message}"
}
finally {
    println "Cleanup done"
}
