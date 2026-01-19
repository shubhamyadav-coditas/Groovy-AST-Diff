// Test Emery advanced MDOS operations
import com.metricstream.systemi.services.mdos.enums.HierarchyAccess

// Get users based on activity and organization
def activityUsers = Emery.mdos.getUsersBasedOnActivityAndOrganization(
    ["ORG_TUPLE_1", "ORG_TUPLE_2"], 
    ["APPROVAL_ACTIVITY", "REVIEW_ACTIVITY"], 
    HierarchyAccess.FLOW_UP
)

// Get users based on role with different access levels
def adminUsers = Emery.mdos.getUsersBasedOnRole(
    ["ROLE_TUPLE_1"], 
    ["ADMIN", "SUPER_ADMIN"], 
    HierarchyAccess.EXACT
)

def managerUsers = Emery.mdos.getUsersBasedOnRole(
    ["ROLE_TUPLE_2"], 
    ["MANAGER"], 
    HierarchyAccess.FLOW_DOWN
)

// Get MDOS display values
def displayValues = Emery.mdos.getMdosDisplayValuesClob(
    "VALUE1,VALUE2,VALUE3", 
    ",", 
    " | ", 
    1
)
