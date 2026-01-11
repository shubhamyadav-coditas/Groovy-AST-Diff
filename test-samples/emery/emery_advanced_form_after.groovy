// Test Emery advanced form operations - MODIFIED
use("USER_PROFILE_FORM")
use("EMPLOYEE_FORM")

def formId = Emery.form.formId("USER_PROFILE_FORM")
def blueprintCode = Emery.form.getBlueprintCode("USER_PROFILE_FORM")
def workflowCode = Emery.form.workFlowCode("USER_PROFILE_FORM")

// Get existing form with different parameters
def existingForm = Emery.form.getForm("USER_PROFILE_FORM", 54321, 98765)
def latestInstance = Emery.form.getLatestFormInstance("USER_PROFILE_FORM", 54321)

USER_PROFILE_FORM formObj = Emery.form.newForm("USER_PROFILE_FORM")
formObj.firstName = "Jane"
formObj.lastName = "Smith"
formObj.email = "jane.smith@example.com"
formObj.department = "Engineering"
formObj.phoneNumber = "+1-555-0123"

// Form field operations
def emailField = F.getField("email")
def deptField = F.getField("department")
F.updateField("status", "PENDING")
F.updateField("lastModified", new Date())

def processInstanceId = formObj.nextProcessInstanceId()
def instanceId = formObj.nextInstanceId()

formObj.save()
formObj.validate()
formObj.submit()

// Additional form for employee
EMPLOYEE_FORM empForm = Emery.form.newForm("EMPLOYEE_FORM")
empForm.employeeId = "EMP001"
empForm.save()
