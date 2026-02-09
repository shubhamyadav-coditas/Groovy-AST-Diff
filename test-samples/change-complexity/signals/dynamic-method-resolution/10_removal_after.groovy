class DynamicConfig {
    private Map values = [:]
    
    Object get(String key) {
        return values[key]
    }
    
    DynamicConfig set(String key, Object value) {
        values[key] = value
        return this
    }
    
    DynamicConfig setHost(String host) {
        return set('host', host)
    }
    
    DynamicConfig setPort(int port) {
        return set('port', port)
    }
    
    String getHost() {
        return values['host']
    }
    
    int getPort() {
        return values['port'] ?: 0
    }
    
    Map getAll() {
        return values
    }
}

def config = new DynamicConfig()
config.setHost("localhost")
config.setPort(8080)
println config.host
println config.port
