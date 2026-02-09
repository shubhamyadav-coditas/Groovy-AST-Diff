class DataStore {
    private List<String> items = []
    
    void addItem(String item) {
        items.add(item)
    }
    
    String getItem(int index) {
        return items.get(index)
    }
    
    int size() {
        return items.size()
    }
}

def store = new DataStore()
store.addItem("first")
store.addItem("second")
println store.getItem(0)
