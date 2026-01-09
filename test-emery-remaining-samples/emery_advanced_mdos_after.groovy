// Test Emery advanced MDOS operations - MODIFIED
import com.metricstream.systemi.services.mdos.enums.HierarchyAccess

// Get users based on activity and organization with additional tuples
def activityUsers = Emery.mdos.getUsersBasedOnActivityAndOrganization(
    ["ORG_TUPLE_1", "ORG_TUPLE_2", "ORG_TUPLE_3"], 
    ["APPROVAL_ACTIVITY", "REVIEW_ACTIVITY", "AUDIT_ACTIVITY"], 
    HierarchyAccess.FLOW_UP
)

// Get users based on role with different access levels
def adminUsers = Emery.mdos.getUsersBasedOnRole(
    ["ROLE_TUPLE_1", "ROLE_TUPLE_4"], 
    ["ADMIN", "SUPER_ADMIN", "SYSTEM_ADMIN"], 
    HierarchyAccess.EXACT
)

def managerUsers = Emery.mdos.getUsersBasedOnRole(
    ["ROLE_TUPLE_2"], 
    ["MANAGER", "SENIOR_MANAGER"], 
    HierarchyAccess.FLOW_DOWN
)

// Additional role query
def auditorUsers = Emery.mdos.getUsersBasedOnRole(
    ["ROLE_TUPLE_3"], 
    ["AUDITOR"], 
    HierarchyAccess.FLAT
)

// Get MDOS display values with different delimiters
def displayValues = Emery.mdos.getMdosDisplayValuesClob(
    "VALUE1,VALUE2,VALUE3,VALUE4", 
    ",", 
    " -> ", 
    1
)

def displayValuesLocale2 = Emery.mdos.getMdosDisplayValuesClob(
    "LOCALE_VALUE1,LOCALE_VALUE2", 
    ",", 
    " | ", 
    2
)
