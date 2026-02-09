// Test Case 05: withCredentials Wrapper Addition
// Before: Direct deployment without credentials

def deployApp() {
    println "Deploying to server..."
    sh 'scp -r dist/ user@server:/var/www/'
    println "Deployment complete"
}

deployApp()
