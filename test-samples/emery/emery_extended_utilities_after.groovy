// Test Emery extended utility operations - MODIFIED
def userId = Emery.util.getUserId("jane.smith")
def formattedName = Emery.util.formattedUserFullName(userId)
def uniqueRowId = Emery.util.nextUniqueRowId()
def secondRowId = Emery.util.nextUniqueRowId()

// Additional context operations
def systemConfig = Emery.ctx.systemConfigurationParameter("MAX_FILE_SIZE_MB")
def uploadConfig = Emery.ctx.systemConfigurationParameter("UPLOAD_TIMEOUT")
def isSDOS = Emery.ctx.isSDOS()
def isMDOS = Emery.ctx.isMDOS()

// Additional string utilities
def encodedValue = Emery.stringUtil.displayEncode("Test, \"Modified Value\"")
def decodedValue = Emery.stringUtil.displayDecode(encodedValue)
def splitValues = Emery.stringUtil.split("A,B,C,D", ",")
def joinedArray = Emery.stringUtil.join(["X", "Y", "Z", "W"], "|")

// Additional date utilities
def currentDate = new Date()
def futureMonth = Emery.dateUtil.addMonths(currentDate, 6)
def futureYear = Emery.dateUtil.addYears(currentDate, 2)
def pastDate = Emery.dateUtil.addDays(currentDate, -30)

// Additional LOV operations
def storedValue = Emery.lov.getLovStoredValue("STATUS_LOV", 1, "Active")
def inactiveStoredValue = Emery.lov.getLovStoredValue("STATUS_LOV", 1, "Inactive")

// Additional logging levels
Emery.log.warn("Warning message: %s", "Low memory available")
Emery.log.error("Error occurred: %s", "Service unavailable")
Emery.log.trace("Trace message: %s", "Method exit")
Emery.log.info("Info message: %s", "Process completed successfully")
