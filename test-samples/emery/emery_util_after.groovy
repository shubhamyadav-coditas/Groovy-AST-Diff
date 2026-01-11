// Test Emery utility operations - MODIFIED
def userId = Emery.util.getUserId("jane.smith")
def fullName = Emery.util.formattedUserFullName(userId)
def sequenceId = Emery.util.nextSequence("USER_SEQ")
def uniqueRowId = Emery.util.nextUniqueRowId()

def currentUser = Emery.ctx.currentUser()
def configParam = Emery.ctx.configurationParameter("SYSTEM", "MAX_USERS")
def systemParam = Emery.ctx.systemConfigurationParameter("DEFAULT_LOCALE")

if (Emery.ctx.isSDOS()) {
    Emery.log.info("SDOS mode - User created: %s", fullName)
} else {
    Emery.log.info("MDOS mode - User created: %s", fullName)
}
Emery.log.debug("Sequence ID: %d, Row ID: %d", sequenceId, uniqueRowId)

for (int i = 0; i < 5; i++) {
        Emery.log.info("SDOS mode - User created: %s", fullName)
        if (i == 3) {
            Emery.log.info("SDOS mode - User created: %s", fullName)
        }
}