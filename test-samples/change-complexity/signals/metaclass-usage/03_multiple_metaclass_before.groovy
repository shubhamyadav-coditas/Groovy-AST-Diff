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

println StringUtils.capitalize("hello")
println NumberUtils.doubleIt(5)
