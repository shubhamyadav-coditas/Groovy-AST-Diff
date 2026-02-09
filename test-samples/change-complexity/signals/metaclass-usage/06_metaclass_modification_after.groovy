class Logger {
    void log(String message) {
        println "[LOG] ${message}"
    }
}

Logger.metaClass.debug = { String message ->
    println "[DEBUG ${timestamp}] ${message}"
}

Logger.metaClass.error = { String message ->
    System.err.println "[ERROR] ${message}"
}

Logger.metaClass.warn = { String message ->
    println "[WARN] ${message}"
}

def logger = new Logger()
logger.log("Application started")
logger.debug("Debug mode enabled")
logger.error("Something went wrong")
logger.warn("Low memory warning")
