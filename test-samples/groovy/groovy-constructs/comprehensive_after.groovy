package com.example.enhanced

import java.util.List
import java.util.Set  // ADDED: new import

String version = "2.0"

def helper = { msg, level -> 
    println "[${level}] ${msg}"
}

class UserService {
    private String database = "postgresql"
    boolean debug = true
    
    UserService() {
        println "Service created"
    }
    
    void save(String data, boolean validate) {
        if (validate) println "Validating..."
        println "Saving: ${data}"
    }
    
    void delete(String id) {
        println "Deleting: ${id}"
    }
}

interface Validator {
    boolean isValid(String input)
}
