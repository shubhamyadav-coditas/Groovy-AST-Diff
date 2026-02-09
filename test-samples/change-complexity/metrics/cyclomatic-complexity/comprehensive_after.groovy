// Test: Comprehensive - after (all complexity types)
// Expected CC: 19 (multiple decision points)

class DataProcessor {
    List<String> process(List<Map> items) {
        def results = []
        
        // For-in loop (+1)
        for (item in items) {
            // Try-catch with multiple catches (+2)
            try {
                // Nested if statements (+3)
                if (item != null) {
                    if (item.active && item.visible) {  // +1 if, +1 &&
                        if (item.type == "premium" || item.type == "standard") {  // +1 if, +1 ||
                            // Safe navigation (+2)
                            def name = item?.name?.toUpperCase()
                            
                            // Elvis operator (+1)
                            def label = name ?: "Unknown"
                            
                            // Ternary operator (+1)
                            def priority = item.priority > 5 ? "high" : "low"
                            
                            results.add("${label}: ${priority}")
                        }
                    }
                }
            } catch (NullPointerException e) {
                println "Null error: ${e.message}"
            } catch (Exception e) {
                println "Error: ${e.message}"
            }
        }
        
        // Switch statement with cases (+4)
        switch (results.size()) {
            case 0:
                println "No results"
                break
            case 1..10:
                println "Few results"
                break
            case 11..100:
                println "Many results"
                break
            default:
                println "Lots of results"
        }
        
        // While loop (+1)
        int i = 0
        while (i < results.size() && results[i] != null) {  // +1 &&
            i++
        }
        
        return results
    }
}
