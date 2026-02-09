// Test Case 09: Gradle Plugin Dependencies Added
// Before: Simple Groovy script

class BuildHelper {
    
    def compile(String sourceDir) {
        println("Compiling from ${sourceDir}")
    }
    
    def test(String testDir) {
        println("Running tests from ${testDir}")
    }
    
    def package(String outputDir) {
        println("Packaging to ${outputDir}")
    }
}

def helper = new BuildHelper()
helper.compile("src/main/groovy")
helper.test("src/test/groovy")
