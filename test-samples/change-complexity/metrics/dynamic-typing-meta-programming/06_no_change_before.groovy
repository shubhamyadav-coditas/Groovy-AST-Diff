// Test: No change (control case) - before
// Same dynamic features in both files

@Component
class ConfigService {
    
    @Value('${app.name}')
    def appName
    
    def getConfig(def key) {
        def value = System.getProperty(key)
        return value ?: "default"
    }
}
