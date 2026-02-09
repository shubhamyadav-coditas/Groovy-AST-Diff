// Test Case 06: Docker Dependencies Added
// Before: Local process execution

class ContainerRunner {
    
    def runProcess(String command) {
        def process = command.execute()
        process.waitFor()
        return process.text
    }
    
    def stopProcess(String pid) {
        "kill ${pid}".execute()
    }
}

def runner = new ContainerRunner()
runner.runProcess("echo 'Hello World'")
