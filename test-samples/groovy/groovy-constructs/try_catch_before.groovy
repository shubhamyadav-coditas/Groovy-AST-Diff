/* --------------------TRY BLOCK 1 // unchanged-------------------- */

try {
    def x = 10 / 2
    println "Result: $x"
} catch (Exception e) {
    println "Error occurred"
} finally {
    println "Cleanup done"
}


/* --------------------TRY BLOCK 2 // moved later-------------------- */

try {
    def list = [1, 2, 3]
    println list[1]
} catch (IndexOutOfBoundsException e) {
    println "Index error"
}


/* --------------------TRY BLOCK 3 // deleted later-------------------- */

try {
    println "Parsed successfully"
} catch (NumberFormatException e) {
    println "Invalid number"
}


/* --------------------TRY BLOCK 4 // moved + modified later-------------------- */

try {
    def result = 10 / 0
    println result
} catch (ArithmeticException e) {
    println "Division by zero"
} finally {
    println "Finally executed"
}
