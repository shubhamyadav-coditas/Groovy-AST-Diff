def utilFunction() {
    println "utility"
}

class Sample {
    static int add(int a, int b) {
        def test1() {
            return "test2"
        }
        return a + b
    }

    public Sample(String name) {
        println "Sample created with $name"
    }

    String greet(String name) {
        return "Hi $name 👋"
    }

    void process(int value, boolean verbose) {
        if (verbose) {
            println "Processing value = $value"
        }
    }

    String log(String msg) {
        return "[LOG] $msg"
    }
}
