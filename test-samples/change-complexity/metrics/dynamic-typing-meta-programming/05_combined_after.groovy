// Test: Combined features - after
// Uses def, annotations, meta-programming methods, and metaClass

@Service
@Transactional
class SimpleProcessor {
    
    @Autowired
    def repository
    
    @Override
    def process(def input) {
        def result = input?.toString()?.toUpperCase()
        def metadata = [type: input.class.simpleName]
        return [result: result, meta: metadata]
    }
    
    @Cacheable("calculations")
    def calculate(def a, def b) {
        def operation = a instanceof String ? "concat" : "add"
        return operation == "add" ? a + b : "$a$b"
    }
    
    // Meta-programming methods
    def invokeMethod(String name, def args) {
        if (name.startsWith("dynamic")) {
            return "Handled dynamically: $name"
        }
        return super.invokeMethod(name, args)
    }
    
    def methodMissing(String name, def args) {
        return "Missing method: $name"
    }
    
    def propertyMissing(String name) {
        return "Missing property: $name"
    }
    
    // metaClass modification
    void setupExtensions() {
        String.metaClass.shout = { -> delegate.toUpperCase() + "!" }
    }
}
