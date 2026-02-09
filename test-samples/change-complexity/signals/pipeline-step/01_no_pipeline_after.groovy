// Test Case 01: Control Case - No Pipeline Steps
// After: Added more regular code, still no pipeline steps

class BuildConfig {
    String projectName = "MyProject"
    String version = "1.0.0"
    String environment = "production"
    
    void configure() {
        println "Configuring ${projectName} v${version}"
        println "Environment: ${environment}"
    }
    
    String getEnvironmentInfo() {
        return "Running in ${environment} mode"
    }
}

def config = new BuildConfig()
config.configure()
println config.getEnvironmentInfo()
