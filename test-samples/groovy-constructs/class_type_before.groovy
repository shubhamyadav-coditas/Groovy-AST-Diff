package demo

enum Status {
    ACTIVE,
    INACTIVE
}

@interface Audit {
    String value()
}

interface Greeter {
    String greet(String name)
}

trait LoggerTrait {
    void log(String msg) {
        println "[LOG] $msg"
    }
}

class Helper {
    int add(int a, int b) {
        return a + b
    }
}

class OldService {
    void execute() {
        println "Old service execution"
    }
}

class Foo {
    String name

    Foo(String name) {
        this.name = name
    }

    String sayHello() {
        return "Hello $name"
    }
}
