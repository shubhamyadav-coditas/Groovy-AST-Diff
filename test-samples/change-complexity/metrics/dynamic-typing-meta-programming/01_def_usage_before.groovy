// Test: def keyword usage - before
// No def keyword, uses explicit types

class DataProcessor {
    
    String processItem(String item) {
        String result = item.toUpperCase()
        return result
    }
    
    int calculate(int a, int b) {
        int sum = a + b
        return sum
    }
}
