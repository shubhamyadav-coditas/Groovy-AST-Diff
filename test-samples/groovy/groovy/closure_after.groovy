// MOVED: calculate closure moved to top
def calculate = { a, b, c ->  // MODIFIED: added third parameter
    return a + b + c  // MODIFIED: added c
}

// MODIFIED: closure logic changed
def greet = { name -> 
    println "Hi there, ${name}!"  // MODIFIED: greeting changed
}

// ADDED: new closure
def validate = { input ->
    return input != null && input.length() > 0
}
