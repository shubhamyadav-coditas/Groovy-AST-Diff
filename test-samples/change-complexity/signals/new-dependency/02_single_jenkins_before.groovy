// Test Case 02: Single Jenkins Dependency Added
// Before: Simple build script without Jenkins

class BuildScript {
    
    def runBuild() {
        println("Starting build...")
        println("Build complete")
    }
}

def script = new BuildScript()
script.runBuild()
