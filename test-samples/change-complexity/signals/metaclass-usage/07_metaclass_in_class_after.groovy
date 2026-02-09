class PluginManager {
    List<String> plugins = []
    
    void registerPlugin(String name) {
        plugins.add(name)
        println "Registered plugin: ${name}"
    }
    
    void listPlugins() {
        plugins.each { println "- ${it}" }
    }
    
    void enhancePlugin(String pluginName, Closure behavior) {
        String.metaClass."${pluginName}Action" = behavior
        println "Enhanced plugin: ${pluginName}"
    }
}

def manager = new PluginManager()
manager.registerPlugin("auth-plugin")
manager.registerPlugin("cache-plugin")
manager.enhancePlugin("auth", { println "Auth action executed" })
manager.listPlugins()
