// Test Emery date and MDOS operations - MODIFIED
import com.metricstream.systemi.services.mdos.enums.HierarchyAccess

def currentDate = new Date()
def futureDate = Emery.dateUtil.addMonths(currentDate, 6)
def pastDate = Emery.dateUtil.addYears(currentDate, -1)
def formattedDate = Emery.dateUtil.convertDateToString(futureDate, "dd/MM/yyyy HH:mm:ss")

def isSameFuture = Emery.dateUtil.isSameDate(currentDate, futureDate)
def isSamePast = Emery.dateUtil.isSameDate(currentDate, pastDate)

def adminUsers = Emery.mdos.getUsersBasedOnRole(["TUPLE_1", "TUPLE_2"], 
                                               ["ADMIN", "MANAGER"], 
                                               HierarchyAccess.FLOW_UP)
def activityUsers = Emery.mdos.getUsersBasedOnActivityAndOrganization(["ORG_1"], 
                                                                      ["APPROVAL"], 
                                                                      HierarchyAccess.FLOW_DOWN)
def displayValues = Emery.mdos.getMdosDisplayValuesClob("VALUE1,VALUE2,VALUE3", ",", " | ", 1)
