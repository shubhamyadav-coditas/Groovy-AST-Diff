// Test Case 10: Input and Milestone Steps (Flow Control)
// Before: Automatic deployment

def deployToProd() {
    sh 'make build'
    sh 'make deploy-prod'
}

deployToProd()
