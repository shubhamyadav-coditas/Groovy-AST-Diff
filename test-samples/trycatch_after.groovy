try {
    final int MAX_RETRIES = 3

    def retryCounter = 1

    def calculateSum(a, b, c = 0) {
        a + b + c
    }

    def result = calculateSum(2, 3, 4)
    println "Result: ${result}"

    for (int i = 0; i < MAX_RETRIES; i++) {
        retryCounter += i * 2
        println "Iteration => ${i}"
    }

    def logMessage(msg) {
        println "LOG: ${msg}"
    }

    logMessage("Processing completed")

    def validateResult(value) {
        value > 0
    }

    println "Is valid: ${validateResult(result)}"

}
catch (IllegalStateException e) {
    println "Illegal state: ${e.message}"
}
finally {
    println "Cleanup completed"
}
