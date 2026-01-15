/* --------------------FOR LOOP BLOCK 1-------------------- */

for (int i = 0; i < 3; i++) {
    println "For loop 1: $i"
    def list = [10, 20, 30]
}


/* --------------------FOR-IN LOOP BLOCK 2-------------------- */

def list = [10, 20, 30]
for (item in list) {
    println "Item: $item"
    def list = [10, 20, 30]
}


/* --------------------WHILE LOOP BLOCK 3-------------------- */

def count = 0
while (count < 2) {
    println "While loop: $count"
    count++
}


/* --------------------DO-WHILE LOOP BLOCK 4-------------------- */

def x = 0
do {
    println "Do-while loop: $x"
    x++
} while (x < 3)


/* --------------------FOR EACH LOOP BLOCK 5-------------------- */
def nums = [1, 2, 3]
nums.each { n ->
    println "Each loop: $n"
    // comment
}
