/**
 * Service class for handling operations.
 * @author Developer
 */
class Service {
    
    // Main execution method
    void execute() {
        start()    // Initialize
        process()  // Do work
        end()      // Cleanup
    }
    
    // Helper methods below
    
    void start() {
        println "Starting"
    }
    
    void process() {
        println "Processing"
    }
    
    void end() {
        println "Ending"
    }
}
