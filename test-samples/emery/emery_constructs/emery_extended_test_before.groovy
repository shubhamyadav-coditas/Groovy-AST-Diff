// Test Emery extended testing operations
def result = processUserData("testuser")

// Basic assertions
Emery.test.assertNotNull(result)
Emery.test.assertEquals("SUCCESS", result.status)
Emery.test.assertTrue(result.isValid)

// Additional assertions
def nullValue = null
Emery.test.assertNull(nullValue)

def falseCondition = (5 > 10)
Emery.test.assertFalse(falseCondition)

// Test delays
Emery.test.delay(500)

def userCount = getUserCount()
Emery.test.assertTrue(userCount > 0)

// Validate error conditions
try {
    def invalidResult = processInvalidData("invalid")
    Emery.test.assertNull(invalidResult)
} catch (Exception e) {
    Emery.test.assertNotNull(e.message)
}
