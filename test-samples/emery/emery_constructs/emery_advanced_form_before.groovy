// Test Emery advanced form operations
use("USER_PROFILE_FORM")

def formId = Emery.form.formId("USER_PROFILE_FORM")
def blueprintCode = Emery.form.getBlueprintCode("USER_PROFILE_FORM")

// Get existing form
def existingForm = Emery.form.getForm("USER_PROFILE_FORM", 12345, 67890)
def latestInstance = Emery.form.getLatestFormInstance("USER_PROFILE_FORM", 12345)

USER_PROFILE_FORM formObj = Emery.form.newForm("USER_PROFILE_FORM")
formObj.firstName = "John"
formObj.lastName = "Doe"
formObj.email = "john.doe@example.com"

// Form field operations
def emailField = F.getField("email")
F.updateField("status", "ACTIVE")

def processInstanceId = formObj.nextProcessInstanceId()
def instanceId = formObj.nextInstanceId()

formObj.save()
formObj.validate()
