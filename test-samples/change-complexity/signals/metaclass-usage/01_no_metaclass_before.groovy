class SimpleClass {
    String name
    int value
    
    String getName() {
        return name
    }
    
    void setValue(int val) {
        this.value = val
    }
}

def instance = new SimpleClass()
instance.name = "Test"
instance.setValue(42)
