// Test Emery utility operations
def userId = Emery.util.getUserId("john.doe")
def fullName = Emery.util.userFullName(userId)
def sequenceId = Emery.util.nextSequence("USER_SEQ")

def currentUser = Emery.ctx.currentUser()
def configParam = Emery.ctx.configurationParameter("SYSTEM", "MAX_USERS")

Emery.log.info("User created: %s", fullName)
Emery.log.debug("Sequence ID: %d", sequenceId)

for (int i = 0; i < 10; i++) {
        Emery.log.info("SDOS mode - User created: %s", fullName)
}
