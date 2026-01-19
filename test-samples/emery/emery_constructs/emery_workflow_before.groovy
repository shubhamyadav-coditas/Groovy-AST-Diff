// Test Emery workflow and context operations
use("APPROVAL_FORM")

def processCode = PROCESS_CODE
def currentStage = CURRENT_STAGE
def targetStage = TARGET_STAGE

APPROVAL_FORM formObj = Emery.form.newForm("APPROVAL_FORM")
formObj.requestId = CONTEXT.get("REQUEST_ID")
formObj.approver = CURRENT_USER

if (currentStage == "PENDING") {
    formObj.status = "IN_REVIEW"
    formObj.submit()
}

Emery.masterTable.updateInstanceId("APPROVAL_FORM", formObj.process_instance_id, 
                                  "REQUEST_ID", formObj.requestId)
