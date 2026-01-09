// Test Emery extended utility operations
def userId = Emery.util.getUserId("john.doe")
def formattedName = Emery.util.formattedUserFullName(userId)
def uniqueRowId = Emery.util.nextUniqueRowId()

// Additional context operations
def systemConfig = Emery.ctx.systemConfigurationParameter("MAX_FILE_SIZE")
def isSDOS = Emery.ctx.isSDOS()
def isMDOS = Emery.ctx.isMDOS()

// Additional string utilities
def encodedValue = Emery.stringUtil.displayEncode("Test, \"Value\"")
def decodedValue = Emery.stringUtil.displayDecode(encodedValue)
def splitValues = Emery.stringUtil.split("A,B,C", ",")
def joinedArray = Emery.stringUtil.join(["X", "Y", "Z"] as String[], "|")

// Additional date utilities
def currentDate = new Date()
def futureMonth = Emery.dateUtil.addMonths(currentDate, 3)
def futureYear = Emery.dateUtil.addYears(currentDate, 1)

// Additional LOV operations
def storedValue = Emery.lov.getLovStoredValue("STATUS_LOV", 1, "Active")

// Additional logging levels
Emery.log.warn("Warning message: %s", "Low disk space")
Emery.log.error("Error occurred: %s", "Database connection failed")
Emery.log.trace("Trace message: %s", "Method entry")
