class DataService {
    List fetchData() {
        return ["item1", "item2", "item3"]
    }
    
    void processData(List data) {
        data.each { println it }
    }
}

def service = new DataService()
service.processData(service.fetchData())
