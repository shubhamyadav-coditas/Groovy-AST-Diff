// Test Case 09: Gradle Plugin Dependencies Added
// After: Gradle API imports for plugin development

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class BuildHelper implements Plugin<Project> {
    
    void apply(Project project) {
        project.task('compile') {
            doLast {
                println("Compiling from ${project.sourceSets.main.groovy.srcDirs}")
            }
        }
        
        project.task('test') {
            doLast {
                println("Running tests")
            }
        }
    }
    
    @TaskAction
    def package(String outputDir) {
        println("Packaging to ${outputDir}")
    }
}
