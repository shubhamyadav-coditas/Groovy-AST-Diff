package com.example.test

import java.util.List

class UserService {
    private String database = "mysql"
    
    UserService() {
        println "Service created"
    }
    
    void save(String data) {
        println "Saving: ${data}"
    }
}

def helper = { msg -> println msg }

String version = "1.0"
