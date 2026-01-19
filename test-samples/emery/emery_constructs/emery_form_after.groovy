// Test Emery form operations - MODIFIED
use("USER_PROFILE_FORM")

def formId = Emery.form.formId("USER_PROFILE_FORM")
def workflowCode = Emery.form.workFlowCode("USER_PROFILE_FORM")
def blueprintCode = Emery.form.getBlueprintCode("USER_PROFILE_FORM")

USER_PROFILE_FORM formObj = Emery.form.newForm("USER_PROFILE_FORM")
formObj.firstName = "Jane"
formObj.lastName = "Smith"
formObj.middleName = "Marie"

F.email = "jane.smith@example.com"
F.status = "PENDING"
F.department = "Engineering"
