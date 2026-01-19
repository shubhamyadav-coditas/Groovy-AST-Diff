// Test Emery integration operations - MODIFIED
import com.metricstream.appstudio.constants.LocaleType

def lovDetails = Emery.lov.getLovDetailsForLovName("STATUS_LOV", 1)
def displayValue = Emery.lov.getLovDisplayValue("STATUS_LOV", 1, "ACTIVE")
def storedValue = Emery.lov.getLovStoredValue("STATUS_LOV", 1, "Active Status")

def encodedString = Emery.stringUtil.displayEncode("Test, Value with 'quotes'")
def decodedString = Emery.stringUtil.displayDecode(encodedString)
def splitValues = Emery.stringUtil.split("A,B,C", ",")
def joinedValues = Emery.stringUtil.join(splitValues, "|")

def locales = Emery.util.fetchLocales(LocaleType.ALL)
def dataTableData = Emery.dataTable.read("CONFIGURATION_TABLE")

Emery.email.sendEmail("NOTIFICATION_TEMPLATE", "ADMIN_MODULE", "admin@example.com", ["user1@example.com", "user2@example.com"], ["cc@example.com"], "Important Notification")
