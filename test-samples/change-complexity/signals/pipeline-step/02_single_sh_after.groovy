// Test Case 02: Single sh Step Addition
// After: Added a single sh command

def buildProject() {
    println "Starting build..."
    sh 'mvn clean install'
    println "Build complete"
}

buildProject()
