// Test Case 05: Kubernetes Dependencies Added
// Before: Simple deployment script

class DeploymentManager {
    
    def deploy(String appName, String version) {
        println("Deploying ${appName} version ${version}")
        // Manual deployment steps
        return true
    }
    
    def rollback(String appName) {
        println("Rolling back ${appName}")
        return true
    }
}

def manager = new DeploymentManager()
manager.deploy("my-app", "1.0.0")
