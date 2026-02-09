// Test Case 05: withCredentials Wrapper Addition
// After: Added withCredentials for secure deployment

def deployApp() {
    println "Deploying to server..."
    withCredentials([usernamePassword(
        credentialsId: 'deploy-creds',
        usernameVariable: 'USER',
        passwordVariable: 'PASS'
    )]) {
        sh "scp -r dist/ ${USER}@server:/var/www/"
    }
    println "Deployment complete"
}

deployApp()
