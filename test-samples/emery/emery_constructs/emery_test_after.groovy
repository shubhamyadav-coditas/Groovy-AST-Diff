// Test Emery testing operations - MODIFIED
def result = processUserData("newuser")
def validationResult = validateUserData(result)

Emery.test.assertNotNull(result)
Emery.test.assertNotNull(validationResult)
Emery.test.assertEquals("COMPLETED", result.status)
Emery.test.assertTrue(result.isValid)
Emery.test.assertFalse(result.hasErrors)

def userCount = getUserCount()
def adminCount = getAdminCount()
Emery.test.assertTrue(userCount > 5)
Emery.test.assertTrue(adminCount >= 1)

if (result.requiresApproval) {
    Emery.test.assertNull(result.approvalDate)
} else {
    Emery.test.assertNotNull(result.completionDate)
}

Emery.test.delay(2000)
