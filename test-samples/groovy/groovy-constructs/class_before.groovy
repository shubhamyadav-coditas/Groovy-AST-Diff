class UserService {
    String name = "service"
}

class Logger {
    void log(String msg) { println msg }

    class A {
        String A1 = "A1"

        class B {
            String B1 = "B1"

            for (int i = 0; i < 10; i++) {
                println i
            }
        }   
    }
}

interface DataProcessor {
    void process()
}

class ConfigManager {
    int version = 1
}

class DatabaseConnector {
    String url = "localhost"
}