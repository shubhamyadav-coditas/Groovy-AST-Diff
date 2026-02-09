// Test Case 06: Docker Dependencies Added
// After: Docker client for container management

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.api.command.CreateContainerResponse

class ContainerRunner {
    
    private DockerClient dockerClient
    
    ContainerRunner() {
        dockerClient = DockerClientBuilder.getInstance().build()
    }
    
    def runContainer(String image, String command) {
        CreateContainerResponse container = dockerClient
            .createContainerCmd(image)
            .withCmd(command.split(" "))
            .exec()
        
        dockerClient.startContainerCmd(container.id).exec()
        return container.id
    }
    
    def stopContainer(String containerId) {
        dockerClient.stopContainerCmd(containerId).exec()
    }
}

def runner = new ContainerRunner()
runner.runContainer("alpine", "echo 'Hello World'")
