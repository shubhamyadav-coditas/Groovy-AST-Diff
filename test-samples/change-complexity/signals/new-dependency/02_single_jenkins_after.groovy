// Test Case 02: Single Jenkins Dependency Added
// After: Added Jenkins pipeline library import

import org.jenkinsci.plugins.workflow.cps.CpsScript

class BuildScript {
    
    def runBuild() {
        println("Starting build...")
        println("Build complete")
    }
    
    def getJenkinsContext() {
        // Access Jenkins context
        return this
    }
}

def script = new BuildScript()
script.runBuild()
