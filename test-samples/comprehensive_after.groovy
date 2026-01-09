// Comprehensive test - AFTER

package com.example.enhanced  // MODIFIED: package changed

import java.util.List
import java.util.Set  // ADDED: new import

// MOVED: version moved to top
String version = "2.0"  // MODIFIED: version updated

// MOVED_MODIFIED: helper moved and changed
def helper = { msg, level -> 
    println "[${level}] ${msg}"  // MODIFIED: added level parameter
}

// MODIFIED: Service class enhanced
class UserService {
    private String database = "postgresql"  // MODIFIED: database changed
    boolean debug = true  // ADDED: new field
    
    // UNCHANGED: constructor stays same
    UserService() {
        println "Service created"
    }
    
    // MODIFIED: method signature changed
    void save(String data, boolean validate) {  // MODIFIED: added parameter
        if (validate) println "Validating..."  // MODIFIED: added validation
        println "Saving: ${data}"
    }
    
    // ADDED: new method
    void delete(String id) {
        println "Deleting: ${id}"
    }
}

// ADDED: new interface
interface Validator {
    boolean isValid(String input)
}
