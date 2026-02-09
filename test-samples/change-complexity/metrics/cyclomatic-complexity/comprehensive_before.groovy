// Test: Comprehensive - before
// Expected CC: 3 (base + 1 if + 1 for-in loop)

class DataProcessor {
    List<String> process(List<Map> items) {
        def results = []
        
        for (item in items) {
            if (item.active) {
                results.add(item.name)
            }
        }
        
        return results
    }
}
