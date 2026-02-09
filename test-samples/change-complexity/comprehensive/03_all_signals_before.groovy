// Comprehensive Test 03: All Signals - Before
// Baseline without any signals

import java.util.List

class ServiceHandler {
    
    List<String> data = []
    
    String processData(String input) {
        return input.trim()
    }
    
    void saveData(String value) {
        data.add(value)
    }
}
