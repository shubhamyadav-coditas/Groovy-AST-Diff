// Test Case 03: Multiple Pipeline Steps Addition
// After: Added multiple pipeline steps

def runCI() {
    println "Starting CI..."
    checkout scm
    sh 'npm install'
    sh 'npm run lint'
    sh 'npm test'
    junit 'reports/*.xml'
    println "CI complete"
}

runCI()
