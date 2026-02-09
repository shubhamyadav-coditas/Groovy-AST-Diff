class SimpleClass {
    String name
    int value
    String description
    
    String getName() {
        return name
    }
    
    void setValue(int val) {
        this.value = val
    }
    
    String getDescription() {
        return description ?: "No description"
    }
}

def instance = new SimpleClass()
instance.name = "Test"
instance.setValue(42)
instance.description = "A simple test"
