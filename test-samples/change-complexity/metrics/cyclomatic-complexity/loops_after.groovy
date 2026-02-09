// Test: Loops - after (multiple loop types)
// Expected CC: 5 (base + 4 loops)

class Processor {
    List<Integer> process(List<Integer> items) {
        def results = []
        
        // For loop
        for (int i = 0; i < items.size(); i++) {
            results.add(items[i] * 2)
        }
        
        // For-in loop
        for (item in items) {
            println item
        }
        
        // While loop
        int j = 0
        while (j < items.size()) {
            println items[j]
            j++
        }
        
        // Do-while loop
        int k = 0
        do {
            println "Iteration ${k}"
            k++
        } while (k < 3)
        
        return results
    }
}
