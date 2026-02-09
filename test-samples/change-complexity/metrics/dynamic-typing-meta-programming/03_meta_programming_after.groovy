// Test: Meta-programming methods - after
// Class with meta-programming method overrides

class Calculator {
    
    Map operations = [:]
    
    int add(int a, int b) {
        return a + b
    }
    
    // Method interception
    Object invokeMethod(String name, Object args) {
        if (operations.containsKey(name)) {
            return operations[name].call(args)
        }
        return super.invokeMethod(name, args)
    }
    
    // Handle missing methods
    Object methodMissing(String name, Object args) {
        return "Method $name not found with args: $args"
    }
    
    // Handle missing properties
    Object propertyMissing(String name) {
        return "Property $name not found"
    }
    
    // Property access interception
    Object getProperty(String name) {
        if (name.startsWith("computed")) {
            return "Computed value for $name"
        }
        return super.getProperty(name)
    }
    
    // Property write interception
    void setProperty(String name, Object value) {
        if (name.startsWith("validated")) {
            super.setProperty(name, value?.toString()?.trim())
        } else {
            super.setProperty(name, value)
        }
    }
}
