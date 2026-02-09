// Test Case 04: Parallel Execution Addition
// After: Added parallel execution block

def runTests() {
    println "Running tests in parallel..."
    parallel(
        'Unit Tests': {
            sh 'npm run test:unit'
        },
        'Integration Tests': {
            sh 'npm run test:integration'
        }
    )
    println "All tests done"
}

runTests()
