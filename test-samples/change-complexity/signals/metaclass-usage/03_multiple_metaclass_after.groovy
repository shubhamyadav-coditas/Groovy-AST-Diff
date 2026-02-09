class StringUtils {
    static String capitalize(String s) {
        return s?.capitalize()
    }
}

class NumberUtils {
    static int doubleIt(int n) {
        return n * 2
    }
}

String.metaClass.reverse = {
    delegate.reverse()
}

StringUtils.metaClass.toLowerCase = { String s ->
    s?.toLowerCase()
}

NumberUtils.metaClass.tripleIt = { int n ->
    n * 3
}

println StringUtils.capitalize("hello")
println NumberUtils.doubleIt(5)
println "hello".reverse()
