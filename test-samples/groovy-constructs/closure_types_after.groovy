/* --------------------CLOSURE 1-------------------- */
def printer = { msg ->
    println "Hello $msg"
}

/* --------------------CLOSURE 2-------------------- */

def square = { x ->
    x * x
}
/* --------------------CLOSURE 3-------------------- */



/* --------------------CLOSURE 4-------------------- */


def adder = { a, b, c ->
    a + b + c + 1
}

/* --------------------CLOSURE 5-------------------- */
def numbers = [1, 2, 3]
numbers.each { n ->
    println "Number: $n"
}