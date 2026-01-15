// Test Emery form operations
use("USER_PROFILE_FORM")

def formId = Emery.form.formId("USER_PROFILE_FORM")
def workflowCode = Emery.form.workFlowCode("USER_PROFILE_FORM")

USER_PROFILE_FORM formObj = Emery.form.newForm("USER_PROFILE_FORM")
formObj.firstName = "John"
formObj.lastName = "Doe"

F.email = "john.doe@example.com"
F.status = "ACTIVE"
