class Config {
    Map settings = [
        'debug': false,
        'timeout': 30,
        'maxRetries': 3
    ]
    
    boolean getDebug() {
        return settings['debug']
    }
    
    int getTimeout() {
        return settings['timeout']
    }
    
    def propertyMissing(String name) {
        if (settings.containsKey(name)) {
            return settings[name]
        }
        throw new MissingPropertyException(name, this.class)
    }
}

def config = new Config()
println "Debug: ${config.debug}"
println "Timeout: ${config.timeout}"
println "MaxRetries: ${config.maxRetries}"