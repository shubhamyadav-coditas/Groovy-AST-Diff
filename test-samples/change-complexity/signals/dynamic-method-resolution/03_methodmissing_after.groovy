class Builder {
    Map properties = [:]
    
    void setName(String name) {
        properties['name'] = name
    }
    
    void setDescription(String desc) {
        properties['description'] = desc
    }
    
    def methodMissing(String name, Object args) {
        if (name.startsWith('set') && args.length == 1) {
            String propName = name[3].toLowerCase() + name[4..-1]
            properties[propName] = args[0]
            return this
        }
        throw new MissingMethodException(name, this.class, args)
    }
    
    Map build() {
        return properties
    }
}

def builder = new Builder()
builder.setName("MyApp")
builder.setDescription("An application")
builder.setVersion("1.0.0")
builder.setAuthor("John")
println builder.build()
