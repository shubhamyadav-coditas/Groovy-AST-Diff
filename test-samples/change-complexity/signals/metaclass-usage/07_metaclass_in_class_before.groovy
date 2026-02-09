class PluginManager {
    List<String> plugins = []
    
    void registerPlugin(String name) {
        plugins.add(name)
        println "Registered plugin: ${name}"
    }
    
    void listPlugins() {
        plugins.each { println "- ${it}" }
    }
}

def manager = new PluginManager()
manager.registerPlugin("auth-plugin")
manager.registerPlugin("cache-plugin")
manager.listPlugins()
