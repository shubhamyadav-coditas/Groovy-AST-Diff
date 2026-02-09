// Test Case 08: Git/Checkout Step Addition
// After: Added git and checkout steps

def prepareWorkspace() {
    println "Preparing workspace..."
    sh 'mkdir -p workspace'
    
    checkout scm
    git branch: 'main', url: 'https://github.com/org/repo.git'
}

prepareWorkspace()
