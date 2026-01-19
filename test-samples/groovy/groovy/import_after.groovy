// Test file for IMPORT/PACKAGE constructs - AFTER

package com.example.enhanced  // MODIFIED: package name changed

import java.util.List
import java.util.Set  // MODIFIED: Map -> Set
import java.time.LocalDate  // ADDED: new import

class DataHandler {
    List<String> data = []
    Set<String> uniqueData = []  // ADDED: new field using Set
}
