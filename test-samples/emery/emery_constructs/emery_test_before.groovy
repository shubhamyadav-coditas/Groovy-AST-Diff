// Test Emery testing operations
def result = processUserData("testuser")

Emery.test.assertNotNull(result)
Emery.test.assertEquals("SUCCESS", result.status)
Emery.test.assertTrue(result.isValid)

def userCount = getUserCount()
Emery.test.assertTrue(userCount > 0)

Emery.test.delay(1000)
