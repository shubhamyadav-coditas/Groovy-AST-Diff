// Test Emery integration operations
import com.metricstream.appstudio.constants.LocaleType

def lovDetails = Emery.lov.getLovDetailsForLovName("STATUS_LOV", 1)
def displayValue = Emery.lov.getLovDisplayValue("STATUS_LOV", 1, "ACTIVE")

def encodedString = Emery.stringUtil.displayEncode("Test, Value")
def joinedValues = Emery.stringUtil.joinForDisplay(["A", "B", "C"])

def locales = Emery.util.fetchLocales(LocaleType.ENABLED)

Emery.email.sendEmail("WELCOME_TEMPLATE", "USER_MODULE", "system@example.com", ["user@example.com"], [:], "Welcome Message")
