class DynamicConfig {
    private Map values = [:]
    
    def propertyMissing(String name) {
        return values[name]
    }
    
    def methodMissing(String name, Object args) {
        if (name.startsWith('set') && args.length == 1) {
            String key = name[3].toLowerCase() + name[4..-1]
            values[key] = args[0]
            return this
        }
        throw new MissingMethodException(name, this.class, args)
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
