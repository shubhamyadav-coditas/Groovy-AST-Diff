class Proxy {
    private Object target
    
    Proxy(Object target) {
        this.target = target
    }
    
    Object getTarget() {
        return target
    }
    
    void setTarget(Object obj) {
        this.target = obj
    }
}

class Service {
    String process(String data) {
        return "Processed: ${data}"
    }
}

def service = new Service()
def proxy = new Proxy(service)
println proxy.getTarget().process("test")
