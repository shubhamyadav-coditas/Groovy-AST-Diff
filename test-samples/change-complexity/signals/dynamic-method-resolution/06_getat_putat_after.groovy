class DataStore {
    private List<String> items = []
    
    void addItem(String item) {
        items.add(item)
    }
    
    String getItem(int index) {
        return items.get(index)
    }
    
    def getAt(int index) {
        if (index < 0) index = items.size() + index
        return items[index]
    }
    
    void putAt(int index, String value) {
        while (items.size() <= index) {
            items.add(null)
        }
        items[index] = value
    }
    
    int size() {
        return items.size()
    }
}

def store = new DataStore()
store.addItem("first")
store.addItem("second")
println store[0]
println store[-1]
store[5] = "sixth"
println store.size()
