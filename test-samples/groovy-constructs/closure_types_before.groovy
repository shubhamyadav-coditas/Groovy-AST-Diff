/* --------------------CLOSURE 1-------------------- */
def printer = { msg ->
    println msg
}

/* --------------------CLOSURE 2-------------------- */
def square = { x ->
    x * x
}

/* --------------------CLOSURE 3-------------------- */
def isEven = { n ->
    n % 2 == 0
}
/* --------------------CLOSURE 4-------------------- */
def adder = { a, b ->
    a + b + 1
}



/* --------------------CLOSURE 5-------------------- */
def numbers = [1, 2, 3]
numbers.each { n ->
    println n
}