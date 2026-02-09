class Logger {
    void log(String message) {
        println "[LOG] ${message}"
    }
}

Logger.metaClass.debug = { String message ->
    println "[DEBUG] ${message}"
}

def logger = new Logger()
logger.log("Application started")
logger.debug("Debug mode enabled")
