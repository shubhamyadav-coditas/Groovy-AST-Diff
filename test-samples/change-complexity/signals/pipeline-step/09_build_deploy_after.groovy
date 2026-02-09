// Test Case 09: Build and Deploy Steps Addition
// After: Added build, test, and deploy steps

def runPipeline() {
    echo 'Starting CI pipeline'
    sh 'npm install'
    build job: 'downstream-job', wait: true
    test()
    
    echo 'Starting CD pipeline'
    deploy()
    sh 'kubectl apply -f k8s/'
}

runPipeline()
