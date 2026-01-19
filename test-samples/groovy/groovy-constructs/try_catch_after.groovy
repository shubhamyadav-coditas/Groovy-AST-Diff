/* --------------------TRY BLOCK 1 // unchanged-------------------- */

try {
    def x = 10 / 2
    println "Result: $x"
} catch (Exception e) {
    println "Error occurred"
} finally {
    println "Cleanup done"
}


/* --------------------TRY BLOCK 3 // deleted later-------------------- */

try {
    println "Parsed value"
} catch (NumberFormatException e) {
    println "Failed to parse number"
} finally {
    println "Parsing attempt finished"
}


/* --------------------TRY BLOCK 4 // moved + modified later-------------------- */

try {
    def result = 20 / 5
    //comment
} catch (ArithmeticException e) {
    println "Math error"
} finally {
    println "Finally executed (modified)"
}


/* --------------------TRY BLOCK 2 // moved later-------------------- */

try {
    def list = [1, 2, 3]
    println list[1]
} catch (IndexOutOfBoundsException e) {
    println "Index error"
}
