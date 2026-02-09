class Inspector {
    void inspect(Object obj) {
        println "Class: ${obj.getClass().name}"
        println "Methods: ${obj.getClass().methods*.name.unique().sort()}"
    }
    
    void inspectMeta(Object obj) {
        def mc = obj.getMetaClass()
        println "MetaClass: ${mc.theClass.name}"
        println "MetaMethods: ${mc.metaMethods*.name.unique().take(5)}"
    }
    
    void addMethod(Class clazz, String methodName, Closure impl) {
        clazz.metaClass."${methodName}" = impl
    }
}

def inspector = new Inspector()
inspector.inspect("Hello")
inspector.inspectMeta("Hello")
inspector.addMethod(String, "shout", { delegate.toUpperCase() + "!" })
println "hello".shout()
