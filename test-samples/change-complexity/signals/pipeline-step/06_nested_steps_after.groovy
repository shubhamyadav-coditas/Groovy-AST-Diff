// Test Case 06: Nested Pipeline Steps
// After: Added nested pipeline steps

def processFiles() {
    def result = "success"
    println(result)
    
    withEnv(['BUILD_ENV=production']) {
        timeout(time: 10, unit: 'MINUTES') {
            sh 'npm run build'
            archiveArtifacts artifacts: 'dist/**'
        }
    }
}

processFiles()
