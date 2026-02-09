class DataService {
    List fetchData() {
        return ["item1", "item2", "item3"]
    }
    
    void processData(List data) {
        data.each { println it }
    }
}

DataService.metaClass {
    fetchFilteredData = { String filter ->
        delegate.fetchData().findAll { it.contains(filter) }
    }
    
    countItems = {
        delegate.fetchData().size()
    }
    
    clearCache = {
        println "Cache cleared"
    }
}

def service = new DataService()
service.processData(service.fetchData())
println "Count: ${service.countItems()}"
