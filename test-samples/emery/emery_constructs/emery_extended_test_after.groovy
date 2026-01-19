// Test Emery extended testing operations - MODIFIED
def result = processUserData("testuser_modified")

// Basic assertions
Emery.test.assertNotNull(result)
Emery.test.assertEquals("COMPLETED", result.status)
Emery.test.assertTrue(result.isValid)
Emery.test.assertTrue(result.hasData)

// Additional assertions
def nullValue = null
def emptyValue = ""
Emery.test.assertNull(nullValue)
Emery.test.assertNotNull(emptyValue)

def falseCondition = (5 > 10)
def trueCondition = (10 > 5)
Emery.test.assertFalse(falseCondition)
Emery.test.assertTrue(trueCondition)

// Test delays with different durations
Emery.test.delay(1000)
Emery.test.delay(250)

def userCount = getUserCount()
def adminCount = getAdminCount()
Emery.test.assertTrue(userCount > 0)
Emery.test.assertTrue(adminCount >= 1)

// Validate error conditions
try {
    def invalidResult = processInvalidData("invalid")
    Emery.test.assertNull(invalidResult)
} catch (Exception e) {
    Emery.test.assertNotNull(e.message)
    Emery.test.assertTrue(e.message.contains("invalid"))
}

// Additional test cases
def dataList = getDataList()
Emery.test.assertNotNull(dataList)
Emery.test.assertFalse(dataList.isEmpty())
