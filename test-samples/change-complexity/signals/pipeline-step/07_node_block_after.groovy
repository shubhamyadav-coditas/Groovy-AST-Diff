// Test Case 07: Node Block Addition (Scripted Pipeline)
// After: Added node block for scripted pipeline

def buildVersion = "1.0.0"

println "Build version: ${buildVersion}"
println "Starting process..."

node('linux') {
    stage('Checkout') {
        checkout scm
    }
    
    stage('Build') {
        sh 'make build'
    }
    
    stage('Test') {
        sh 'make test'
    }
}
