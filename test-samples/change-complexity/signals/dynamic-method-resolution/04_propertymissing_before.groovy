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
}

def config = new Config()
println "Debug: ${config.debug}"
println "Timeout: ${config.timeout}"
