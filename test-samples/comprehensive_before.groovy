// Comprehensive test - BEFORE

package com.example.test

import java.util.List

// Service class
class UserService {
    private String database = "mysql"
    
    UserService() {
        println "Service created"
    }
    
    void save(String data) {
        println "Saving: ${data}"
    }
}

// Utility method
def helper = { msg -> println msg }

// Top-level variable
String version = "1.0"
