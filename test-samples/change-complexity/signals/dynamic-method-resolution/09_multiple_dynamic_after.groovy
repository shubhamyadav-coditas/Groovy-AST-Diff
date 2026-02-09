class Proxy {
    private Object target
    private List<String> callLog = []
    
    Proxy(Object target) {
        this.target = target
    }
    
    Object getTarget() {
        return target
    }
    
    void setTarget(Object obj) {
        this.target = obj
    }
    
    def invokeMethod(String name, Object args) {
        callLog.add("Method: ${name}")
        if (target.respondsTo(name)) {
            return target."${name}"(*args)
        }
        throw new MissingMethodException(name, this.class, args)
    }
    
    def methodMissing(String name, Object args) {
        callLog.add("Missing method: ${name}")
        return "Method ${name} not found on target"
    }
    
    def propertyMissing(String name) {
        callLog.add("Missing property: ${name}")
        if (target.hasProperty(name)) {
            return target."${name}"
        }
        throw new MissingPropertyException(name, this.class)
    }
    
    List<String> getCallLog() {
        return callLog
    }
}

class Service {
    String name = "MyService"
    
    String process(String data) {
        return "Processed: ${data}"
    }
}

def service = new Service()
def proxy = new Proxy(service)
println proxy.process("test")
println proxy.name
println proxy.callLog
