class Builder {
    Map properties = [:]
    
    void setName(String name) {
        properties['name'] = name
    }
    
    void setDescription(String desc) {
        properties['description'] = desc
    }
    
    Map build() {
        return properties
    }
}

def builder = new Builder()
builder.setName("MyApp")
builder.setDescription("An application")
println builder.build()
