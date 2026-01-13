def utilFunction() {
    println "utility"
}

class Sample {
    public Sample(String name) {
        println "Sample created with $name"
    }

    String greet(String name) {
        return "Hello $name"
    }

    static int add(int a, int b) {
        def test1() {
            return "test1"
        }
        return a + b
    }

    private void cleanup() {
        println "cleanup"
    }

    void process(int value) {
        println "Processing $value"
    }
}
