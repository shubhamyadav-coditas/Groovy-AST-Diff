class UserService {
    String name = "enhanced_service"
}

class SecurityManager {
    boolean isSecure = true
}

interface DataProcessor {
    void process()
}

class Logger {
    void log(String msg, String level) { println "[${level}] ${msg}" }

    class A {
        String A1 = "A1 enhanced"

        class B {
            String B1 = "B1 enhanced"

            for (int i = 0; i < 10; i++) {
                println i+10
            }
        }   
    }
}

class ConfigManager {
    int version = 1
}