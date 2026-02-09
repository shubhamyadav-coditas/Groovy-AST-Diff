// Test Case 01: Control Case - No Pipeline Steps
// Before: Regular Groovy code without any pipeline steps

class BuildConfig {
    String projectName = "MyProject"
    String version = "1.0.0"
    
    void configure() {
        println "Configuring ${projectName} v${version}"
    }
}

def config = new BuildConfig()
config.configure()
