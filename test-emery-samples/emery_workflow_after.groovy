// Test Emery workflow and context operations - MODIFIED
use("APPROVAL_FORM")

def processCode = PROCESS_CODE
def currentStage = CURRENT_STAGE
def targetStage = TARGET_STAGE
def transitionCode = TRANSITION_CODE

APPROVAL_FORM formObj = Emery.form.newForm("APPROVAL_FORM")
formObj.requestId = CONTEXT.get("REQUEST_ID")
formObj.approver = CURRENT_USER
formObj.department = CONTEXT.get("DEPARTMENT")
formObj.priority = "HIGH"

if (currentStage == "PENDING") {
    formObj.status = "APPROVED"
    formObj.approvalDate = new Date()
    formObj.comments = "Auto-approved based on criteria"
    formObj.submit()
} else if (currentStage == "IN_REVIEW") {
    formObj.status = "REJECTED"
    formObj.rejectionReason = "Insufficient documentation"
} else {
    formObj.status = "PENDING"
}

Emery.masterTable.updateInstanceId("APPROVAL_FORM", formObj.process_instance_id, 
                                  "REQUEST_ID", formObj.requestId)

def latestInstance = Emery.form.getLatestFormInstance("APPROVAL_FORM", formObj.process_instance_id)
