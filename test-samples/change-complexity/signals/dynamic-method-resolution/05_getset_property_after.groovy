class Entity {
    private Map data = [:]
    private Set<String> changedFields = []
    
    String name
    int age
    
    Entity(String name, int age) {
        this.name = name
        this.age = age
    }
    
    def getProperty(String propertyName) {
        println "Reading property: ${propertyName}"
        return this.@"${propertyName}"
    }
    
    void setProperty(String propertyName, Object value) {
        println "Setting property: ${propertyName} = ${value}"
        changedFields.add(propertyName)
        this.@"${propertyName}" = value
    }
    
    Set<String> getChangedFields() {
        return changedFields
    }
    
    String toString() {
        return "Entity(name=${name}, age=${age})"
    }
}

def entity = new Entity("John", 30)
entity.name = "Jane"
println entity.name
println "Changed: ${entity.getChangedFields()}"
