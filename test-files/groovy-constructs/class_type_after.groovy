package demo2

enum Status {
    ACTIVE,
    INACTIVE,
    BOLD
}

@interface Audit {
    String values()
}

interface Greeter {
    String greet(String firstName, String lastName)

}

class Helper {
    int add(int a, int b, int c) {
        return a + b + c
    }
}

trait LoggerTrait {
    void log(String msg) {
        println "[LOG]2 $msg"
    }
}

class Foo {
    String name

    Foo(String name) {
        this.name = name
    }

    String sayHello() {
        return "Hi $name 👋"
    }
}

class NewService {
    void execute() {
        println "New service execution"
    }
}
