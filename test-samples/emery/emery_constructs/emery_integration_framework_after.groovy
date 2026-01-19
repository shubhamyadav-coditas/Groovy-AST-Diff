// Test Emery integration framework operations - MODIFIED
import com.metricstream.appstudio.dsl.engine.domain.InputType

// CIF operations
def cifRequest = [
    endpoint: "USER_SERVICE_V2",
    method: "POST",
    parameters: [userId: "12345", includeDetails: true]
]
def cifResponse = Emery.integration.cif.execute(cifRequest)

// CIS operations  
def cisPayload = [
    service: "NOTIFICATION_SERVICE",
    action: "SEND_SMS",
    data: [recipient: "user@example.com", subject: "Test Alert", priority: "HIGH"]
]
def cisResult = Emery.integration.cis.invoke(cisPayload)

// Hook chain processing
def hookContext = [
    formName: "USER_FORM",
    stage: "POST_SAVE",
    data: [userId: 123, status: "PENDING", timestamp: new Date()]
]
Emery.integration.hookChain.execute("USER_HOOKS", hookContext)

// Additional CIF call
def cifBatchRequest = [
    endpoint: "BATCH_SERVICE",
    method: "POST",
    parameters: [batchId: "BATCH_001"]
]
def batchResponse = Emery.integration.cif.execute(cifBatchRequest)
