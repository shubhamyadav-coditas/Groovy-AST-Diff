class Entity {
    private Map data = [:]
    
    String name
    int age
    
    Entity(String name, int age) {
        this.name = name
        this.age = age
    }
    
    String toString() {
        return "Entity(name=${name}, age=${age})"
    }
}

def entity = new Entity("John", 30)
println entity
println entity.name
