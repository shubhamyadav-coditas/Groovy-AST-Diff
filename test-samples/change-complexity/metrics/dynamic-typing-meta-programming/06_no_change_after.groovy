// Test: No change (control case) - after
// Same dynamic features in both files (identical)

@Component
class ConfigService {
    
    @Value('${app.name}')
    def appName
    
    def getConfig(def key) {
        def value = System.getProperty(key)
        return value ?: "default"
    }
}
