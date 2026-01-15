void error(String message) {
    // MOVED_MODIFIED: Moved to top AND modified implementation
    System.err.println "[ERROR] ${new Date()}: ${message}"
}

void info(String message) {
    println "[INFO] ${message}"
}

void warn(String message) {
    // MOVED_MODIFIED: Same position but modified implementation  
    System.out.println "[WARNING] ${message.toUpperCase()}"
}
