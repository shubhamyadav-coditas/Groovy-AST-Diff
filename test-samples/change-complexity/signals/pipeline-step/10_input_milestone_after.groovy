// Test Case 10: Input and Milestone Steps (Flow Control)
// After: Added approval gates and milestones

def deployToProd() {
    milestone(1)
    sh 'make build'
    
    input message: 'Deploy to production?', ok: 'Deploy'
    
    milestone(2)
    lock('production-deploy') {
        sh 'make deploy-prod'
    }
}

deployToProd()
