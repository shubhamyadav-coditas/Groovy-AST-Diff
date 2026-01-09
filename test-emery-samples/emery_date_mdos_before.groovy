// Test Emery date and MDOS operations
import com.metricstream.systemi.services.mdos.enums.HierarchyAccess

def currentDate = new Date()
def futureDate = Emery.dateUtil.addDays(currentDate, 30)
def formattedDate = Emery.dateUtil.convertDateToString(futureDate, "yyyy-MM-dd")

def isSame = Emery.dateUtil.isSameDate(currentDate, futureDate)

def users = Emery.mdos.getUsersBasedOnRole(["TUPLE_1"], ["ADMIN"], HierarchyAccess.FLAT)
def displayValues = Emery.mdos.getMdosDisplayValuesClob("VALUE1,VALUE2", ",", "|", 1)
